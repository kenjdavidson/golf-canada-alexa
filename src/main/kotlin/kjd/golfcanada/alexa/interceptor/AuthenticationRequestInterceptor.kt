package kjd.golfcanada.alexa.interceptor

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.dispatcher.request.interceptor.RequestInterceptor
import com.amazon.ask.model.IntentRequest
import com.amazon.ask.model.LaunchRequest
import com.amazon.ask.model.SessionEndedRequest
import kjd.golfcanada.alexa.exception.AccountLinkingException
import org.slf4j.LoggerFactory

/**
 * Request interceptor that checks if authentication is required for the incoming request.
 *
 * This interceptor determines whether the user needs to have their Golf Canada account
 * linked before proceeding with certain requests. Built-in Amazon intents (like Help, Stop,
 * Cancel) and session management requests do not require authentication.
 *
 * When authentication is required but not available, this interceptor throws an
 * [AccountLinkingException] which should be handled by the AccountLinkingExceptionHandler.
 */
class AuthenticationRequestInterceptor : RequestInterceptor {

    private val logger = LoggerFactory.getLogger(AuthenticationRequestInterceptor::class.java)

    /**
     * Processes the incoming request to check if authentication is required.
     *
     * @param input The handler input containing the request envelope
     * @throws AccountLinkingException if authentication is required but not available
     */
    override fun process(input: HandlerInput) {
        if (requiresAuthentication(input) && !isAuthenticated(input)) {
            logger.info("Authentication required but not available for request")
            throw AccountLinkingException()
        }
    }

    /**
     * Determines if the request requires authentication.
     *
     * The following request types do NOT require authentication:
     * - LaunchRequest (opening the skill)
     * - SessionEndedRequest (closing the skill)
     * - Built-in Amazon intents: AMAZON.HelpIntent, AMAZON.StopIntent, AMAZON.CancelIntent
     *
     * All other requests (typically custom intents that interact with Golf Canada data)
     * require authentication.
     *
     * @param input The handler input containing the request
     * @return true if authentication is required, false otherwise
     */
    fun requiresAuthentication(input: HandlerInput): Boolean {
        val request = input.requestEnvelope.request

        return when (request) {
            is LaunchRequest -> false
            is SessionEndedRequest -> false
            is IntentRequest -> {
                val intentName = request.intent?.name
                !INTENTS_NOT_REQUIRING_AUTH.contains(intentName)
            }
            else -> true
        }
    }

    /**
     * Checks if the user is authenticated by verifying the presence of an access token.
     *
     * Checks both the Alexa account-linking access token and the static access token
     * stored in request attributes (populated by [StaticCredentialInterceptor] when
     * static credentials are configured).
     *
     * @param input The handler input containing the request envelope
     * @return true if the user has an access token, false otherwise
     */
    fun isAuthenticated(input: HandlerInput): Boolean {
        val accessToken = input.requestEnvelope.context?.system?.user?.accessToken
        if (!accessToken.isNullOrBlank()) return true

        val staticToken = input.attributesManager.requestAttributes
            ?.get(StaticCredentialInterceptor.STATIC_TOKEN_KEY) as? String
        return !staticToken.isNullOrBlank()
    }

    companion object {
        /**
         * List of built-in Amazon intents that do not require authentication.
         */
        private val INTENTS_NOT_REQUIRING_AUTH = setOf(
            "AMAZON.HelpIntent",
            "AMAZON.StopIntent",
            "AMAZON.CancelIntent",
            "AMAZON.FallbackIntent",
            "AMAZON.NavigateHomeIntent"
        )
    }
}
