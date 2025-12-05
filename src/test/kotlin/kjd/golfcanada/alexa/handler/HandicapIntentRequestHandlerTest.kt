package kjd.golfcanada.alexa.handler

import com.amazon.ask.attributes.AttributesManager
import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.model.Context
import com.amazon.ask.model.Intent
import com.amazon.ask.model.IntentRequest
import com.amazon.ask.model.RequestEnvelope
import com.amazon.ask.model.Slot
import com.amazon.ask.model.User as AlexaUser
import com.amazon.ask.model.interfaces.system.SystemState
import com.amazon.ask.model.ui.PlainTextOutputSpeech
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import kjd.golfcanada.alexa.exception.AccountLinkingException
import kjd.golfcanada.alexa.interceptor.UserProfileInterceptor
import kjd.golfcanada.alexa.data.HandicapSummaryData
import kjd.golfcanada.alexa.util.TemplateFactoryUtil
import kjd.golfcanada.client.model.HandicapCalculation
import kjd.golfcanada.client.model.User
import kjd.golfcanada.client.provider.ApiClientProvider
import kjd.golfcanada.client.provider.ApiClientWrapper

class HandicapIntentRequestHandlerTest : DescribeSpec({
    context("canHandle") {
        it("should return true for GOLFCANADA.Handicap") {
            val templateFactory = TemplateFactoryUtil.getTemplateFactory()
            val apiClientProvider = mockk<ApiClientProvider>(relaxed = true)
            val input = HandlerInput.builder()
                .withRequestEnvelope(RequestEnvelope.builder()
                    .withRequest(IntentRequest.builder()
                        .withIntent(Intent.builder()
                            .withName("GOLFCANADA.Handicap")
                            .build()
                        ).build()
                    )
                    .build()
                )
                .withTemplateFactory(templateFactory)
                .build()

            val handler = HandicapIntentRequestHandler(apiClientProvider)
            val canHandle = handler.canHandle(input)

            canHandle shouldBe true
        }

        it("should return false for other intents") {
            val templateFactory = TemplateFactoryUtil.getTemplateFactory()
            val apiClientProvider = mockk<ApiClientProvider>(relaxed = true)
            val input = HandlerInput.builder()
                .withRequestEnvelope(RequestEnvelope.builder()
                    .withRequest(IntentRequest.builder()
                        .withIntent(Intent.builder()
                            .withName("AMAZON.StopIntent")
                            .build()
                        ).build()
                    )
                    .build()
                )
                .withTemplateFactory(templateFactory)
                .build()

            val handler = HandicapIntentRequestHandler(apiClientProvider)
            val canHandle = handler.canHandle(input)

            canHandle shouldBe false
        }
    }

    context("handle - own handicap") {
        it("should throw AccountLinkingException when no access token") {
            val apiClientProvider = mockk<ApiClientProvider>(relaxed = true)
            val attributesManager = mockk<AttributesManager>(relaxed = true)
            every { attributesManager.sessionAttributes } returns mutableMapOf()

            val input = mockk<HandlerInput>(relaxed = true)
            every { input.requestEnvelope } returns RequestEnvelope.builder()
                .withRequest(IntentRequest.builder()
                    .withIntent(Intent.builder()
                        .withName("GOLFCANADA.Handicap")
                        .build())
                    .build())
                .build()
            every { input.attributesManager } returns attributesManager

            val handler = HandicapIntentRequestHandler(apiClientProvider)

            shouldThrow<AccountLinkingException> {
                handler.handle(input)
            }
        }
    }

    context("handle - friend handicap") {
        it("should throw AccountLinkingException when no access token with FriendFullName") {
            val apiClientProvider = mockk<ApiClientProvider>(relaxed = true)
            val attributesManager = mockk<AttributesManager>(relaxed = true)
            every { attributesManager.sessionAttributes } returns mutableMapOf()

            val friendSlot = Slot.builder()
                .withName("FriendFullName")
                .withValue("John Smith")
                .build()

            val input = mockk<HandlerInput>(relaxed = true)
            every { input.requestEnvelope } returns RequestEnvelope.builder()
                .withRequest(IntentRequest.builder()
                    .withIntent(Intent.builder()
                        .withName("GOLFCANADA.Handicap")
                        .withSlots(mapOf("FriendFullName" to friendSlot))
                        .build())
                    .build())
                .build()
            every { input.attributesManager } returns attributesManager

            val handler = HandicapIntentRequestHandler(apiClientProvider)

            shouldThrow<AccountLinkingException> {
                handler.handle(input)
            }
        }

        it("should throw AccountLinkingException when no access token with FriendFirstName") {
            val apiClientProvider = mockk<ApiClientProvider>(relaxed = true)
            val attributesManager = mockk<AttributesManager>(relaxed = true)
            every { attributesManager.sessionAttributes } returns mutableMapOf()

            val friendSlot = Slot.builder()
                .withName("FriendFirstName")
                .withValue("John")
                .build()

            val input = mockk<HandlerInput>(relaxed = true)
            every { input.requestEnvelope } returns RequestEnvelope.builder()
                .withRequest(IntentRequest.builder()
                    .withIntent(Intent.builder()
                        .withName("GOLFCANADA.Handicap")
                        .withSlots(mapOf("FriendFirstName" to friendSlot))
                        .build())
                    .build())
                .build()
            every { input.attributesManager } returns attributesManager

            val handler = HandicapIntentRequestHandler(apiClientProvider)

            shouldThrow<AccountLinkingException> {
                handler.handle(input)
            }
        }
    }
})
