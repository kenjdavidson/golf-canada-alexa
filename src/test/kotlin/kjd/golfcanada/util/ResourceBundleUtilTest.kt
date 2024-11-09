package kjd.golfcanada.util

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.model.LaunchRequest
import com.amazon.ask.model.Request
import com.amazon.ask.model.RequestEnvelope
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.util.*
import kotlin.test.DefaultAsserter

class ResourceBundleUtilTest : DescribeSpec({
    describe("getResourceBundle") {
        it("loads the appropriate Locale EN") {
            val request = LaunchRequest.builder()
                .withLocale("en-CA")
                .build()
            val requestEnvelope = RequestEnvelope.builder()
                .withRequest(request)
                .build()
            val input = HandlerInput.builder()
                .withRequestEnvelope(requestEnvelope)
                .build()

            val resourceBundle = ResourceBundleUtil.getResourceBundle(input)
            val introduction = resourceBundle.getString("skill.name")

            introduction shouldBe "Golf Canada"
        }

        it("loads the appropriate Locale FR") {
            val request = LaunchRequest.builder()
                .withLocale("fr-CA")
                .build()
            val requestEnvelope = RequestEnvelope.builder()
                .withRequest(request)
                .build()
            val input = HandlerInput.builder()
                .withRequestEnvelope(requestEnvelope)
                .build()

            val resourceBundle = ResourceBundleUtil.getResourceBundle(input)
            val introduction = resourceBundle.getString("skill.name")

            introduction shouldBe "Golf Canadien"
        }

        it("loads the default Locale when no match") {
            val request = LaunchRequest.builder()
                .withLocale("sp_SP")
                .build()
            val requestEnvelope = RequestEnvelope.builder()
                .withRequest(request)
                .build()
            val input = HandlerInput.builder()
                .withRequestEnvelope(requestEnvelope)
                .build()

            val resourceBundle = ResourceBundleUtil.getResourceBundle(input)
            val introduction = resourceBundle.getString("skill.name")

            introduction shouldBe "Golf Canada"
        }
    }
})