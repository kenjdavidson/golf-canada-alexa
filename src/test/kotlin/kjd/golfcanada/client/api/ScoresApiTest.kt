package kjd.golfcanada.client.api

import io.kotest.core.annotation.EnabledIf
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldNotBe
import kjd.golfcanada.client.model.ScoreData

/**
 * Tests for the ScoresApi client.
 * 
 * Extends AuthenticatedApiTest to get pre-authenticated access to the Golf Canada API.
 * These tests are only enabled when TEST_USERNAME and TEST_PASSWORD environment variables are set.
 */
@EnabledIf(UsernamePasswordCondition::class)
class ScoresApiTest : AuthenticatedApiTest({
    
    lateinit var scoresApi: ScoresApi
    
    beforeSpec {
        scoresApi = ScoresApi(client = authenticatedClient)
    }
    
    describe("getHandicapCalculation") {
        it("should retrieve handicap calculation for authenticated user") {
            val userId = authToken.user?.id ?: throw IllegalStateException("User ID not found in auth token")
            val handicapCalculation = scoresApi.getHandicapCalculation(userId)
            
            handicapCalculation.shouldNotBeNull()
            // Verify basic structure
            handicapCalculation.name.shouldNotBeNull()
        }
        
        it("should return handicap calculation with scores") {
            val userId = authToken.user?.id ?: throw IllegalStateException("User ID not found in auth token")
            val handicapCalculation = scoresApi.getHandicapCalculation(userId)
            
            handicapCalculation.shouldNotBeNull()
            // Scores array should be present (though may be empty)
            handicapCalculation.scores shouldNotBe null
        }
    }
    
    describe("getScoreData") {
        it("should retrieve score data for authenticated user") {
            val userId = authToken.user?.id ?: throw IllegalStateException("User ID not found in auth token")
            val scoreData = scoresApi.getScoreData(userId)
            
            scoreData.shouldNotBeNull()
            // Verify basic structure
            scoreData.score.shouldNotBeNull()
            scoreData.facility.shouldNotBeNull()
        }
        
        it("should return score data with score details") {
            val userId = authToken.user?.id ?: throw IllegalStateException("User ID not found in auth token")
            val scoreData = scoresApi.getScoreData(userId)
            
            scoreData.shouldNotBeNull()
            val score = scoreData.score
            score.shouldNotBeNull()
            
            // Verify key fields are present
            score.individualId shouldNotBe null
            score.date shouldNotBe null
            score.courseId shouldNotBe null
            score.teeId shouldNotBe null
            score.holesPlayed shouldNotBe null
            score.formatPlayed shouldNotBe null
        }
        
        it("should return score data with hole scores array") {
            val userId = authToken.user?.id ?: throw IllegalStateException("User ID not found in auth token")
            val scoreData = scoresApi.getScoreData(userId)
            
            scoreData.shouldNotBeNull()
            val score = scoreData.score
            score.shouldNotBeNull()
            
            // Hole scores array should be present
            score.holeScores shouldNotBe null
        }
        
        it("should return score data with facility information") {
            val userId = authToken.user?.id ?: throw IllegalStateException("User ID not found in auth token")
            val scoreData = scoresApi.getScoreData(userId)
            
            scoreData.shouldNotBeNull()
            val facility = scoreData.facility
            facility.shouldNotBeNull()
            
            // Verify facility fields
            facility.id shouldNotBe null
            facility.name shouldNotBe null
            facility.nationalAssociation shouldNotBe null
            facility.courses shouldNotBe null
        }
        
        it("should return score data with course and tee details") {
            val userId = authToken.user?.id ?: throw IllegalStateException("User ID not found in auth token")
            val scoreData = scoresApi.getScoreData(userId)
            
            scoreData.shouldNotBeNull()
            val facility = scoreData.facility
            facility.shouldNotBeNull()
            
            val courses = facility.courses
            courses.shouldNotBeNull()
            
            // If courses exist, verify structure
            if (courses.isNotEmpty()) {
                val course = courses.first()
                course.id shouldNotBe null
                course.name shouldNotBe null
                course.courseStatus shouldNotBe null
                course.tees shouldNotBe null
                
                // If tees exist, verify structure
                course.tees?.takeIf { it.isNotEmpty() }?.first()?.let { tee ->
                    tee.id shouldNotBe null
                    tee.name shouldNotBe null
                    tee.holes shouldNotBe null
                }
            }
        }
    }
})
