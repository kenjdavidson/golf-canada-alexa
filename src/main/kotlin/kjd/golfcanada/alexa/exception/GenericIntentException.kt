package kjd.golfcanada.alexa.exception

/**
 * Generic exception that can be thrown by any intent handler when an error occurs
 * that should be handled with a generic error response to the user.
 *
 * This exception is used by intent handlers to indicate that an error occurred
 * and the user should receive a generic error message.
 */
class GenericIntentException(
    message: String = "An error occurred while processing your request",
    cause: Throwable? = null
) : RuntimeException(message, cause)
