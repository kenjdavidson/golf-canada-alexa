package kjd.golfcanada.client.model

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.OffsetDateTime

class AuthTokenTest : DescribeSpec({
    describe("AuthToken") {
        it("should just work") {
            val expireDate = OffsetDateTime.now()
            val authToken = AuthToken(
                "accessToken",
                "refreshToken",
                expireDate,
                expiresIn = 3600,
                tokenType = "bearer",
                user = null
            )

            authToken.accessToken shouldBe "accessToken"
            authToken.refreshToken shouldBe "refreshToken"
            authToken.expireDate shouldBe expireDate
            authToken.expiresIn shouldBe 3600
            authToken.user shouldBe null
        }
    }
})
