package kjd.golfcanada.alexa.handler

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.dispatcher.request.handler.RequestHandler
import com.amazon.ask.model.Response
import com.amazon.ask.request.Predicates.intentName
import kjd.golfcanada.alexa.IntentName
import kjd.golfcanada.alexa.data.UserProfileSession
import kjd.golfcanada.alexa.exception.NoUserDetailsException
import kjd.golfcanada.alexa.interceptor.UserProfileInterceptor
import java.util.*

/**
 * Handles requests about player's membership information.
 */
class PlayerProfileMembershipIntentHandler : RequestHandler {
    override fun canHandle(input: HandlerInput): Boolean =
        input.matches(intentName(IntentName.PLAYER_PROFILE_MEMBERSHIP.value))

    override fun handle(input: HandlerInput): Optional<Response> {
        val sessionAttributes = input.attributesManager.sessionAttributes
        
        // Get user profile from session
        val userProfile = sessionAttributes[UserProfileInterceptor.USER_SESSION_KEY] as? UserProfileSession
            ?: throw NoUserDetailsException()
        
        // Prepare data model for the template
        val dataModel = userProfile.toResponseData()
        
        return input.generateTemplateResponse("PlayerProfileMembershipIntentResponse", dataModel)
    }
}
