package kjd.golfcanada.alexa.handler

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.dispatcher.request.handler.RequestHandler
import com.amazon.ask.model.Response
import com.amazon.ask.model.SessionEndedRequest
import com.amazon.ask.request.Predicates.requestType
import java.util.*

class SessionEndedRequestHandler: RequestHandler {
    override fun canHandle(input: HandlerInput): Boolean =
        input.matches(requestType(SessionEndedRequest::class.java))

    override fun handle(input: HandlerInput): Optional<Response> {
        // At this point here is nothing to clean-up
        return input.responseBuilder.build()
    }
}