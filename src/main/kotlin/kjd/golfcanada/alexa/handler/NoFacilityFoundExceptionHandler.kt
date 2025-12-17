package kjd.golfcanada.alexa.handler

import com.amazon.ask.dispatcher.exception.ExceptionHandler
import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.model.Response
import kjd.golfcanada.alexa.exception.NoFacilityFoundException
import org.slf4j.LoggerFactory
import java.util.Optional

/**
 * Exception handler that handles [NoFacilityFoundException] and returns an appropriate
 * error response to the user.
 *
 * This handler logs the error and uses the FreeMarker template system to generate
 * appropriate error responses based on whether a facility name was specified.
 */
class NoFacilityFoundExceptionHandler : ExceptionHandler {

    private val logger = LoggerFactory.getLogger(NoFacilityFoundExceptionHandler::class.java)

    /**
     * Determines if this handler can handle the given exception.
     *
     * @param input The handler input
     * @param throwable The exception that was thrown
     * @return true if the exception is a NoFacilityFoundException, false otherwise
     */
    override fun canHandle(input: HandlerInput, throwable: Throwable): Boolean {
        return throwable is NoFacilityFoundException
    }

    /**
     * Handles the NoFacilityFoundException by logging the error and returning
     * an appropriate error response to the user.
     *
     * @param input The handler input
     * @param throwable The NoFacilityFoundException
     * @return A response indicating the facility was not found
     */
    override fun handle(input: HandlerInput, throwable: Throwable): Optional<Response> {
        val exception = throwable as NoFacilityFoundException
        logger.error("Handling NoFacilityFoundException: ${exception.message}", exception)

        val templateName = if (exception.facilityName != null) {
            "FacilityNotFoundResponse"
        } else {
            "NoDefaultFacilityResponse"
        }
        
        val dataModel = exception.facilityName?.let { 
            mapOf("facilityName" to it)
        } ?: emptyMap()

        return input.generateTemplateResponse(templateName, dataModel)
    }
}
