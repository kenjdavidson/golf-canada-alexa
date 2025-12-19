package kjd.golfcanada.client.provider

import kjd.golfcanada.client.provider.mock.MockHttpInterceptor
import okhttp3.OkHttpClient
import org.openapitools.client.infrastructure.ApiClient
import org.slf4j.LoggerFactory

/**
 * Mock implementation of IApiClientProvider that returns data from local JSON resource files.
 * 
 * This provider is used for local development and integration testing without requiring
 * a live connection to the Golf Canada API. It loads mock responses from JSON files
 * stored in the resources/client directory following the naming convention:
 * - /resources/client/{serviceName}/{methodName}_{parameter}.json
 * - /resources/client/{serviceName}/{methodName}.json (fallback)
 * 
 * The mock provider uses an OkHttp interceptor to intercept HTTP requests and return
 * mock responses from JSON resource files instead of making real network calls.
 * 
 * Usage:
 * Set the MOCK_API environment variable to "true" to enable mock mode:
 * ```
 * MOCK_API=true sam local invoke
 * ```
 */
class MockApiClientProvider : IApiClientProvider {
    private val logger = LoggerFactory.getLogger(MockApiClientProvider::class.java)
    
    /**
     * The base mocked ApiClient with HTTP interceptor for returning mock responses.
     * This client uses a MockHttpInterceptor to intercept all HTTP requests and return
     * mock data from JSON resource files instead of making real network calls.
     */
    private val baseMockApiClient: ApiClient = createMockApiClient()
    
    /**
     * Creates a mock ApiClient that uses an HTTP interceptor to return mock responses.
     * The interceptor parses the request URL and loads the corresponding JSON resource file.
     */
    private fun createMockApiClient(): ApiClient {
        val baseUrl = System.getProperties().getProperty(
            ApiClient.baseUrlKey, 
            "https://scg.golfcanada.ca"
        )
        
        logger.info("Creating MockApiClientProvider with mock HTTP interceptor")
        
        // Build OkHttpClient with mock interceptor
        val mockHttpClient = OkHttpClient.Builder()
            .addInterceptor(MockHttpInterceptor())
            .build()
        
        return ApiClient(baseUrl, mockHttpClient)
    }
    
    /**
     * Gets a mock API client wrapper that returns data from JSON resource files.
     * 
     * This creates a new ApiClientWrapper based on the mock base client. All HTTP requests
     * made through this client will be intercepted and mock responses will be returned
     * from JSON files in src/main/resources/client/.
     * 
     * @param accessToken The user-specific OAuth access token (logged but not used for authentication in mock mode)
     * @return A new ApiClientWrapper with mock HTTP interceptor
     */
    override fun getClient(accessToken: String): ApiClientWrapper {
        logger.info("Creating mock API client wrapper (access token will be logged but not used)")
        
        // Create a new client builder based on the mock base client
        // The access token is not used in mock mode, but we maintain the same interface
        val mockClient = baseMockApiClient.newBuilder()
            .addInterceptor { chain ->
                val request = chain.request()
                logger.debug("Mock request: ${request.method} ${request.url}")
                chain.proceed(request)
            }
            .build()
        
        return ApiClientWrapper(mockClient)
    }
    
    /**
     * Extension function to create a new ApiClient builder based on an existing ApiClient.
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
        fun addInterceptor(interceptor: okhttp3.Interceptor): ApiClientBuilder {
            httpClientBuilder.addInterceptor(interceptor)
            return this
        }
        
        fun build(): ApiClient {
            return ApiClient(baseUrl, httpClientBuilder.build())
        }
    }
}
