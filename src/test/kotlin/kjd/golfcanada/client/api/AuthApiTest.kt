package kjd.golfcanada.client.api

import io.kotest.core.annotation.EnabledIf
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kjd.golfcanada.auth.AuthenticationHandler.Companion.DEFAULT_SCOPES
import kjd.golfcanada.client.api.AuthApi.GrantTypeGetAuthToken

@EnabledIf(UsernamePasswordCondition::class)
class AuthApiTest: DescribeSpec({
    lateinit var authApi: AuthApi
    lateinit var username: String
    lateinit var password: String

    beforeEach {
        authApi = AuthApi()
        username = System.getenv("TEST_USERNAME") ?: ""
        password = System.getenv("TEST_PASSWORD") ?: ""
    }

    context("getAuthToken") {
        it("should login successfully") {
            val authToken = authApi.getAuthToken(GrantTypeGetAuthToken.PASSWORD, DEFAULT_SCOPES, username = username, password = password)

            println(authToken)

            authToken shouldNotBe null
            authToken.accessToken shouldNotBe null
            authToken.refreshToken shouldNotBe null
            authToken.expiresIn shouldBe 3600

            val refreshedToken = authApi.getAuthToken(GrantTypeGetAuthToken.REFRESH_TOKEN, DEFAULT_SCOPES, refreshToken = authToken.refreshToken)

            println(refreshedToken)

            refreshedToken shouldNotBe null
            refreshedToken.accessToken shouldNotBe null
            refreshedToken.refreshToken shouldNotBe null
            refreshedToken.expiresIn shouldBe 3600
        }
    }
})