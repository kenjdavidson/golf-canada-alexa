package kjd.golfcanada.alexa.handler

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.model.IntentRequest
import com.amazon.ask.model.RequestEnvelope
import com.amazon.ask.model.ui.PlainTextOutputSpeech
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kjd.golfcanada.alexa.util.TemplateFactoryUtil

class CancelAndStopIntentHandlerTest : DescribeSpec({
    context("canHandle") {
        it("should return true for StopIntent") {
            TODO("Not yet implemented")
        }

        it("should return true for CancelIntent") {
            TODO("Not yet implemented")
        }
        it("should return false for other than StopIntent or CancelIntent") {
            TODO("Not yet implemented")
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

            val handler = CancelAndStopIntentHandler()

            val response = handler.handle(input).get()
            val outputSpeech = response.outputSpeech as PlainTextOutputSpeech

            outputSpeech.type shouldBe "PlainText"
            outputSpeech.text shouldBe "Goodbye and hit em straight!"
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

            val handler = CancelAndStopIntentHandler()

            val response = handler.handle(input).get()
            val outputSpeech = response.outputSpeech as PlainTextOutputSpeech

            outputSpeech.type shouldBe "PlainText"
            outputSpeech.text shouldBe "Goodbye and hit em straight!"
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

            val handler = CancelAndStopIntentHandler()

            val response = handler.handle(input).get()
            val outputSpeech = response.outputSpeech as PlainTextOutputSpeech

            outputSpeech.type shouldBe "PlainText"
            outputSpeech.text shouldBe "Au revoir et frappez-les directement!"
        }
    }
})
