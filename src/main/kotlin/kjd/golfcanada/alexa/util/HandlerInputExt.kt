package kjd.golfcanada.alexa.util

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import kjd.golfcanada.alexa.data.UserProfileSession
import kjd.golfcanada.alexa.exception.NoUserDetailsException
import kjd.golfcanada.alexa.interceptor.UserProfileInterceptor

/**
 * Extension function to retrieve the UserProfileSession from session or throw NoUserDetailsException.
 * 
 * This provides a consistent way to access user information across handlers,
 * ensuring proper exception handling when user details are not available.
 * 
 * @return The UserProfileSession object from session
 * @throws NoUserDetailsException if user is not available in session or user ID is null
 */
fun HandlerInput.getUserOrThrow(): UserProfileSession {
    val sessionAttributes = this.attributesManager.sessionAttributes
    val userProfileSession = sessionAttributes[UserProfileInterceptor.USER_SESSION_KEY] as? UserProfileSession
    
    if (userProfileSession?.id == null) {
        throw NoUserDetailsException()
    }
    
    return userProfileSession
}
