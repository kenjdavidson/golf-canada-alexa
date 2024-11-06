package kjd.golfcanada.auth.impl

import kjd.golfcanada.auth.AuthTokenKey
import kjd.golfcanada.auth.TokenRepository
import kjd.golfcanada.client.model.AuthToken
import org.apache.commons.lang3.mutable.Mutable
import java.util.concurrent.ConcurrentHashMap

class TokenRepositoryMapImpl(
    private val store: MutableMap<AuthTokenKey, AuthToken> = ConcurrentHashMap<AuthTokenKey, AuthToken>()
): TokenRepository<AuthTokenKey, AuthToken> {
    override fun store(key: AuthTokenKey, token: AuthToken) {
        store[key] = token
    }

    override fun get(key: AuthTokenKey): AuthToken? {
        return store[key]
    }
}