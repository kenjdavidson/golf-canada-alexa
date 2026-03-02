package kjd.golfcanada.client.api

import io.kotest.core.annotation.EnabledIf
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldNotBeEmpty
import kjd.golfcanada.client.model.GameTrackerSummary

/**
 * Tests for the GameTrackerApi client.
 * 
 * Extends AuthenticatedApiTest to get pre-authenticated access to the Golf Canada API.
 * These tests are only enabled when TEST_USERNAME and TEST_PASSWORD environment variables are set.
 */
@EnabledIf(UsernamePasswordCondition::class)
class GameTrackerApiTest : AuthenticatedApiTest({
    
    lateinit var gameTrackerApi: GameTrackerApi
    lateinit var summary: GameTrackerSummary
    var userId: Long = 0L
    var courseId: Long = 0L
    
    beforeSpec {
        userId = authToken.user?.id ?: throw IllegalStateException("User ID not found in auth token")
        courseId = authToken.user?.scoreDefaults?.courseId?.toLong() 
            ?: throw IllegalStateException("Course ID not found in auth token")
        
        gameTrackerApi = GameTrackerApi(client = authenticatedClient)
        summary = gameTrackerApi.getGameTrackerSummary(
            courseId = courseId,
            individualId = userId,
            range = "2025"
        )
    }
    
    describe("getGameTrackerSummary") {
        it("should retrieve game tracker summary for authenticated user") {
            summary.shouldNotBeNull()
        }
        
        it("should return scoring statistics") {
            summary.shouldNotBeNull()
            // Verify that at least some scoring statistics are present
            // These fields might be 0 or null depending on user's history
            summary.albatross shouldNotBe null
            summary.eagles shouldNotBe null
            summary.birdies shouldNotBe null
            summary.pars shouldNotBe null
            summary.bogeys shouldNotBe null
            summary.doubles shouldNotBe null
            summary.others shouldNotBe null
        }
        
        it("should return putting statistics") {
            summary.shouldNotBeNull()
            summary.totalPutts shouldNotBe null
            summary.puttsPerRound shouldNotBe null
            summary.puttsPerHole shouldNotBe null
        }
        
        it("should return greens in regulation statistics") {
            summary.shouldNotBeNull()
            summary.girHit shouldNotBe null
            summary.girOpportunities shouldNotBe null
            summary.girPercent shouldNotBe null
        }
        
        it("should return fairways in regulation statistics") {
            summary.shouldNotBeNull()
            summary.firHit shouldNotBe null
            summary.firMissedLeft shouldNotBe null
            summary.firMissedRight shouldNotBe null
            summary.firMissedLong shouldNotBe null
            summary.firMissedShort shouldNotBe null
            summary.firMissedOther shouldNotBe null
            summary.firOpportunities shouldNotBe null
            summary.firPercent shouldNotBe null
        }
        
        it("should return scrambling and other statistics") {
            summary.shouldNotBeNull()
            summary.scrambling shouldNotBe null
            summary.upDown shouldNotBe null
            summary.sandSave shouldNotBe null
            summary.bounceBack shouldNotBe null
        }
        
        it("should return par averages") {
            summary.shouldNotBeNull()
            summary.par3 shouldNotBe null
            summary.par4 shouldNotBe null
            summary.par5 shouldNotBe null
        }
        
        it("should return scores array") {
            summary.shouldNotBeNull()
            summary.scores shouldNotBe null
            // Scores may be empty or contain data depending on user's history
        }
        
        it("should return hole statistics array") {
            summary.shouldNotBeNull()
            summary.holeStatistics shouldNotBe null
            // Hole statistics may be empty or contain data depending on user's history
        }
        
        it("should support Last20 range") {
            val last20Summary = gameTrackerApi.getGameTrackerSummary(
                courseId = courseId,
                individualId = userId,
                range = "Last20"
            )
            
            last20Summary.shouldNotBeNull()
        }
        
        it("should support year range (e.g., 2025)") {
            val yearSummary = gameTrackerApi.getGameTrackerSummary(
                courseId = courseId,
                individualId = userId,
                range = "2025"
            )
            
            yearSummary.shouldNotBeNull()
        }
    }
})
