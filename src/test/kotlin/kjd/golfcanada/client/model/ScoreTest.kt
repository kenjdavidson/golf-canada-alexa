package kjd.golfcanada.client.model

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class ScoreTest : DescribeSpec({
    describe("Score") {
        it("should create a Score with all properties") {
            val score = Score(
                course = "Dragon's Fire Golf Club",
                datePlayed = "2025-11-23T00:00:00",
                holesPlayed = 18,
                score = 80,
                isUsedInCalc = false
            )

            score.course shouldBe "Dragon's Fire Golf Club"
            score.datePlayed shouldBe "2025-11-23T00:00:00"
            score.holesPlayed shouldBe 18
            score.score shouldBe 80
            score.isUsedInCalc shouldBe false
        }

        it("should support null values") {
            val score = Score(
                course = null,
                datePlayed = null,
                holesPlayed = null,
                score = null,
                isUsedInCalc = null
            )

            score.course shouldBe null
            score.datePlayed shouldBe null
            score.holesPlayed shouldBe null
            score.score shouldBe null
            score.isUsedInCalc shouldBe null
        }
    }
})
