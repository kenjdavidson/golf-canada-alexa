package kjd.golfcanada.alexa.handler

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.dispatcher.request.handler.RequestHandler
import com.amazon.ask.model.Response
import com.amazon.ask.request.Predicates.intentName
import java.util.*

/**
 * Provides information on what is currently available for the application.
 */
class HelpIntentHandler : RequestHandler {
    override fun canHandle(input: HandlerInput): Boolean =
        input.matches(intentName("AMAZON.HelpIntent"))

    override fun handle(input: HandlerInput): Optional<Response> {
        return input.generateTemplateResponse("HelpIntentResponse", emptyMap())
    }
}