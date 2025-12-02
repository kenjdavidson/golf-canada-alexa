package kjd.golfcanada.alexa.data

/**
 * Represents a subset of user profile information stored in session attributes.
 *
 * This data class contains only the user information that is actively used by handlers,
 * rather than storing the full User object from the API client model.
 *
 * @property firstName The user's first name
 * @property lastName The user's last name
 * @property membershipLevel The user's membership level (e.g., "Gold", "Silver")
 * @property golfCanadaCardId The user's Golf Canada card identifier
 * @property expirationDate The expiration date of the user's membership
 * @property facilityName The name of the user's default facility
 * @property postHoleByHole Whether the user posts scores hole-by-hole
 */
data class UserProfileSession(
    val firstName: String? = null,
    val lastName: String? = null,
    val membershipLevel: String? = null,
    val golfCanadaCardId: String? = null,
    val expirationDate: String? = null,
    val facilityName: String? = null,
    val postHoleByHole: Boolean? = null
)
