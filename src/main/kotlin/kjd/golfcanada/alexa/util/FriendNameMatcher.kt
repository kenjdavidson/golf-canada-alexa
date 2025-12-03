package kjd.golfcanada.alexa.util

import kjd.golfcanada.client.model.Friend

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
        val friendName = friend.name?.lowercase()?.trim() ?: return false
        
        return when (matchType) {
            MatchType.EXACT -> friendName == normalizedQuery
            MatchType.STARTS_WITH -> friendName.startsWith(normalizedQuery)
            MatchType.CONTAINS -> friendName.contains(normalizedQuery)
            MatchType.QUERY_CONTAINS_NAME_PART -> {
                val nameParts = friendName.split(" ")
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
