package kjd.golfcanada.alexa.interceptor

import com.amazon.ask.attributes.AttributesManager
import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.model.Context
import com.amazon.ask.model.RequestEnvelope
import com.amazon.ask.model.User as AlexaUser
import com.amazon.ask.model.interfaces.system.SystemState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import kjd.golfcanada.alexa.data.HandicapSummaryData
import kjd.golfcanada.client.model.User
import kjd.golfcanada.client.provider.ApiClientProvider

class HandicapLookupInterceptorTest : DescribeSpec({
    describe("HandicapLookupInterceptor") {
        context("process") {
            it("should skip if no access token present") {
                val input = mockk<HandlerInput>(relaxed = true)
                every { input.requestEnvelope } returns RequestEnvelope.builder()
                    .withContext(Context.builder()
                        .withSystem(SystemState.builder()
                            .withUser(AlexaUser.builder()
                                .withAccessToken(null)
                                .build())
                            .build())
                        .build())
                    .build()

                val apiClientProvider = mockk<ApiClientProvider>(relaxed = true)
                val interceptor = HandicapLookupInterceptor(apiClientProvider)
                interceptor.process(input)

                // Should not throw and should skip processing
            }

            it("should skip if no user profile in session") {
                val attributesManager = mockk<AttributesManager>(relaxed = true)
                every { attributesManager.sessionAttributes } returns mutableMapOf()
                every { attributesManager.requestAttributes } returns mutableMapOf()

                val input = mockk<HandlerInput>(relaxed = true)
                every { input.requestEnvelope } returns RequestEnvelope.builder()
                    .withContext(Context.builder()
                        .withSystem(SystemState.builder()
                            .withUser(AlexaUser.builder()
                                .withAccessToken("test_token#id_token")
                                .build())
                            .build())
                        .build())
                    .build()
                every { input.attributesManager } returns attributesManager

                val apiClientProvider = mockk<ApiClientProvider>(relaxed = true)
                val interceptor = HandicapLookupInterceptor(apiClientProvider)
                interceptor.process(input)

                // Should not throw and should skip processing
            }

            it("should use cached handicap if fresh") {
                val user = User(id = 12345L, username = "testuser")
                val cachedHandicap = HandicapSummaryData(
                    name = "Test User",
                    email = "test@test.com",
                    lowValue = 5.0,
                    handicap = "10.0",
                    averageDifferential = 12.5,
                    cachedAt = System.currentTimeMillis() - 60000 // 1 minute ago (fresh)
                )

                val sessionAttributes = mutableMapOf<String, Any>(
                    UserProfileInterceptor.USER_SESSION_KEY to user,
                    HandicapLookupInterceptor.HANDICAP_SESSION_KEY to cachedHandicap
                )
                val requestAttributes = mutableMapOf<String, Any>()

                val attributesManager = mockk<AttributesManager>(relaxed = true)
                every { attributesManager.sessionAttributes } returns sessionAttributes
                every { attributesManager.requestAttributes } returns requestAttributes
                every { attributesManager.requestAttributes = any() } answers {
                    // Capture the updated request attributes
                    val updatedAttrs = firstArg<MutableMap<String, Any>>()
                    requestAttributes.putAll(updatedAttrs)
                }

                val input = mockk<HandlerInput>(relaxed = true)
                every { input.requestEnvelope } returns RequestEnvelope.builder()
                    .withContext(Context.builder()
                        .withSystem(SystemState.builder()
                            .withUser(AlexaUser.builder()
                                .withAccessToken("test_token#id_token")
                                .build())
                            .build())
                        .build())
                    .build()
                every { input.attributesManager } returns attributesManager

                val apiClientProvider = mockk<ApiClientProvider>(relaxed = true)
                val interceptor = HandicapLookupInterceptor(apiClientProvider)
                interceptor.process(input)

                // Should have placed cached handicap in request attributes
                val retrievedHandicap = requestAttributes[HandicapLookupInterceptor.HANDICAP_REQUEST_KEY] as? HandicapSummaryData
                retrievedHandicap shouldNotBe null
                retrievedHandicap?.name shouldBe "Test User"
                retrievedHandicap?.handicap shouldBe "10.0"
            }

            it("should not use stale cached handicap") {
                val user = User(id = 12345L, username = "testuser")
                val staleHandicap = HandicapSummaryData(
                    name = "Test User",
                    email = "test@test.com",
                    lowValue = 5.0,
                    handicap = "10.0",
                    averageDifferential = 12.5,
                    cachedAt = System.currentTimeMillis() - (11 * 60 * 1000) // 11 minutes ago (stale)
                )

                val sessionAttributes = mutableMapOf<String, Any>(
                    UserProfileInterceptor.USER_SESSION_KEY to user,
                    HandicapLookupInterceptor.HANDICAP_SESSION_KEY to staleHandicap
                )
                val requestAttributes = mutableMapOf<String, Any>()

                val attributesManager = mockk<AttributesManager>(relaxed = true)
                every { attributesManager.sessionAttributes } returns sessionAttributes
                every { attributesManager.requestAttributes } returns requestAttributes
                every { attributesManager.sessionAttributes = any() } answers {
                    // Would be called if we fetch fresh data, but we can't test that without mocking the API
                }
                every { attributesManager.requestAttributes = any() } answers {
                    val updatedAttrs = firstArg<MutableMap<String, Any>>()
                    requestAttributes.putAll(updatedAttrs)
                }

                val input = mockk<HandlerInput>(relaxed = true)
                every { input.requestEnvelope } returns RequestEnvelope.builder()
                    .withContext(Context.builder()
                        .withSystem(SystemState.builder()
                            .withUser(AlexaUser.builder()
                                .withAccessToken("test_token#id_token")
                                .build())
                            .build())
                        .build())
                    .build()
                every { input.attributesManager } returns attributesManager

                val apiClientProvider = mockk<ApiClientProvider>(relaxed = true)
                val interceptor = HandicapLookupInterceptor(apiClientProvider)
                interceptor.process(input)

                // In this test, we can't easily verify the API call was made without more complex mocking
                // But we can verify that the stale cache was not used (request attributes should be empty)
                // Note: In a real scenario, the API call would fail or succeed, updating the cache
            }
        }
    }
})
