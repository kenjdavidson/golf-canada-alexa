package kjd.golfcanada.auth

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import com.fasterxml.jackson.databind.JsonNode
import kjd.golfcanada.auth.ext.assertHttpMethod
import kjd.golfcanada.auth.ext.assertQueryParameter
import kjd.golfcanada.auth.impl.TokenRepositoryMapImpl
import kjd.golfcanada.client.api.AuthApi
import kjd.golfcanada.client.model.AuthToken
import kjd.golfcanada.client.model.code
import kjd.golfcanada.client.model.toJson
import okhttp3.internal.http.HttpMethod

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

    /**
     * Default constructor used during creation
     */
    @SuppressWarnings("unused")
    constructor() : this(
        AuthApi(),
        System.getenv("CLIENT_ID"),
        System.getenv("CLIENT_SECRET"),
    )

    override fun handleRequest(event: APIGatewayV2HTTPEvent, context: Context): APIGatewayProxyResponseEvent {
        return try {
            when(event.rawPath) {
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
        event.assertHttpMethod("GET") { invalidAuthenticationRequest(ErrorCode.INVALID_API_CALL) }
        event.assertQueryParameter("client_id", clientId) { invalidAuthenticationRequest(ErrorCode.INVALID_CLIENT_ID) }

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

    /**
     * Handles the actual login/submission from the login page.  There are currently a couple requirements
     * to complete this request successfully:
     * - The client id must match
     * - The request must be a POST
     */
    private fun handleCodeRequest(event: APIGatewayV2HTTPEvent): APIGatewayProxyResponseEvent {
        event.assertHttpMethod("POST") { invalidAuthenticationRequest(RuntimeException("Invalid Authentication method")) }
        event.assertQueryParameter("client_id", clientId) { invalidAuthenticationRequest(ErrorCode.INVALID_CLIENT_ID) }
        event.assertQueryParameter("response_type", "code") { invalidAuthenticationRequest(ErrorCode.INVALID_RESPONSE_TYPE) }
        val redirectUri = event.assertQueryParameter("redirect_uri") { invalidAuthenticationRequest(ErrorCode.INVALID_REDIRECT_URI) }
        val scope = event.assertQueryParameter("scope") { invalidAuthenticationRequest(ErrorCode.INVALID_SCOPE) }
        val state = event.assertQueryParameter("state") { invalidAuthenticationRequest(ErrorCode.INVALID_STATE) }
        val username = event.assertQueryParameter("username") { invalidAuthenticationRequest(ErrorCode.INVALID_USERNAME) }
        val password = event.assertQueryParameter("password") { invalidAuthenticationRequest(ErrorCode.INVALID_PASSWORD) }

        return try {
            val authToken = authApi.getAuthToken(
                AuthApi.GrantTypeGetAuthToken.PASSWORD,
                scope,
                username,
                password
            )

            val code = authToken.code()
            tokenRepository.store(AuthTokenKey(state, code), authToken)

            APIGatewayProxyResponseEvent().apply {
                statusCode = 301
                body = "${redirectUri}?code=${code}&state=${state}"
            }
        } catch (exception: Exception) {
            invalidAuthenticationRequest(ErrorCode.CLIENT_ERROR)
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
        event.assertHttpMethod("POST") { invalidAuthenticationRequest(RuntimeException("173: Invalid Authentication method")) }
        event.assertQueryParameter("client_id", clientId) { invalidAuthenticationRequest(ErrorCode.INVALID_CLIENT_ID) }
        event.assertQueryParameter("client_secret", clientSecret) { invalidAuthenticationRequest(ErrorCode.INVALID_SECRET) }
        val grantType = event.assertQueryParameter("grant_type") { invalidAuthenticationRequest(ErrorCode.INVALID_GRANT_TYPE) }
        val state = event.assertQueryParameter("state") { invalidAuthenticationRequest(ErrorCode.INVALID_STATE) }
        val code = event.assertQueryParameter("code") { invalidAuthenticationRequest(ErrorCode.INVALID_CODE) }

        return if (grantType == "refresh_token") {
            val refreshToken = event.assertQueryParameter("refresh_token") { invalidAuthenticationRequest(ErrorCode.INVALID_GRANT_TYPE) }
            handleRefreshRequest(refreshToken)
        } else {
            handleAuthCodeRequest(state, code)
        }
    }

    /**
     * Handles the refresh of the token.  This just makes a new getAuthToken request and returns it.
     */
    private fun handleRefreshRequest(refreshToken: String): APIGatewayProxyResponseEvent {
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
     * @param state the session state
     * @param code the returned code to retrieve the access token
     */
    private fun handleAuthCodeRequest(state: String, code: String): APIGatewayProxyResponseEvent {
        return tokenRepository.get(AuthTokenKey(state, code))?.let { authToken ->
            authTokenResponse(authToken)
        } ?: invalidAuthenticationRequest(ErrorCode.CODE_NOT_FOUND)
    }

    /**
     * Build the login page template.
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
            body = authToken.toJson()
        }

    companion object {
        const val DEFAULT_SCOPES = "address email offline_access openid phone profile roles"
    }
}