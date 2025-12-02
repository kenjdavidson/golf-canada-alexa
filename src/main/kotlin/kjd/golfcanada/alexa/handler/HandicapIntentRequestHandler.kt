package kjd.golfcanada.alexa.handler

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.dispatcher.request.handler.RequestHandler
import com.amazon.ask.model.IntentRequest
import com.amazon.ask.model.Response
import com.amazon.ask.request.Predicates.intentName
import kjd.golfcanada.alexa.exception.AccountLinkingException
import kjd.golfcanada.alexa.interceptor.HandicapLookupInterceptor
import kjd.golfcanada.alexa.model.HandicapSummaryData
import kjd.golfcanada.client.api.ScoresApi
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

        val friendFullNameSlot = slots?.get("FriendName")
        val firstNameSlot = slots?.get("FirstName")
        
        val friendName = friendFullNameSlot?.value ?: firstNameSlot?.value

        return if (friendName != null) {
            handleFriendHandicap(input, friendName)
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
     * NOTE: This currently requires a name-to-ID resolution mechanism which is not yet implemented.
     * A future enhancement would need to add a service/API to map friend names to individualIds.
     * 
     * @param input The handler input
     * @param friendName The name of the friend
     * @return Response with the friend's handicap information
     */
    private fun handleFriendHandicap(input: HandlerInput, friendName: String): Optional<Response> {
        val accessToken = input.requestEnvelope.context?.system?.user?.accessToken

        if (accessToken.isNullOrBlank()) {
            logger.warn("No access token available for fetching friend handicap")
            throw AccountLinkingException()
        }

        logger.info("Friend handicap requested for: $friendName")
        
        // TODO: Implement name-to-ID resolution
        // This would typically involve:
        // 1. Calling a Golf Canada API to search for members by name
        // 2. Disambiguating if multiple matches are found
        // 3. Using the resolved individualId to fetch handicap data
        
        // For now, return an error indicating this feature is not yet available
        val dataModel = mapOf(
            "error" to "Friend handicap lookup by name is not yet available. Please check back later."
        )
        return input.generateTemplateResponse("HandicapIntentErrorResponse", dataModel)
    }
}
