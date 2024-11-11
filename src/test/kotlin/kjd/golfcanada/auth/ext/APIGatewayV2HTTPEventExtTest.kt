package kjd.golfcanada.auth.ext

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent.RequestContext.Http
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent.RequestContext
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kjd.golfcanada.auth.AuthenticationException
import kjd.golfcanada.auth.ErrorCode
import kjd.golfcanada.auth.invalidAuthenticationRequest
import org.junit.jupiter.api.assertThrows

class APIGatewayV2HTTPEventExtTest: DescribeSpec({
    fun buildEvent(path: String, queryParams: Map<String,String> = emptyMap()) =
        APIGatewayV2HTTPEvent.builder()
            .withRequestContext(
                RequestContext.builder()
                    .withHttp(Http.builder()
                        .withMethod("GET").build())
                    .build()
            )
            .withQueryStringParameters(queryParams)
            .build()

    describe("assertHttpMethod") {
        it("should throw when not expected method") {
            val event = buildEvent("GET")

            assertThrows<AuthenticationException> {
                event.assertHttpMethod("POST") { invalidAuthenticationRequest(ErrorCode.INVALID_API_CALL) }
            }
        }

        it("should not throw when expected method") {
            val event = buildEvent("GET")
            event.assertHttpMethod("GET") { invalidAuthenticationRequest(ErrorCode.INVALID_API_CALL) }
        }
    }

    describe("assertQueryParameter matches") {
        it("should throw when not matching expected") {
            val event = buildEvent("GET")

            assertThrows<AuthenticationException> {
                event.assertQueryParameter("param1", "value") { invalidAuthenticationRequest(ErrorCode.INVALID_API_CALL) }
            }
        }

        it("should return value when matching") {
            val event = buildEvent("GET", mapOf(
                "param1" to "value"
            ))

            event.assertQueryParameter("param1", "value") { invalidAuthenticationRequest(ErrorCode.INVALID_API_CALL) }
        }
    }

    describe("assertQueryParameter exists") {
        it("should throw when not exists") {
            val event = buildEvent("GET")

            assertThrows<AuthenticationException> {
                event.assertQueryParameter("param1") { invalidAuthenticationRequest(ErrorCode.INVALID_API_CALL) }
            }
        }

        it("should return value when exists") {
            val event = buildEvent("GET", mapOf(
                "param1" to "value"
            ))

            val value = event.assertQueryParameter("param1", "value") { invalidAuthenticationRequest(ErrorCode.INVALID_API_CALL) }
            value shouldBe "value"
        }
    }
})