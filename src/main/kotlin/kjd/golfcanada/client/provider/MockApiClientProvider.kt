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
     * IMPORTANT: This is currently a PLACEHOLDER implementation that returns a standard
     * ApiClientWrapper backed by real API calls. The mock infrastructure is in place
     * (MockResourceLoader, JSON files, etc.) but cannot be fully utilized because the
     * OpenAPI-generated API classes are final and cannot be extended.
     * 
     * To implement full mocking, one of these approaches is needed:
     * 1. HTTP interceptor at OkHttp level to intercept requests and return mock responses
     * 2. Local mock server (e.g., WireMock) that returns predefined responses
     * 3. Modify OpenAPI generator to create open classes or interfaces
     * 
     * Until then, setting MOCK_API=true will log but still make real API calls.
     * 
     * @param accessToken The user-specific OAuth access token
     * @return A new ApiClientWrapper (currently using real API, not mocked)
     */
    override fun getClient(accessToken: String): ApiClientWrapper {
        logger.warn("MockApiClientProvider is enabled but NOT FULLY IMPLEMENTED")
        logger.warn("This will still make REAL API calls - mocking requires HTTP-level interception")
        logger.warn("See MOCKING_FRAMEWORK.md for implementation details")
        
        // TODO: Implement HTTP-level mocking via OkHttp interceptor
        // For now, return a standard provider that makes real API calls
        val baseProvider = ApiClientProvider()
        return baseProvider.getClient(accessToken)
    }
}
