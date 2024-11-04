package kjd.golfcanada.auth

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import kjd.golfcanada.client.api.AuthApi
import kjd.golfcanada.client.model.AuthToken
import java.util.concurrent.ConcurrentHashMap

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
    private val tokenRepository: Map<AuthTokenKey, AuthToken> = ConcurrentHashMap()
) {
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

    fun handleRequest(event: APIGatewayV2HTTPEvent): APIGatewayProxyResponseEvent {
        return when(event.rawPath) {
            "/login" -> handleLoginRequest(event)
            "/code" -> handleCodeRequest(event)
            "/authToken" -> handleAuthTokenRequest(event)
            else -> APIGatewayProxyResponseEvent().apply {
                statusCode = 400
                body = "Unknown authentication request"
            }
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
        event.queryStringParameters.getOrDefault("client_id", "").let {
            if (it !== clientId) {
                return invalidClientIdResponse(it)
            }
        }

        return APIGatewayProxyResponseEvent().apply {
            statusCode = 200
            body = buildLoginPage(event)
        }
    }

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
        event.queryStringParameters.getOrDefault("client_id", "").let {
            if (it !== clientId) {
                return invalidClientIdResponse(it)
            }
        }

        return APIGatewayProxyResponseEvent()
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
        event.queryStringParameters.getOrDefault("client_id", "").let {
            if (it !== clientId) {
                return invalidClientIdResponse(it)
            }
        }

        return if (event.queryStringParameters.containsKey("refresh_token")) {
            handleRefreshRequest(event)
        } else {
            handleAuthCodeRequest(event)
        }
    }

    /**
     * Handles the refresh of the token.  This just makes a new getAuthToken request and returns it.
     */
    private fun handleRefreshRequest(event: APIGatewayV2HTTPEvent): APIGatewayProxyResponseEvent {
        return APIGatewayProxyResponseEvent()
    }

    /**
     * Attempt to grab the AuthToken from the TOKEN_STORAGE or returns a 400.  This will also remove the
     * AuthToken, so that we aren't keeping anything around for long periods of time.   Should probably
     * look into implementing a timer, which will remove old Tokens at regular intervals.
     */
    private fun handleAuthCodeRequest(event: APIGatewayV2HTTPEvent): APIGatewayProxyResponseEvent {
        val code = event.queryStringParameters["code"]
        val state = event.queryStringParameters["state"]
        if (code.isNullOrEmpty() or state.isNullOrEmpty())
            return codeOrStateNotProvided(code, state)

        val authTokenKey = AuthTokenKey(code!!, state!!)
        tokenRepository[authTokenKey]

        return APIGatewayProxyResponseEvent().apply {
            statusCode = 400
            body = "Unable to process request"
        }
    }

    private fun getLoginPageTemplate() =
        this::class.java.getResource("login.html")?.readText() ?: "<html/>"

    companion object {
        const val DEFAULT_SCOPES = "address email offline_access openid phone profile roles"
    }
}