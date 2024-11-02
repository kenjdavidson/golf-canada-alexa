package kjd.golfcanada.alexa.handler

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.model.LaunchRequest
import com.amazon.ask.model.Response
import java.util.*

/**
 * Handles launching the application by default.
 *
 * Handles utterances such as:
 * - open golf canada
 * - hey golf canada
 * - hi golf canada
 * - etc
 */
class LaunchRequestHandler : com.amazon.ask.dispatcher.request.handler.impl.LaunchRequestHandler {
    override fun canHandle(input: HandlerInput?, request: LaunchRequest?): Boolean = true

    override fun handle(input: HandlerInput?, request: LaunchRequest?): Optional<Response> {
        TODO("Not yet implemented")
    }
}