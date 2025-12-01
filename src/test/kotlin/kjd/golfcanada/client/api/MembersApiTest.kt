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
})
