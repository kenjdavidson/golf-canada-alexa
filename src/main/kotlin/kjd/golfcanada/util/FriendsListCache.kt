package kjd.golfcanada.util

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import kjd.golfcanada.alexa.util.HasFriendName
import kjd.golfcanada.client.model.Friend
import kjd.golfcanada.client.provider.ApiClientWrapper

/**
 * Lightweight representation of a friend containing only essential information.
 * 
 * This class is used for session storage to minimize data size, containing only
 * the friend's member ID, full name, and handicap rather than the complete Friend DTO.
 * 
 * @property memberId The friend's unique member identifier
 * @property name The friend's full name
 * @property handicap The friend's handicap index (optional)
 */
data class FriendInfo(
    val memberId: Long?,
    override val name: String?,
    val handicap: String? = null
) : HasFriendName

/**
 * Cache utility for managing friends list data in Alexa session attributes.
 * 
 * This utility provides a simple caching mechanism to avoid repeatedly fetching
 * the friends list from the Golf Canada API within a single Alexa session.
 * 
 * The cache:
 * - Stores only minimal friend information (ID and name) in session attributes
 * - Checks session storage before making API calls
 * - Automatically fetches and populates the session when cache is empty
 * - Returns the cached list for subsequent requests in the same session
 * 
 * Example usage:
 * ```kotlin
 * apiClientProvider.withAuthenticatedClient(input) { client ->
 *     val friends = FriendsListCache.get(input, client, userId)
 *     // Use friends list...
 * }
 * ```
 */
object FriendsListCache {
    
    /**
     * Session attribute key for storing the cached friends list.
     */
    private const val FRIENDS_CACHE_KEY = "friends_list_cache"
    
    /**
     * Gets the friends list from session cache or fetches from API if not cached.
     * 
     * This method implements the following logic:
     * 1. Check if friends list exists in session attributes
     * 2. If found, return the cached list
     * 3. If not found, fetch from API using the provided client
     * 4. Convert full Friend DTOs to lightweight FriendInfo objects
     * 5. Store FriendInfo list in session attributes
     * 6. Return the friends list
     * 
     * @param input The HandlerInput containing session attributes
     * @param client The authenticated API client wrapper
     * @param userId The user's member ID for fetching friends
     * @return List of FriendInfo objects (empty list if no friends found)
     */
    fun get(input: HandlerInput, client: ApiClientWrapper, userId: Long): List<FriendInfo> {
        val sessionAttributes = input.attributesManager.sessionAttributes
        
        // Check if friends list is already cached in session
        @Suppress("UNCHECKED_CAST")
        val cachedFriends = sessionAttributes[FRIENDS_CACHE_KEY] as? List<Map<String, Any?>>
        
        if (cachedFriends != null) {
            // Return cached friends list, converting from serialized map format
            return cachedFriends.mapNotNull { friendMap ->
                val memberId = when (val id = friendMap["memberId"]) {
                    is Number -> id.toLong()
                    is String -> id.toLongOrNull()
                    else -> null
                }
                val name = friendMap["name"] as? String
                val handicap = friendMap["handicap"] as? String
                FriendInfo(memberId, name, handicap)
            }
        }
        
        // Friends list not cached, fetch from API
        val friends = client.members.getFriends(userId)
        
        // Convert to lightweight FriendInfo objects
        val friendInfoList = friends.map { friend ->
            FriendInfo(
                memberId = friend.individualId,
                name = friend.name,
                handicap = friend.handicap
            )
        }
        
        // Store in session as serializable maps
        val serializableFriends = friendInfoList.map { friendInfo ->
            mapOf(
                "memberId" to friendInfo.memberId,
                "name" to friendInfo.name,
                "handicap" to friendInfo.handicap
            )
        }
        sessionAttributes[FRIENDS_CACHE_KEY] = serializableFriends
        input.attributesManager.sessionAttributes = sessionAttributes
        
        return friendInfoList
    }
    
    /**
     * Converts a list of Friend DTOs to FriendInfo objects.
     * 
     * This is a utility method for converting the full Friend DTO from the API
     * to the minimal FriendInfo representation used by the cache.
     * 
     * @param friends List of Friend DTOs from the API
     * @return List of FriendInfo objects
     */
    fun fromFriends(friends: List<Friend>): List<FriendInfo> {
        return friends.map { friend ->
            FriendInfo(
                memberId = friend.individualId,
                name = friend.name,
                handicap = friend.handicap
            )
        }
    }
}
