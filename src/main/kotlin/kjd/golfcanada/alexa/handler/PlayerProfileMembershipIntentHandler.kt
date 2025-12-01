package kjd.golfcanada.alexa.handler

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.dispatcher.request.handler.RequestHandler
import com.amazon.ask.model.Response
import com.amazon.ask.request.Predicates.intentName
import kjd.golfcanada.alexa.exception.NoUserDetailsException
import kjd.golfcanada.alexa.interceptor.UserProfileInterceptor
import kjd.golfcanada.client.model.User
import java.util.*

/**
 * Handles requests about player's membership information.
 */
class PlayerProfileMembershipIntentHandler : RequestHandler {
    override fun canHandle(input: HandlerInput): Boolean =
        input.matches(intentName("PlayerProfileMembershipIntent"))

    override fun handle(input: HandlerInput): Optional<Response> {
        val sessionAttributes = input.attributesManager.sessionAttributes
        
        // Get user from session
        val user = sessionAttributes[UserProfileInterceptor.USER_SESSION_KEY] as? User
            ?: throw NoUserDetailsException()
        
        // Prepare data model for the template
        val dataModel = mutableMapOf<String, Any>()
        user.firstName?.let { dataModel["firstName"] = it }
        user.lastName?.let { dataModel["lastName"] = it }
        user.membershipLevel?.let { dataModel["membershipLevel"] = it }
        user.golfCanadaCardId?.let { dataModel["golfCanadaCardId"] = it }
        user.expirationDate?.let { dataModel["expirationDate"] = it }
        user.scoreDefaults?.postHoleByHole?.let { dataModel["postHoleByHole"] = it }
        user.scoreDefaults?.facilityName?.let { dataModel["facilityName"] = it }
        
        return input.generateTemplateResponse("PlayerProfileMembershipIntentResponse", dataModel)
    }
}
