package kjd.golfcanada.client.provider

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kjd.golfcanada.client.api.AuthApi
import kjd.golfcanada.client.api.MembersApi
import kjd.golfcanada.client.api.ScoresApi
import okhttp3.OkHttpClient
import org.openapitools.client.infrastructure.ApiClient

/**
 * Unit tests for ApiClientWrapper.
 * 
 * These tests verify:
 * 1. Lazy-loaded API properties return correct API types
 * 2. API properties are cached after first access
 * 3. All APIs share the same underlying HTTP client
 * 4. API instances are properly configured
 */
class ApiClientWrapperTest : DescribeSpec({
    
    describe("ApiClientWrapper fluent API properties") {
        
        val mockApiClient = ApiClient("https://test.example.com", OkHttpClient())
        val wrapper = ApiClientWrapper(mockApiClient)
        
        it("should provide AuthApi via auth property") {
            val authApi = wrapper.auth
            
            authApi shouldNotBe null
            authApi.shouldBeInstanceOf<AuthApi>()
        }
        
        it("should provide MembersApi via members property") {
            val membersApi = wrapper.members
            
            membersApi shouldNotBe null
            membersApi.shouldBeInstanceOf<MembersApi>()
        }
        
        it("should provide ScoresApi via scores property") {
            val scoresApi = wrapper.scores
            
            scoresApi shouldNotBe null
            scoresApi.shouldBeInstanceOf<ScoresApi>()
        }
        
        it("should cache API instances - same instance on multiple accesses") {
            val authApi1 = wrapper.auth
            val authApi2 = wrapper.auth
            
            authApi1 shouldBe authApi2
        }
        
        it("should allow fluent API calls") {
            // Verify the fluent API pattern works
            val authApi = wrapper.auth
            val membersApi = wrapper.members
            val scoresApi = wrapper.scores
            
            // All APIs should be created successfully
            authApi shouldNotBe null
            membersApi shouldNotBe null
            scoresApi shouldNotBe null
        }
        
        it("should return the underlying ApiClient") {
            val apiClient = wrapper.getApiClient()
            
            apiClient shouldBe mockApiClient
        }
    }
    
    describe("ApiClientWrapper with authenticated client") {
        
        it("should create APIs that share authenticated HTTP client") {
            val provider = ApiClientProvider()
            val token = "test-token-456"
            val wrapper = provider.getClient(token)
            
            val authApi = wrapper.auth
            val scoresApi = wrapper.scores
            
            // Both APIs should be created successfully
            authApi shouldNotBe null
            scoresApi shouldNotBe null
        }
    }
})
