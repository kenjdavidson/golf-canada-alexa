package kjd.golfcanada.alexa.interceptor

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.dispatcher.request.interceptor.RequestInterceptor
import kjd.golfcanada.client.model.ScoreDefaults
import kjd.golfcanada.client.model.User
import org.slf4j.LoggerFactory
import java.util.Base64

/**
 * Request interceptor that parses the access token as a JWT and extracts user profile information.
 *
 * This interceptor decodes the JWT access token and maps the claims to a [User] object,
 * which is then stored in the session attributes for use by handlers.
 *
 * The JWT payload contains claims that are mapped to the User model properties.
 */
class UserProfileInterceptor : RequestInterceptor {

    private val logger = LoggerFactory.getLogger(UserProfileInterceptor::class.java)

    /**
     * Processes the incoming request to extract user profile from the JWT access token.
     *
     * If an access token is present and a user profile is not already in session attributes,
     * this interceptor will decode the JWT and store the extracted user profile.
     *
     * @param input The handler input containing the request envelope
     */
    override fun process(input: HandlerInput) {
        val accessToken = input.requestEnvelope.context?.system?.user?.accessToken

        if (accessToken.isNullOrBlank()) {
            logger.debug("No access token present, skipping user profile extraction")
            return
        }

        val sessionAttributes = input.attributesManager.sessionAttributes

        if (sessionAttributes.containsKey(USER_SESSION_KEY)) {
            logger.debug("User profile already in session attributes")
            return
        }

        try {
            val user = parseJwtToUser(accessToken)
            sessionAttributes[USER_SESSION_KEY] = user
            input.attributesManager.sessionAttributes = sessionAttributes
            logger.info("User profile extracted from JWT and stored in session")
        } catch (e: Exception) {
            logger.warn("Failed to parse JWT access token: ${e.message}")
        }
    }

    /**
     * Parses a JWT access token and extracts the claims into a [User] object.
     *
     * Note: This method does not verify the JWT signature because the token comes from Alexa's
     * account linking process and has already been validated by Amazon. The token is stored
     * in the Alexa user profile after successful OAuth authentication with Golf Canada.
     *
     * @param jwt The JWT access token string
     * @return A [User] object populated with claims from the JWT
     * @throws IllegalArgumentException if the JWT format is invalid or payload cannot be decoded
     */
    internal fun parseJwtToUser(jwt: String): User {
        val parts = jwt.split(".")
        require(parts.size >= 2) { "Invalid JWT format" }

        val payload = String(Base64.getUrlDecoder().decode(parts[1]))
        val claims = parseJsonToMap(payload)

        return mapClaimsToUser(claims)
    }

    /**
     * Parses a JSON string into a Map.
     *
     * This is a simple JSON parser for the JWT payload claims.
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseJsonToMap(json: String): Map<String, Any> {
        // Using Moshi for JSON parsing since it's already a dependency
        val moshi = com.squareup.moshi.Moshi.Builder().build()
        val adapter = moshi.adapter(Map::class.java)
        return adapter.fromJson(json) as? Map<String, Any> ?: emptyMap()
    }

    /**
     * Maps JWT claims to a [User] object.
     *
     * @param claims The map of JWT claims
     * @return A [User] object populated with the claim values
     */
    internal fun mapClaimsToUser(claims: Map<String, Any>): User {
        val scoreDefaults = ScoreDefaults(
            facilityName = claims[CLAIM_DEFAULT_FACILITY_NAME]?.toString(),
            facilityId = claims[CLAIM_DEFAULT_FACILITY_ID]?.toString()?.toIntOrNull(),
            courseId = claims[CLAIM_DEFAULT_COURSE_ID]?.toString()?.toIntOrNull(),
            teeId = claims[CLAIM_DEFAULT_TEE_ID]?.toString()?.toIntOrNull(),
            nationalAssociation = claims[CLAIM_DEFAULT_NATIONAL_ASSOCIATION]?.toString(),
            postHoleByHole = claims[CLAIM_POST_HOLE_BY_HOLE]?.toString()?.toBooleanOrNull()
        )

        return User(
            id = claims["sub"]?.toString()?.toLongOrNull(),
            username = claims["name"]?.toString(),
            authUserId = claims[CLAIM_AUTH_USER_ID]?.toString()?.toLongOrNull(),
            networkId = claims[CLAIM_NETWORK_ID]?.toString(),
            golfCanadaCardId = claims[CLAIM_GOLF_CANADA_CARD_ID]?.toString(),
            firstName = claims[CLAIM_GIVEN_NAME]?.toString(),
            lastName = claims[CLAIM_SURNAME]?.toString(),
            email = claims[CLAIM_EMAIL]?.toString(),
            handicap = claims[CLAIM_HANDICAP]?.toString(),
            membershipLevel = claims[CLAIM_MEMBERSHIP_LEVEL]?.toString(),
            clubManagementGroupId = claims[CLAIM_CLUB_MANAGEMENT_GROUP_ID]?.toString()?.toIntOrNull(),
            allowScorePosting = claims[CLAIM_ALLOW_SCORE_POSTING]?.toString()?.toBooleanOrNull(),
            termsAndConditionsDate = claims[CLAIM_TERMS_AND_CONDITIONS_DATE]?.toString(),
            scoreDefaults = scoreDefaults
        )
    }

    /**
     * Parses a boolean string case-insensitively.
     * Returns true for "true" (case-insensitive), false for "false" (case-insensitive), null otherwise.
     */
    private fun String.toBooleanOrNull(): Boolean? {
        return when (this.lowercase()) {
            "true" -> true
            "false" -> false
            else -> null
        }
    }

    companion object {
        /**
         * Session attribute key for the user profile.
         */
        const val USER_SESSION_KEY = "user_profile"

        // JWT Claim keys from Golf Canada's identity provider
        private const val CLAIM_AUTH_USER_ID = "http://schemas.golfnet.com/authuserid"
        private const val CLAIM_NETWORK_ID = "http://schemas.golfnet.com/networkid"
        private const val CLAIM_GOLF_CANADA_CARD_ID = "http://schemas.golfnet.com/golfcanadacardid"
        private const val CLAIM_GIVEN_NAME = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname"
        private const val CLAIM_SURNAME = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname"
        private const val CLAIM_EMAIL = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress"
        private const val CLAIM_HANDICAP = "http://schemas.golfnet.com/handicap"
        private const val CLAIM_MEMBERSHIP_LEVEL = "http://schemas.golfnet.com/membershiplevel"
        private const val CLAIM_CLUB_MANAGEMENT_GROUP_ID = "http://schemas.golfnet.com/clubmanagementgroupid"
        private const val CLAIM_ALLOW_SCORE_POSTING = "http://schemas.golfnet.com/allowscoreposting"
        private const val CLAIM_TERMS_AND_CONDITIONS_DATE = "http://schemas.golfnet.com/termsandconditionsdate"
        private const val CLAIM_DEFAULT_FACILITY_NAME = "http://schemas.golfnet.com/defaultfacilityname"
        private const val CLAIM_DEFAULT_FACILITY_ID = "http://schemas.golfnet.com/defaultfacilityid"
        private const val CLAIM_DEFAULT_COURSE_ID = "http://schemas.golfnet.com/defaultcourseid"
        private const val CLAIM_DEFAULT_TEE_ID = "http://schemas.golfnet.com/defaultteeid"
        private const val CLAIM_DEFAULT_NATIONAL_ASSOCIATION = "http://schemas.golfnet.com/defaultnationalassociation"
        private const val CLAIM_POST_HOLE_BY_HOLE = "http://schemas.golfnet.com/postholebyhole"
    }
}
