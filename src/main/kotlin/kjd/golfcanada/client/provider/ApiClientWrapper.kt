package kjd.golfcanada.client.provider

import kjd.golfcanada.client.api.AuthApi
import kjd.golfcanada.client.api.MembersApi
import kjd.golfcanada.client.api.ScoresApi
import org.openapitools.client.infrastructure.ApiClient

/**
 * Wrapper for ApiClient that provides fluent access to API endpoints.
 * 
 * This wrapper encapsulates an ApiClient instance that has been configured with
 * user-specific authentication via an OkHttp interceptor. It provides lazy-loaded
 * properties for accessing API endpoints with a fluent interface.
 * 
 * The wrapper ensures that:
 * - All API instances share the same HTTP client (with connection pooling)
 * - Authentication is handled transparently via the interceptor
 * - No user-specific tokens are stored in static/shared memory
 * - API instances are created once and cached for reuse
 * 
 * Usage:
 * ```
 * val wrapper = apiClientProvider.getClient(accessToken)
 * val friends = wrapper.members.getFriends()
 * val snapshot = wrapper.members.getSnapshot()
 * ```
 * 
 * @param apiClient The authenticated ApiClient instance
 */
class ApiClientWrapper internal constructor(private val apiClient: ApiClient) {
    
    /**
     * Lazy-loaded AuthApi instance.
     * Created once on first access and reused for subsequent calls.
     */
    val auth: AuthApi by lazy {
        AuthApi(apiClient.baseUrl, apiClient.client)
    }
    
    /**
     * Lazy-loaded MembersApi instance.
     * Created once on first access and reused for subsequent calls.
     */
    val members: MembersApi by lazy {
        MembersApi(apiClient.baseUrl, apiClient.client)
    }
    
    /**
     * Lazy-loaded ScoresApi instance.
     * Created once on first access and reused for subsequent calls.
     */
    val scores: ScoresApi by lazy {
        ScoresApi(apiClient.baseUrl, apiClient.client)
    }
    
    /**
     * Creates a new AuthApi instance using the wrapped ApiClient.
     * 
     * @deprecated Use the `auth` property for fluent API access
     * @return A new AuthApi instance configured with user-specific authentication
     */
    @Deprecated("Use the 'auth' property instead", ReplaceWith("auth"))
    fun createAuthApi(): AuthApi {
        return AuthApi(apiClient.baseUrl, apiClient.client)
    }
    
    /**
     * Creates a new MembersApi instance using the wrapped ApiClient.
     * 
     * @deprecated Use the `members` property for fluent API access
     * @return A new MembersApi instance configured with user-specific authentication
     */
    @Deprecated("Use the 'members' property instead", ReplaceWith("members"))
    fun createMembersApi(): MembersApi {
        return MembersApi(apiClient.baseUrl, apiClient.client)
    }
    
    /**
     * Creates a new ScoresApi instance using the wrapped ApiClient.
     * 
     * @deprecated Use the `scores` property for fluent API access
     * @return A new ScoresApi instance configured with user-specific authentication
     */
    @Deprecated("Use the 'scores' property instead", ReplaceWith("scores"))
    fun createScoresApi(): ScoresApi {
        return ScoresApi(apiClient.baseUrl, apiClient.client)
    }
    
    /**
     * Gets the underlying ApiClient instance.
     * 
     * This method is provided for advanced use cases where direct access to the
     * ApiClient is needed. In most cases, you should use the lazy-loaded properties
     * to access API instances.
     * 
     * @return The wrapped ApiClient instance
     */
    fun getApiClient(): ApiClient = apiClient
}
