package kjd.golfcanada.alexa.interceptor

import com.amazon.ask.attributes.AttributesManager
import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.model.LaunchRequest
import com.amazon.ask.model.RequestEnvelope
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kjd.golfcanada.client.api.AuthApi
import kjd.golfcanada.client.model.AuthToken
import org.openapitools.client.infrastructure.ClientException

class StaticCredentialInterceptorTest : DescribeSpec({

    val validToken = AuthToken(
        accessToken = "test-access-token",
        refreshToken = "test-refresh-token",
        idToken = "test-id-token",
        expiresIn = 3600,
        tokenType = "bearer"
    )

    context("isEnabled") {
        it("should be true when both username and password are set") {
            val interceptor = StaticCredentialInterceptor("user@example.com", "password123")
            interceptor.isEnabled shouldBe true
        }

        it("should be false when username is null") {
            val interceptor = StaticCredentialInterceptor(null, "password123")
            interceptor.isEnabled shouldBe false
        }

        it("should be false when password is null") {
            val interceptor = StaticCredentialInterceptor("user@example.com", null)
            interceptor.isEnabled shouldBe false
        }

        it("should be false when username is blank") {
            val interceptor = StaticCredentialInterceptor("", "password123")
            interceptor.isEnabled shouldBe false
        }

        it("should be false when password is blank") {
            val interceptor = StaticCredentialInterceptor("user@example.com", "")
            interceptor.isEnabled shouldBe false
        }
    }

    context("getValidToken") {
        it("should authenticate and return a concatenated token") {
            val authApi = mockk<AuthApi>()
            every {
                authApi.getAuthToken(
                    AuthApi.GrantTypeGetAuthToken.PASSWORD,
                    StaticCredentialInterceptor.STATIC_AUTH_SCOPES,
                    username = "user@example.com",
                    password = "password123"
                )
            } returns validToken

            val interceptor = StaticCredentialInterceptor("user@example.com", "password123", authApi)
            val token = interceptor.getValidToken()

            token shouldNotBe null
            token shouldBe "test-access-token#test-id-token"
        }

        it("should return null when authentication fails") {
            val authApi = mockk<AuthApi>()
            every {
                authApi.getAuthToken(any(), any(), username = any(), password = any())
            } throws ClientException("Authentication failed", statusCode = 401)

            val interceptor = StaticCredentialInterceptor("user@example.com", "bad-password", authApi)
            val token = interceptor.getValidToken()

            token shouldBe null
        }

        it("should use cached token on second call without re-authenticating") {
            val authApi = mockk<AuthApi>()
            every {
                authApi.getAuthToken(any(), any(), username = any(), password = any())
            } returns validToken

            val interceptor = StaticCredentialInterceptor("user@example.com", "password123", authApi)
            interceptor.getValidToken()
            interceptor.getValidToken()

            verify(exactly = 1) {
                authApi.getAuthToken(any(), any(), username = any(), password = any())
            }
        }

        it("should return null when static credentials are not configured") {
            val authApi = mockk<AuthApi>()
            val interceptor = StaticCredentialInterceptor(null, null, authApi)

            val token = interceptor.getValidToken()

            token shouldBe null
            verify(exactly = 0) {
                authApi.getAuthToken(any(), any(), username = any(), password = any())
            }
        }
    }

    context("process") {
        it("should store token in request attributes when credentials are configured") {
            val authApi = mockk<AuthApi>()
            every {
                authApi.getAuthToken(
                    AuthApi.GrantTypeGetAuthToken.PASSWORD,
                    StaticCredentialInterceptor.STATIC_AUTH_SCOPES,
                    username = "user@example.com",
                    password = "password123"
                )
            } returns validToken

            val attributesCapture = slot<Map<String, Any>>()
            val attributesManager = mockk<AttributesManager>()
            every { attributesManager.requestAttributes } returns mutableMapOf()
            every { attributesManager.requestAttributes = capture(attributesCapture) } returns Unit

            val input = mockk<HandlerInput>()
            every { input.requestEnvelope } returns RequestEnvelope.builder()
                .withRequest(LaunchRequest.builder().build())
                .build()
            every { input.attributesManager } returns attributesManager

            val interceptor = StaticCredentialInterceptor("user@example.com", "password123", authApi)
            interceptor.process(input)

            attributesCapture.isCaptured shouldBe true
            attributesCapture.captured[StaticCredentialInterceptor.STATIC_TOKEN_KEY] shouldBe "test-access-token#test-id-token"
        }

        it("should not store token when credentials are not configured") {
            val authApi = mockk<AuthApi>()
            val attributesManager = mockk<AttributesManager>()
            val input = mockk<HandlerInput>()
            every { input.attributesManager } returns attributesManager

            val interceptor = StaticCredentialInterceptor(null, null, authApi)
            interceptor.process(input)

            verify(exactly = 0) { attributesManager.requestAttributes }
            verify(exactly = 0) { authApi.getAuthToken(any(), any(), username = any(), password = any()) }
        }

        it("should not store token when authentication fails") {
            val authApi = mockk<AuthApi>()
            every {
                authApi.getAuthToken(any(), any(), username = any(), password = any())
            } throws RuntimeException("Network error")

            val attributesCapture = slot<Map<String, Any>>()
            val attributesManager = mockk<AttributesManager>()
            every { attributesManager.requestAttributes } returns mutableMapOf()
            every { attributesManager.requestAttributes = capture(attributesCapture) } returns Unit

            val input = mockk<HandlerInput>()
            every { input.requestEnvelope } returns RequestEnvelope.builder()
                .withRequest(LaunchRequest.builder().build())
                .build()
            every { input.attributesManager } returns attributesManager

            val interceptor = StaticCredentialInterceptor("user@example.com", "password123", authApi)
            interceptor.process(input)

            attributesCapture.isCaptured shouldBe false
        }
    }

    context("companion object") {
        it("STATIC_TOKEN_KEY should be the expected constant") {
            StaticCredentialInterceptor.STATIC_TOKEN_KEY shouldBe "static_access_token"
        }

        it("STATIC_AUTH_SCOPES should include required Golf Canada scopes") {
            StaticCredentialInterceptor.STATIC_AUTH_SCOPES shouldBe
                "address email offline_access openid phone profile roles"
        }
    }
})
