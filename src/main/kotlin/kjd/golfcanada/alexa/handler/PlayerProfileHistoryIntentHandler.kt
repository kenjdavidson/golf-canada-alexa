package kjd.golfcanada.alexa.handler
import jakarta.inject.Singleton

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.dispatcher.request.handler.RequestHandler
import com.amazon.ask.model.Response
import com.amazon.ask.request.Predicates.intentName
import kjd.golfcanada.alexa.IntentName
import java.util.*

/**
 * Handles requests about player's score history.
 */
@Singleton
class PlayerProfileHistoryIntentHandler : RequestHandler {
    override fun canHandle(input: HandlerInput): Boolean =
        input.matches(intentName(IntentName.PLAYER_PROFILE_HISTORY.value))

    override fun handle(input: HandlerInput): Optional<Response> {
        return input.generateTemplateResponse("PlayerProfileHistoryIntentResponse", emptyMap())
    }
}
