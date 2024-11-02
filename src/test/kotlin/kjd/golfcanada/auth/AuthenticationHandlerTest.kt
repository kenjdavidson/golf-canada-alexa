package kjd.golfcanada.auth

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import kjd.golfcanada.client.api.AuthApi
import java.util.*

class AuthenticationHandlerTest: DescribeSpec({
    lateinit var authApi: AuthApi

    beforeEach {
        authApi = mockk<AuthApi>()
    }

    describe("handleRequest") {
        describe("/code") {

        }

        describe("/authToken") {

        }

        describe("invalid requests") {
            it("should return 400 when invalid client_id") {
                val handler = AuthenticationHandler(authApi)

                val event = APIGatewayV2HTTPEvent.builder()
                    .withRawPath("/")
                    .build()
                val response = handler.handleRequest(event)

                response shouldNotBe null
                response.statusCode shouldBe 400
                response.body shouldBe "Unknown authentication request"
            }

            it("should return 400 when invalid path") {
                val clientId = UUID.randomUUID()
                val handler = AuthenticationHandler(authApi)

                val event = APIGatewayV2HTTPEvent.builder()
                    .withRawPath("/")
                    .build()
                val response = handler.handleRequest(event)

                response shouldNotBe null
                response.statusCode shouldBe 400
                response.body shouldBe "Unknown authentication request"
            }
        }
    }
})