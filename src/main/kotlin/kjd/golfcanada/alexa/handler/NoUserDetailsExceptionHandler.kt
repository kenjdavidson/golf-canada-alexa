package kjd.golfcanada.alexa.handler

import com.amazon.ask.dispatcher.exception.ExceptionHandler
import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.model.Response
import kjd.golfcanada.alexa.exception.NoUserDetailsException
import org.slf4j.LoggerFactory
import java.util.Optional

/**
 * Exception handler that handles [NoUserDetailsException] and returns a localized response
 * informing the user that they need to re-authenticate.
 *
 * This handler uses the FreeMarker template system to generate appropriate responses
 * in the user's preferred language.
 */
class NoUserDetailsExceptionHandler : ExceptionHandler {

    private val logger = LoggerFactory.getLogger(NoUserDetailsExceptionHandler::class.java)

    /**
     * Determines if this handler can handle the given exception.
     *
     * @param input The handler input
     * @param throwable The exception that was thrown
     * @return true if the exception is a NoUserDetailsException, false otherwise
     */
    override fun canHandle(input: HandlerInput, throwable: Throwable): Boolean {
        return throwable is NoUserDetailsException
    }

    /**
     * Handles the NoUserDetailsException by returning a response that instructs
     * the user to re-authenticate using the Alexa app.
     *
     * The response includes:
     * - A Link Account card to guide the user through account linking
     * - A speech output in the user's preferred language
     * - The session ends after the response
     *
     * @param input The handler input
     * @param throwable The NoUserDetailsException
     * @return A response with re-authentication instructions
     */
    override fun handle(input: HandlerInput, throwable: Throwable): Optional<Response> {
        logger.info("Handling NoUserDetailsException, returning account linking response")

        return input.generateTemplateResponse("AccountLinkingRequiredResponse", emptyMap())
    }
}
