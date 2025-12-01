package kjd.golfcanada.alexa.handler

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.model.LaunchRequest
import com.amazon.ask.model.RequestEnvelope
import com.amazon.ask.model.ui.PlainTextOutputSpeech
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kjd.golfcanada.alexa.util.TemplateFactoryUtil

class LaunchRequestHandlerTest : DescribeSpec({
    context("canHandle") {
        it("should return true for LaunchRequest") {
            val templateFactory = TemplateFactoryUtil.getTemplateFactory()
            val input = HandlerInput.builder()
                .withRequestEnvelope(RequestEnvelope.builder()
                    .withRequest(LaunchRequest.builder().build())
                    .build()
                )
                .withTemplateFactory(templateFactory)
                .build()

            val handler = LaunchRequestHandler()
            val canHandle = handler.canHandle(input, request = null)

            canHandle shouldBe true
        }
    }

    context("handle") {
        it("should return response") {
            val templateFactory = TemplateFactoryUtil.getTemplateFactory()
            val input = HandlerInput.builder()
                .withRequestEnvelope(RequestEnvelope.builder()
                    .withRequest(LaunchRequest.builder().build())
                    .build()
                )
                .withTemplateFactory(templateFactory)
                .build()

            val handler = LaunchRequestHandler()

            val response = handler.handle(input, null).get()
            val outputSpeech = response.outputSpeech as PlainTextOutputSpeech

            response.shouldEndSession shouldBe false
            outputSpeech.type shouldBe "PlainText"
            outputSpeech.text shouldBe "Welcome to Golf Canada! You can ask me about your handicap, recent scores, membership status, and more. What would you like to know?"
        }

        it("should return en response") {
            val templateFactory = TemplateFactoryUtil.getTemplateFactory()
            val input = HandlerInput.builder()
                .withRequestEnvelope(RequestEnvelope.builder()
                    .withRequest(LaunchRequest.builder()
                        .withLocale("en-CA").build())
                    .build()
                )
                .withTemplateFactory(templateFactory)
                .build()

            val handler = LaunchRequestHandler()

            val response = handler.handle(input, null).get()
            val outputSpeech = response.outputSpeech as PlainTextOutputSpeech

            response.shouldEndSession shouldBe false
            outputSpeech.type shouldBe "PlainText"
            outputSpeech.text shouldBe "Welcome to Golf Canada! You can ask me about your handicap, recent scores, membership status, and more. What would you like to know?"
        }

        it("should return fr response") {
            val templateFactory = TemplateFactoryUtil.getTemplateFactory()
            val input = HandlerInput.builder()
                .withRequestEnvelope(RequestEnvelope.builder()
                    .withRequest(LaunchRequest.builder()
                        .withLocale("fr-CA").build())
                    .build()
                )
                .withTemplateFactory(templateFactory)
                .build()

            val handler = LaunchRequestHandler()

            val response = handler.handle(input, null).get()
            val outputSpeech = response.outputSpeech as PlainTextOutputSpeech

            response.shouldEndSession shouldBe false
            outputSpeech.type shouldBe "PlainText"
            outputSpeech.text shouldBe "Bienvenue à Golf Canada! Vous pouvez me demander votre handicap, vos scores récents, votre statut de membre, et plus encore. Qu'aimeriez-vous savoir?"
        }
    }
})
