package kjd.golfcanada.alexa.util

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import kjd.golfcanada.alexa.exception.NoUserDetailsException
import kjd.golfcanada.alexa.interceptor.UserProfileInterceptor
import kjd.golfcanada.client.model.User

/**
 * Extension function to retrieve the User from session or throw NoUserDetailsException.
 * 
 * This provides a consistent way to access user information across handlers,
 * ensuring proper exception handling when user details are not available.
 * 
 * @return The User object from session
 * @throws NoUserDetailsException if user is not available in session or user ID is null
 */
fun HandlerInput.getUserOrThrow(): User {
    val sessionAttributes = this.attributesManager.sessionAttributes
    val user = sessionAttributes[UserProfileInterceptor.USER_SESSION_KEY] as? User
    
    if (user?.id == null) {
        throw NoUserDetailsException()
    }
    
    return user
}
