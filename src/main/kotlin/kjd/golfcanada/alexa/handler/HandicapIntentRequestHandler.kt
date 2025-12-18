package kjd.golfcanada.alexa.handler

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.dispatcher.request.handler.RequestHandler
import com.amazon.ask.model.IntentRequest
import com.amazon.ask.model.Response
import com.amazon.ask.request.Predicates.intentName
import kjd.golfcanada.alexa.IntentName
import kjd.golfcanada.alexa.data.HandicapSummaryData
import kjd.golfcanada.alexa.data.UserProfileSession
import kjd.golfcanada.alexa.exception.AccountLinkingException
import kjd.golfcanada.alexa.exception.GenericIntentException
import kjd.golfcanada.alexa.exception.GolfCanadaApiException
import kjd.golfcanada.alexa.util.FriendNameMatcher
import kjd.golfcanada.alexa.util.getUserOrThrow
import kjd.golfcanada.client.provider.IApiClientProvider
import kjd.golfcanada.client.provider.withAuthenticatedClient
import kjd.golfcanada.util.FriendsListCache
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
    private val apiClientProvider: IApiClientProvider
) : RequestHandler {

    private val logger = LoggerFactory.getLogger(HandicapIntentRequestHandler::class.java)

    override fun canHandle(input: HandlerInput): Boolean =
        input.matches(intentName(IntentName.HANDICAP.value))

    override fun handle(input: HandlerInput): Optional<Response> {
        val request = input.requestEnvelope.request as IntentRequest
        val slots = request.intent?.slots

        // Get user profile from session - validate once for all handicap requests
        val userProfile = input.getUserOrThrow()

        val friendFullNameSlot = slots?.get("FriendFullName")
        val friendFirstNameSlot = slots?.get("FriendFirstName")
        
        val friendQuery = friendFullNameSlot?.value ?: friendFirstNameSlot?.value

        return if (friendQuery != null) {
            handleFriendHandicap(input, friendQuery)
        } else {
            handleOwnHandicap(input, userProfile)
        }
    }

    /**
     * Handles the request for the current user's handicap by making an API call.
     * 
     * @param input The handler input
     * @param userProfile The validated user profile session
     * @return Response with the user's handicap information
     */
    private fun handleOwnHandicap(input: HandlerInput, userProfile: UserProfileSession): Optional<Response> {
        logger.info("Own handicap requested")
        
        try {
            // Use the withAuthenticatedClient extension to simplify API client access
            return apiClientProvider.withAuthenticatedClient(input) { client ->
                // Fetch user's handicap
                val handicapCalculation = client.scores.getHandicapCalculation(userProfile.id!!)
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
     * Handles the request for a friend's handicap by using cached friends list.
     * 
     * This method:
     * 1. Retrieves the user's friends list from cache or API via FriendsListCache
     * 2. Performs fuzzy matching on the raw search query against friend names
     * 3. Handles ambiguity if multiple matches are found
     * 4. Returns the friend's handicap information
     * 
     * @param input The handler input
     * @param friendQuery The raw search query from the user (e.g., "Dean Ellis" or "Deano")
     * @return Response with the friend's handicap information
     */
    private fun handleFriendHandicap(input: HandlerInput, friendQuery: String): Optional<Response> {
        logger.info("Friend handicap requested")
        
        try {
            // Use the withAuthenticatedClient extension to simplify API client access
            return apiClientProvider.withAuthenticatedClient(input) { client ->
                // Get friends list from cache or API
                val friends = FriendsListCache.get(input, client)
                
                if (friends.isEmpty()) {
                    logger.info("No friends found for user")
                    return@withAuthenticatedClient input.generateTemplateResponse("HandicapIntentNoFriendsResponse", emptyMap())
                }
                
                // Perform fuzzy matching using FriendNameMatcher
                val matches = FriendNameMatcher.findMatchesBy(friends, friendQuery) { it.name }
                
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
                        val dataModel = mutableMapOf<String, Any>(
                            "name" to friend.name
                        )
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
