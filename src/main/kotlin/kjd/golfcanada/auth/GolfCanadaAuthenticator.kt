package kjd.golfcanada.auth

import kjd.golfcanada.client.model.AuthToken

/**
 * Abstracts Golf Canada credential retrieval so the authentication Lambda can run in either
 * production (OAuth) mode or dev (static-credentials) mode via Micronaut conditional beans.
 */
interface GolfCanadaAuthenticator {

    /**
     * Authenticates against the Golf Canada API and returns an [AuthToken].
     *
     * @param username Golf Canada username (may be ignored by the implementation)
     * @param password Golf Canada password (may be ignored by the implementation)
     * @param scopes OAuth scopes to request
     * @return the resulting [AuthToken]
     */
    fun authenticate(username: String, password: String, scopes: String): AuthToken
}
