package kjd.golfcanada.client.model

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class HandicapHistoryEntryTest : DescribeSpec({
    describe("HandicapHistoryEntry") {
        it("should create a HandicapHistoryEntry with all properties") {
            val entry = HandicapHistoryEntry(
                date = "2025-04-18T00:00:00",
                handicap = 9.7,
                lowHandicap = 9.3
            )

            entry.date shouldBe "2025-04-18T00:00:00"
            entry.handicap shouldBe 9.7
            entry.lowHandicap shouldBe 9.3
        }

        it("should create a HandicapHistoryEntry with null properties") {
            val entry = HandicapHistoryEntry(
                date = null,
                handicap = null,
                lowHandicap = null
            )

            entry.date shouldBe null
            entry.handicap shouldBe null
            entry.lowHandicap shouldBe null
        }

        it("should create a HandicapHistoryEntry with default properties") {
            val entry = HandicapHistoryEntry()

            entry.date shouldBe null
            entry.handicap shouldBe null
            entry.lowHandicap shouldBe null
        }
    }
})
