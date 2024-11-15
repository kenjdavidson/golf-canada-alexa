package kjd.golfcanada.client.api

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import io.kotest.core.annotation.EnabledIf
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import kjd.golfcanada.TestContext
import kjd.golfcanada.auth.AuthenticationHandler
import kjd.golfcanada.auth.AuthenticationHandler.Companion.DEFAULT_SCOPES
import kjd.golfcanada.client.api.AuthApi.GrantTypeGetAuthToken
import kjd.golfcanada.client.model.code
import kjd.golfcanada.util.buildBody
import org.junit.jupiter.api.assertThrows
import java.util.*

@EnabledIf(UsernamePasswordCondition::class)
class AuthApiTest: DescribeSpec({
    lateinit var authApi: AuthApi
    lateinit var username: String
    lateinit var password: String

    fun apiGatewayHttpEventBuilder(
        method: String,
        rawPath: String
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

    beforeEach {
        authApi = AuthApi()
        username = System.getenv("TEST_USERNAME") ?: ""
        password = System.getenv("TEST_PASSWORD") ?: ""
    }

    describe("getAuthToken") {
        it("should login successfully") {
            val authToken = authApi.getAuthToken(GrantTypeGetAuthToken.PASSWORD, DEFAULT_SCOPES, username = username, password = password)

            authToken shouldNotBe null
            authToken.accessToken shouldNotBe null
            authToken.refreshToken shouldNotBe null
            authToken.expiresIn shouldBe 3600

            val refreshedToken = authApi.getAuthToken(GrantTypeGetAuthToken.REFRESH_TOKEN, DEFAULT_SCOPES, refreshToken = authToken.refreshToken)

            refreshedToken shouldNotBe null
            refreshedToken.accessToken shouldNotBe null
            refreshedToken.refreshToken shouldNotBe null
            refreshedToken.expiresIn shouldBe 3600

            println(authToken)
            println(refreshedToken)
        }

        it("should return bad request when invalid login") {
            val response = assertThrows<Exception> {
                authApi.getAuthToken(GrantTypeGetAuthToken.PASSWORD, DEFAULT_SCOPES, username = "${username}1", password = password)
            }

            response shouldNotBe null
            response.message shouldBe "Client error : 400 Bad Request"
        }
    }

    describe("AuthenticationHandler") {
        it("should successfully make request and return redirect") {
            val authToken = authApi.getAuthToken(GrantTypeGetAuthToken.PASSWORD, DEFAULT_SCOPES, username = username, password = password)

            authToken shouldNotBe null
            authToken.accessToken shouldNotBe null
            authToken.refreshToken shouldNotBe null
            authToken.expiresIn shouldBe 3600

            val uuid = UUID.randomUUID().toString()
            val handler = AuthenticationHandler(AuthApi(), uuid, uuid)

            val event = apiGatewayHttpEventBuilder("POST", "/code")
                .withBody(buildBody(mapOf(
                    "client_id" to uuid,
                    "response_type" to "code",
                    "redirect_uri" to "https://redirect_uri.com",
                    "state" to "1234567890",
                    "scope" to DEFAULT_SCOPES,
                    "grant_type" to "code",
                    "username" to username,
                    "password" to password
                )))
                .build()
            val response = handler.handleRequest(event, TestContext())

            response.statusCode shouldBe 301
            response.body shouldContain "https://redirect_uri.com?code="
            response.body shouldContain "&state=1234567890"
        }
    }
})