package kjd.golfcanada.alexa.handler

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.dispatcher.request.handler.RequestHandler
import com.amazon.ask.model.Response
import com.amazon.ask.request.Predicates.intentName
import java.util.*

/**
 * Handles requests about favorite player's score history.
 */
class FavoritePlayerHistoryIntentHandler : RequestHandler {
    override fun canHandle(input: HandlerInput): Boolean =
        input.matches(intentName("FavoritePlayerHistoryIntent"))

    override fun handle(input: HandlerInput): Optional<Response> {
        return input.generateTemplateResponse("FavoritePlayerHistoryIntentResponse", emptyMap())
    }
}
