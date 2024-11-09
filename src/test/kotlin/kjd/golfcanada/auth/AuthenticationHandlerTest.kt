package kjd.golfcanada.auth

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kjd.golfcanada.TestContext
import kjd.golfcanada.client.api.AuthApi
import kjd.golfcanada.client.model.AuthToken
import java.util.*

class AuthenticationHandlerTest : DescribeSpec({
    lateinit var authApi: AuthApi
    lateinit var tokenRepository: TokenRepository<AuthTokenKey, AuthToken>

    val clientId = UUID.randomUUID().toString()
    val clientSecret = UUID.randomUUID().toString()

    fun apiGatewayHttpEventBuilder(
        method: String,
        rawPath: String,
        queryParameters: Map<String, String> = emptyMap()
    ) =
        APIGatewayV2HTTPEvent.builder()
            .withRequestContext(
                APIGatewayV2HTTPEvent.RequestContext.builder()
                    .withHttp(
                        APIGatewayV2HTTPEvent.RequestContext.Http.builder().withMethod(method.uppercase()).build()
                    )
                    .build()
            )
            .withRawPath(rawPath)
            .withQueryStringParameters(
                mapOf("client_id" to clientId).plus(queryParameters)
            )

    fun authToken() =
        AuthToken(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            "2024-01-01T00:00:00",
            3600,
            tokenType = "bearer"
        )

    beforeEach {
        authApi = mockk<AuthApi>()
        tokenRepository = mockk<TokenRepository<AuthTokenKey, AuthToken>>()
    }

    describe("handleRequest") {
        describe("/login") {
            it("should return 400 when not GET method") {
                // TODO: find an Enum somewhere to use values for
                listOf("POST", "PUT", "PATCH", "DELETE", "OPTION").forEach { method ->
                    val handler = AuthenticationHandler(
                        authApi,
                        clientId,
                        clientSecret
                    )

                    val invalidClientId = UUID.randomUUID().toString()
                    val event = APIGatewayV2HTTPEvent.builder()
                        .withRequestContext(
                            APIGatewayV2HTTPEvent.RequestContext.builder()
                                .withHttp(
                                    APIGatewayV2HTTPEvent.RequestContext.Http.builder().withMethod(method).build()
                                )
                                .build()
                        )
                        .withRawPath("/login")
                        .withQueryStringParameters(
                            mapOf(
                                "client_id" to invalidClientId
                            )
                        )
                        .build()
                    val response = handler.handleRequest(event, TestContext())

                    response shouldNotBe null
                    response.statusCode shouldBe 400
                    response.body shouldBe "Invalid Authentication method"
                }
            }

            it("should return 401 when invalid client_id") {
                val handler = AuthenticationHandler(
                    authApi,
                    clientId,
                    clientSecret
                )

                val invalidClientId = UUID.randomUUID().toString()
                val event = APIGatewayV2HTTPEvent.builder()
                    .withRequestContext(
                        APIGatewayV2HTTPEvent.RequestContext.builder()
                            .withHttp(APIGatewayV2HTTPEvent.RequestContext.Http.builder().withMethod("GET").build())
                            .build()
                    )
                    .withRawPath("/login")
                    .withQueryStringParameters(
                        mapOf(
                            "client_id" to invalidClientId
                        )
                    )
                    .build()
                val response = handler.handleRequest(event, TestContext())

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

                val event = apiGatewayHttpEventBuilder(
                    "GET", "/login",
                    mapOf(
                        "redirect_uri" to "https://redirect_uri.com",
                        "response_type" to "code",
                        "scope" to AuthenticationHandler.DEFAULT_SCOPES,
                        "state" to "1234567890"
                    )
                )
                    .build()
                val response = handler.handleRequest(event, TestContext())

                response shouldNotBe null
                response.statusCode shouldBe 200
                response.headers shouldContain Pair("Content-Type", "text/html")
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
            it("should return 400 when not GET method") {
                // TODO: find an Enum somewhere to use values for
                listOf("GET", "PUT", "PATCH", "DELETE", "OPTION").forEach { method ->
                    val handler = AuthenticationHandler(
                        authApi,
                        clientId,
                        clientSecret
                    )

                    val event = apiGatewayHttpEventBuilder(method, "/code").build()
                    val response = handler.handleRequest(event, TestContext())

                    response shouldNotBe null
                    response.statusCode shouldBe 400
                    response.body shouldBe "Invalid Authentication method"
                }
            }

            it("should return 401 when invalid client_id") {
                val handler = AuthenticationHandler(
                    authApi,
                    clientId,
                    clientSecret
                )

                val invalidClientId = UUID.randomUUID().toString()
                val event = apiGatewayHttpEventBuilder(
                    "POST", "/code", mapOf(
                        "client_id" to invalidClientId
                    )
                ).build()
                val response = handler.handleRequest(event, TestContext())

                response shouldNotBe null
                response.statusCode shouldBe 401
                response.body shouldBe "Invalid Client Id: '$invalidClientId'"
            }

            it ("should return invalid authorization when failed") {
                val handler = AuthenticationHandler(
                    authApi,
                    clientId,
                    clientSecret
                )

                val authToken = authToken()
                every {
                    authApi.getAuthToken(
                        AuthApi.GrantTypeGetAuthToken.PASSWORD,
                        "username",
                        "password1",
                        AuthenticationHandler.DEFAULT_SCOPES
                    )
                } returns (authToken)

                val event = apiGatewayHttpEventBuilder(
                    "POST", "/code", mapOf(
                        "redirect_uri" to "https://redirect_uri.com",
                        "response_type" to "code",
                        "scope" to AuthenticationHandler.DEFAULT_SCOPES,
                        "state" to "1234567890"
                    )
                )
                    .build()
                val response = handler.handleRequest(event, TestContext())

                response shouldNotBe null
                response.statusCode shouldBe 302
                response.body shouldBe "https://redirect_uri.com?state=1234567890&code=${authToken.hashCode()}"

                verify(exactly = 1) {
                    authApi.getAuthToken(
                        AuthApi.GrantTypeGetAuthToken.PASSWORD,
                        "username",
                        "password1",
                        AuthenticationHandler.DEFAULT_SCOPES
                    )
                }
                verify(exactly = 1) {
                    tokenRepository.store(
                        AuthTokenKey("1234567890", "${authToken.hashCode()}"),
                        authToken
                    )
                }
            }

            it("should authorize and redirect") {
                val handler = AuthenticationHandler(
                    authApi,
                    clientId,
                    clientSecret
                )

                val authToken = authToken()
                every {
                    authApi.getAuthToken(
                        AuthApi.GrantTypeGetAuthToken.PASSWORD,
                        "username",
                        "password1",
                        AuthenticationHandler.DEFAULT_SCOPES
                    )
                } returns (authToken)

                val event = apiGatewayHttpEventBuilder(
                    "POST", "/code", mapOf(
                        "redirect_uri" to "https://redirect_uri.com",
                        "response_type" to "code",
                        "scope" to AuthenticationHandler.DEFAULT_SCOPES,
                        "state" to "1234567890"
                    )
                )
                    .build()
                val response = handler.handleRequest(event, TestContext())

                response shouldNotBe null
                response.statusCode shouldBe 302
                response.body shouldBe "https://redirect_uri.com?state=1234567890&code=${authToken.hashCode()}"

                verify(exactly = 1) {
                    authApi.getAuthToken(
                        AuthApi.GrantTypeGetAuthToken.PASSWORD,
                        "username",
                        "password1",
                        AuthenticationHandler.DEFAULT_SCOPES
                    )
                }
                verify(exactly = 1) {
                    tokenRepository.store(
                        AuthTokenKey("1234567890", "${authToken.hashCode()}"),
                        authToken
                    )
                }
            }
        }

        describe("/authToken") {
            it ("should throw authentication error when no state found") {

            }

            it("should throw authentication error when no code found") {

            }

            it("should return AuthToken when found") {

            }

            it("should refresh api token when requested") {

            }
        }

        describe("invalid requests") {
            it("should return 400 when invalid path") {
                val handler = AuthenticationHandler(
                    authApi,
                    clientId,
                    clientSecret
                )

                val event = apiGatewayHttpEventBuilder("GET", "/").build()
                val response = handler.handleRequest(event, TestContext())

                response shouldNotBe null
                response.statusCode shouldBe 400
                response.body shouldBe "Unknown authentication request"
            }
        }
    }

})