package kjd.golfcanada.alexa.data

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class UserProfileSessionTest : DescribeSpec({
    context("toResponseData") {
        it("should include all non-null fields in the response data") {
            val userProfile = UserProfileSession(
                firstName = "John",
                lastName = "Doe",
                membershipLevel = "Gold",
                golfCanadaCardId = "12345678",
                expirationDate = "2/1/2026 12:00:00 AM",
                facilityName = "Blue Springs Golf Club",
                postHoleByHole = true
            )

            val responseData = userProfile.toResponseData()

            responseData shouldContainExactly mapOf(
                "firstName" to "John",
                "lastName" to "Doe",
                "membershipLevel" to "Gold",
                "golfCanadaCardId" to "12345678",
                "expirationDate" to "2/1/2026 12:00:00 AM",
                "facilityName" to "Blue Springs Golf Club",
                "postHoleByHole" to true
            )
        }

        it("should exclude null fields from the response data") {
            val userProfile = UserProfileSession(
                firstName = "Jane",
                lastName = null,
                membershipLevel = "Silver",
                golfCanadaCardId = null,
                expirationDate = null,
                facilityName = null,
                postHoleByHole = null
            )

            val responseData = userProfile.toResponseData()

            responseData shouldContainExactly mapOf(
                "firstName" to "Jane",
                "membershipLevel" to "Silver"
            )
        }

        it("should return empty map when all fields are null") {
            val userProfile = UserProfileSession()

            val responseData = userProfile.toResponseData()

            responseData.isEmpty() shouldBe true
        }

        it("should handle boolean false value correctly") {
            val userProfile = UserProfileSession(
                firstName = "Test",
                postHoleByHole = false
            )

            val responseData = userProfile.toResponseData()

            responseData shouldContainExactly mapOf(
                "firstName" to "Test",
                "postHoleByHole" to false
            )
        }

        it("should return a new map instance on each call") {
            val userProfile = UserProfileSession(
                firstName = "John",
                lastName = "Doe"
            )

            val responseData1 = userProfile.toResponseData()
            val responseData2 = userProfile.toResponseData()
            
            // Verify that each call returns a new instance (not the same reference)
            (responseData1 === responseData2) shouldBe false
            // But with the same content
            responseData1 shouldContainExactly responseData2
        }
    }
})
