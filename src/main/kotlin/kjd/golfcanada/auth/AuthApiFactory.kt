package kjd.golfcanada.auth

import io.micronaut.context.annotation.Factory
import jakarta.inject.Singleton
import kjd.golfcanada.client.api.AuthApi

/**
 * Micronaut factory that exposes the generated [AuthApi] as a singleton bean so it can be
 * injected into [AuthenticationHandler] and the authenticator implementations.
 */
@Factory
class AuthApiFactory {

    @Singleton
    fun authApi(): AuthApi = AuthApi()
}
