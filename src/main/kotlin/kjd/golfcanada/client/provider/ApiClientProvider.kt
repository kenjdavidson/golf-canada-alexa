package kjd.golfcanada.client.provider

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import org.openapitools.client.infrastructure.ApiClient

/**
 * Extension function to create a new ApiClient builder based on an existing ApiClient.
 * This allows cloning the ApiClient with the same base URL and HTTP client configuration.
 */
private fun ApiClient.newBuilder(): ApiClientBuilder {
    return ApiClientBuilder(this.baseUrl, this.client.newBuilder())
}

/**
 * Builder for creating new ApiClient instances based on an existing client.
 */
private class ApiClientBuilder(
    private val baseUrl: String,
    private val httpClientBuilder: OkHttpClient.Builder
) {
    fun addInterceptor(interceptor: Interceptor): ApiClientBuilder {
        httpClientBuilder.addInterceptor(interceptor)
        return this
    }
    
    fun build(): ApiClient {
        return ApiClient(baseUrl, httpClientBuilder.build())
    }
}

/**
 * Provider for API client instances.
 * 
 * This class ensures that:
 * 1. The expensive HTTP client resources (connection pools, etc.) are initialized only once per instance
 * 2. User-specific access tokens are never stored in static memory or shared between requests
 * 3. Each request gets its own client instance with a request-specific AuthInterceptor
 * 
 * Instances of this class should be created once (e.g., in the Skill initialization) and passed
 * as a dependency to classes that need API client access. This makes it effectively a singleton
 * while allowing for better testability and dependency injection.
 */
class ApiClientProvider : IApiClientProvider {
    
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
    override fun getClient(accessToken: String): ApiClientWrapper {
        // Create authenticated ApiClient by cloning the base client and adding the interceptor
        // Using newBuilder() on the base ApiClient ensures we reuse the same base URL,
        // connection pool, and configuration, while adding request-specific authentication
        val authenticatedApiClient = baseApiClient.newBuilder()
            .addInterceptor(AuthInterceptor(accessToken))
            .build()
        
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
}
