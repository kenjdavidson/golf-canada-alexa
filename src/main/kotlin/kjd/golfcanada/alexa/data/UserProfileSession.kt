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
) {
    /**
     * Converts the user profile to a map suitable for template response data.
     * Only includes non-null values.
     *
     * @return A map containing the non-null profile fields
     */
    fun toResponseData(): Map<String, Any> {
        val dataModel = mutableMapOf<String, Any>()
        firstName?.let { dataModel["firstName"] = it }
        lastName?.let { dataModel["lastName"] = it }
        membershipLevel?.let { dataModel["membershipLevel"] = it }
        golfCanadaCardId?.let { dataModel["golfCanadaCardId"] = it }
        expirationDate?.let { dataModel["expirationDate"] = it }
        postHoleByHole?.let { dataModel["postHoleByHole"] = it }
        facilityName?.let { dataModel["facilityName"] = it }
        return dataModel
    }
}
