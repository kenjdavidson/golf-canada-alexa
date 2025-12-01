package kjd.golfcanada.alexa.handler

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.dispatcher.request.handler.RequestHandler
import com.amazon.ask.model.Response
import com.amazon.ask.request.Predicates.intentName
import java.util.*

/**
 * Handles requests to add a scorecard.
 */
class AddScorecardIntentHandler : RequestHandler {
    override fun canHandle(input: HandlerInput): Boolean =
        input.matches(intentName("AddScorecardIntent"))

    override fun handle(input: HandlerInput): Optional<Response> {
        return input.generateTemplateResponse("AddScorecardIntentResponse", emptyMap())
    }
}
