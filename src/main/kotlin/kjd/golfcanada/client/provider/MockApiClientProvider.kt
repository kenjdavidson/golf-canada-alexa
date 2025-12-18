package kjd.golfcanada.client.provider

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
 * The mock provider uses the same object mapper configuration as the production ApiClient
 * to ensure date formats and snake_case/camelCase mappings are identical.
 * 
 * Usage:
 * Set the MOCK_API environment variable to "true" to enable mock mode:
 * ```
 * MOCK_API=true sam local invoke
 * ```
 * 
 * Note: Since the generated API classes are final and cannot be extended, the mock
 * implementation returns a regular ApiClientWrapper. The actual mocking would need to be
 * implemented at the HTTP client level or by using a mock server. This serves as a placeholder
 * for future implementation.
 */
class MockApiClientProvider : IApiClientProvider {
    private val logger = LoggerFactory.getLogger(MockApiClientProvider::class.java)
    
    /**
     * Gets a mock API client wrapper.
     * 
     * Note: Currently returns a standard ApiClientWrapper. Full mock implementation
     * with JSON resource loading requires either making the generated API classes open
     * or implementing mocking at the HTTP client level.
     * 
     * @param accessToken The user-specific OAuth access token
     * @return A new ApiClientWrapper
     */
    override fun getClient(accessToken: String): ApiClientWrapper {
        logger.info("Mock API provider is enabled but not fully implemented yet")
        logger.info("Returning standard API client - actual mocking TODO")
        
        // For now, just use a regular provider
        // Full implementation would require either:
        // 1. Making generated API classes open
        // 2. Implementing mock at OkHttp interceptor level
        // 3. Using a local mock server
        val baseProvider = ApiClientProvider()
        return baseProvider.getClient(accessToken)
    }
}
