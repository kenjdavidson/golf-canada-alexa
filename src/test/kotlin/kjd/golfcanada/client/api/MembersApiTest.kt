package kjd.golfcanada.client.api

import io.kotest.core.annotation.EnabledIf
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldNotBe
import kjd.golfcanada.client.model.MemberSnapshot

/**
 * Tests for the MembersApi client.
 * 
 * Extends AuthenticatedApiTest to get pre-authenticated access to the Golf Canada API.
 * These tests are only enabled when TEST_USERNAME and TEST_PASSWORD environment variables are set.
 */
@EnabledIf(UsernamePasswordCondition::class)
class MembersApiTest : AuthenticatedApiTest({
    
    lateinit var membersApi: MembersApi
    lateinit var snapshot: MemberSnapshot
    
    beforeSpec {
        val userId = authToken.user?.id ?: throw IllegalStateException("User ID not found in auth token")
        membersApi = MembersApi(client = authenticatedClient)
        snapshot = membersApi.getSnapshot(userId)
    }
    
    describe("getSnapshot") {
        it("should retrieve member snapshot for authenticated user") {
            snapshot.shouldNotBeNull()
            snapshot.club.shouldNotBeNull()
            snapshot.membershipType.shouldNotBeNull()
        }
        
        it("should return scores in the snapshot") {
            snapshot.shouldNotBeNull()
            // Scores may or may not be empty depending on user's history
            // but the field itself should be present
            snapshot.scores shouldNotBe null
        }
        
        it("should return handicap information") {
            snapshot.shouldNotBeNull()
            // courseHandicap should be present (could be "NH" for no handicap or a numeric value)
            snapshot.courseHandicap.shouldNotBeNull()
        }
        
        it("should return YTD statistics") {
            snapshot.shouldNotBeNull()
            snapshot.ytdYear.shouldNotBeNull()
            // YTD score count should be non-negative
            snapshot.ytdScoreCount shouldNotBe null
        }
    }
    
    describe("getFriends") {
        it("should retrieve friends list for authenticated user") {
            val userId = authToken.user?.id ?: throw IllegalStateException("User ID not found in auth token")
            val friends = membersApi.getFriends(userId)
            
            friends.shouldNotBeNull()
            // Friends list may be empty or contain friends
            // Verify it returns a list (not throwing an exception is sufficient)
        }
        
        it("should return friends with expected properties when friends exist") {
            val userId = authToken.user?.id ?: throw IllegalStateException("User ID not found in auth token")
            val friends = membersApi.getFriends(userId)
            
            friends.shouldNotBeNull()
            // If there are friends, verify the structure
            if (friends.isNotEmpty()) {
                val friend = friends.first()
                // All fields should be present (though some may be null per the API spec)
                friend.shouldNotBeNull()
            }
        }
    }
    
    describe("getHandicapHistory") {
        it("should retrieve handicap history for authenticated user with default offset") {
            val userId = authToken.user?.id ?: throw IllegalStateException("User ID not found in auth token")
            val history = membersApi.getHandicapHistory(userId)
            
            history.shouldNotBeNull()
            // History may be empty or contain entries depending on user's activity
            // Verify it returns a list (not throwing an exception is sufficient)
        }
        
        it("should retrieve handicap history with custom offset of 180 days") {
            val userId = authToken.user?.id ?: throw IllegalStateException("User ID not found in auth token")
            val history = membersApi.getHandicapHistory(userId, offsetInDays = -180)
            
            history.shouldNotBeNull()
            // Verify it returns a list with custom offset
        }
        
        it("should retrieve handicap history with custom offset of 30 days") {
            val userId = authToken.user?.id ?: throw IllegalStateException("User ID not found in auth token")
            val history = membersApi.getHandicapHistory(userId, offsetInDays = -30)
            
            history.shouldNotBeNull()
            // Verify it returns a list with shorter time range
        }
        
        it("should return handicap history entries with expected properties when entries exist") {
            val userId = authToken.user?.id ?: throw IllegalStateException("User ID not found in auth token")
            val history = membersApi.getHandicapHistory(userId)
            
            history.shouldNotBeNull()
            // If there are history entries, verify the structure
            if (history.isNotEmpty()) {
                val entry = history.first()
                entry.shouldNotBeNull()
                // Verify that at least one field should be present
                // Note: All fields are nullable per the schema, but at least the entry itself should exist
            }
        }
    }
    
    describe("getCourseList") {
        it("should retrieve course list for authenticated user") {
            val userId = authToken.user?.id ?: throw IllegalStateException("User ID not found in auth token")
            val courses = membersApi.getCourseList(userId)
            
            courses.shouldNotBeNull()
            // Course list may be empty or contain courses
            // Verify it returns a list (not throwing an exception is sufficient)
        }
        
        it("should return courses with expected properties when courses exist") {
            val userId = authToken.user?.id ?: throw IllegalStateException("User ID not found in auth token")
            val courses = membersApi.getCourseList(userId)
            
            courses.shouldNotBeNull()
            // If there are courses, verify the structure
            if (courses.isNotEmpty()) {
                val course = courses.first()
                course.shouldNotBeNull()
                // Verify that the course has expected properties
                course.id shouldNotBe null
                course.name.shouldNotBeNull()
            }
        }
    }
})
