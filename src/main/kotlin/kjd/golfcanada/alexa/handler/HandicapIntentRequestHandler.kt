package kjd.golfcanada.alexa.handler

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.dispatcher.request.handler.RequestHandler
import com.amazon.ask.model.IntentRequest
import com.amazon.ask.model.Response
import com.amazon.ask.request.Predicates.intentName
import kjd.golfcanada.alexa.IntentName
import kjd.golfcanada.alexa.exception.AccountLinkingException
import kjd.golfcanada.alexa.exception.GenericIntentException
import kjd.golfcanada.alexa.exception.GolfCanadaApiException
import kjd.golfcanada.alexa.exception.NoUserDetailsException
import kjd.golfcanada.alexa.interceptor.UserProfileInterceptor
import kjd.golfcanada.alexa.data.HandicapSummaryData
import kjd.golfcanada.alexa.util.FriendNameMatcher
import kjd.golfcanada.client.model.User
import kjd.golfcanada.client.provider.ApiClientProvider
import kjd.golfcanada.client.provider.withAuthenticatedClient
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Handles requests for handicap information.
 * 
 * This handler supports two scenarios:
 * 1. Own Handicap: When no friend slot is provided, fetches the current user's handicap
 *    from the Golf Canada API
 * 2. Friend's Handicap: When a friend slot is provided, makes a live API call to fetch
 *    the friend's handicap
 * 
 * Example utterances:
 * - "What's my handicap?"
 * - "Tell me my handicap index"
 * - "What is John's handicap?"
 * - "Get handicap for player 12345"
 */
class HandicapIntentRequestHandler(
    private val apiClientProvider: ApiClientProvider
) : RequestHandler {

    private val logger = LoggerFactory.getLogger(HandicapIntentRequestHandler::class.java)

    override fun canHandle(input: HandlerInput): Boolean =
        input.matches(intentName(IntentName.HANDICAP.value))

    override fun handle(input: HandlerInput): Optional<Response> {
        val request = input.requestEnvelope.request as IntentRequest
        val slots = request.intent?.slots

        // Get user profile from session - validate once for all handicap requests
        val sessionAttributes = input.attributesManager.sessionAttributes
        val user = sessionAttributes[UserProfileInterceptor.USER_SESSION_KEY] as? User
        if (user?.id == null) {
            logger.warn("No user profile available for handicap request")
            throw NoUserDetailsException()
        }

        val friendFullNameSlot = slots?.get("FriendFullName")
        val friendFirstNameSlot = slots?.get("FriendFirstName")
        
        val friendQuery = friendFullNameSlot?.value ?: friendFirstNameSlot?.value

        return if (friendQuery != null) {
            handleFriendHandicap(input, user, friendQuery)
        } else {
            handleOwnHandicap(input, user)
        }
    }

    /**
     * Handles the request for the current user's handicap by making an API call.
     * 
     * @param input The handler input
     * @param user The validated user from session
     * @return Response with the user's handicap information
     */
    private fun handleOwnHandicap(input: HandlerInput, user: User): Optional<Response> {
        logger.info("Own handicap requested")
        
        try {
            // Use the withAuthenticatedClient extension to simplify API client access
            return apiClientProvider.withAuthenticatedClient(input) { client ->
                // Fetch user's handicap
                val handicapCalculation = client.scores.getHandicapCalculation(user.id!!)
                val handicapSummary = HandicapSummaryData.fromDTO(handicapCalculation)

                logger.info("Returning own handicap")

                input.generateTemplateResponse("HandicapIntentResponse", handicapSummary.toResponseData())
            }
        } catch (e: AccountLinkingException) {
            logger.warn("No access token available for fetching own handicap")
            throw e
        } catch (e: Exception) {
            logger.error("Failed to fetch own handicap: ${e.message}", e)
            throw GenericIntentException("Failed to fetch own handicap", e)
        }
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
     * @param user The validated user from session
     * @param friendQuery The raw search query from the user (e.g., "Dean Ellis" or "Deano")
     * @return Response with the friend's handicap information
     */
    private fun handleFriendHandicap(input: HandlerInput, user: User, friendQuery: String): Optional<Response> {
        logger.info("Friend handicap requested")
        
        try {
            // Use the withAuthenticatedClient extension to simplify API client access
            return apiClientProvider.withAuthenticatedClient(input) { client ->
                // Get friends list
                val friends = client.members.getFriends(user.id!!)
                
                if (friends.isEmpty()) {
                    logger.info("No friends found for user")
                    return@withAuthenticatedClient input.generateTemplateResponse("HandicapIntentNoFriendsResponse", emptyMap())
                }
                
                // Perform fuzzy matching using FriendNameMatcher
                val matches = FriendNameMatcher.findMatches(friends, friendQuery)
                
                when {
                    matches.isEmpty() -> {
                        logger.info("No matching friend found")
                        val dataModel = mapOf(
                            "query" to friendQuery
                        )
                        input.generateTemplateResponse("HandicapIntentNoMatchingFriendResponse", dataModel)
                    }
                    matches.size > 1 -> {
                        logger.info("Multiple matches found")
                        val dataModel = mapOf(
                            "count" to matches.size,
                            "query" to friendQuery,
                            "friends" to matches
                        )
                        input.generateTemplateResponse("HandicapIntentMultipleFriendsResponse", dataModel)
                    }
                    else -> {
                        val friend = matches.first()
                        logger.info("Found matching friend")
                        
                        // Return friend's handicap information
                        val dataModel = mutableMapOf<String, Any>()
                        friend.name?.let { dataModel["name"] = it }
                        friend.handicap?.let { dataModel["handicap"] = it }
                        
                        input.generateTemplateResponse("HandicapIntentFriendResponse", dataModel)
                    }
                }
            }
        } catch (e: AccountLinkingException) {
            logger.warn("No access token available for fetching friend handicap")
            throw e
        } catch (e: Exception) {
            logger.error("Failed to fetch friend handicap: ${e.message}", e)
            throw GolfCanadaApiException("Error fetching friend handicap", e)
        }
    }
}
