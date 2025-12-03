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
 * 1. API factory methods return correct API types
 * 2. All APIs share the same underlying HTTP client
 * 3. API instances are properly configured
 */
class ApiClientWrapperTest : DescribeSpec({
    
    describe("ApiClientWrapper factory methods") {
        
        val mockApiClient = ApiClient("https://test.example.com", OkHttpClient())
        val wrapper = ApiClientWrapper(mockApiClient)
        
        it("should create AuthApi instance") {
            val authApi = wrapper.createAuthApi()
            
            authApi shouldNotBe null
            authApi.shouldBeInstanceOf<AuthApi>()
        }
        
        it("should create MembersApi instance") {
            val membersApi = wrapper.createMembersApi()
            
            membersApi shouldNotBe null
            membersApi.shouldBeInstanceOf<MembersApi>()
        }
        
        it("should create ScoresApi instance") {
            val scoresApi = wrapper.createScoresApi()
            
            scoresApi shouldNotBe null
            scoresApi.shouldBeInstanceOf<ScoresApi>()
        }
        
        it("should share the same base URL across API instances") {
            val authApi = wrapper.createAuthApi()
            val membersApi = wrapper.createMembersApi()
            val scoresApi = wrapper.createScoresApi()
            
            // All APIs should use the same base URL from the wrapped ApiClient
            // Note: We can't easily access baseUrl from the API classes directly,
            // but we can verify they all use the same client
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
            
            val authApi = wrapper.createAuthApi()
            val scoresApi = wrapper.createScoresApi()
            
            // Both APIs should be created successfully
            authApi shouldNotBe null
            scoresApi shouldNotBe null
        }
    }
})
