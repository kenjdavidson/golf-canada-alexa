package kjd.golfcanada.auth

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import kjd.golfcanada.client.api.AuthApi
import kjd.golfcanada.client.model.AuthToken
import java.util.*
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
    val authApi: AuthApi
) {
    constructor() : this(AuthApi())

    fun handleRequest(event: APIGatewayV2HTTPEvent): APIGatewayProxyResponseEvent {
        event.queryStringParameters.getOrDefault("client_id", "").let {
            if (it !== CLIENT_ID) {
                return invalidClientIdResponse(it)
            }
        }

        return when(event.rawPath) {
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
    private fun handleCodeRequest(event: APIGatewayV2HTTPEvent): APIGatewayProxyResponseEvent {
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
        return if (event.queryStringParameters.containsKey("refresh_token")) {
            handleRefreshRequest(event)
        } else {
            handleAuthCodeRequest(event)
        }
    }

    /**
     * Calls through to the
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
        val clientId = event.queryStringParameters["client_id"] ?: "NOT_PROVIDED"
        if (clientId != CLIENT_ID)
            return invalidClientIdResponse(clientId)

        val code = event.queryStringParameters["code"]
        val state = event.queryStringParameters["state"]
        if (code.isNullOrEmpty() or state.isNullOrEmpty())
            return codeOrStateNotProvided(code, state)

        TOKEN_STORAGE[AuthTokenKey(code!!, state!!)]

        return APIGatewayProxyResponseEvent().apply {
            statusCode = 400
            body = "Unable to process request"
        }
    }

    companion object Store {
        val TOKEN_STORAGE: Map<AuthTokenKey, AuthToken> = ConcurrentHashMap()

        val CLIENT_ID: String = System.getenv("CLIENT_ID") ?: UUID.randomUUID().toString()
        val CLIENT_SECRET: String = System.getenv("CLIENT_SECRET") ?: UUID.randomUUID().toString()
        val CLIENT_SCOPES: String = System.getenv("CLIENT_SCOPES") ?: "address email offline_access openid phone profile roles"
    }
}