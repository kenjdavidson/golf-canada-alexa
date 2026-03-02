package kjd.golfcanada.alexa.interceptor

import com.amazon.ask.attributes.AttributesManager
import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.model.Context
import com.amazon.ask.model.Intent
import com.amazon.ask.model.IntentRequest
import com.amazon.ask.model.LaunchRequest
import com.amazon.ask.model.RequestEnvelope
import com.amazon.ask.model.SessionEndedRequest
import com.amazon.ask.model.User
import com.amazon.ask.model.interfaces.system.SystemState
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kjd.golfcanada.alexa.exception.AccountLinkingException

class AuthenticationRequestInterceptorTest : DescribeSpec({

    val interceptor = AuthenticationRequestInterceptor()

    context("requiresAuthentication") {
        it("should return false for LaunchRequest") {
            val input = HandlerInput.builder()
                .withRequestEnvelope(
                    RequestEnvelope.builder()
                        .withRequest(LaunchRequest.builder().build())
                        .build()
                )
                .build()

            interceptor.requiresAuthentication(input) shouldBe false
        }

        it("should return false for SessionEndedRequest") {
            val input = HandlerInput.builder()
                .withRequestEnvelope(
                    RequestEnvelope.builder()
                        .withRequest(SessionEndedRequest.builder().build())
                        .build()
                )
                .build()

            interceptor.requiresAuthentication(input) shouldBe false
        }

        it("should return false for AMAZON.HelpIntent") {
            val input = HandlerInput.builder()
                .withRequestEnvelope(
                    RequestEnvelope.builder()
                        .withRequest(
                            IntentRequest.builder()
                                .withIntent(Intent.builder().withName("AMAZON.HelpIntent").build())
                                .build()
                        )
                        .build()
                )
                .build()

            interceptor.requiresAuthentication(input) shouldBe false
        }

        it("should return false for AMAZON.StopIntent") {
            val input = HandlerInput.builder()
                .withRequestEnvelope(
                    RequestEnvelope.builder()
                        .withRequest(
                            IntentRequest.builder()
                                .withIntent(Intent.builder().withName("AMAZON.StopIntent").build())
                                .build()
                        )
                        .build()
                )
                .build()

            interceptor.requiresAuthentication(input) shouldBe false
        }

        it("should return false for AMAZON.CancelIntent") {
            val input = HandlerInput.builder()
                .withRequestEnvelope(
                    RequestEnvelope.builder()
                        .withRequest(
                            IntentRequest.builder()
                                .withIntent(Intent.builder().withName("AMAZON.CancelIntent").build())
                                .build()
                        )
                        .build()
                )
                .build()

            interceptor.requiresAuthentication(input) shouldBe false
        }

        it("should return false for AMAZON.FallbackIntent") {
            val input = HandlerInput.builder()
                .withRequestEnvelope(
                    RequestEnvelope.builder()
                        .withRequest(
                            IntentRequest.builder()
                                .withIntent(Intent.builder().withName("AMAZON.FallbackIntent").build())
                                .build()
                        )
                        .build()
                )
                .build()

            interceptor.requiresAuthentication(input) shouldBe false
        }

        it("should return true for custom intents") {
            val input = HandlerInput.builder()
                .withRequestEnvelope(
                    RequestEnvelope.builder()
                        .withRequest(
                            IntentRequest.builder()
                                .withIntent(Intent.builder().withName("GetHandicapIntent").build())
                                .build()
                        )
                        .build()
                )
                .build()

            interceptor.requiresAuthentication(input) shouldBe true
        }
    }

    context("isAuthenticated") {
        it("should return true when access token is present") {
            val input = HandlerInput.builder()
                .withRequestEnvelope(
                    RequestEnvelope.builder()
                        .withRequest(LaunchRequest.builder().build())
                        .withContext(
                            Context.builder()
                                .withSystem(
                                    SystemState.builder()
                                        .withUser(
                                            User.builder()
                                                .withAccessToken("valid-access-token")
                                                .build()
                                        )
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .build()

            interceptor.isAuthenticated(input) shouldBe true
        }

        it("should return false when access token is null") {
            val input = HandlerInput.builder()
                .withRequestEnvelope(
                    RequestEnvelope.builder()
                        .withRequest(LaunchRequest.builder().build())
                        .withContext(
                            Context.builder()
                                .withSystem(
                                    SystemState.builder()
                                        .withUser(
                                            User.builder()
                                                .withAccessToken(null)
                                                .build()
                                        )
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .build()

            interceptor.isAuthenticated(input) shouldBe false
        }

        it("should return false when access token is empty") {
            val input = HandlerInput.builder()
                .withRequestEnvelope(
                    RequestEnvelope.builder()
                        .withRequest(LaunchRequest.builder().build())
                        .withContext(
                            Context.builder()
                                .withSystem(
                                    SystemState.builder()
                                        .withUser(
                                            User.builder()
                                                .withAccessToken("")
                                                .build()
                                        )
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .build()

            interceptor.isAuthenticated(input) shouldBe false
        }

        it("should return false when context is null") {
            val input = HandlerInput.builder()
                .withRequestEnvelope(
                    RequestEnvelope.builder()
                        .withRequest(LaunchRequest.builder().build())
                        .build()
                )
                .build()

            interceptor.isAuthenticated(input) shouldBe false
        }

        it("should return true when static token is present in request attributes") {
            val attributesManager = mockk<AttributesManager>()
            every { attributesManager.requestAttributes } returns mutableMapOf<String, Any>(
                StaticCredentialInterceptor.STATIC_TOKEN_KEY to "static-access-token#static-id-token"
            )

            val input = mockk<HandlerInput>()
            every { input.requestEnvelope } returns RequestEnvelope.builder()
                .withRequest(LaunchRequest.builder().build())
                .build()
            every { input.attributesManager } returns attributesManager

            interceptor.isAuthenticated(input) shouldBe true
        }

        it("should return false when static token in request attributes is blank") {
            val attributesManager = mockk<AttributesManager>()
            every { attributesManager.requestAttributes } returns mutableMapOf<String, Any>(
                StaticCredentialInterceptor.STATIC_TOKEN_KEY to ""
            )

            val input = mockk<HandlerInput>()
            every { input.requestEnvelope } returns RequestEnvelope.builder()
                .withRequest(LaunchRequest.builder().build())
                .build()
            every { input.attributesManager } returns attributesManager

            interceptor.isAuthenticated(input) shouldBe false
        }
    }

    context("process") {
        it("should not throw when authentication is not required") {
            val input = HandlerInput.builder()
                .withRequestEnvelope(
                    RequestEnvelope.builder()
                        .withRequest(LaunchRequest.builder().build())
                        .build()
                )
                .build()

            // Should not throw
            interceptor.process(input)
        }

        it("should not throw when authenticated") {
            val input = HandlerInput.builder()
                .withRequestEnvelope(
                    RequestEnvelope.builder()
                        .withRequest(
                            IntentRequest.builder()
                                .withIntent(Intent.builder().withName("GetHandicapIntent").build())
                                .build()
                        )
                        .withContext(
                            Context.builder()
                                .withSystem(
                                    SystemState.builder()
                                        .withUser(
                                            User.builder()
                                                .withAccessToken("valid-access-token")
                                                .build()
                                        )
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .build()

            // Should not throw
            interceptor.process(input)
        }

        it("should throw AccountLinkingException when authentication required but not authenticated") {
            val input = HandlerInput.builder()
                .withRequestEnvelope(
                    RequestEnvelope.builder()
                        .withRequest(
                            IntentRequest.builder()
                                .withIntent(Intent.builder().withName("GetHandicapIntent").build())
                                .build()
                        )
                        .build()
                )
                .build()

            shouldThrow<AccountLinkingException> {
                interceptor.process(input)
            }
        }
    }
})
