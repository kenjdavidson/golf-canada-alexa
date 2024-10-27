package kjd.golfcanada.alexa

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.dispatcher.request.handler.RequestHandler
import com.amazon.ask.model.Response
import com.amazon.ask.request.Predicates.intentName
import java.util.*

/**
 * Provides information on what is currently available for the application.
 */
class HelpRequestHandler: RequestHandler {
    override fun canHandle(input: HandlerInput): Boolean {
        return input.matches(intentName("AMAZON.HelpIntent"))
    }

    override fun handle(input: HandlerInput): Optional<Response> {
        val speechText = "You can say hello to me!"
        return input.responseBuilder
            .withSpeech(speechText)
            .withSimpleCard("HelloWorld", speechText)
            .withReprompt(speechText)
            .build()
    }
}