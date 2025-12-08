package kjd.golfcanada.client.provider

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import kjd.golfcanada.alexa.exception.AccountLinkingException
import kjd.golfcanada.client.model.extractAccessToken

/**
 * Extension function that provides a fluent way to execute API calls with an authenticated client.
 * 
 * This function handles the common pattern of:
 * 1. Extracting the access token from the HandlerInput
 * 2. Validating that the access token exists
 * 3. Creating an authenticated API client
 * 4. Executing the provided block with the authenticated client
 * 
 * Example usage:
 * ```kotlin
 * apiClientProvider.withAuthenticatedClient(input) { client ->
 *     val friends = client.members.getFriends(userId)
 *     val handicap = client.scores.getHandicapCalculation(userId)
 *     // ... more API calls
 * }
 * ```
 * 
 * @param input The HandlerInput from the Alexa request
 * @param block The lambda function to execute with the authenticated client
 * @return The result of the lambda function
 * @throws AccountLinkingException if no access token is available in the request
 */
inline fun <T> ApiClientProvider.withAuthenticatedClient(
    input: HandlerInput,
    block: (ApiClientWrapper) -> T
): T {
    val accessToken = input.requestEnvelope.context?.system?.user?.accessToken
    
    if (accessToken.isNullOrBlank()) {
        throw AccountLinkingException()
    }
    
    val actualAccessToken = accessToken.extractAccessToken()
    val clientWrapper = getClient(actualAccessToken)
    
    return block(clientWrapper)
}
