package kjd.golfcanada.alexa.handler

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.dispatcher.request.handler.RequestHandler
import com.amazon.ask.model.IntentRequest
import com.amazon.ask.model.Response
import com.amazon.ask.request.Predicates.intentName
import kjd.golfcanada.alexa.exception.AccountLinkingException
import kjd.golfcanada.alexa.exception.GolfCanadaApiException
import kjd.golfcanada.alexa.interceptor.HandicapLookupInterceptor
import kjd.golfcanada.alexa.interceptor.UserProfileInterceptor
import kjd.golfcanada.alexa.model.HandicapSummaryData
import kjd.golfcanada.alexa.util.FriendNameMatcher
import kjd.golfcanada.client.api.MembersApi
import kjd.golfcanada.client.api.ScoresApi
import kjd.golfcanada.client.model.User
import kjd.golfcanada.client.model.extractAccessToken
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Handles requests for handicap information.
 * 
 * This handler supports two scenarios:
 * 1. Own Handicap: When no friend slot is provided, returns the current user's handicap
 *    from Request Attributes (cached by HandicapLookupInterceptor)
 * 2. Friend's Handicap: When a friend slot is provided, makes a live API call to fetch
 *    the friend's handicap (no caching for friends)
 * 
 * Example utterances:
 * - "What's my handicap?"
 * - "Tell me my handicap index"
 * - "What is John's handicap?"
 * - "Get handicap for player 12345"
 */
class HandicapIntentRequestHandler : RequestHandler {

    private val logger = LoggerFactory.getLogger(HandicapIntentRequestHandler::class.java)

    override fun canHandle(input: HandlerInput): Boolean =
        input.matches(intentName("GOLFCANADA.HandicapIntent"))

    override fun handle(input: HandlerInput): Optional<Response> {
        val request = input.requestEnvelope.request as IntentRequest
        val slots = request.intent?.slots

        val friendFullNameSlot = slots?.get("FriendFullName")
        val friendFirstNameSlot = slots?.get("FriendFirstName")
        
        val friendQuery = friendFullNameSlot?.value ?: friendFirstNameSlot?.value

        return if (friendQuery != null) {
            handleFriendHandicap(input, friendQuery)
        } else {
            handleOwnHandicap(input)
        }
    }

    /**
     * Handles the request for the current user's handicap using cached data.
     * 
     * @param input The handler input
     * @return Response with the user's handicap information
     */
    private fun handleOwnHandicap(input: HandlerInput): Optional<Response> {
        val requestAttributes = input.attributesManager.requestAttributes
        val handicapSummary = requestAttributes[HandicapLookupInterceptor.HANDICAP_REQUEST_KEY] as? HandicapSummaryData

        if (handicapSummary == null) {
            logger.warn("No handicap data available in request attributes")
            val dataModel = mapOf(
                "error" to "Unable to retrieve your handicap information at this time."
            )
            return input.generateTemplateResponse("HandicapIntentErrorResponse", dataModel)
        }

        logger.info("Returning own handicap: ${handicapSummary.handicap}")

        return input.generateTemplateResponse("HandicapIntentResponse", handicapSummary.toResponseData())
    }

    /**
     * Handles the request for a friend's handicap by making a live API call.
     * 
     * This method:
     * 1. Retrieves the user's friends list from the Golf Canada API
     * 2. Performs fuzzy matching on the raw search query against friend names
     * 3. Handles ambiguity if multiple matches are found
     * 4. Fetches and returns the friend's handicap information
     * 
     * @param input The handler input
     * @param friendQuery The raw search query from the user (e.g., "Dean Ellis" or "Deano")
     * @return Response with the friend's handicap information
     */
    private fun handleFriendHandicap(input: HandlerInput, friendQuery: String): Optional<Response> {
        val accessToken = input.requestEnvelope.context?.system?.user?.accessToken

        if (accessToken.isNullOrBlank()) {
            logger.warn("No access token available for fetching friend handicap")
            throw AccountLinkingException()
        }

        logger.info("Friend handicap requested for: $friendQuery")
        
        try {
            val actualAccessToken = accessToken.extractAccessToken()
            
            // Get user profile from session
            val sessionAttributes = input.attributesManager.sessionAttributes
            val user = sessionAttributes[UserProfileInterceptor.USER_SESSION_KEY] as? User
            if (user?.id == null) {
                logger.warn("No user profile available for fetching friends list")
                val dataModel = mapOf(
                    "error" to "Unable to retrieve your profile information."
                )
                return input.generateTemplateResponse("HandicapIntentErrorResponse", dataModel)
            }
            
            // Set access token on ApiClient companion object
            org.openapitools.client.infrastructure.ApiClient.accessToken = actualAccessToken
            val membersApi = MembersApi()
            
            // Get friends list
            val friends = membersApi.getFriends(user.id)
            
            if (friends.isEmpty()) {
                logger.info("No friends found for user ${user.id}")
                return input.generateTemplateResponse("HandicapIntentNoFriendsResponse", emptyMap())
            }
            
            // Perform fuzzy matching using FriendNameMatcher
            val matches = FriendNameMatcher.findMatches(friends, friendQuery)
            
            when {
                matches.isEmpty() -> {
                    logger.info("No matching friend found for query: $friendQuery")
                    val dataModel = mapOf(
                        "query" to friendQuery
                    )
                    return input.generateTemplateResponse("HandicapIntentNoMatchingFriendResponse", dataModel)
                }
                matches.size > 1 -> {
                    logger.info("Multiple matches found for query: $friendQuery")
                    val dataModel = mapOf(
                        "count" to matches.size,
                        "query" to friendQuery,
                        "friends" to matches
                    )
                    return input.generateTemplateResponse("HandicapIntentMultipleFriendsResponse", dataModel)
                }
                else -> {
                    val friend = matches.first()
                    logger.info("Found matching friend: ${friend.name} (ID: ${friend.individualId})")
                    
                    // Return friend's handicap information
                    val dataModel = mutableMapOf<String, Any>()
                    friend.name?.let { dataModel["name"] = it }
                    friend.handicap?.let { dataModel["handicap"] = it }
                    
                    return input.generateTemplateResponse("HandicapIntentFriendResponse", dataModel)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch friend handicap for query '$friendQuery': ${e.message}", e)
            throw GolfCanadaApiException("Error fetching friend handicap", e)
        }
    }
}
