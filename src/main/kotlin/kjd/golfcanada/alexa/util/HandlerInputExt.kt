package kjd.golfcanada.alexa.util

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import kjd.golfcanada.alexa.data.UserProfileSession
import kjd.golfcanada.alexa.exception.NoUserDetailsException
import kjd.golfcanada.alexa.interceptor.UserProfileInterceptor
import kjd.golfcanada.client.model.User

/**
 * Extension function to retrieve the User from session or throw NoUserDetailsException.
 * 
 * This provides a consistent way to access user information across handlers,
 * ensuring proper exception handling when user details are not available.
 * 
 * Note: This function looks for UserProfileSession in the session attributes and converts
 * it to a User object for backward compatibility with existing handlers.
 * 
 * @return A User object constructed from the UserProfileSession
 * @throws NoUserDetailsException if user is not available in session or user ID is null
 */
fun HandlerInput.getUserOrThrow(): User {
    val sessionAttributes = this.attributesManager.sessionAttributes
    val userProfileSession = sessionAttributes[UserProfileInterceptor.USER_SESSION_KEY] as? UserProfileSession
    
    if (userProfileSession?.id == null) {
        throw NoUserDetailsException()
    }
    
    // Convert UserProfileSession to User for backward compatibility
    return User(
        id = userProfileSession.id,
        firstName = userProfileSession.firstName,
        lastName = userProfileSession.lastName,
        email = null, // Not stored in session
        membershipLevel = userProfileSession.membershipLevel,
        golfCanadaCardId = userProfileSession.golfCanadaCardId,
        expirationDate = userProfileSession.expirationDate
    )
}
