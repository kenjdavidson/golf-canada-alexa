package kjd.golfcanada.util

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.assertThrows

class StringExtTest: DescribeSpec({
    describe("equalsOrthrow") {
        it("should return true when equals") {
            val equals = "testing".equalsOrThrows("testing") {
                IllegalArgumentException(it)
            }

            equals shouldBe true
        }

        it("should throw when not equals") {
            val exception = assertThrows<IllegalArgumentException> {
                "testing".equalsOrThrows("testing1") {
                    IllegalArgumentException(it)
                }
            }

            exception shouldNotBe null
            exception.message shouldBe "testing"
        }
    }
})