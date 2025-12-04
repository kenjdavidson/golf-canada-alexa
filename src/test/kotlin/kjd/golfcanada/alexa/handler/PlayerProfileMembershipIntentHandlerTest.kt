package kjd.golfcanada.alexa.handler

import com.amazon.ask.attributes.AttributesManager
import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.model.Intent
import com.amazon.ask.model.IntentRequest
import com.amazon.ask.model.RequestEnvelope
import com.amazon.ask.model.ui.PlainTextOutputSpeech
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import kjd.golfcanada.alexa.data.UserProfileSession
import kjd.golfcanada.alexa.exception.NoUserDetailsException
import kjd.golfcanada.alexa.interceptor.UserProfileInterceptor
import kjd.golfcanada.alexa.util.TemplateFactoryUtil

class PlayerProfileMembershipIntentHandlerTest : DescribeSpec({
    context("canHandle") {
        it("should return true for GOLFCANADA.PlayerProfileMembership") {
            val templateFactory = TemplateFactoryUtil.getTemplateFactory()
            val input = HandlerInput.builder()
                .withRequestEnvelope(RequestEnvelope.builder()
                    .withRequest(IntentRequest.builder()
                        .withIntent(Intent.builder()
                            .withName("GOLFCANADA.PlayerProfileMembership")
                            .build()
                        ).build()
                    )
                    .build()
                )
                .withTemplateFactory(templateFactory)
                .build()

            val handler = PlayerProfileMembershipIntentHandler()
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

            val handler = PlayerProfileMembershipIntentHandler()
            val canHandle = handler.canHandle(input)

            canHandle shouldBe false
        }
    }

    context("handle") {
        it("should throw NoUserDetailsException when user profile is not in session") {
            val attributesManager = mockk<AttributesManager>(relaxed = true)
            every { attributesManager.sessionAttributes } returns mutableMapOf()
            
            val input = mockk<HandlerInput>(relaxed = true)
            every { input.requestEnvelope } returns RequestEnvelope.builder()
                .withRequest(IntentRequest.builder().build())
                .build()
            every { input.attributesManager } returns attributesManager

            val handler = PlayerProfileMembershipIntentHandler()

            shouldThrow<NoUserDetailsException> {
                handler.handle(input)
            }
        }

        it("should return response with user membership information") {
            val userProfile = UserProfileSession(
                firstName = "John",
                lastName = "Doe",
                membershipLevel = "Gold",
                golfCanadaCardId = "12345678",
                expirationDate = "2/1/2026 12:00:00 AM",
                facilityName = "Blue Springs Golf Club",
                postHoleByHole = true
            )
            
            val attributesManager = mockk<AttributesManager>(relaxed = true)
            val sessionAttributes: MutableMap<String, Any> = mutableMapOf(
                UserProfileInterceptor.USER_SESSION_KEY to userProfile
            )
            every { attributesManager.sessionAttributes } returns sessionAttributes
            
            val input = mockk<HandlerInput>(relaxed = true)
            every { input.requestEnvelope } returns RequestEnvelope.builder()
                .withRequest(IntentRequest.builder().build())
                .build()
            every { input.attributesManager } returns attributesManager
            every { input.generateTemplateResponse(any(), any()) } answers {
                val dataModel = secondArg<Map<String, Any>>()
                val postHoleByHoleText = if (dataModel["postHoleByHole"] == true) "required" else "not required"
                val text = "Hello ${dataModel["firstName"]} ${dataModel["lastName"]}. " +
                        "Your membership level is ${dataModel["membershipLevel"]}. " +
                        "Your membership expires on ${dataModel["expirationDate"]}. " +
                        "Your Golf Canada card ID is ${dataModel["golfCanadaCardId"]}. " +
                        "Your default scoring is set to ${dataModel["facilityName"]}. " +
                        "Hole by hole scoring is $postHoleByHoleText."
                val response = com.amazon.ask.model.Response.builder()
                    .withOutputSpeech(PlainTextOutputSpeech.builder().withText(text).build())
                    .withShouldEndSession(false)
                    .build()
                java.util.Optional.of(response)
            }

            val handler = PlayerProfileMembershipIntentHandler()

            val response = handler.handle(input).get()
            val outputSpeech = response.outputSpeech as PlainTextOutputSpeech

            response.shouldEndSession shouldBe false
            outputSpeech.type shouldBe "PlainText"
            outputSpeech.text shouldContain "John Doe"
            outputSpeech.text shouldContain "Gold"
            outputSpeech.text shouldContain "12345678"
            outputSpeech.text shouldContain "2/1/2026"
            outputSpeech.text shouldContain "Blue Springs Golf Club"
            outputSpeech.text shouldContain "required"
        }

        it("should return fr response with user membership information") {
            val userProfile = UserProfileSession(
                firstName = "Jean",
                lastName = "Dupont",
                membershipLevel = "Argent",
                golfCanadaCardId = "87654321",
                expirationDate = "1/3/2025 12:00:00 AM",
                facilityName = "Club de Golf Vert",
                postHoleByHole = false
            )
            
            val attributesManager = mockk<AttributesManager>(relaxed = true)
            val sessionAttributes: MutableMap<String, Any> = mutableMapOf(
                UserProfileInterceptor.USER_SESSION_KEY to userProfile
            )
            every { attributesManager.sessionAttributes } returns sessionAttributes
            
            val input = mockk<HandlerInput>(relaxed = true)
            every { input.requestEnvelope } returns RequestEnvelope.builder()
                .withRequest(IntentRequest.builder()
                    .withLocale("fr-CA").build())
                .build()
            every { input.attributesManager } returns attributesManager
            every { input.generateTemplateResponse(any(), any()) } answers {
                val dataModel = secondArg<Map<String, Any>>()
                val postHoleByHoleText = if (dataModel["postHoleByHole"] == false) "non obligatoire" else "obligatoire"
                val text = "Bonjour ${dataModel["firstName"]} ${dataModel["lastName"]}. " +
                        "Votre niveau d'adhésion est ${dataModel["membershipLevel"]}. " +
                        "Votre adhésion expire le ${dataModel["expirationDate"]}. " +
                        "Votre numéro de carte Golf Canada est ${dataModel["golfCanadaCardId"]}. " +
                        "Votre score par défaut est défini à ${dataModel["facilityName"]}. " +
                        "La saisie trou par trou est $postHoleByHoleText."
                val response = com.amazon.ask.model.Response.builder()
                    .withOutputSpeech(PlainTextOutputSpeech.builder().withText(text).build())
                    .withShouldEndSession(false)
                    .build()
                java.util.Optional.of(response)
            }

            val handler = PlayerProfileMembershipIntentHandler()

            val response = handler.handle(input).get()
            val outputSpeech = response.outputSpeech as PlainTextOutputSpeech

            response.shouldEndSession shouldBe false
            outputSpeech.type shouldBe "PlainText"
            outputSpeech.text shouldContain "Jean Dupont"
            outputSpeech.text shouldContain "Argent"
            outputSpeech.text shouldContain "87654321"
            outputSpeech.text shouldContain "1/3/2025"
            outputSpeech.text shouldContain "Club de Golf Vert"
            outputSpeech.text shouldContain "non obligatoire"
        }
    }
})
