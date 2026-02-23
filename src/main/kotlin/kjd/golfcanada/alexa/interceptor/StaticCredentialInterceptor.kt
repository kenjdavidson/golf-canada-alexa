package kjd.golfcanada.alexa.interceptor

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.dispatcher.request.interceptor.RequestInterceptor
import kjd.golfcanada.client.api.AuthApi
import kjd.golfcanada.client.model.AuthToken
import kjd.golfcanada.client.model.withConcatenatedToken
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

/**
 * Request interceptor that provides authentication using statically configured credentials.
 *
 * This interceptor supports a "single-user" deployment mode where Golf Canada credentials
 * (username/password) are stored in Lambda environment variables (deployed via GitHub Secrets)
 * instead of requiring Alexa Account Linking and the OAuth authentication Lambda.
 *
 * When [STATIC_USERNAME_ENV] and [STATIC_PASSWORD_ENV] environment variables are set, this
 * interceptor will authenticate with Golf Canada directly and store the resulting access token
 * in request attributes under [STATIC_TOKEN_KEY]. Downstream interceptors and handlers
 * can retrieve this token when no Alexa account-linking token is present.
 *
 * The token is cached in memory with a TTL based on `expires_in` to avoid re-authenticating
 * on every request. The cache is per Lambda container instance, so cold starts will
 * re-authenticate.
 *
 * **Security Considerations:**
 * - Credentials are stored in Lambda environment variables (set from GitHub Secrets at deploy time)
 * - The access token is cached in-memory and is not persisted between Lambda container instances
 * - This mode bypasses Alexa Account Linking, so the skill must be kept private/development-only
 * - See docs/PRIVATE_SKILL_SPIKE.md for a full discussion of risks and alternatives
 *
 * @param username Golf Canada username (from [STATIC_USERNAME_ENV] env var)
 * @param password Golf Canada password (from [STATIC_PASSWORD_ENV] env var)
 * @param authApi Golf Canada authentication API client
 */
class StaticCredentialInterceptor(
    private val username: String?,
    private val password: String?,
    private val authApi: AuthApi = AuthApi()
) : RequestInterceptor {

    private val logger = LoggerFactory.getLogger(StaticCredentialInterceptor::class.java)

    private data class CachedToken(val concatenatedToken: String, val expiresAt: Instant)

    private val cachedToken = AtomicReference<CachedToken?>(null)

    /**
     * Returns true when static credentials are configured (both username and password set).
     */
    val isEnabled: Boolean
        get() = !username.isNullOrBlank() && !password.isNullOrBlank()

    /**
     * Authenticates with static credentials and stores the resulting access token
     * in request attributes for use by downstream interceptors and handlers.
     *
     * Has no effect when static credentials are not configured.
     */
    override fun process(input: HandlerInput) {
        if (!isEnabled) {
            return
        }

        val token = getValidToken() ?: return

        val requestAttributes = input.attributesManager.requestAttributes ?: mutableMapOf()
        requestAttributes[STATIC_TOKEN_KEY] = token
        input.attributesManager.requestAttributes = requestAttributes

        logger.debug("Static access token stored in request attributes")
    }

    /**
     * Returns a valid (non-expired) concatenated access token, authenticating if necessary.
     *
     * @return the concatenated token string, or null if authentication fails or credentials are not configured
     */
    internal fun getValidToken(): String? {
        if (!isEnabled) return null

        val cached = cachedToken.get()
        if (cached != null && Instant.now().isBefore(cached.expiresAt)) {
            logger.debug("Using cached static access token")
            return cached.concatenatedToken
        }

        return try {
            logger.info("Authenticating with static Golf Canada credentials")
            val authToken = authApi.getAuthToken(
                AuthApi.GrantTypeGetAuthToken.PASSWORD,
                STATIC_AUTH_SCOPES,
                username = username ?: return null,
                password = password ?: return null
            )
            cacheToken(authToken)
        } catch (e: Exception) {
            logger.error("Failed to authenticate with static credentials: ${e.message}")
            null
        }
    }

    private fun cacheToken(authToken: AuthToken): String {
        val expiresInSeconds = authToken.expiresIn?.toLong() ?: DEFAULT_EXPIRY_SECONDS
        val expiresAt = Instant.now().plusSeconds(expiresInSeconds - EXPIRY_BUFFER_SECONDS)
        val concatenatedToken = authToken.withConcatenatedToken().accessToken ?: ""
        cachedToken.set(CachedToken(concatenatedToken, expiresAt))
        logger.info("Cached static access token, expires at: $expiresAt")
        return concatenatedToken
    }

    companion object {
        /** Request attribute key under which the static access token is stored. */
        const val STATIC_TOKEN_KEY = "static_access_token"

        /** Environment variable name for the Golf Canada username in static mode. */
        const val STATIC_USERNAME_ENV = "GOLF_CANADA_USERNAME"

        /** Environment variable name for the Golf Canada password in static mode. */
        const val STATIC_PASSWORD_ENV = "GOLF_CANADA_PASSWORD"

        /**
         * OAuth scopes required for full skill functionality.
         * Matches AuthenticationHandler.DEFAULT_SCOPES for consistency.
         */
        const val STATIC_AUTH_SCOPES = "address email offline_access openid phone profile roles"

        private const val DEFAULT_EXPIRY_SECONDS = 3600L
        private const val EXPIRY_BUFFER_SECONDS = 60L

        /**
         * Creates a [StaticCredentialInterceptor] from environment variables.
         */
        fun fromEnvironment(authApi: AuthApi = AuthApi()): StaticCredentialInterceptor =
            StaticCredentialInterceptor(
                username = System.getenv(STATIC_USERNAME_ENV),
                password = System.getenv(STATIC_PASSWORD_ENV),
                authApi = authApi
            )
    }
}
