package kjd.golfcanada.client.model

import org.openapitools.client.infrastructure.Serializer

const val TOKEN_DELIMITER = "#"

fun AuthToken.code(): String =
    "${hashCode()}".replace("-","@")

fun AuthToken.toJson(): String =
    Serializer.moshi.adapter(AuthToken::class.java).toJson(this)

/**
 * Creates a copy of this AuthToken with the access_token concatenated with id_token.
 * Format: access_token#id_token
 */
fun AuthToken.withConcatenatedToken(): AuthToken =
    this.copy(
        accessToken = if (idToken.isNullOrBlank()) {
            accessToken
        } else {
            "$accessToken$TOKEN_DELIMITER$idToken"
        }
    )

/**
 * Extracts the actual access token from a concatenated token string.
 * If the token contains the delimiter, returns the part before it.
 * Otherwise, returns the token as-is.
 */
fun String.extractAccessToken(): String =
    this.split(TOKEN_DELIMITER).first()

/**
 * Extracts the id token from a concatenated token string.
 * If the token contains the delimiter, returns the part after it.
 * Otherwise, returns null.
 */
fun String.extractIdToken(): String? =
    this.split(TOKEN_DELIMITER, limit = 2).takeIf { it.size > 1 }?.get(1)
