package kjd.golfcanada.alexa.exception

/**
 * Exception thrown when there is an error communicating with the Golf Canada API.
 *
 * This exception is used by handlers that make Golf Canada API calls to indicate
 * that an API error occurred and the user should try again later.
 */
class GolfCanadaApiException(
    message: String = "An error occurred while communicating with the Golf Canada API",
    cause: Throwable? = null
) : RuntimeException(message, cause)
