package kjd.golfcanada.client.model

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class MemberSnapshotTest : DescribeSpec({
    describe("MemberSnapshot") {
        it("should create a MemberSnapshot with all properties") {
            val scores = listOf(
                Score(
                    course = "Dragon's Fire Golf Club",
                    datePlayed = "2025-11-23T00:00:00",
                    holesPlayed = 18,
                    score = 80,
                    isUsedInCalc = false
                ),
                Score(
                    course = "Hidden Lake Golf Club - NEW COURSE",
                    datePlayed = "2025-11-02T00:00:00",
                    holesPlayed = 18,
                    score = 88,
                    isUsedInCalc = false
                )
            )

            val memberSnapshot = MemberSnapshot(
                club = "Blue Springs Golf Club",
                membershipType = "Gold",
                expirationDate = "2026-02-01T00:00:00",
                homeCourse = null,
                defaultTee = null,
                courseHandicap = "NH",
                winterHandicap = "NH",
                winterCourseHandicap = "NH",
                isWinterHandicapDisplayed = false,
                scores = scores,
                clubManagementGroup = "GolfAssociationOfOntario",
                ytdScoreCount = 100,
                ytdScoreAverage = 84.5,
                ytdYear = 2025,
                transferClubName = null,
                transferRequestDate = null,
                lastCardRequestedOn = "2019-04-10T13:19:37.807",
                expectedDeliveryOn = null,
                isCardsSentToClub = false,
                lowHandicap = "5.8",
                lowHandicapOn = "2025-09-06T00:00:00",
                adjustedLowHandicap = "NH",
                displayLowHandicap = "5.8",
                adjustedLowHandicapStartOn = null,
                adjustedLowHandicapEndOn = null,
                lowHandicapAdjustedOn = null,
                isLowIndexAdjusted = false,
                subscriptionRenewsOn = null
            )

            memberSnapshot.club shouldBe "Blue Springs Golf Club"
            memberSnapshot.membershipType shouldBe "Gold"
            memberSnapshot.expirationDate shouldBe "2026-02-01T00:00:00"
            memberSnapshot.homeCourse shouldBe null
            memberSnapshot.defaultTee shouldBe null
            memberSnapshot.courseHandicap shouldBe "NH"
            memberSnapshot.winterHandicap shouldBe "NH"
            memberSnapshot.winterCourseHandicap shouldBe "NH"
            memberSnapshot.isWinterHandicapDisplayed shouldBe false
            memberSnapshot.scores shouldNotBe null
            memberSnapshot.scores?.size shouldBe 2
            memberSnapshot.clubManagementGroup shouldBe "GolfAssociationOfOntario"
            memberSnapshot.ytdScoreCount shouldBe 100
            memberSnapshot.ytdScoreAverage shouldBe 84.5
            memberSnapshot.ytdYear shouldBe 2025
            memberSnapshot.transferClubName shouldBe null
            memberSnapshot.transferRequestDate shouldBe null
            memberSnapshot.lastCardRequestedOn shouldBe "2019-04-10T13:19:37.807"
            memberSnapshot.expectedDeliveryOn shouldBe null
            memberSnapshot.isCardsSentToClub shouldBe false
            memberSnapshot.lowHandicap shouldBe "5.8"
            memberSnapshot.lowHandicapOn shouldBe "2025-09-06T00:00:00"
            memberSnapshot.adjustedLowHandicap shouldBe "NH"
            memberSnapshot.displayLowHandicap shouldBe "5.8"
            memberSnapshot.adjustedLowHandicapStartOn shouldBe null
            memberSnapshot.adjustedLowHandicapEndOn shouldBe null
            memberSnapshot.lowHandicapAdjustedOn shouldBe null
            memberSnapshot.isLowIndexAdjusted shouldBe false
            memberSnapshot.subscriptionRenewsOn shouldBe null
        }
    }
})
