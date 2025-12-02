package kjd.golfcanada.alexa.handler

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.model.IntentRequest
import com.amazon.ask.model.RequestEnvelope
import com.amazon.ask.model.ui.LinkAccountCard
import com.amazon.ask.model.ui.PlainTextOutputSpeech
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kjd.golfcanada.alexa.exception.NoUserDetailsException
import kjd.golfcanada.alexa.util.TemplateFactoryUtil

class NoUserDetailsExceptionHandlerTest : DescribeSpec({

    val handler = NoUserDetailsExceptionHandler()

    context("canHandle") {
        it("should return true for NoUserDetailsException") {
            val input = HandlerInput.builder()
                .withRequestEnvelope(
                    RequestEnvelope.builder()
                        .withRequest(IntentRequest.builder().build())
                        .build()
                )
                .build()

            val exception = NoUserDetailsException()

            handler.canHandle(input, exception) shouldBe true
        }

        it("should return false for other exceptions") {
            val input = HandlerInput.builder()
                .withRequestEnvelope(
                    RequestEnvelope.builder()
                        .withRequest(IntentRequest.builder().build())
                        .build()
                )
                .build()

            val exception = RuntimeException("Some other error")

            handler.canHandle(input, exception) shouldBe false
        }
    }

    context("handle") {
        it("should return response with account linking card") {
            val templateFactory = TemplateFactoryUtil.getTemplateFactory()
            val input = HandlerInput.builder()
                .withRequestEnvelope(
                    RequestEnvelope.builder()
                        .withRequest(IntentRequest.builder().build())
                        .build()
                )
                .withTemplateFactory(templateFactory)
                .build()

            val exception = NoUserDetailsException()

            val response = handler.handle(input, exception).get()

            response.shouldEndSession shouldBe true
            response.card.shouldBeInstanceOf<LinkAccountCard>()
            response.outputSpeech.shouldBeInstanceOf<PlainTextOutputSpeech>()
        }

        it("should return en response") {
            val templateFactory = TemplateFactoryUtil.getTemplateFactory()
            val input = HandlerInput.builder()
                .withRequestEnvelope(
                    RequestEnvelope.builder()
                        .withRequest(
                            IntentRequest.builder()
                                .withLocale("en-CA")
                                .build()
                        )
                        .build()
                )
                .withTemplateFactory(templateFactory)
                .build()

            val exception = NoUserDetailsException()

            val response = handler.handle(input, exception).get()
            val outputSpeech = response.outputSpeech as PlainTextOutputSpeech

            response.shouldEndSession shouldBe true
            outputSpeech.text shouldBe "To use this feature, you need to link your Golf Canada account. Please open the Alexa app and link your account from the skill settings."
        }

        it("should return fr response") {
            val templateFactory = TemplateFactoryUtil.getTemplateFactory()
            val input = HandlerInput.builder()
                .withRequestEnvelope(
                    RequestEnvelope.builder()
                        .withRequest(
                            IntentRequest.builder()
                                .withLocale("fr-CA")
                                .build()
                        )
                        .build()
                )
                .withTemplateFactory(templateFactory)
                .build()

            val exception = NoUserDetailsException()

            val response = handler.handle(input, exception).get()
            val outputSpeech = response.outputSpeech as PlainTextOutputSpeech

            response.shouldEndSession shouldBe true
            outputSpeech.text shouldBe "Pour utiliser cette fonctionnalité, vous devez associer votre compte Golf Canada. Veuillez ouvrir l'application Alexa et associer votre compte dans les paramètres de la compétence."
        }
    }
})
