package kjd.golfcanada.client.provider

/**
 * Interface for providing API client instances.
 * 
 * This interface allows for different implementations of the API client provider,
 * such as the production ApiClientProvider that makes real HTTP calls, and
 * MockApiClientProvider that returns data from local JSON resource files.
 * 
 * Implementations should ensure:
 * 1. HTTP client resources (connection pools, etc.) are initialized efficiently
 * 2. User-specific access tokens are never stored in static memory or shared between requests
 * 3. Each request gets its own client instance with request-specific authentication
 */
interface IApiClientProvider {
    
    /**
     * Gets a new API client wrapper with user-specific authentication.
     * 
     * @param accessToken The user-specific OAuth access token for this request
     * @return A new ApiClientWrapper with request-specific authentication
     */
    fun getClient(accessToken: String): ApiClientWrapper
}
