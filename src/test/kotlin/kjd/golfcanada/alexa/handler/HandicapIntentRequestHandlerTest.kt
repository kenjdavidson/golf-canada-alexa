package kjd.golfcanada.alexa.handler

import com.amazon.ask.attributes.AttributesManager
import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.model.Intent
import com.amazon.ask.model.IntentRequest
import com.amazon.ask.model.RequestEnvelope
import com.amazon.ask.model.Slot
import com.amazon.ask.model.ui.PlainTextOutputSpeech
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import kjd.golfcanada.alexa.exception.AccountLinkingException
import kjd.golfcanada.alexa.interceptor.HandicapLookupInterceptor
import kjd.golfcanada.alexa.model.HandicapSummaryData
import kjd.golfcanada.alexa.util.TemplateFactoryUtil

class HandicapIntentRequestHandlerTest : DescribeSpec({
    context("canHandle") {
        it("should return true for GOLFCANADA.HandicapIntent") {
            val templateFactory = TemplateFactoryUtil.getTemplateFactory()
            val input = HandlerInput.builder()
                .withRequestEnvelope(RequestEnvelope.builder()
                    .withRequest(IntentRequest.builder()
                        .withIntent(Intent.builder()
                            .withName("GOLFCANADA.HandicapIntent")
                            .build()
                        ).build()
                    )
                    .build()
                )
                .withTemplateFactory(templateFactory)
                .build()

            val handler = HandicapIntentRequestHandler()
            val canHandle = handler.canHandle(input)

            canHandle shouldBe true
        }

        it("should return false for other intents") {
            val templateFactory = TemplateFactoryUtil.getTemplateFactory()
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

            val handler = HandicapIntentRequestHandler()
            val canHandle = handler.canHandle(input)

            canHandle shouldBe false
        }
    }

    context("handle - own handicap") {
        it("should return own handicap from request attributes") {
            val handicapSummary = HandicapSummaryData(
                name = "Ken Davidson",
                email = "ken.j.davidson@live.ca",
                lowValue = 5.8,
                handicap = "8.9",
                averageDifferential = 9.0
            )

            val requestAttributes = mutableMapOf<String, Any>(
                HandicapLookupInterceptor.HANDICAP_REQUEST_KEY to handicapSummary
            )

            val attributesManager = mockk<AttributesManager>(relaxed = true)
            every { attributesManager.requestAttributes } returns requestAttributes

            val input = mockk<HandlerInput>(relaxed = true)
            every { input.requestEnvelope } returns RequestEnvelope.builder()
                .withRequest(IntentRequest.builder()
                    .withIntent(Intent.builder()
                        .withName("GOLFCANADA.HandicapIntent")
                        .build())
                    .build())
                .build()
            every { input.attributesManager } returns attributesManager
            every { input.generateTemplateResponse(any(), any()) } answers {
                val templateName = firstArg<String>()
                val dataModel = secondArg<Map<String, Any>>()
                
                templateName shouldBe "HandicapIntentResponse"
                
                val text = buildString {
                    if (dataModel.containsKey("name")) {
                        append("Hello ${dataModel["name"]}. ")
                    }
                    append("Your current handicap index is ${dataModel["handicap"]}.")
                    if (dataModel.containsKey("lowValue")) {
                        append(" Your low handicap index is ${dataModel["lowValue"]}.")
                    }
                    if (dataModel.containsKey("averageDifferential")) {
                        append(" Your average differential is ${dataModel["averageDifferential"]}.")
                    }
                }
                
                val response = com.amazon.ask.model.Response.builder()
                    .withOutputSpeech(PlainTextOutputSpeech.builder().withText(text).build())
                    .withShouldEndSession(false)
                    .build()
                java.util.Optional.of(response)
            }

            val handler = HandicapIntentRequestHandler()
            val response = handler.handle(input).get()
            val outputSpeech = response.outputSpeech as PlainTextOutputSpeech

            response.shouldEndSession shouldBe false
            outputSpeech.type shouldBe "PlainText"
            outputSpeech.text shouldContain "Ken Davidson"
            outputSpeech.text shouldContain "8.9"
            outputSpeech.text shouldContain "5.8"
            outputSpeech.text shouldContain "9.0"
        }

        it("should return error when no handicap data available") {
            val requestAttributes = mutableMapOf<String, Any>()

            val attributesManager = mockk<AttributesManager>(relaxed = true)
            every { attributesManager.requestAttributes } returns requestAttributes

            val input = mockk<HandlerInput>(relaxed = true)
            every { input.requestEnvelope } returns RequestEnvelope.builder()
                .withRequest(IntentRequest.builder()
                    .withIntent(Intent.builder()
                        .withName("GOLFCANADA.HandicapIntent")
                        .build())
                    .build())
                .build()
            every { input.attributesManager } returns attributesManager
            every { input.generateTemplateResponse(any(), any()) } answers {
                val templateName = firstArg<String>()
                val dataModel = secondArg<Map<String, Any>>()
                
                templateName shouldBe "HandicapIntentErrorResponse"
                
                val response = com.amazon.ask.model.Response.builder()
                    .withOutputSpeech(PlainTextOutputSpeech.builder()
                        .withText(dataModel["error"] as String)
                        .build())
                    .withShouldEndSession(false)
                    .build()
                java.util.Optional.of(response)
            }

            val handler = HandicapIntentRequestHandler()
            val response = handler.handle(input).get()
            val outputSpeech = response.outputSpeech as PlainTextOutputSpeech

            response.shouldEndSession shouldBe false
            outputSpeech.text shouldContain "Unable to retrieve"
        }
    }

    context("handle - friend handicap") {
        it("should throw AccountLinkingException when no access token") {
            val attributesManager = mockk<AttributesManager>(relaxed = true)
            every { attributesManager.requestAttributes } returns mutableMapOf()

            val friendSlot = Slot.builder()
                .withName("Friend")
                .withValue("1538533")
                .build()

            val input = mockk<HandlerInput>(relaxed = true)
            every { input.requestEnvelope } returns RequestEnvelope.builder()
                .withRequest(IntentRequest.builder()
                    .withIntent(Intent.builder()
                        .withName("GOLFCANADA.HandicapIntent")
                        .withSlots(mapOf("Friend" to friendSlot))
                        .build())
                    .build())
                .build()
            every { input.attributesManager } returns attributesManager

            val handler = HandicapIntentRequestHandler()

            shouldThrow<AccountLinkingException> {
                handler.handle(input)
            }
        }
    }
})
