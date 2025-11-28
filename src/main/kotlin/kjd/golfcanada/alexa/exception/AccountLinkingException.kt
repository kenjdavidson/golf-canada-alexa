package kjd.golfcanada.alexa.exception

/**
 * Exception thrown when account linking is required for a request.
 *
 * This exception is used by the AuthenticationRequestInterceptor to indicate
 * that the user needs to link their Golf Canada account to use the requested feature.
 */
class AccountLinkingException(
    message: String = "Account linking is required for this request"
) : RuntimeException(message)
