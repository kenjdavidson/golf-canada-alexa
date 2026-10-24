package kjd.golfcanada.auth.impl

import io.micronaut.context.annotation.Requires
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import kjd.golfcanada.auth.GolfCanadaAuthenticator
import kjd.golfcanada.client.api.AuthApi
import kjd.golfcanada.client.model.AuthToken

/**
 * Development-mode authenticator that uses statically configured Golf Canada credentials
 * instead of credentials submitted via the OAuth login form.
 *
 * This bean is activated when the [GOLF_CANADA_USERNAME_PROPERTY] property is set, which
 * happens automatically when the [GOLF_CANADA_USERNAME_ENV] environment variable is present.
 */
@Singleton
@Requires(property = GOLF_CANADA_USERNAME_PROPERTY)
class DevAuthenticator(
    @Value("\${$GOLF_CANADA_USERNAME_PROPERTY}") private val username: String,
    @Value("\${golf.canada.password}") private val password: String,
    private val authApi: AuthApi
) : GolfCanadaAuthenticator {

    override fun authenticate(username: String, password: String, scopes: String): AuthToken =
        authApi.getAuthToken(AuthApi.GrantTypeGetAuthToken.PASSWORD, scopes, this.username, this.password)
}

/** Micronaut property name that corresponds to the [GOLF_CANADA_USERNAME_ENV] environment variable. */
const val GOLF_CANADA_USERNAME_PROPERTY = "golf.canada.username"

/** Environment variable name for the static Golf Canada username. */
const val GOLF_CANADA_USERNAME_ENV = "GOLF_CANADA_USERNAME"
