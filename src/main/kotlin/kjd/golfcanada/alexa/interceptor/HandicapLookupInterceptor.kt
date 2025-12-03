package kjd.golfcanada.alexa.interceptor

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.dispatcher.request.interceptor.RequestInterceptor
import kjd.golfcanada.alexa.model.HandicapSummaryData
import kjd.golfcanada.client.model.User
import kjd.golfcanada.client.model.extractAccessToken
import kjd.golfcanada.client.provider.ApiClientProvider
import org.slf4j.LoggerFactory

/**
 * Request interceptor that caches the current user's handicap information in Session Attributes
 * with a Time-To-Live (TTL) mechanism.
 * 
 * This interceptor runs before the HandicapIntentRequestHandler and ensures that:
 * 1. The current user's handicap is cached in Session Attributes with a 10-minute TTL
 * 2. If the cache is fresh, it's retrieved and placed in Request Attributes
 * 3. If the cache is missing or stale, a network call is made to fetch fresh data
 * 
 * The cached data is stored in Session Attributes for persistence across multiple turns
 * in the same session, while Request Attributes are used for the current request only.
 * 
 * @param apiClientProvider The API client provider for creating authenticated API clients
 */
class HandicapLookupInterceptor(
    private val apiClientProvider: ApiClientProvider
) : RequestInterceptor {

    private val logger = LoggerFactory.getLogger(HandicapLookupInterceptor::class.java)

    /**
     * Processes the incoming request to ensure the user's handicap is cached and available.
     * 
     * @param input The handler input containing the request envelope and attributes
     */
    override fun process(input: HandlerInput) {
        val accessToken = input.requestEnvelope.context?.system?.user?.accessToken

        // Skip if no access token (user not authenticated)
        if (accessToken.isNullOrBlank()) {
            logger.debug("No access token present, skipping handicap lookup")
            return
        }

        val sessionAttributes = input.attributesManager.sessionAttributes
        val requestAttributes = input.attributesManager.requestAttributes

        // Get user profile from session attributes (should be set by UserProfileInterceptor)
        val user = sessionAttributes[UserProfileInterceptor.USER_SESSION_KEY] as? User
        if (user?.id == null) {
            logger.debug("No user profile in session, skipping handicap lookup")
            return
        }

        // Check if we have cached handicap data
        val cachedHandicap = sessionAttributes[HANDICAP_SESSION_KEY] as? HandicapSummaryData

        if (cachedHandicap != null && isCacheFresh(cachedHandicap)) {
            logger.debug("Using cached handicap data for user ${user.id}")
            requestAttributes[HANDICAP_REQUEST_KEY] = cachedHandicap
            input.attributesManager.requestAttributes = requestAttributes
            return
        }

        // Cache is missing or stale, fetch fresh data
        logger.info("Fetching fresh handicap data for user ${user.id}")
        try {
            val actualAccessToken = accessToken.extractAccessToken()
            
            // Get authenticated API client from provider
            val clientWrapper = apiClientProvider.getClient(actualAccessToken)
            val scoresApi = clientWrapper.createScoresApi()
            
            val handicapCalculation = scoresApi.getHandicapCalculation(user.id)
            val handicapSummary = HandicapSummaryData.fromDTO(handicapCalculation)

            // Store in session attributes for future requests
            sessionAttributes[HANDICAP_SESSION_KEY] = handicapSummary
            input.attributesManager.sessionAttributes = sessionAttributes

            // Store in request attributes for current handler
            requestAttributes[HANDICAP_REQUEST_KEY] = handicapSummary
            input.attributesManager.requestAttributes = requestAttributes

            logger.info("Successfully cached handicap data for user ${user.id}")
        } catch (e: Exception) {
            logger.error("Failed to fetch handicap data for user ${user.id}: ${e.message}", e)
            // Don't throw - let the handler decide how to handle missing handicap data
        }
    }

    /**
     * Checks if the cached handicap data is still fresh based on the TTL.
     * 
     * @param handicap The cached handicap summary data
     * @return true if the cache is fresh, false if it's stale
     */
    private fun isCacheFresh(handicap: HandicapSummaryData): Boolean {
        val now = System.currentTimeMillis()
        val age = now - handicap.cachedAt
        return age < TTL_MILLIS
    }

    companion object {
        /**
         * Session attribute key for the cached handicap summary.
         */
        const val HANDICAP_SESSION_KEY = "handicap_summary"

        /**
         * Request attribute key for the handicap summary (for current request only).
         */
        const val HANDICAP_REQUEST_KEY = "handicap_summary_request"

        /**
         * Time-To-Live for cached handicap data in milliseconds (10 minutes).
         */
        private const val TTL_MILLIS = 10 * 60 * 1000L // 10 minutes
    }
}
