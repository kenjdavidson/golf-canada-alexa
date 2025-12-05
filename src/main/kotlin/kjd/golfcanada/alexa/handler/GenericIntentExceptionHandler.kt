package kjd.golfcanada.alexa.handler

import com.amazon.ask.dispatcher.exception.ExceptionHandler
import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.model.Response
import kjd.golfcanada.alexa.exception.GenericIntentException
import org.slf4j.LoggerFactory
import java.util.Optional

/**
 * Exception handler that handles [GenericIntentException] and returns a generic error response
 * to the user.
 *
 * This handler logs the error and uses the FreeMarker template system to generate
 * appropriate error responses in the user's preferred language.
 */
class GenericIntentExceptionHandler : ExceptionHandler {

    private val logger = LoggerFactory.getLogger(GenericIntentExceptionHandler::class.java)

    /**
     * Determines if this handler can handle the given exception.
     *
     * @param input The handler input
     * @param throwable The exception that was thrown
     * @return true if the exception is a GenericIntentException, false otherwise
     */
    override fun canHandle(input: HandlerInput, throwable: Throwable): Boolean {
        return throwable is GenericIntentException
    }

    /**
     * Handles the GenericIntentException by logging the error and returning a
     * generic error response to the user.
     *
     * @param input The handler input
     * @param throwable The GenericIntentException
     * @return A response with a generic error message
     */
    override fun handle(input: HandlerInput, throwable: Throwable): Optional<Response> {
        logger.error("Handling GenericIntentException: ${throwable.message}", throwable)

        return input.generateTemplateResponse("GenericIntentErrorResponse", emptyMap())
    }
}
