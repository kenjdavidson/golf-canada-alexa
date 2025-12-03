package kjd.golfcanada.alexa.handler

import com.amazon.ask.dispatcher.exception.ExceptionHandler
import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.model.Response
import kjd.golfcanada.alexa.exception.GolfCanadaApiException
import org.slf4j.LoggerFactory
import java.util.Optional

/**
 * Exception handler that handles [GolfCanadaApiException] and returns a localized response
 * informing the user that an API error occurred.
 *
 * This handler uses the FreeMarker template system to generate appropriate responses
 * in the user's preferred language.
 */
class GolfCanadaApiExceptionHandler : ExceptionHandler {

    private val logger = LoggerFactory.getLogger(GolfCanadaApiExceptionHandler::class.java)

    /**
     * Determines if this handler can handle the given exception.
     *
     * @param input The handler input
     * @param throwable The exception that was thrown
     * @return true if the exception is a GolfCanadaApiException, false otherwise
     */
    override fun canHandle(input: HandlerInput, throwable: Throwable): Boolean {
        return throwable is GolfCanadaApiException
    }

    /**
     * Handles the GolfCanadaApiException by returning a response that instructs
     * the user to try again later.
     *
     * @param input The handler input
     * @param throwable The GolfCanadaApiException
     * @return A response with error message
     */
    override fun handle(input: HandlerInput, throwable: Throwable): Optional<Response> {
        logger.error("Handling GolfCanadaApiException: ${throwable.message}", throwable)

        return input.generateTemplateResponse("GolfCanadaApiErrorResponse", emptyMap())
    }
}
