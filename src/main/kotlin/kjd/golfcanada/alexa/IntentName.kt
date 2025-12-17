package kjd.golfcanada.alexa

/**
 * Enum representing all custom Golf Canada Alexa skill intents.
 * These intent names follow the convention GOLFCANADA.[Name] to match the model.json.
 */
enum class IntentName(val value: String) {
    HANDICAP("GOLFCANADA.Handicap"),
    PLAYER_PROFILE_MEMBERSHIP("GOLFCANADA.PlayerProfileMembership"),
    PLAYER_PROFILE_HISTORY("GOLFCANADA.PlayerProfileHistory"),
    FAVORITE_PLAYER_HISTORY("GOLFCANADA.FavoritePlayerHistory"),
    ADD_SCORECARD("GOLFCANADA.AddScorecard"),
    FAVORITE_COURSES("GOLFCANADA.FavoriteCourses"),
    COURSE_HANDICAP("GOLFCANADA.CourseHandicap");
    
    override fun toString(): String = value
}
