package kjd.golfcanada.alexa.exception

/**
 * Exception thrown when a requested facility cannot be found.
 *
 * This exception is used by the course handicap handler to indicate that
 * a facility search returned no results or that a user has no default facility configured.
 */
class NoFacilityFoundException(
    val facilityName: String?,
    message: String = "Facility not found: ${facilityName ?: "default facility"}",
    cause: Throwable? = null
) : RuntimeException(message, cause)
