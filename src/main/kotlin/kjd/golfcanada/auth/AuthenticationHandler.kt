package kjd.golfcanada.auth

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import kjd.golfcanada.auth.ext.assertHttpMethod
import kjd.golfcanada.auth.ext.assertQueryParameter
import kjd.golfcanada.auth.ext.assertValue
import kjd.golfcanada.auth.impl.TokenRepositoryMapImpl
import kjd.golfcanada.client.api.AuthApi
import kjd.golfcanada.client.model.AuthToken
import kjd.golfcanada.client.model.code
import kjd.golfcanada.client.model.toJson
import kjd.golfcanada.client.model.withConcatenatedToken
import org.openapitools.client.infrastructure.ClientException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URLDecoder
import java.util.*

/**
 * Lambda based OAuth wrapper for Golf Canada authentication.
 *
 * Golf Canada doesn't provide standard OAuth features, so we need to provide a wrapper around that logic to
 * scam our way into the services.  This is done by accepting all three of the endpoints through
 * the configured Lambda URL:
 * - /code
 * - /authToken
 * - /refresh
 *
 * which will return the appropriate content based on the Account Linking documentation; any other
 * attempt will throw an exception.
 *
 * Shadiness!!! At this point the code and authToken state are maintained locally.  The assumption
 * is that at this point there won't be much traffic and that the /code and /authToken requests
 * hit the same instance.   If this happens to get more hits, then we'll look into
 * implementing some cross function storage.
 *
 * See the links:
 * - https://developer.amazon.com/en-US/docs/alexa/account-linking/account-linking-concepts.html
 * - https://developer.amazon.com/en-US/docs/alexa/account-linking/configure-authorization-code-grant.html
 * - https://docs.aws.amazon.com/lambda/latest/dg/urls-invocation.html
 */
class AuthenticationHandler internal constructor(
    private val authApi: AuthApi,
    private val clientId: String,
    private val clientSecret: String,
    private val tokenRepository: TokenRepository<AuthTokenKey, AuthToken> = TokenRepositoryMapImpl()
): RequestHandler<APIGatewayV2HTTPEvent, APIGatewayProxyResponseEvent> {
    private val loginPage = getLoginPageTemplate()
    private val logger: Logger = LoggerFactory.getLogger(AuthenticationHandler::class.java)

    /**
     * Default constructor used during creation
     */
    @Suppress("unused")
    constructor() : this(
        AuthApi(),
        System.getenv("CLIENT_ID"),
        System.getenv("CLIENT_SECRET")
    ) {
        logger.info("Building AuthenticationHandler with client credentials")
    }

    /**
     * Main entry point provides routing for the expected requests and "global" error handling.
     *
     * @param event simulated API Gateway event from Lambda URL
     * @param context Lambda context
     * @returns API Gateway response matching Alexa Account Linking requirements
     */
    override fun handleRequest(event: APIGatewayV2HTTPEvent, context: Context): APIGatewayProxyResponseEvent {
        logger.debug("Attempting request with query params: {}", event.queryStringParameters)
        logger.debug("Attempting request with body: {}", event.body)

        return try {
            when(event.rawPath) {
                "/favicon.ico" -> handleFavIcon(event)
                "/login" -> handleLoginRequest(event)
                "/code" -> handleCodeRequest(event)
                "/authToken" -> handleAuthTokenRequest(event)
                else -> invalidAuthenticationRequest(ErrorCode.INVALID_API_CALL)
            }
        } catch (exception: AuthenticationException) {
            exception.response
        } catch (exception: Exception) {
            invalidAuthenticationRequest(exception)
        }
    }

    /**
     * Return the fav-icon.
     *
     * TODO: return the binary content of an icon, shouldn't be too bad.
     */
    private fun handleFavIcon(event: APIGatewayV2HTTPEvent): APIGatewayProxyResponseEvent {
        return APIGatewayProxyResponseEvent().apply {
            statusCode = 200
            body = ""
        }
    }

    /**
     * Handles the authorization/refresh request from Alexa app that starts the OAuth process.  The request is handled
     * by the /code end point and expects the following query parameters to be provided:
     * - client_id which will be a random UUID generated and provided to all Alexa skill handlers through CLIENT_ID
     * - redirect_uri will need to be passed through to the API Gateway request with a 3xx redirect status
     * - response_type which will always be code
     * - scope list of additional scopes required, these will be configured on Alexa app based on the Golf Canada API
     * - state is a random value that needs to be maintained when requested the code and token
     *
     * This request will make the OAuth request to Golf Canada endpoint and store the Token information in a map
     * (shady) so that it's available for the /authToken request.
     *
     * See:
     * - https://developer.amazon.com/en-US/docs/alexa/account-linking/configure-authorization-code-grant.html#authorization-url
     *
     * @param event the API Gateway event from Lambda URL
     * @return the API Gateway proxy response redirecting to the redirect_uri with the state and code parameters
     */
    private fun handleLoginRequest(event: APIGatewayV2HTTPEvent): APIGatewayProxyResponseEvent {
        event.assertHttpMethod("GET") {
            logger.error("Attempt GET /login with incorrect HTTP Method")
            invalidAuthenticationRequest(ErrorCode.INVALID_API_CALL)
        }
        event.assertQueryParameter("client_id", clientId) { invalidClientId ->
            logger.error("Attempt to validate client_id failed with value '${invalidClientId}'")
            invalidAuthenticationRequest(ErrorCode.INVALID_CLIENT_ID)
        }
        event.assertQueryParameter("response_type", "code") { invalidResponseType ->
            logger.error("Invalid response_type '$invalidResponseType' for GET /login request")
            invalidAuthenticationRequest(ErrorCode.INVALID_RESPONSE_TYPE)
        }
        event.assertQueryParameter("state") {
            logger.error("State was not provided for GET /login request")
            invalidAuthenticationRequest(ErrorCode.INVALID_STATE)
        }

        return APIGatewayProxyResponseEvent().apply {
            statusCode = 200
            headers = mapOf("Content-Type" to "text/html")
            body = buildLoginPage(event)
        }
    }

    // TODO: separate this out in to a PageBuilder or TemplateBuilder
    private fun buildLoginPage(event: APIGatewayV2HTTPEvent): String {
        val parameters = mapOf(
            "client_id" to event.queryStringParameters?.getOrDefault("client_id", ""),
            "redirect_uri" to event.queryStringParameters?.getOrDefault("redirect_uri", ""),
            "response_type" to event.queryStringParameters?.getOrDefault("response_type", ""),
            "scope" to event.queryStringParameters?.getOrDefault("scope", ""),
            "state" to event.queryStringParameters?.getOrDefault("state", ""),
            "label_username" to "Username",
            "label_password" to "Password",
            "label_login" to "Login",
            "label_cancel" to "Cancel",
        )

        var loginString = loginPage
        parameters.forEach { (parameter, value) ->
            loginString = loginString.replace("{{$parameter}}", value ?: "")
        }

        return loginString
    }

    private fun parseBodyParameters(event: APIGatewayV2HTTPEvent): Map<String, String> {
        val bodyContent = event.body?.let { String(Base64.getUrlDecoder().decode(it)) }
        if (bodyContent.isNullOrEmpty()) return emptyMap()
        return bodyContent.split("&")
            .map { parameters -> parameters.split("=") }
            .associate { keyValues -> keyValues[0] to URLDecoder.decode(keyValues[1], "UTF-8") }
    }

    /**
     * Handles the actual login/submission from the login page.   Will attempt to log in and redirect
     * to the provided url.
     *
     * @param event from the simulated Lambda API Gateway
     * @return the redirect request with code and state if valid or the login screen with error
     */
    private fun handleCodeRequest(event: APIGatewayV2HTTPEvent): APIGatewayProxyResponseEvent {
        return try {
            event.assertHttpMethod("POST") { invalidAuthenticationRequest(RuntimeException("Invalid Authentication method")) }

            val parameters = parseBodyParameters(event)
            parameters.assertValue("client_id", clientId) {
                invalidAuthenticationRequest(ErrorCode.INVALID_CLIENT_ID)
            }
            parameters.assertValue("response_type", "code") {
                invalidAuthenticationRequest(ErrorCode.INVALID_RESPONSE_TYPE)
            }

            val redirectUri = parameters.assertValue("redirect_uri") {
                invalidAuthenticationRequest(ErrorCode.INVALID_REDIRECT_URI)
            }
            val scopes = parameters.assertValue("scope") {
                invalidAuthenticationRequest(ErrorCode.INVALID_SCOPE)
            }
            val state = parameters.assertValue("state") {
                invalidAuthenticationRequest(ErrorCode.INVALID_STATE)
            }
            val username = parameters.assertValue("username") {
                invalidAuthenticationRequest(ErrorCode.INVALID_USERNAME)
            }.lowercase().trim()
            val password = parameters.assertValue("password") {
                invalidAuthenticationRequest(ErrorCode.INVALID_PASSWORD)
            }.trim()

            logger.info("Attempting login with provided credentials and scopes")
            val authToken = authApi.getAuthToken(
                AuthApi.GrantTypeGetAuthToken.PASSWORD,
                scopes,
                username,
                password
            )

            val code = authToken.code()
            tokenRepository.store(AuthTokenKey(state, code), authToken)

            APIGatewayProxyResponseEvent().apply {
                statusCode = 301
                body = "${redirectUri}?code=${code}&state=${state}"
                headers = mapOf(
                    "Set-Cookie" to "${COOKIE_STATE}=${state}; HttpOnly; Secure;"
                )
            }
        } catch(exception: AuthenticationException) {
            logger.error("Invalid authentication request", exception)
            exception.response
        } catch (exception: ClientException) {
            logger.error("error occurred while calling AuthApi#getAuthToken", exception)
            invalidAuthenticationRequest(ErrorCode.INVALID_API_CALL)
        } catch (exception: Exception) {
            logger.error("Error occurred attempting POST /code", exception)
            invalidAuthenticationRequest(ErrorCode.CLIENT_ERROR)
        }
    }

    /**
     * Authenticates request clientId and clientSecret based on where they are in the event:
     * - body parameters
     * - http basic header
     *
     * @param event from the simulated Lambda API Gateway
     * @param parameters map containing the body content expected for Account linking
     * @throws AuthenticationException when fails clientId or clientSecret check
     */
    private fun authenticateRequest(event: APIGatewayV2HTTPEvent, parameters: Map<String, String>) {
        event.headers?.get("Authorization")?.let {
            val userPassword = it.substring(6).split(":")
            if (String(Base64.getDecoder().decode(userPassword[0])) != clientId) {
                throw AuthenticationException(invalidAuthenticationRequest(ErrorCode.INVALID_CLIENT_ID))
            }
            if (String(Base64.getDecoder().decode(userPassword[1])) != clientSecret) {
                throw AuthenticationException(invalidAuthenticationRequest(ErrorCode.INVALID_SECRET))
            }
        } ?: parameters.let {
            it.assertValue("client_id", clientId) {
                invalidAuthenticationRequest(ErrorCode.INVALID_CLIENT_ID) }
            it.assertValue("client_secret", clientSecret) {
                invalidAuthenticationRequest(ErrorCode.INVALID_SECRET) }
        }
    }

    /**
     * Handles the POST /authToken that is used to exchange the code provided earlier with the actual OAuth and
     * refresh tokens.  The form encoded input will be:
     * - grant_type which is always authentication_code
     * - code which will be the code provided from handleCodeRequest
     * - client_id which will be provided to both applications with CLIENT_ID
     * - client_secrete which will be provided to both applications with CLIENT_SECRET
     * - refresh_token if the token is expired
     *
     * See:
     * - https://developer.amazon.com/en-US/docs/alexa/account-linking/configure-authorization-code-grant.html#tokens
     *
     * @param event the API Gateway event from Lambda URL
     * @return a successful 200 with the OAuth token
     */
    private fun handleAuthTokenRequest(event: APIGatewayV2HTTPEvent): APIGatewayProxyResponseEvent {
        event.assertHttpMethod("POST") {
            invalidAuthenticationRequest(RuntimeException("173: Invalid Authentication method")) }

        val parameters = parseBodyParameters(event)
        authenticateRequest(event, parameters)

        val grantType = parameters.assertValue("grant_type") {
            invalidAuthenticationRequest(ErrorCode.INVALID_GRANT_TYPE) }

        return when (grantType) {
            "refresh_token" -> handleRefreshRequest(event, parameters)
            "authorization_code" -> handleAuthCodeRequest(event, parameters)
            else -> {
                logger.error("Request has grant_type '{}' which is not valid", grantType)
                invalidAuthenticationRequest(ErrorCode.INVALID_GRANT_TYPE)
            }
        }
    }

    /**
     * Handles the refresh of the token.  This just makes a new getAuthToken request and returns it.
     *
     * @param event provided from Lambda URL
     * @param parameters map containing pre-parsed query parameters
     * @return Api gateway response containing auth token
     *
     * @throws AuthenticationException when refresh_token does not exist
     * @throws AuthenticationException if there is a failure refreshing token
     */
    private fun handleRefreshRequest(
        event: APIGatewayV2HTTPEvent,
        parameters: Map<String, String>
    ): APIGatewayProxyResponseEvent {
        val refreshToken = parameters.assertValue("refresh_token") {
            invalidAuthenticationRequest(ErrorCode.INVALID_REFRESH_TOKEN) }

        return try {
            val authToken = authApi.getAuthToken(
                AuthApi.GrantTypeGetAuthToken.REFRESH_TOKEN,
                DEFAULT_SCOPES,
                refreshToken = refreshToken
            )
            authTokenResponse(authToken)
        } catch (exception: Exception) {
            invalidAuthenticationRequest(ErrorCode.INVALID_API_CALL)
        }
    }

    /**
     * Attempt to grab the AuthToken from the TOKEN_STORAGE or returns a 400.  This will also remove the
     * AuthToken, so that we aren't keeping anything around for long periods of time.   Should probably
     * look into implementing a timer, which will remove old Tokens at regular intervals.
     *
     * Expects the code and state to be provided within query parameters.  Also expects a cookie containing the state
     * that should be matched, otherwise an invalid Authentication is thrown.
     *
     * @param event provided from Lambda URL
     * @param parameters map containing pre-parsed query parameters
     * @return Api gateway response
     *
     * @throws AuthenticationException when state, code are invalid
     * @throws AuthenticationException when state does not match session state
     * @throws AuthenticationException if there is no valid code found for the request
     */
    private fun handleAuthCodeRequest(
        event: APIGatewayV2HTTPEvent,
        parameters: Map<String, String>
    ): APIGatewayProxyResponseEvent {
        val state = parameters.assertValue("state") {
            invalidAuthenticationRequest(ErrorCode.INVALID_STATE) }
        val code = parameters.assertValue("code") {
            invalidAuthenticationRequest(ErrorCode.INVALID_CODE) }
        val sessionState = event.cookies
            ?.firstOrNull { cookie -> cookie.startsWith(COOKIE_STATE) }
            ?.split("=")
            ?.last()

        if (state != sessionState) {
            throw AuthenticationException(invalidAuthenticationRequest(ErrorCode.INVALID_STATE))
        }

        return tokenRepository.get(AuthTokenKey(state, code))?.let { authToken ->
            authTokenResponse(authToken)
        } ?: invalidAuthenticationRequest(ErrorCode.CODE_NOT_FOUND)
    }

    /**
     * Build the login page template.
     *
     * TODO: needs to be updated to manage languages from the event.
     *
     * @return the HTML content for the login page.
     */
    private fun getLoginPageTemplate() =
        this::class.java.getResource("login.html")?.readText() ?: "<html/>"

    /**
     * Builds the response for a successful AuthToken
     *
     * @param authToken the AuthToken providing access and refresh tokens
     * @return successful API Gateway response
     */
    private fun authTokenResponse(authToken: AuthToken) =
        APIGatewayProxyResponseEvent().apply {
            statusCode = 200
            headers = mapOf("Content-Type" to "application/json;charset=UTF-8")
            body = authToken.withConcatenatedToken().toJson()
        }

    companion object {
        const val DEFAULT_SCOPES = "address email offline_access openid phone profile roles"
        const val COOKIE_STATE = "x-csrf-token"
    }
}