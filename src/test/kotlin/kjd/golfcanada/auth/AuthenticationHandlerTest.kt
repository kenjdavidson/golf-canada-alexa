package kjd.golfcanada.auth

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import kjd.golfcanada.client.api.AuthApi
import kjd.golfcanada.client.model.AuthToken
import java.time.OffsetDateTime
import java.util.*

class AuthenticationHandlerTest: DescribeSpec({
    lateinit var authApi: AuthApi

    val clientId = UUID.randomUUID().toString()
    val clientSecret = UUID.randomUUID().toString()

    fun buildAPIGatewayHTTPEvent(rawPath: String) =
        APIGatewayV2HTTPEvent.builder()
            .withRawPath(rawPath)
            .withQueryStringParameters(mapOf(
                "client_id" to clientId
            ))

    fun authToken() =
        AuthToken(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            OffsetDateTime.now(),
            3600,
            tokenType = "bearer"
        )

    beforeEach {
        authApi = mockk<AuthApi>()
    }

    describe("handleRequest") {
        describe("/login") {
            it("should return 401 when invalid client_id") {
                val handler = AuthenticationHandler(
                    authApi,
                    clientId,
                    clientSecret
                )

                val invalidClientId = UUID.randomUUID().toString()
                val event = APIGatewayV2HTTPEvent.builder()
                    .withRawPath("/login")
                    .withQueryStringParameters(mapOf(
                        "client_id" to invalidClientId
                    ))
                    .build()
                val response = handler.handleRequest(event)

                response shouldNotBe null
                response.statusCode shouldBe 401
                response.body shouldBe "Invalid Client Id: '$invalidClientId'"
            }

            // TODO clean this test the hell up!
            it("should return the login page") {
                val handler = AuthenticationHandler(
                    authApi,
                    clientId,
                    clientSecret
                )

                val event = APIGatewayV2HTTPEvent.builder()
                    .withRawPath("/login")
                    .withQueryStringParameters(mapOf(
                        "client_id" to clientId,
                        "redirect_uri" to "https://redirect_uri.com",
                        "response_type" to "code",
                        "scope" to AuthenticationHandler.DEFAULT_SCOPES,
                        "state" to "1234567890"
                    ))
                    .build()
                val response = handler.handleRequest(event)

                response shouldNotBe null
                response.statusCode shouldBe 200
                response.body shouldContain "value=\"$clientId\""
                response.body shouldContain "value=\"https://redirect_uri.com\""
                response.body shouldContain "value=\"code\""
                response.body shouldContain "value=\"${AuthenticationHandler.DEFAULT_SCOPES}\""
                response.body shouldContain "value=\"1234567890\""
                response.body shouldContain "aria-label=\"Username\""
                response.body shouldContain "aria-label=\"Password\""
                response.body shouldContain "<button type=\"submit\">Login</button>"
                response.body shouldContain "<button type=\"reset\">Cancel</button>"
            }
        }

        describe("/code") {
            it("should return 401 when invalid client_id") {
                val handler = AuthenticationHandler(
                    authApi,
                    clientId,
                    clientSecret
                )

                val invalidClientId = UUID.randomUUID().toString()
                val event = APIGatewayV2HTTPEvent.builder()
                    .withRawPath("/code")
                    .withQueryStringParameters(mapOf(
                        "client_id" to invalidClientId
                    ))
                    .build()
                val response = handler.handleRequest(event)

                response shouldNotBe null
                response.statusCode shouldBe 401
                response.body shouldBe "Invalid Client Id: '$invalidClientId'"
            }

            it("should authorize and return a code when successful") {
                val handler = AuthenticationHandler(
                    authApi,
                    clientId,
                    clientSecret
                )

                val authToken = authToken()
                every {
                    authApi.getAuthToken(
                        "password",
                        "username",
                        "password",
                        AuthenticationHandler.DEFAULT_SCOPES
                    )
                } returns(authToken)

                val event = buildAPIGatewayHTTPEvent("/code").build()
                val response = handler.handleRequest(event)

                response shouldNotBe null
                response.statusCode shouldBe 401
                response.body shouldBe "Invalid client_id provided"
            }
        }

        describe("/authToken") {
            TODO("not yet implemented")
        }

        describe("invalid requests") {
            it("should return 400 when invalid path") {
                val handler = AuthenticationHandler(
                    authApi,
                    clientId,
                    clientSecret
                )

                val event = buildAPIGatewayHTTPEvent("/").build()
                val response = handler.handleRequest(event)

                response shouldNotBe null
                response.statusCode shouldBe 400
                response.body shouldBe "Unknown authentication request"
            }
        }
    }

})