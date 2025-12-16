package kjd.golfcanada.client.api

import io.kotest.core.annotation.EnabledIf
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeGreaterThan as shouldBeGreaterThanDouble
import io.kotest.matchers.collections.shouldNotBeEmpty

/**
 * Tests for the CoursesApi client.
 * 
 * Extends AuthenticatedApiTest to get pre-authenticated access to the Golf Canada API.
 * These tests are only enabled when TEST_USERNAME and TEST_PASSWORD environment variables are set.
 * 
 * Example usage:
 * ```kotlin
 * val coursesApi = CoursesApi(client = authenticatedClient)
 * val response = coursesApi.getCourseHandicapInfo(
 *     facilityId = 20679L,
 *     handicapPercent = 100,
 *     individualId = 1538533L
 * )
 * ```
 */
@EnabledIf(UsernamePasswordCondition::class)
class CoursesApiTest : AuthenticatedApiTest({
    
    lateinit var coursesApi: CoursesApi
    
    beforeSpec {
        coursesApi = CoursesApi(client = authenticatedClient)
    }
    
    describe("getCourseHandicapInfo") {
        it("should return course handicap info with expected properties") {
            // Using the example from the issue: Glen Abbey Golf Club
            val facilityId = 20679L
            val handicapPercent = 100
            val individualId = authToken.user?.id ?: throw IllegalStateException("User ID not found in auth token")
            
            val response = coursesApi.getCourseHandicapInfo(
                facilityId = facilityId,
                handicapPercent = handicapPercent,
                individualId = individualId
            )
            
            response.shouldNotBeNull()
            response.individualId.shouldNotBeNull()
            response.name.shouldNotBeNull()
            response.handicapPercent.shouldNotBeNull()
            response.handicapPercent shouldBe handicapPercent
            response.individualId shouldBe individualId
        }
        
        it("should return facility information") {
            val facilityId = 20679L
            val handicapPercent = 100
            val individualId = authToken.user?.id ?: throw IllegalStateException("User ID not found in auth token")
            
            val response = coursesApi.getCourseHandicapInfo(
                facilityId = facilityId,
                handicapPercent = handicapPercent,
                individualId = individualId
            )
            
            response.facility.shouldNotBeNull()
            response.facility?.id.shouldNotBeNull()
            response.facility?.id shouldBe facilityId
            response.facility?.name.shouldNotBeNull()
            response.facility?.nationalAssociation.shouldNotBeNull()
            response.facility?.city.shouldNotBeNull()
            response.facility?.region.shouldNotBeNull()
            response.facility?.postalCode.shouldNotBeNull()
        }
        
        it("should return courses with tees and handicap information") {
            val facilityId = 20679L
            val handicapPercent = 100
            val individualId = authToken.user?.id ?: throw IllegalStateException("User ID not found in auth token")
            
            val response = coursesApi.getCourseHandicapInfo(
                facilityId = facilityId,
                handicapPercent = handicapPercent,
                individualId = individualId
            )
            
            response.facility?.courses.shouldNotBeNull()
            response.facility?.courses?.shouldNotBeEmpty()
            
            val course = response.facility?.courses?.first()
            course.shouldNotBeNull()
            course?.id.shouldNotBeNull()
            course?.name.shouldNotBeNull()
            course?.status.shouldNotBeNull()
        }
        
        it("should return tees with handicap and target score information") {
            val facilityId = 20679L
            val handicapPercent = 100
            val individualId = authToken.user?.id ?: throw IllegalStateException("User ID not found in auth token")
            
            val response = coursesApi.getCourseHandicapInfo(
                facilityId = facilityId,
                handicapPercent = handicapPercent,
                individualId = individualId
            )
            
            val course = response.facility?.courses?.first()
            course?.tees.shouldNotBeNull()
            course?.tees?.shouldNotBeEmpty()
            
            val tee = course?.tees?.first()
            tee.shouldNotBeNull()
            tee?.name.shouldNotBeNull()
            tee?.rating.shouldNotBeNull()
            tee?.slope.shouldNotBeNull()
            tee?.par.shouldNotBeNull()
            tee?.handicap.shouldNotBeNull()
            tee?.targetScore.shouldNotBeNull()
            tee?.playingHandicap.shouldNotBeNull()
        }
        
        it("should return valid rating and slope values") {
            val facilityId = 20679L
            val handicapPercent = 100
            val individualId = authToken.user?.id ?: throw IllegalStateException("User ID not found in auth token")
            
            val response = coursesApi.getCourseHandicapInfo(
                facilityId = facilityId,
                handicapPercent = handicapPercent,
                individualId = individualId
            )
            
            val tee = response.facility?.courses?.first()?.tees?.first()
            tee.shouldNotBeNull()
            
            // Rating should be a positive number typically between 60-80
            tee?.rating shouldNotBe null
            tee?.rating?.let { it shouldBeGreaterThanDouble 0.0 }
            
            // Slope should be a positive integer typically between 55-155
            tee?.slope shouldNotBe null
            tee?.slope?.let { it shouldBeGreaterThan 0 }
            
            // Target score should be greater than par
            tee?.targetScore shouldNotBe null
            tee?.targetScore?.let { targetScore ->
                tee.par?.let { par ->
                    targetScore shouldBeGreaterThan par
                }
            }
        }
        
        it("should return golf pros information when available") {
            val facilityId = 20679L
            val handicapPercent = 100
            val individualId = authToken.user?.id ?: throw IllegalStateException("User ID not found in auth token")
            
            val response = coursesApi.getCourseHandicapInfo(
                facilityId = facilityId,
                handicapPercent = handicapPercent,
                individualId = individualId
            )
            
            response.facility?.golfPros.shouldNotBeNull()
            // Golf pros list may be empty or contain pros
            // If there are pros, verify the structure
            if (response.facility?.golfPros?.isNotEmpty() == true) {
                val pro = response.facility?.golfPros?.first()
                pro.shouldNotBeNull()
                pro?.name.shouldNotBeNull()
                pro?.type.shouldNotBeNull()
                // ID may be null according to API response
            }
        }
        
        it("should handle different handicap percentages") {
            val facilityId = 20679L
            val individualId = authToken.user?.id ?: throw IllegalStateException("User ID not found in auth token")
            
            // Test with 90% handicap
            val response90 = coursesApi.getCourseHandicapInfo(
                facilityId = facilityId,
                handicapPercent = 90,
                individualId = individualId
            )
            
            response90.shouldNotBeNull()
            response90.handicapPercent shouldBe 90
            
            // Test with 80% handicap
            val response80 = coursesApi.getCourseHandicapInfo(
                facilityId = facilityId,
                handicapPercent = 80,
                individualId = individualId
            )
            
            response80.shouldNotBeNull()
            response80.handicapPercent shouldBe 80
        }
        
        it("should return facility address information") {
            val facilityId = 20679L
            val handicapPercent = 100
            val individualId = authToken.user?.id ?: throw IllegalStateException("User ID not found in auth token")
            
            val response = coursesApi.getCourseHandicapInfo(
                facilityId = facilityId,
                handicapPercent = handicapPercent,
                individualId = individualId
            )
            
            response.facility.shouldNotBeNull()
            // address1 should be present
            response.facility?.address1 shouldNotBe null
            // address2 may be null
            response.facility?.city.shouldNotBeNull()
            response.facility?.region.shouldNotBeNull()
            response.facility?.postalCode.shouldNotBeNull()
        }
        
        it("should return facility contact information") {
            val facilityId = 20679L
            val handicapPercent = 100
            val individualId = authToken.user?.id ?: throw IllegalStateException("User ID not found in auth token")
            
            val response = coursesApi.getCourseHandicapInfo(
                facilityId = facilityId,
                handicapPercent = handicapPercent,
                individualId = individualId
            )
            
            response.facility.shouldNotBeNull()
            response.facility?.phone.shouldNotBeNull()
            // website may be null
            response.facility?.showCNLink.shouldNotBeNull()
        }
    }
})
