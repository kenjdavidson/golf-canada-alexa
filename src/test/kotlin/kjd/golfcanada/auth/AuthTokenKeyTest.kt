package kjd.golfcanada.auth

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import kjd.golfcanada.client.model.AuthToken

class AuthTokenKeyTest : DescribeSpec({
    describe("AuthTokenKey") {
        it("should just work") {
            val authTokenKey = AuthTokenKey("code", "state")

            authTokenKey shouldBe AuthTokenKey("code", "state")
        }
    }
})
