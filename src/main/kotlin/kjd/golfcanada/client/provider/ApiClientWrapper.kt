package kjd.golfcanada.client.provider

import kjd.golfcanada.client.api.AuthApi
import kjd.golfcanada.client.api.MembersApi
import kjd.golfcanada.client.api.ScoresApi
import org.openapitools.client.infrastructure.ApiClient

/**
 * Wrapper for ApiClient that provides convenient access to API endpoints.
 * 
 * This wrapper encapsulates an ApiClient instance that has been configured with
 * user-specific authentication via an OkHttp interceptor. It provides factory methods
 * to create API endpoint instances that share the same authenticated HTTP client.
 * 
 * The wrapper ensures that:
 * - All API instances share the same HTTP client (with connection pooling)
 * - Authentication is handled transparently via the interceptor
 * - No user-specific tokens are stored in static/shared memory
 * 
 * @param apiClient The authenticated ApiClient instance
 */
class ApiClientWrapper internal constructor(private val apiClient: ApiClient) {
    
    /**
     * Creates a new AuthApi instance using the wrapped ApiClient.
     * 
     * @return A new AuthApi instance configured with user-specific authentication
     */
    fun createAuthApi(): AuthApi {
        return AuthApi(apiClient.baseUrl, apiClient.client)
    }
    
    /**
     * Creates a new MembersApi instance using the wrapped ApiClient.
     * 
     * @return A new MembersApi instance configured with user-specific authentication
     */
    fun createMembersApi(): MembersApi {
        return MembersApi(apiClient.baseUrl, apiClient.client)
    }
    
    /**
     * Creates a new ScoresApi instance using the wrapped ApiClient.
     * 
     * @return A new ScoresApi instance configured with user-specific authentication
     */
    fun createScoresApi(): ScoresApi {
        return ScoresApi(apiClient.baseUrl, apiClient.client)
    }
    
    /**
     * Gets the underlying ApiClient instance.
     * 
     * This method is provided for advanced use cases where direct access to the
     * ApiClient is needed. In most cases, you should use the factory methods above
     * to create API instances.
     * 
     * @return The wrapped ApiClient instance
     */
    fun getApiClient(): ApiClient = apiClient
}
