package kjd.golfcanada.alexa.handler

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.model.Intent
import com.amazon.ask.model.IntentRequest
import com.amazon.ask.model.RequestEnvelope
import com.amazon.ask.model.ui.PlainTextOutputSpeech
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kjd.golfcanada.alexa.util.TemplateFactoryUtil

class PlayerProfileHistoryIntentHandlerTest : DescribeSpec({
    context("canHandle") {
        it("should return true for GOLFCANADA.PlayerProfileHistory") {
            val templateFactory = TemplateFactoryUtil.getTemplateFactory()
            val input = HandlerInput.builder()
                .withRequestEnvelope(RequestEnvelope.builder()
                    .withRequest(IntentRequest.builder()
                        .withIntent(Intent.builder()
                            .withName("GOLFCANADA.PlayerProfileHistory")
                            .build()
                        ).build()
                    )
                    .build()
                )
                .withTemplateFactory(templateFactory)
                .build()

            val handler = PlayerProfileHistoryIntentHandler()
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

            val handler = PlayerProfileHistoryIntentHandler()
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

            val handler = PlayerProfileHistoryIntentHandler()

            val response = handler.handle(input).get()
            val outputSpeech = response.outputSpeech as PlainTextOutputSpeech

            response.shouldEndSession shouldBe false
            outputSpeech.type shouldBe "PlainText"
            outputSpeech.text shouldBe "Your score history is not yet available. This feature is under development."
        }

        it("should return fr response") {
            val templateFactory = TemplateFactoryUtil.getTemplateFactory()
            val input = HandlerInput.builder()
                .withRequestEnvelope(RequestEnvelope.builder()
                    .withRequest(IntentRequest.builder()
                        .withLocale("fr-CA").build())
                    .build()
                )
                .withTemplateFactory(templateFactory)
                .build()

            val handler = PlayerProfileHistoryIntentHandler()

            val response = handler.handle(input).get()
            val outputSpeech = response.outputSpeech as PlainTextOutputSpeech

            response.shouldEndSession shouldBe false
            outputSpeech.type shouldBe "PlainText"
            outputSpeech.text shouldBe "Votre historique de scores n'est pas encore disponible. Cette fonctionnalité est en cours de développement."
        }
    }
})
