package kjd.golfcanada.auth

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class ErrorCodeTest : DescribeSpec({
    context("toString") {
        ErrorCode.entries.forEach { errorCode ->
            it("${errorCode.name} should print the ${errorCode.code}: ${errorCode.errorResponse}") {
                errorCode.toString() shouldBe "${errorCode.code}: ${errorCode.errorResponse}"
            }
        }
    }
})
