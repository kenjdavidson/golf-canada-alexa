package kjd.golfcanada.alexa.exception

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class AccountLinkingExceptionTest : DescribeSpec({

    context("AccountLinkingException") {
        it("should have default message") {
            val exception = AccountLinkingException()
            exception.message shouldBe "Account linking is required for this request"
        }

        it("should accept custom message") {
            val customMessage = "Custom error message"
            val exception = AccountLinkingException(customMessage)
            exception.message shouldBe customMessage
        }

        it("should extend RuntimeException") {
            val exception = AccountLinkingException()
            exception.shouldBeInstanceOf<RuntimeException>()
        }
    }
})
