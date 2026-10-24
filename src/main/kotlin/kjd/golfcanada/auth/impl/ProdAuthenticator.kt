package kjd.golfcanada.auth.impl

import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton
import kjd.golfcanada.auth.GolfCanadaAuthenticator
import kjd.golfcanada.client.api.AuthApi
import kjd.golfcanada.client.model.AuthToken

/**
 * Production-mode authenticator that forwards the credentials submitted via the OAuth login
 * form directly to the Golf Canada API.
 *
 * This bean is activated when the [GOLF_CANADA_USERNAME_PROPERTY] property is **not** set,
 * i.e., when the skill is operating in standard OAuth Account Linking mode.
 */
@Singleton
@Requires(missingProperty = GOLF_CANADA_USERNAME_PROPERTY)
class ProdAuthenticator(private val authApi: AuthApi) : GolfCanadaAuthenticator {

    override fun authenticate(username: String, password: String, scopes: String): AuthToken =
        authApi.getAuthToken(AuthApi.GrantTypeGetAuthToken.PASSWORD, scopes, username, password)
}
