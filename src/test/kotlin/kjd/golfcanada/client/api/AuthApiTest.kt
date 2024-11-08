package kjd.golfcanada.client.api

import io.kotest.core.annotation.EnabledIf
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldNotBe
import kjd.golfcanada.auth.AuthenticationHandler.Companion.DEFAULT_SCOPES

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
            val authToken = authApi.getAuthToken("password", username, password, DEFAULT_SCOPES)

            print(authToken)
            authToken shouldNotBe null
            authToken.accessToken shouldNotBe null
            authToken.refreshToken shouldNotBe null
        }
    }
})