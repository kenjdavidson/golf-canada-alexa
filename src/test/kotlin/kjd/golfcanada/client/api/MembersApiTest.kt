package kjd.golfcanada.client.api

import io.kotest.core.annotation.EnabledIf
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for the MembersApi client.
 * 
 * Extends AuthenticatedApiTest to get pre-authenticated access to the Golf Canada API.
 * These tests are only enabled when TEST_USERNAME and TEST_PASSWORD environment variables are set.
 */
@EnabledIf(UsernamePasswordCondition::class)
class MembersApiTest : AuthenticatedApiTest({
    
    describe("getSnapshot") {
        it("should retrieve member snapshot for authenticated user") {
            val userId = authToken.user?.id
            userId.shouldNotBeNull()
            
            val membersApi = MembersApi(client = authenticatedClient)
            val snapshot = membersApi.getSnapshot(userId)
            
            snapshot.shouldNotBeNull()
            snapshot.club.shouldNotBeNull()
            snapshot.membershipType.shouldNotBeNull()
        }
        
        it("should return scores in the snapshot") {
            val userId = authToken.user?.id
            userId.shouldNotBeNull()
            
            val membersApi = MembersApi(client = authenticatedClient)
            val snapshot = membersApi.getSnapshot(userId)
            
            snapshot.shouldNotBeNull()
            // Scores may or may not be empty depending on user's history
            // but the field itself should be present
            snapshot.scores shouldNotBe null
        }
        
        it("should return handicap information") {
            val userId = authToken.user?.id
            userId.shouldNotBeNull()
            
            val membersApi = MembersApi(client = authenticatedClient)
            val snapshot = membersApi.getSnapshot(userId)
            
            snapshot.shouldNotBeNull()
            // courseHandicap should be present (could be "NH" for no handicap or a numeric value)
            snapshot.courseHandicap.shouldNotBeNull()
        }
        
        it("should return YTD statistics") {
            val userId = authToken.user?.id
            userId.shouldNotBeNull()
            
            val membersApi = MembersApi(client = authenticatedClient)
            val snapshot = membersApi.getSnapshot(userId)
            
            snapshot.shouldNotBeNull()
            snapshot.ytdYear.shouldNotBeNull()
            // YTD score count should be non-negative
            snapshot.ytdScoreCount shouldNotBe null
        }
    }
})
