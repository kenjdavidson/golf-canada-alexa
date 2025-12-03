package kjd.golfcanada.client.provider

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import org.openapitools.client.infrastructure.ApiClient

/**
 * Thread-safe singleton provider for API client instances.
 * 
 * This class ensures that:
 * 1. The expensive HTTP client resources (connection pools, etc.) are initialized only once per Lambda container
 * 2. User-specific access tokens are never stored in static memory or shared between requests
 * 3. Each request gets its own client instance with a request-specific AuthInterceptor
 * 
 * The singleton pattern is implemented using the Instance Holder Idiom (also known as 
 * Initialization-on-demand holder idiom), which is thread-safe and efficient without requiring 
 * explicit synchronization.
 */
class ApiClientProvider private constructor() {
    
    /**
     * The base ApiClient with shared HTTP resources.
     * This client is initialized once and reused across all requests.
     * It does NOT contain any user-specific authentication information.
     */
    private val baseApiClient: ApiClient = createBaseApiClient()
    
    /**
     * Creates the base ApiClient with optimized HTTP client configuration.
     * This method is called only once during the lifetime of the Lambda container.
     */
    private fun createBaseApiClient(): ApiClient {
        val baseUrl = System.getProperties().getProperty(
            ApiClient.baseUrlKey, 
            "https://scg.golfcanada.ca"
        )
        
        // Build OkHttpClient with connection pooling for efficient reuse
        val httpClient = OkHttpClient.Builder()
            // Connection pool allows reusing connections across requests
            // This is safe because connections don't carry user-specific state
            .build()
        
        return ApiClient(baseUrl, httpClient)
    }
    
    /**
     * Gets a new API client wrapper with user-specific authentication.
     * 
     * This method creates a new client instance based on the shared base client,
     * but injects a request-specific AuthInterceptor configured with the provided
     * access token. This ensures that:
     * - The expensive HTTP client resources are reused (connection pools, etc.)
     * - User-specific tokens are never stored in static/shared memory
     * - Each request has its own isolated authentication context
     * 
     * @param accessToken The user-specific OAuth access token for this request
     * @return A new ApiClientWrapper with request-specific authentication
     */
    fun getClient(accessToken: String): ApiClientWrapper {
        // Clone the base HTTP client and add request-specific interceptor
        val authenticatedHttpClient = baseApiClient.client.newBuilder()
            .addInterceptor(AuthInterceptor(accessToken))
            .build()
        
        // Create a new ApiClient with the authenticated HTTP client
        val authenticatedApiClient = ApiClient(baseApiClient.baseUrl, authenticatedHttpClient)
        
        return ApiClientWrapper(authenticatedApiClient)
    }
    
    /**
     * OkHttp interceptor that injects the Authorization header with the access token.
     * 
     * This interceptor is created new for every request and contains the user-specific
     * access token. It adds the "Authorization: Bearer <token>" header to all outgoing
     * requests.
     * 
     * @param accessToken The OAuth access token to inject into requests
     */
    private class AuthInterceptor(private val accessToken: String) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val originalRequest = chain.request()
            
            // Add Authorization header with Bearer token
            val authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
            
            return chain.proceed(authenticatedRequest)
        }
    }
    
    companion object {
        /**
         * Gets the singleton instance of ApiClientProvider.
         * 
         * This uses the Instance Holder Idiom for thread-safe lazy initialization.
         * The JVM guarantees that the InstanceHolder class is loaded and initialized
         * only when it's first accessed, and the JVM's class initialization guarantees
         * ensure thread safety without explicit synchronization.
         * 
         * @return The singleton ApiClientProvider instance
         */
        @JvmStatic
        fun getInstance(): ApiClientProvider = InstanceHolder.INSTANCE
        
        /**
         * Holder class for the singleton instance.
         * 
         * This inner static class is loaded only when getInstance() is called for the first time,
         * and the JVM guarantees thread-safe initialization of the INSTANCE field.
         */
        private object InstanceHolder {
            val INSTANCE = ApiClientProvider()
        }
    }
}
