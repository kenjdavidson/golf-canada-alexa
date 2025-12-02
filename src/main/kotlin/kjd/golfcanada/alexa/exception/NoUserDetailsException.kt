package kjd.golfcanada.alexa.exception

/**
 * Exception thrown when user profile information is required but not available in the session.
 *
 * This exception is used by handlers that need user profile data to indicate
 * that the user should re-authenticate to restore their session data.
 */
class NoUserDetailsException(
    message: String = "User profile information is not available in the session"
) : RuntimeException(message)
