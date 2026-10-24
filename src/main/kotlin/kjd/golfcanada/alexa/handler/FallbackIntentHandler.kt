package kjd.golfcanada.alexa.handler
import jakarta.inject.Singleton

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.dispatcher.request.handler.RequestHandler
import com.amazon.ask.model.Response
import com.amazon.ask.request.Predicates.intentName
import java.util.*

/**
 * Handles fallback intent when Alexa doesn't understand the request.
 */
@Singleton
class FallbackIntentHandler : RequestHandler {
    override fun canHandle(input: HandlerInput): Boolean =
        input.matches(intentName("AMAZON.FallbackIntent"))

    override fun handle(input: HandlerInput): Optional<Response> {
        return input.generateTemplateResponse("FallbackIntentResponse", emptyMap())
    }
}
