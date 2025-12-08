package kjd.golfcanada.alexa.util

import kjd.golfcanada.client.model.Friend

/**
 * Interface for objects that have a friend name.
 * 
 * This interface can be implemented by friend-like classes to enable name-based matching.
 * However, since the Friend DTO is generated code, FriendNameMatcher also provides
 * a generic findMatchesBy method that accepts a lambda for name extraction,
 * allowing type-safe matching without requiring interface implementation.
 */
interface HasFriendName {
    val name: String?
}

/**
 * Utility class for matching friend names against a search query using fuzzy matching.
 * 
 * This class provides a predicate that can be used to filter a list of friends
 * based on a search query. The matching logic uses a 4-tier approach:
 * 1. Exact match (case-insensitive)
 * 2. Starts with query (case-insensitive)
 * 3. Contains query (case-insensitive)
 * 4. Query contains any part of the friend's name (for nicknames or partial names)
 */
object FriendNameMatcher {
    
    /**
     * Finds friends whose names match the search query using fuzzy matching.
     * 
     * Matching logic (in priority order):
     * 1. Exact match (case-insensitive)
     * 2. Starts with query (case-insensitive)
     * 3. Contains query (case-insensitive)
     * 4. Query contains any part of the friend's name (case-insensitive)
     * 
     * @param friends List of friends to search
     * @param query The search query
     * @return List of matching friends
     */
    fun findMatches(friends: List<Friend>, query: String): List<Friend> {
        if (query.isBlank()) return emptyList()
        
        val normalizedQuery = query.trim().lowercase()
        
        // Try exact match first
        val exactMatches = friends.filter { matches(it, normalizedQuery, MatchType.EXACT) }
        if (exactMatches.isNotEmpty()) return exactMatches
        
        // Try starts with
        val startsWithMatches = friends.filter { matches(it, normalizedQuery, MatchType.STARTS_WITH) }
        if (startsWithMatches.isNotEmpty()) return startsWithMatches
        
        // Try contains query
        val containsMatches = friends.filter { matches(it, normalizedQuery, MatchType.CONTAINS) }
        if (containsMatches.isNotEmpty()) return containsMatches
        
        // Try query contains any part of friend's name (for nicknames or partial names)
        val queryContainsPart = friends.filter { matches(it, normalizedQuery, MatchType.QUERY_CONTAINS_NAME_PART) }
        if (queryContainsPart.isNotEmpty()) return queryContainsPart
        
        return emptyList()
    }
    
    /**
     * Generic version that finds objects whose names match the search query using fuzzy matching.
     * 
     * This method works with any object that has a name property accessible via the getName lambda.
     * 
     * Matching logic (in priority order):
     * 1. Exact match (case-insensitive)
     * 2. Starts with query (case-insensitive)
     * 3. Contains query (case-insensitive)
     * 4. Query contains any part of the friend's name (case-insensitive)
     * 
     * @param items List of items to search
     * @param query The search query
     * @param getName Lambda to extract the name from each item
     * @return List of matching items
     */
    fun <T> findMatchesBy(items: List<T>, query: String, getName: (T) -> String?): List<T> {
        if (query.isBlank()) return emptyList()
        
        val normalizedQuery = query.trim().lowercase()
        
        // Try exact match first
        val exactMatches = items.filter { matchesByName(getName(it), normalizedQuery, MatchType.EXACT) }
        if (exactMatches.isNotEmpty()) return exactMatches
        
        // Try starts with
        val startsWithMatches = items.filter { matchesByName(getName(it), normalizedQuery, MatchType.STARTS_WITH) }
        if (startsWithMatches.isNotEmpty()) return startsWithMatches
        
        // Try contains query
        val containsMatches = items.filter { matchesByName(getName(it), normalizedQuery, MatchType.CONTAINS) }
        if (containsMatches.isNotEmpty()) return containsMatches
        
        // Try query contains any part of friend's name (for nicknames or partial names)
        val queryContainsPart = items.filter { matchesByName(getName(it), normalizedQuery, MatchType.QUERY_CONTAINS_NAME_PART) }
        if (queryContainsPart.isNotEmpty()) return queryContainsPart
        
        return emptyList()
    }
    
    /**
     * Creates a predicate function that can be used with filter() to find matching friends.
     * 
     * @param query The search query
     * @return A predicate function that returns true if a friend matches the query
     */
    fun createPredicate(query: String): (Friend) -> Boolean {
        if (query.isBlank()) return { false }
        
        val normalizedQuery = query.trim().lowercase()
        
        return { friend ->
            matches(friend, normalizedQuery, MatchType.EXACT) ||
            matches(friend, normalizedQuery, MatchType.STARTS_WITH) ||
            matches(friend, normalizedQuery, MatchType.CONTAINS) ||
            matches(friend, normalizedQuery, MatchType.QUERY_CONTAINS_NAME_PART)
        }
    }
    
    private fun matches(friend: Friend, normalizedQuery: String, matchType: MatchType): Boolean {
        val friendName = friend.name ?: return false
        return matchesByName(friendName, normalizedQuery, matchType)
    }
    
    /**
     * Matches a friend name against a normalized query.
     * 
     * @param friendName The friend's name (unnormalized)
     * @param normalizedQuery The search query (already normalized - lowercase and trimmed)
     * @param matchType The type of match to perform
     * @return true if the name matches the query according to the match type
     */
    private fun matchesByName(friendName: String?, normalizedQuery: String, matchType: MatchType): Boolean {
        val name = friendName?.lowercase()?.trim() ?: return false
        
        return when (matchType) {
            MatchType.EXACT -> name == normalizedQuery
            MatchType.STARTS_WITH -> name.startsWith(normalizedQuery)
            MatchType.CONTAINS -> name.contains(normalizedQuery)
            MatchType.QUERY_CONTAINS_NAME_PART -> {
                val nameParts = name.split(" ")
                nameParts.any { part -> normalizedQuery.contains(part) && part.length > 2 }
            }
        }
    }
    
    private enum class MatchType {
        EXACT,
        STARTS_WITH,
        CONTAINS,
        QUERY_CONTAINS_NAME_PART
    }
}
