package kjd.golfcanada.alexa

/**
 * Enum representing all custom Golf Canada Alexa skill intents.
 * These intent names follow the convention GOLFCANADA.IntentName to match the model.json.
 */
enum class IntentName(val value: String) {
    HANDICAP("GOLFCANADA.HandicapIntent"),
    PLAYER_PROFILE_MEMBERSHIP("GOLFCANADA.PlayerProfileMembershipIntent"),
    PLAYER_PROFILE_HISTORY("GOLFCANADA.PlayerProfileHistoryIntent"),
    FAVORITE_PLAYER_HISTORY("GOLFCANADA.FavoritePlayerHistoryIntent"),
    ADD_SCORECARD("GOLFCANADA.AddScorecardIntent");
    
    override fun toString(): String = value
}
