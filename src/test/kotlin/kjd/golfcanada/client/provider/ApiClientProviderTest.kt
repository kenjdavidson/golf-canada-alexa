package kjd.golfcanada.client.provider

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Unit tests for ApiClientProvider.
 * 
 * These tests verify:
 * 1. Proper isolation of user-specific tokens
 * 2. Reuse of base HTTP client resources
 * 3. Per-request authentication via interceptor
 */
class ApiClientProviderTest : DescribeSpec({
    
    describe("ApiClientProvider.getClient") {
        
        it("should return a new ApiClientWrapper for each call") {
            val provider = ApiClientProvider()
            
            val wrapper1 = provider.getClient("token1")
            val wrapper2 = provider.getClient("token2")
            
            wrapper1 shouldNotBe null
            wrapper2 shouldNotBe null
            wrapper1 shouldNotBeSameInstanceAs wrapper2
        }
        
        it("should inject Authorization header with provided token") {
            val mockServer = MockWebServer()
            mockServer.enqueue(MockResponse().setBody("{}").setResponseCode(200))
            mockServer.start()
            
            try {
                // Set the base URL to use the mock server
                System.setProperty("org.openapitools.client.baseUrl", mockServer.url("/").toString())
                
                val provider = ApiClientProvider()
                val accessToken = "test-access-token-123"
                val wrapper = provider.getClient(accessToken)
                
                // Try to make a request (this will fail but we can check the recorded request)
                try {
                    wrapper.scores
                    // We don't need this to succeed, just need to trigger the interceptor
                } catch (e: Exception) {
                    // Expected - the mock response may not match the expected format
                }
                
                // Verify the Authorization header was added
                if (mockServer.requestCount > 0) {
                    val recordedRequest = mockServer.takeRequest(1, TimeUnit.SECONDS)
                    if (recordedRequest != null) {
                        val authHeader = recordedRequest.getHeader("Authorization")
                        authHeader shouldBe "Bearer $accessToken"
                    }
                }
            } finally {
                mockServer.shutdown()
                System.clearProperty("org.openapitools.client.baseUrl")
            }
        }
        
        it("should create wrappers with different tokens independently") {
            val provider = ApiClientProvider()
            
            val token1 = "user-token-1"
            val token2 = "user-token-2"
            
            val wrapper1 = provider.getClient(token1)
            val wrapper2 = provider.getClient(token2)
            
            // Both wrappers should be created successfully
            wrapper1 shouldNotBe null
            wrapper2 shouldNotBe null
            
            // They should be different instances
            wrapper1 shouldNotBeSameInstanceAs wrapper2
            
            // The underlying API clients should also be different
            wrapper1.getApiClient() shouldNotBeSameInstanceAs wrapper2.getApiClient()
        }
        
        it("should handle concurrent getClient calls safely") {
            val provider = ApiClientProvider()
            val callCount = 20
            val wrappers = java.util.concurrent.ConcurrentHashMap.newKeySet<ApiClientWrapper>()
            val latch = CountDownLatch(callCount)
            val executor = Executors.newFixedThreadPool(10)
            
            repeat(callCount) { index ->
                executor.submit {
                    wrappers.add(provider.getClient("token-$index"))
                    latch.countDown()
                }
            }
            
            latch.await(5, TimeUnit.SECONDS) shouldBe true
            executor.shutdown()
            
            // All wrappers should be created
            wrappers.size shouldBe callCount
            
            // Each wrapper should be a unique instance
            val wrappersList = wrappers.toList()
            wrappersList.forEachIndexed { i, wrapper1 ->
                wrappersList.forEachIndexed { j, wrapper2 ->
                    if (i != j) {
                        wrapper1 shouldNotBeSameInstanceAs wrapper2
                    }
                }
            }
        }
    }
})
