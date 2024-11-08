package kjd.golfcanada.client.model

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class AuthTokenTest : DescribeSpec({
    describe("AuthToken") {
        it("should just work") {
            val authToken = AuthToken(
                "accessToken",
                "refreshToken",
                "idToken",
                expiresIn = 3600,
                tokenType = "bearer",
                user = null
            )

            authToken.accessToken shouldBe "accessToken"
            authToken.refreshToken shouldBe "refreshToken"
            authToken.idToken shouldBe "idToken"
            authToken.expiresIn shouldBe 3600
            authToken.user shouldBe null
        }
    }
})
