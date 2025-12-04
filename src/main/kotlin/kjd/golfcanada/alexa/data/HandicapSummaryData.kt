package kjd.golfcanada.alexa.data

import kjd.golfcanada.client.model.HandicapCalculation

/**
 * A lightweight summary of a user's handicap information for caching in session attributes.
 * 
 * This model contains only the essential fields from [HandicapCalculation] that are needed
 * for the Alexa skill responses, reducing the size of data stored in session attributes.
 * 
 * @property name The full name of the golfer
 * @property email The email address of the golfer
 * @property lowValue The low handicap index value
 * @property handicap The current handicap index
 * @property averageDifferential The average differential (avgDiff)
 * @property cachedAt The timestamp (in milliseconds) when this data was cached
 */
data class HandicapSummaryData(
    val name: String?,
    val email: String?,
    val lowValue: Double?,
    val handicap: String?,
    val averageDifferential: Double?,
    val cachedAt: Long = System.currentTimeMillis()
) {
    /**
     * Converts this HandicapSummaryData to a response data map for template rendering.
     * 
     * @return An immutable map containing non-null fields for use in response templates
     */
    fun toResponseData(): Map<String, Any> {
        val dataModel = mutableMapOf<String, Any>()
        name?.let { dataModel["name"] = it }
        handicap?.let { dataModel["handicap"] = it }
        lowValue?.let { dataModel["lowValue"] = it }
        averageDifferential?.let { dataModel["averageDifferential"] = it }
        return dataModel.toMap()
    }

    companion object {
        /**
         * Creates a HandicapSummaryData instance from a HandicapCalculation DTO.
         * 
         * @param dto The HandicapCalculation object from the API
         * @return A HandicapSummaryData instance with data extracted from the DTO
         */
        fun fromDTO(dto: HandicapCalculation): HandicapSummaryData {
            return HandicapSummaryData(
                name = dto.name,
                email = dto.email,
                lowValue = dto.lowValue,
                handicap = dto.handicap,
                averageDifferential = dto.avgDiff,
                cachedAt = System.currentTimeMillis()
            )
        }
    }
}
