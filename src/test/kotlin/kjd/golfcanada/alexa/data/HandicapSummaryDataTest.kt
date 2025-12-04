package kjd.golfcanada.alexa.data

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kjd.golfcanada.client.model.HandicapCalculation

class HandicapSummaryDataTest : DescribeSpec({
    describe("HandicapSummaryData") {
        context("fromDTO") {
            it("should create HandicapSummaryData from HandicapCalculation DTO") {
                val dto = HandicapCalculation(
                    name = "Ken Davidson",
                    email = "ken.j.davidson@live.ca",
                    club = "Blue Springs Golf Club",
                    handicap = "8.9",
                    handicapDate = "2025-11-23T00:00:00",
                    scores = emptyList(),
                    clubManagementGroup = "Test Group",
                    scoreCount = 20,
                    numCalcScores = 8,
                    handicapScores = emptyList(),
                    adjustedScores = emptyList(),
                    esrTriggerScores = emptyList(),
                    exceptionalScores = emptyList(),
                    pccScores = emptyList(),
                    committeeScores = emptyList(),
                    diffSum = 72.3,
                    avgDiff = 9.0,
                    lowValue = 5.8,
                    isLowValueAdjusted = false,
                    lowIndexAdjustedOn = null,
                    calcValue = 9.0,
                    adjustedValue = 8.9,
                    lowValueDifference = 3.2,
                    softCap = 3.1,
                    hardCap = 5.0,
                    lowScoreCountAdjustment = 0,
                    adjustedAvgDiff = 9.0
                )

                val summary = HandicapSummaryData.fromDTO(dto)

                summary.name shouldBe "Ken Davidson"
                summary.email shouldBe "ken.j.davidson@live.ca"
                summary.lowValue shouldBe 5.8
                summary.handicap shouldBe "8.9"
                summary.averageDifferential shouldBe 9.0
                summary.cachedAt shouldNotBe null
            }

            it("should handle null values in DTO") {
                val dto = HandicapCalculation(
                    name = null,
                    email = null,
                    club = null,
                    handicap = null,
                    handicapDate = null,
                    scores = emptyList(),
                    clubManagementGroup = null,
                    scoreCount = null,
                    numCalcScores = null,
                    handicapScores = emptyList(),
                    adjustedScores = emptyList(),
                    esrTriggerScores = emptyList(),
                    exceptionalScores = emptyList(),
                    pccScores = emptyList(),
                    committeeScores = emptyList(),
                    diffSum = null,
                    avgDiff = null,
                    lowValue = null,
                    isLowValueAdjusted = null,
                    lowIndexAdjustedOn = null,
                    calcValue = null,
                    adjustedValue = null,
                    lowValueDifference = null,
                    softCap = null,
                    hardCap = null,
                    lowScoreCountAdjustment = null,
                    adjustedAvgDiff = null
                )

                val summary = HandicapSummaryData.fromDTO(dto)

                summary.name shouldBe null
                summary.email shouldBe null
                summary.lowValue shouldBe null
                summary.handicap shouldBe null
                summary.averageDifferential shouldBe null
                summary.cachedAt shouldNotBe null
            }
        }

        context("cachedAt") {
            it("should set cachedAt to current time when created") {
                val before = System.currentTimeMillis()
                val summary = HandicapSummaryData(
                    name = "Test",
                    email = "test@test.com",
                    lowValue = 5.0,
                    handicap = "10.0",
                    averageDifferential = 12.5
                )
                val after = System.currentTimeMillis()

                (summary.cachedAt >= before) shouldBe true
                (summary.cachedAt <= after) shouldBe true
            }
        }
    }
})
