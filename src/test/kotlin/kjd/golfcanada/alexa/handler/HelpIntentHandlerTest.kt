package kjd.golfcanada.alexa.handler

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.model.Intent
import com.amazon.ask.model.IntentRequest
import com.amazon.ask.model.RequestEnvelope
import com.amazon.ask.model.Response
import com.amazon.ask.model.ui.PlainTextOutputSpeech
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kjd.golfcanada.alexa.util.TemplateFactoryUtil
import kjd.golfcanada.util.responseFixture

class HelpRequestHandlerTest : DescribeSpec({
    context("canHandle") {
        it("should return true for HelpIntent") {
            val templateFactory = TemplateFactoryUtil.getTemplateFactory()
            val input = HandlerInput.builder()
                .withRequestEnvelope(RequestEnvelope.builder()
                    .withRequest(IntentRequest.builder()
                        .withIntent(Intent.builder()
                            .withName("AMAZON.HelpIntent")
                            .build()
                        ).build()
                    )
                    .build()
                )
                .withTemplateFactory(templateFactory)
                .build()

            val handler = HelpIntentHandler()
            val canHandle = handler.canHandle(input)

            canHandle shouldBe true
        }

        it("should return false for other than HelpIntent") {
            val templateFactory = TemplateFactoryUtil.getTemplateFactory()
            val input = HandlerInput.builder()
                .withRequestEnvelope(RequestEnvelope.builder()
                    .withRequest(IntentRequest.builder()
                        .withIntent(Intent.builder()
                            .withName("AMAZON.StartIntent")
                            .build()
                        ).build()
                    )
                    .build()
                )
                .withTemplateFactory(templateFactory)
                .build()

            val handler = HelpIntentHandler()
            val canHandle = handler.canHandle(input)

            canHandle shouldBe false
        }
    }

    context("handle") {
        it("should return response") {
            val templateFactory = TemplateFactoryUtil.getTemplateFactory()
            val input = HandlerInput.builder()
                .withRequestEnvelope(RequestEnvelope.builder()
                    .withRequest(IntentRequest.builder().build())
                    .build()
                )
                .withTemplateFactory(templateFactory)
                .build()

            val handler = HelpIntentHandler()

            val response = handler.handle(input).get()
            val outputSpeech = response.outputSpeech as PlainTextOutputSpeech

            response.shouldEndSession shouldBe false
            outputSpeech.type shouldBe "PlainText"
            outputSpeech.text shouldBe "Feel free to ask me about things like handicaps, recent scores, course details among other things.  For more information check out the Alexa app skill page.".trimIndent()
        }

        it("should return en response") {
            val templateFactory = TemplateFactoryUtil.getTemplateFactory()
            val input = HandlerInput.builder()
                .withRequestEnvelope(RequestEnvelope.builder()
                    .withRequest(IntentRequest.builder()
                        .withLocale("en-CA").build()
                    )
                    .build()
                )
                .withTemplateFactory(templateFactory)
                .build()

            val handler = HelpIntentHandler()

            val response = handler.handle(input).get()
            val outputSpeech = response.outputSpeech as PlainTextOutputSpeech

            response.shouldEndSession shouldBe false
            outputSpeech.type shouldBe "PlainText"
            outputSpeech.text shouldBe "Feel free to ask me about things like handicaps, recent scores, course details among other things.  For more information check out the Alexa app skill page.".trimIndent()
        }

        it("should return fr response") {
            val templateFactory = TemplateFactoryUtil.getTemplateFactory()
            val input = HandlerInput.builder()
                .withRequestEnvelope(RequestEnvelope.builder()
                    .withRequest(IntentRequest.builder()
                        .withLocale("fr-CA").build()
                    )
                    .build()
                )
                .withTemplateFactory(templateFactory)
                .build()

            val handler = HelpIntentHandler()

            val response = handler.handle(input).get()
            val outputSpeech = response.outputSpeech as PlainTextOutputSpeech

            response.shouldEndSession shouldBe false
            outputSpeech.type shouldBe "PlainText"
            outputSpeech.text shouldBe "N'hésitez pas à me poser des questions sur des choses comme les handicaps, les scores récents, les détails des cours, entre autres.  Pour plus d'informations, consultez la page des compétences de l'application Alexa.".trimIndent()
        }
    }
})
