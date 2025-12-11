package kjd.golfcanada.client.api

import io.kotest.core.annotation.EnabledIf
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.collections.shouldNotBeEmpty

/**
 * Tests for the FacilitiesApi client.
 * 
 * Extends AuthenticatedApiTest to get pre-authenticated access to the Golf Canada API.
 * These tests are only enabled when TEST_USERNAME and TEST_PASSWORD environment variables are set.
 * 
 * Example usage:
 * ```kotlin
 * val facilitiesApi = FacilitiesApi(client = authenticatedClient)
 * // With nationalAssociation (recommended)
 * val response = facilitiesApi.searchFacilities(
 *     nationalAssociation = "RCGA",
 *     dollarTop = 10,
 *     text = "glen"
 * )
 * // Without nationalAssociation (returns empty result)
 * val emptyResponse = facilitiesApi.searchFacilities(text = "glen")
 * ```
 */
@EnabledIf(UsernamePasswordCondition::class)
class FacilitiesApiTest : AuthenticatedApiTest({
    
    lateinit var facilitiesApi: FacilitiesApi
    
    beforeSpec {
        facilitiesApi = FacilitiesApi(client = authenticatedClient)
    }
    
    describe("searchFacilities") {
        it("should return empty result when nationalAssociation is null") {
            val response = facilitiesApi.searchFacilities(
                text = "glen"
            )
            
            response.shouldNotBeNull()
            response.totalCount.shouldNotBeNull()
            response.totalCount shouldBe 0
            response.facilities.shouldNotBeNull()
        }
        
        it("should return facilities with expected properties") {
            val response = facilitiesApi.searchFacilities(
                nationalAssociation = "RCGA",
                text = "glen"
            )
            
            response.shouldNotBeNull()
            response.totalCount.shouldNotBeNull()
            response.totalCount!! shouldBeGreaterThan 0
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
                nationalAssociation = "RCGA",
                dollarTop = 5,
                text = "glen"
            )
            
            response.shouldNotBeNull()
            response.facilities.shouldNotBeNull()
            // The API should return at most 5 results
            response.facilities!!.size shouldBe minOf(5, response.totalCount ?: 5)
        }
        
        it("should filter by nationalAssociation parameter") {
            val response = facilitiesApi.searchFacilities(
                nationalAssociation = "RCGA",
                text = "glen"
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
