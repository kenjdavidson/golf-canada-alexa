package kjd.golfcanada.alexa.exception

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class NoUserDetailsExceptionTest : DescribeSpec({

    context("NoUserDetailsException") {
        it("should have default message") {
            val exception = NoUserDetailsException()
            exception.message shouldBe "User profile information is not available in the session"
        }

        it("should accept custom message") {
            val customMessage = "Custom error message"
            val exception = NoUserDetailsException(customMessage)
            exception.message shouldBe customMessage
        }

        it("should extend RuntimeException") {
            val exception = NoUserDetailsException()
            exception.shouldBeInstanceOf<RuntimeException>()
        }
    }
})
