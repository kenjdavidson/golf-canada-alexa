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
import kjd.golfcanada.auth.AuthenticationHandler.Companion.DEFAULT_SCOPES
import kjd.golfcanada.auth.impl.TokenRepositoryMapImpl
import kjd.golfcanada.client.api.AuthApi
import kjd.golfcanada.client.model.AuthToken
import kjd.golfcanada.client.model.code
import kjd.golfcanada.client.model.toJson
import kjd.golfcanada.util.apiGatewayHttpEventBuilder
import kjd.golfcanada.util.authToken
import kjd.golfcanada.util.buildBody
import org.apache.http.client.ClientProtocolException
import org.apache.http.client.HttpResponseException
import org.openapitools.client.infrastructure.ClientException
import java.util.*

class AuthenticationHandlerTest : DescribeSpec({
    lateinit var authApi: AuthApi
    lateinit var tokenRepository: TokenRepository<AuthTokenKey, AuthToken>

    val clientId = UUID.randomUUID().toString()
    val clientSecret = UUID.randomUUID().toString()
    val state = "1234567890"

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
                    response.body shouldBe """{
   "error": "invalid_request"
   "code": "100"
}"""
                }
            }

            it("should return 400 when invalid client_id") {
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
                response.statusCode shouldBe 400
                response.body shouldBe """{
   "error": "unauthorized_client"
   "code": "101"
}"""
            }

            // TODO clean this test the hell up!
            it("should return the login page") {
                val handler = AuthenticationHandler(
                    authApi,
                    clientId,
                    clientSecret
                )

                val event = apiGatewayHttpEventBuilder(
                    "GET", "/login", clientId,
                    mapOf(
                        "redirect_uri" to "https://redirect_uri.com",
                        "response_type" to "code",
                        "scope" to AuthenticationHandler.DEFAULT_SCOPES,
                        "state" to state
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
                    response.body shouldBe """{
   "error": "invalid_request"
   "cause": "Invalid Authentication method"
}"""
                }
            }

            it("should return 400 when body is not provided") {
                val handler = AuthenticationHandler(
                    authApi,
                    clientId,
                    clientSecret
                )

                val event = apiGatewayHttpEventBuilder("POST", "/code").build()
                val response = handler.handleRequest(event, TestContext())

                response shouldNotBe null
                response.statusCode shouldBe 400
                response.body shouldBe """{
   "error": "invalid_request"
   "code": "112"
}"""
            }

            it("should return 400 when invalid client_id") {
                val handler = AuthenticationHandler(
                    authApi,
                    clientId,
                    clientSecret
                )

                val event = apiGatewayHttpEventBuilder("POST", "/code",  clientId)
                    .withBody(buildBody(mapOf("username" to "username")))
                    .build()
                val response = handler.handleRequest(event, TestContext())

                response shouldNotBe null
                response.statusCode shouldBe 400
                response.body shouldBe """{
   "error": "unauthorized_client"
   "code": "101"
}"""
            }

            it("should return 400 when invalid response_type") {
                val handler = AuthenticationHandler(
                    authApi,
                    clientId,
                    clientSecret
                )

                val event = apiGatewayHttpEventBuilder("POST", "/code", clientId)
                    .withBody(buildBody(mapOf(
                        "username" to "username",
                        "client_id" to clientId,
                        "response_type" to "something"
                    )))
                    .build()
                val response = handler.handleRequest(event, TestContext())

                response shouldNotBe null
                response.statusCode shouldBe 400
                response.body shouldBe """{
   "error": "invalid_request"
   "code": "106"
}"""
            }

            it("should return 400 when missing redirect uri") {
                val handler = AuthenticationHandler(
                    authApi,
                    clientId,
                    clientSecret
                )

                val event = apiGatewayHttpEventBuilder("POST", "/code", clientId)
                    .withBody(buildBody(mapOf(
                        "username" to "username",
                        "client_id" to clientId,
                        "response_type" to "code"
                    )))
                    .build()
                val response = handler.handleRequest(event, TestContext())

                response shouldNotBe null
                response.statusCode shouldBe 400
                response.body shouldBe """{
   "error": "invalid_request"
   "code": "107"
}"""
            }

            it("should return 400 when missing scope") {
                val handler = AuthenticationHandler(
                    authApi,
                    clientId,
                    clientSecret
                )

                val event = apiGatewayHttpEventBuilder("POST", "/code", clientId)
                    .withBody(buildBody(mapOf(
                        "username" to "username",
                        "client_id" to clientId,
                        "response_type" to "code",
                        "redirect_uri" to "https://redirect-uri.com"
                    )))
                    .build()
                val response = handler.handleRequest(event, TestContext())

                response shouldNotBe null
                response.statusCode shouldBe 400
                response.body shouldBe """{
   "error": "invalid_scope"
   "code": "108"
}"""
            }

            it("should return 400 when missing state") {
                val handler = AuthenticationHandler(
                    authApi,
                    clientId,
                    clientSecret
                )

                val event = apiGatewayHttpEventBuilder("POST", "/code", clientId)
                    .withBody(buildBody(mapOf(
                        "username" to "username",
                        "client_id" to clientId,
                        "response_type" to "code",
                        "redirect_uri" to "https://redirect-uri.com",
                        "scope" to "some scopes here"
                    )))
                    .build()
                val response = handler.handleRequest(event, TestContext())

                response shouldNotBe null
                response.statusCode shouldBe 400
                response.body shouldBe """{
   "error": "invalid_request"
   "code": "104"
}"""
            }

            it ("should return invalid authorization when AuthApi fails") {
                val handler = AuthenticationHandler(
                    authApi,
                    clientId,
                    clientSecret
                )

                val authToken = authToken()
                every {
                    authApi.getAuthToken(
                        AuthApi.GrantTypeGetAuthToken.PASSWORD,
                        AuthenticationHandler.DEFAULT_SCOPES,
                        "username",
                        "password",
                        null
                    )
                } throws ClientException("Invalid Request", 400)

                val event = apiGatewayHttpEventBuilder("POST", "/code", clientId)
                    .withBody(buildBody(mapOf(
                        "username" to "username",
                        "password" to "password",
                        "client_id" to clientId,
                        "response_type" to "code",
                        "redirect_uri" to "https://redirect-uri.com",
                        "scope" to DEFAULT_SCOPES,
                        "state" to state
                    )))
                    .build()
                val response = handler.handleRequest(event, TestContext())

                response shouldNotBe null
                response.statusCode shouldBe 400
                response.body shouldBe """{
   "error": "invalid_request"
   "code": "100"
}"""

                verify(exactly = 1) {
                    authApi.getAuthToken(
                        AuthApi.GrantTypeGetAuthToken.PASSWORD,
                        AuthenticationHandler.DEFAULT_SCOPES,
                        "username",
                        "password",
                        null
                    )
                }
                verify(exactly = 0) {
                    tokenRepository.store(
                        AuthTokenKey(state, "${authToken.hashCode()}"),
                        authToken
                    )
                }
            }

            it("should authorize and redirect") {
                val handler = AuthenticationHandler(
                    authApi,
                    clientId,
                    clientSecret,
                    tokenRepository
                )

                val authToken = authToken()
                every {
                    authApi.getAuthToken(
                        AuthApi.GrantTypeGetAuthToken.PASSWORD,
                        AuthenticationHandler.DEFAULT_SCOPES,
                        "username",
                        "password",
                        null
                    )
                } returns authToken
                every {
                    tokenRepository
                        .store(any(), any())
                } returns Unit

                val event = apiGatewayHttpEventBuilder("POST", "/code", clientId)
                    .withBody(buildBody(mapOf(
                        "username" to "username",
                        "password" to "password",
                        "client_id" to clientId,
                        "response_type" to "code",
                        "redirect_uri" to "https://redirect_uri.com",
                        "scope" to DEFAULT_SCOPES,
                        "state" to state
                    )))
                    .build()
                val response = handler.handleRequest(event, TestContext())

                response shouldNotBe null
                response.statusCode shouldBe 301
                response.body shouldBe "https://redirect_uri.com?code=${authToken.code()}&state=1234567890"

                verify(exactly = 1) {
                    authApi.getAuthToken(
                        AuthApi.GrantTypeGetAuthToken.PASSWORD,
                        AuthenticationHandler.DEFAULT_SCOPES,
                        "username",
                        "password",
                        null
                    )
                }
                verify(exactly = 1) {
                    tokenRepository.store(
                        AuthTokenKey(state, authToken.code()),
                        authToken
                    )
                }
            }
        }

        describe("/authToken") {
            it("should return 400 when not GET method") {
                // TODO: find an Enum somewhere to use values for
                listOf("GET", "PUT", "PATCH", "DELETE", "OPTION").forEach { method ->
                    val handler = AuthenticationHandler(
                        authApi,
                        clientId,
                        clientSecret
                    )

                    val event = APIGatewayV2HTTPEvent.builder()
                        .withRequestContext(
                            APIGatewayV2HTTPEvent.RequestContext.builder()
                                .withHttp(
                                    APIGatewayV2HTTPEvent.RequestContext.Http.builder().withMethod(method).build()
                                )
                                .build()
                        )
                        .withRawPath("/authToken")
                        .build()
                    val response = handler.handleRequest(event, TestContext())

                    response shouldNotBe null
                    response.statusCode shouldBe 400
                    response.body shouldBe """{
   "error": "invalid_request"
   "cause": "173: Invalid Authentication method"
}"""
                }
            }

            it("should return 400 when invalid client_id") {
                val handler = AuthenticationHandler(
                    authApi,
                    clientId,
                    clientSecret
                )

                val invalidClientId = UUID.randomUUID().toString()
                val event = apiGatewayHttpEventBuilder("POST", "/authToken", clientId, mapOf(
                    "client_id" to invalidClientId
                ))
                    .build()
                val response = handler.handleRequest(event, TestContext())

                response shouldNotBe null
                response.statusCode shouldBe 400
                response.body shouldBe """{
   "error": "unauthorized_client"
   "code": "101"
}"""
            }

            it("should return 400 when invalid client_secret") {
                val handler = AuthenticationHandler(
                    authApi,
                    clientId,
                    clientSecret
                )

                val event = apiGatewayHttpEventBuilder("POST", "/authToken", clientId, mapOf(
                    "client_secret" to ""
                ))
                    .build()
                val response = handler.handleRequest(event, TestContext())

                response shouldNotBe null
                response.statusCode shouldBe 400
                response.body shouldBe """{
   "error": "unauthorized_client"
   "code": "102"
}"""
            }

            it ("should throw authentication error when no state found") {
                val handler = AuthenticationHandler(
                    authApi,
                    clientId,
                    clientSecret
                )

                val event = apiGatewayHttpEventBuilder("POST", "/authToken", clientId, mapOf(
                    "client_secret" to clientSecret
                ))
                    .build()
                val response = handler.handleRequest(event, TestContext())

                response shouldNotBe null
                response.statusCode shouldBe 400
                response.body shouldBe """{
   "error": "invalid_grant"
   "code": "111"
}"""
            }

            it("should throw authentication error when no code found") {
                val handler = AuthenticationHandler(
                    authApi,
                    clientId,
                    clientSecret
                )

                val event = apiGatewayHttpEventBuilder("POST", "/authToken", clientId, mapOf(
                    "client_secret" to clientSecret,
                    "state" to state
                ))
                    .build()
                val response = handler.handleRequest(event, TestContext())

                response shouldNotBe null
                response.statusCode shouldBe 400
                response.body shouldBe """{
   "error": "invalid_grant"
   "code": "111"
}"""
            }

            it("should return AuthToken when found") {
                val tokenRepo = TokenRepositoryMapImpl()
                val handler = AuthenticationHandler(
                    authApi,
                    clientId,
                    clientSecret,
                    tokenRepo
                )

                val authToken = authToken()
                tokenRepo.store(AuthTokenKey(state, authToken.code()), authToken)

                val event = apiGatewayHttpEventBuilder("POST", "/authToken", clientId, mapOf(
                    "client_id" to clientId,
                    "client_secret" to clientSecret,
                    "state" to state,
                    "code" to authToken.code(),
                    "grant_type" to "code"
                ))
                    .build()
                val response = handler.handleRequest(event, TestContext())

                response shouldNotBe null
                response.statusCode shouldBe 200
                response.body shouldBe authToken.toJson()

                tokenRepo.get(AuthTokenKey(state, authToken.code())) shouldBe null
            }
        }

        describe("/authToken refresh_code") {

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
                response.body shouldBe """{
   "error": "invalid_request"
   "code": "100"
}"""
            }
        }
    }

})