package kjd.golfcanada.client.model

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.time.OffsetDateTime

class UserTest : DescribeSpec({
    describe("User") {
        it("should just work") {
            val user = User(
                allowedScorePosting = true,
                authUserId = 1111111111L,
                clubManagementGroup = 1,
                email = "test@email.com",
                expirationDate = "2024-01-01T00:00:00",
                firstName = "Test",
                fullName = "Test User",
                golfCanadaCardId = "11111111",
                handicap = "2.1",
                id = 1111111111L,
                lastName = "User",
                membershipLevel = "Gold",
                networkId = "1",
                termsAndConditionsDate = "2024-01-01T00:00:00",
                username = "test_user_1",
                scoreDefaults = ScoreDefaults(
                    "Golf and Country Club",
                    "RGNA"
                )
            )
        }
    }
})
