package kjd.golfcanada.alexa

import com.squareup.moshi.Moshi
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.io.File

class InteractionModelTest : DescribeSpec({
    describe("the supplied en-US interaction model") {
        val root = Moshi.Builder().build().adapter(Any::class.java)
            .fromJson(File("model/en-US.json").readText()) as Map<*, *>
        val interactionModel = root["interactionModel"] as Map<*, *>
        val languageModel = interactionModel["languageModel"] as Map<*, *>
        val intents = (languageModel["intents"] as List<*>).map { it as Map<*, *> }

        it("preserves the supplied invocation, intents, and tee values") {
            languageModel["invocationName"] shouldBe "golf canada"
            intents.map { it["name"] } shouldBe listOf(
                "AMAZON.CancelIntent", "AMAZON.HelpIntent", "AMAZON.StopIntent",
                "AMAZON.NavigateHomeIntent", "AMAZON.FallbackIntent",
                "PlayerProfileHandicapIntent", "FavoritePlayerHistoryIntent",
                "PlayerProfileMembershipIntent", "PlayerProfileHistoryIntent",
                "AddScorecardIntent", "FavoritePlayerListIntent"
            )
            val tee = (languageModel["types"] as List<*>).single() as Map<*, *>
            tee["name"] shouldBe "TEE"
            (tee["values"] as List<*>).map {
                ((it as Map<*, *>)["name"] as Map<*, *>)["value"]
            } shouldBe listOf("Black", "Gold", "Blue", "Silver", "White", "Green", "Red")
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
