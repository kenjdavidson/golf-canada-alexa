package kjd.golfcanada.client.api

import io.kotest.core.annotation.EnabledIf
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.collections.shouldNotBeEmpty

/**
 * Tests for the FacilitiesApi client.
 * 
 * These tests verify the facility search functionality.
 * Tests are only enabled when TEST_USERNAME and TEST_PASSWORD environment variables are set.
 * 
 * Example usage:
 * ```kotlin
 * val facilitiesApi = FacilitiesApi()
 * val response = facilitiesApi.searchFacilities(
 *     dollarTop = 10,
 *     nationalAssociation = "RCGA",
 *     text = "glen"
 * )
 * println("Found ${response.totalCount} facilities")
 * response.facilities?.forEach { facility ->
 *     println("${facility.name} in ${facility.city}, ${facility.region}")
 * }
 * ```
 */
@EnabledIf(UsernamePasswordCondition::class)
class FacilitiesApiTest : DescribeSpec({
    
    lateinit var facilitiesApi: FacilitiesApi
    
    beforeSpec {
        facilitiesApi = FacilitiesApi()
    }
    
    describe("searchFacilities") {
        it("should search for facilities with text parameter") {
            val response = facilitiesApi.searchFacilities(
                text = "glen"
            )
            
            response.shouldNotBeNull()
            response.totalCount.shouldNotBeNull()
            response.totalCount!! shouldBeGreaterThan 0
            response.facilities.shouldNotBeNull()
            response.facilities!!.shouldNotBeEmpty()
        }
        
        it("should return facilities with expected properties") {
            val response = facilitiesApi.searchFacilities(
                text = "glen",
                nationalAssociation = "RCGA"
            )
            
            response.shouldNotBeNull()
            response.facilities.shouldNotBeNull()
            response.facilities!!.shouldNotBeEmpty()
            
            val facility = response.facilities!!.first()
            facility.shouldNotBeNull()
            facility.id.shouldNotBeNull()
            facility.name.shouldNotBeNull()
            facility.city.shouldNotBeNull()
            facility.region.shouldNotBeNull()
            facility.nationalAssociation.shouldNotBeNull()
            facility.nationalAssociation shouldBe "RCGA"
        }
        
        it("should respect the \$top parameter") {
            val response = facilitiesApi.searchFacilities(
                dollarTop = 5,
                text = "glen",
                nationalAssociation = "RCGA"
            )
            
            response.shouldNotBeNull()
            response.facilities.shouldNotBeNull()
            // The API should return at most 5 results
            response.facilities!!.size shouldBe minOf(5, response.totalCount ?: 5)
        }
        
        it("should filter by nationalAssociation parameter") {
            val response = facilitiesApi.searchFacilities(
                text = "glen",
                nationalAssociation = "RCGA"
            )
            
            response.shouldNotBeNull()
            response.facilities.shouldNotBeNull()
            response.facilities!!.shouldNotBeEmpty()
            
            // All facilities should have RCGA as nationalAssociation
            response.facilities!!.forEach { facility ->
                facility.nationalAssociation shouldBe "RCGA"
            }
        }
    }
})
