package kjd.golfcanada.client.provider

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.model.Context
import com.amazon.ask.model.RequestEnvelope
import com.amazon.ask.model.User
import com.amazon.ask.model.interfaces.system.SystemState
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import kjd.golfcanada.alexa.exception.AccountLinkingException

/**
 * Unit tests for ApiClientProvider extension functions.
 * 
 * These tests verify:
 * 1. The withAuthenticatedClient function properly extracts and validates access tokens
 * 2. The function creates an authenticated client and executes the provided block
 * 3. The function throws appropriate exceptions when no access token is available
 */
class ApiClientProviderExtTest : DescribeSpec({
    
    describe("ApiClientProvider.withAuthenticatedClient") {
        
        it("should extract access token and execute block with authenticated client") {
            val provider = ApiClientProvider()
            val mockInput = mockk<HandlerInput>()
            val mockRequestEnvelope = mockk<RequestEnvelope>()
            val mockContext = mockk<Context>()
            val mockSystemState = mockk<SystemState>()
            val mockUser = mockk<User>()
            
            val accessToken = "test-access-token"
            
            every { mockInput.requestEnvelope } returns mockRequestEnvelope
            every { mockRequestEnvelope.context } returns mockContext
            every { mockContext.system } returns mockSystemState
            every { mockSystemState.user } returns mockUser
            every { mockUser.accessToken } returns accessToken
            
            var blockExecuted = false
            var receivedWrapper: ApiClientWrapper? = null
            
            val result = provider.withAuthenticatedClient(mockInput) { client ->
                blockExecuted = true
                receivedWrapper = client
                "success"
            }
            
            blockExecuted shouldBe true
            receivedWrapper shouldNotBe null
            result shouldBe "success"
        }
        
        it("should extract access token from concatenated token (with id_token)") {
            val provider = ApiClientProvider()
            val mockInput = mockk<HandlerInput>()
            val mockRequestEnvelope = mockk<RequestEnvelope>()
            val mockContext = mockk<Context>()
            val mockSystemState = mockk<SystemState>()
            val mockUser = mockk<User>()
            
            // Simulate a concatenated token (access_token#id_token)
            val concatenatedToken = "access-token-part#id-token-part"
            
            every { mockInput.requestEnvelope } returns mockRequestEnvelope
            every { mockRequestEnvelope.context } returns mockContext
            every { mockContext.system } returns mockSystemState
            every { mockSystemState.user } returns mockUser
            every { mockUser.accessToken } returns concatenatedToken
            
            var blockExecuted = false
            
            val result = provider.withAuthenticatedClient(mockInput) { client ->
                blockExecuted = true
                client
            }
            
            blockExecuted shouldBe true
            result shouldNotBe null
        }
        
        it("should throw AccountLinkingException when access token is null") {
            val provider = ApiClientProvider()
            val mockInput = mockk<HandlerInput>()
            val mockRequestEnvelope = mockk<RequestEnvelope>()
            val mockContext = mockk<Context>()
            val mockSystemState = mockk<SystemState>()
            val mockUser = mockk<User>()
            
            every { mockInput.requestEnvelope } returns mockRequestEnvelope
            every { mockRequestEnvelope.context } returns mockContext
            every { mockContext.system } returns mockSystemState
            every { mockSystemState.user } returns mockUser
            every { mockUser.accessToken } returns null
            
            shouldThrow<AccountLinkingException> {
                provider.withAuthenticatedClient(mockInput) { _ ->
                    "should not be executed"
                }
            }
        }
        
        it("should throw AccountLinkingException when access token is blank") {
            val provider = ApiClientProvider()
            val mockInput = mockk<HandlerInput>()
            val mockRequestEnvelope = mockk<RequestEnvelope>()
            val mockContext = mockk<Context>()
            val mockSystemState = mockk<SystemState>()
            val mockUser = mockk<User>()
            
            every { mockInput.requestEnvelope } returns mockRequestEnvelope
            every { mockRequestEnvelope.context } returns mockContext
            every { mockContext.system } returns mockSystemState
            every { mockSystemState.user } returns mockUser
            every { mockUser.accessToken } returns "   "
            
            shouldThrow<AccountLinkingException> {
                provider.withAuthenticatedClient(mockInput) { _ ->
                    "should not be executed"
                }
            }
        }
        
        it("should throw AccountLinkingException when access token is empty string") {
            val provider = ApiClientProvider()
            val mockInput = mockk<HandlerInput>()
            val mockRequestEnvelope = mockk<RequestEnvelope>()
            val mockContext = mockk<Context>()
            val mockSystemState = mockk<SystemState>()
            val mockUser = mockk<User>()
            
            every { mockInput.requestEnvelope } returns mockRequestEnvelope
            every { mockRequestEnvelope.context } returns mockContext
            every { mockContext.system } returns mockSystemState
            every { mockSystemState.user } returns mockUser
            every { mockUser.accessToken } returns ""
            
            shouldThrow<AccountLinkingException> {
                provider.withAuthenticatedClient(mockInput) { _ ->
                    "should not be executed"
                }
            }
        }
        
        it("should return the result from the block") {
            val provider = ApiClientProvider()
            val mockInput = mockk<HandlerInput>()
            val mockRequestEnvelope = mockk<RequestEnvelope>()
            val mockContext = mockk<Context>()
            val mockSystemState = mockk<SystemState>()
            val mockUser = mockk<User>()
            
            every { mockInput.requestEnvelope } returns mockRequestEnvelope
            every { mockRequestEnvelope.context } returns mockContext
            every { mockContext.system } returns mockSystemState
            every { mockSystemState.user } returns mockUser
            every { mockUser.accessToken } returns "test-token"
            
            data class TestResult(val value: String)
            
            val result = provider.withAuthenticatedClient(mockInput) { _ ->
                TestResult("computed value")
            }
            
            result shouldBe TestResult("computed value")
        }
        
        it("should propagate exceptions thrown in the block") {
            val provider = ApiClientProvider()
            val mockInput = mockk<HandlerInput>()
            val mockRequestEnvelope = mockk<RequestEnvelope>()
            val mockContext = mockk<Context>()
            val mockSystemState = mockk<SystemState>()
            val mockUser = mockk<User>()
            
            every { mockInput.requestEnvelope } returns mockRequestEnvelope
            every { mockRequestEnvelope.context } returns mockContext
            every { mockContext.system } returns mockSystemState
            every { mockSystemState.user } returns mockUser
            every { mockUser.accessToken } returns "test-token"
            
            shouldThrow<IllegalStateException> {
                provider.withAuthenticatedClient(mockInput) { _ ->
                    throw IllegalStateException("Test exception")
                }
            }
        }
    }
})
