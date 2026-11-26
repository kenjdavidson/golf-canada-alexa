package kjd.golfcanada.alexa

import com.squareup.moshi.Moshi
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.io.File

class InteractionModelTest : DescribeSpec({
    describe("the supplied interaction model") {
        val root = Moshi.Builder().build().adapter(Any::class.java)
            .fromJson(File("model/model.json").readText()) as Map<*, *>
        val interactionModel = root["interactionModel"] as Map<*, *>
        val languageModel = interactionModel["languageModel"] as Map<*, *>
        val intents = (languageModel["intents"] as List<*>).map { it as Map<*, *> }

        it("preserves the invocation, intents, and tee values") {
            languageModel["invocationName"] shouldBe "golf canada"
            intents.map { it["name"] } shouldBe listOf(
                "AMAZON.CancelIntent", "AMAZON.HelpIntent", "AMAZON.StopIntent",
                "AMAZON.NavigateHomeIntent", "AMAZON.FallbackIntent",
                "GOLFCANADA.Handicap", "GOLFCANADA.FavoritePlayerHistory",
                "GOLFCANADA.PlayerProfileMembership", "GOLFCANADA.PlayerProfileHistory",
                "GOLFCANADA.AddScorecard", "GOLFCANADA.FavoriteCourses",
                "GOLFCANADA.CourseHandicap"
            )
            val tee = (languageModel["types"] as List<*>).single() as Map<*, *>
            tee["name"] shouldBe "TeeName"
            (tee["values"] as List<*>).map {
                ((it as Map<*, *>)["name"] as Map<*, *>)["value"]
            } shouldBe listOf("Green", "Red", "White", "Blue", "Silver", "Black", "Gold", "Blue White")
        }

        it("declares every slot used by a sample") {
            val slotPattern = Regex("\\{([^}]+)}")
            intents.forEach { intent ->
                val slots = (intent["slots"] as? List<*>)?.map {
                    (it as Map<*, *>)["name"]
                }?.toSet() ?: emptySet()
                (intent["samples"] as List<*>).forEach { sample ->
                    slotPattern.findAll(sample as String).forEach { match ->
                        slots.contains(match.groupValues[1]) shouldBe true
                    }
                }
            }
        }
    }
})
