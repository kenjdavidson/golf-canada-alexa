package kjd.golfcanada.alexa.interceptor

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.model.Context
import com.amazon.ask.model.IntentRequest
import com.amazon.ask.model.RequestEnvelope
import com.amazon.ask.model.interfaces.system.SystemState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldBeNull
import kjd.golfcanada.alexa.AuthTokenContext
import kjd.golfcanada.alexa.util.TemplateFactoryUtil
import java.util.Optional

class AuthenticationInterceptorTest : DescribeSpec({
    beforeTest {
        AuthTokenContext.clearToken()
    }

    afterTest {
        AuthTokenContext.clearToken()
    }

    context("process (request interceptor)") {
        it("should set auth token when apiAccessToken is present") {
            val testToken = "test-api-access-token"
            val templateFactory = TemplateFactoryUtil.getTemplateFactory()
            val input = HandlerInput.builder()
                .withRequestEnvelope(RequestEnvelope.builder()
                    .withContext(Context.builder()
                        .withSystem(SystemState.builder()
                            .withApiAccessToken(testToken)
                            .build()
                        )
                        .build()
                    )
                    .withRequest(IntentRequest.builder().build())
                    .build()
                )
                .withTemplateFactory(templateFactory)
                .build()

            val interceptor = AuthenticationInterceptor()
            interceptor.process(input)

            AuthTokenContext.getToken() shouldBe testToken
        }

        it("should not set auth token when context is null") {
            val templateFactory = TemplateFactoryUtil.getTemplateFactory()
            val input = HandlerInput.builder()
                .withRequestEnvelope(RequestEnvelope.builder()
                    .withRequest(IntentRequest.builder().build())
                    .build()
                )
                .withTemplateFactory(templateFactory)
                .build()

            val interceptor = AuthenticationInterceptor()
            interceptor.process(input)

            AuthTokenContext.getToken().shouldBeNull()
        }

        it("should not set auth token when system is null") {
            val templateFactory = TemplateFactoryUtil.getTemplateFactory()
            val input = HandlerInput.builder()
                .withRequestEnvelope(RequestEnvelope.builder()
                    .withContext(Context.builder().build())
                    .withRequest(IntentRequest.builder().build())
                    .build()
                )
                .withTemplateFactory(templateFactory)
                .build()

            val interceptor = AuthenticationInterceptor()
            interceptor.process(input)

            AuthTokenContext.getToken().shouldBeNull()
        }

        it("should not set auth token when apiAccessToken is null") {
            val templateFactory = TemplateFactoryUtil.getTemplateFactory()
            val input = HandlerInput.builder()
                .withRequestEnvelope(RequestEnvelope.builder()
                    .withContext(Context.builder()
                        .withSystem(SystemState.builder().build())
                        .build()
                    )
                    .withRequest(IntentRequest.builder().build())
                    .build()
                )
                .withTemplateFactory(templateFactory)
                .build()

            val interceptor = AuthenticationInterceptor()
            interceptor.process(input)

            AuthTokenContext.getToken().shouldBeNull()
        }
    }

    context("process (response interceptor)") {
        it("should clear auth token") {
            AuthTokenContext.setToken("some-token")
            AuthTokenContext.getToken() shouldBe "some-token"

            val templateFactory = TemplateFactoryUtil.getTemplateFactory()
            val input = HandlerInput.builder()
                .withRequestEnvelope(RequestEnvelope.builder()
                    .withRequest(IntentRequest.builder().build())
                    .build()
                )
                .withTemplateFactory(templateFactory)
                .build()

            val interceptor = AuthenticationInterceptor()
            interceptor.process(input, Optional.empty())

            AuthTokenContext.getToken().shouldBeNull()
        }
    }
})
