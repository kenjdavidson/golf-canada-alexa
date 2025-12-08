package kjd.golfcanada.util

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import kjd.golfcanada.alexa.data.FriendInfo
import kjd.golfcanada.alexa.util.getUserOrThrow
import kjd.golfcanada.client.model.Friend
import kjd.golfcanada.client.provider.ApiClientWrapper

/**
 * Cache utility for managing friends list data in Alexa session attributes.
 * 
 * This utility provides a simple caching mechanism to avoid repeatedly fetching
 * the friends list from the Golf Canada API within a single Alexa session.
 * 
 * The cache:
 * - Stores only minimal friend information (ID, name, and handicap) in session attributes
 * - Checks session storage before making API calls
 * - Automatically fetches and populates the session when cache is empty
 * - Returns the cached list for subsequent requests in the same session
 * 
 * Example usage:
 * ```kotlin
 * apiClientProvider.withAuthenticatedClient(input) { client ->
 *     val friends = FriendsListCache.get(input, client)
 *     // friends is a List<FriendInfo> with memberId, name, and handicap
 * }
 * ```
 */
object FriendsListCache {
    
    /**
     * Session attribute key for storing the cached friends list.
     */
    private const val FRIENDS_LIST_KEY = "friends_list"
    
    /**
     * Gets the friends list from session cache or fetches from API if not cached.
     * 
     * This method implements the following logic:
     * 1. Get user ID from session attributes (populated by UserProfileInterceptor)
     * 2. Check if friends list exists in session attributes
     * 3. If found, return the cached list
     * 4. If not found, fetch from API using the provided client
     * 5. Convert full Friend DTOs to lightweight FriendInfo objects
     * 6. Store FriendInfo list in session attributes
     * 7. Return the friends list
     * 
     * @param input The HandlerInput containing session attributes
     * @param client The authenticated API client wrapper
     * @return List of FriendInfo objects (empty list if no friends found)
     * @throws NoUserDetailsException if user ID is not available in session
     */
    fun get(input: HandlerInput, client: ApiClientWrapper): List<FriendInfo> {
        val sessionAttributes = input.attributesManager.sessionAttributes
        
        // Get user from session using extension function
        val user = input.getUserOrThrow()
        val userId = user.id!!
        
        // Check if friends list is already cached in session
        @Suppress("UNCHECKED_CAST")
        val cachedFriends = sessionAttributes[FRIENDS_LIST_KEY] as? List<FriendInfo>
        
        if (cachedFriends != null) {
            return cachedFriends
        }
        
        // Friends list not cached, fetch from API
        val friends = client.members.getFriends(userId)
        
        // Convert to lightweight FriendInfo objects, filtering out friends with null id or name
        val friendInfoList = friends.mapNotNull { friend ->
            val id = friend.individualId ?: return@mapNotNull null
            val name = friend.name ?: return@mapNotNull null
            FriendInfo(
                memberId = id,
                name = name,
                handicap = friend.handicap
            )
        }
        
        // Store FriendInfo list directly in session
        sessionAttributes[FRIENDS_LIST_KEY] = friendInfoList
        input.attributesManager.sessionAttributes = sessionAttributes
        
        return friendInfoList
    }
    
    /**
     * Converts a list of Friend DTOs to FriendInfo objects.
     * 
     * This is a utility method for converting the full Friend DTO from the API
     * to the minimal FriendInfo representation used by the cache.
     * Friends with null id or name are filtered out.
     * 
     * @param friends List of Friend DTOs from the API
     * @return List of FriendInfo objects (excludes friends with null id or name)
     */
    fun fromFriends(friends: List<Friend>): List<FriendInfo> {
        return friends.mapNotNull { friend ->
            val id = friend.individualId ?: return@mapNotNull null
            val name = friend.name ?: return@mapNotNull null
            FriendInfo(
                memberId = id,
                name = name,
                handicap = friend.handicap
            )
        }
    }
}
