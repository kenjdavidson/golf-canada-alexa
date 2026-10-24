package kjd.golfcanada.auth.impl

import jakarta.inject.Singleton
import kjd.golfcanada.auth.AuthTokenKey
import kjd.golfcanada.auth.TokenRepository
import kjd.golfcanada.client.model.AuthToken
import java.util.concurrent.ConcurrentHashMap

@Singleton
class TokenRepositoryMapImpl : TokenRepository<AuthTokenKey, AuthToken> {
    private val store: MutableMap<AuthTokenKey, AuthToken> = ConcurrentHashMap()

    override fun store(key: AuthTokenKey, token: AuthToken) {
        store[key] = token
    }

    override fun get(key: AuthTokenKey): AuthToken? {
        return store.remove(key)
    }
}