package kjd.golfcanada.util

import com.amazon.ask.attributes.AttributesManager
import com.amazon.ask.dispatcher.request.handler.HandlerInput
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kjd.golfcanada.alexa.data.FriendInfo
import kjd.golfcanada.alexa.data.UserProfileSession
import kjd.golfcanada.alexa.interceptor.UserProfileInterceptor
import kjd.golfcanada.client.api.MembersApi
import kjd.golfcanada.client.model.Friend
import kjd.golfcanada.client.model.User
import kjd.golfcanada.client.provider.ApiClientWrapper

/**
 * Tests for the FriendsListCache utility.
 * 
 * These tests verify that the FriendsListCache correctly:
 * - Fetches friends from API when cache is empty
 * - Returns cached friends when available in session
 * - Stores minimal FriendInfo data (not full Friend DTO)
 * - Handles empty friends lists
 * - Converts Friend DTOs to FriendInfo correctly
 */
class FriendsListCacheTest : DescribeSpec({
    
    describe("FriendsListCache.get") {
        
        context("when friends list is not cached") {
            it("should fetch friends from API and cache them") {
                // Arrange
                val userId = 12345L
                val userProfileSession = UserProfileSession(id = userId)
                val apiFriends = listOf(
                    Friend(individualId = 1001, name = "Dean Ellis", handicap = "8.5"),
                    Friend(individualId = 1002, name = "Jane Smith", handicap = "12.3")
                )
                
                val sessionAttributes = mutableMapOf<String, Any>(
                    UserProfileInterceptor.USER_SESSION_KEY to userProfileSession
                )
                val attributesManager = mockk<AttributesManager>()
                every { attributesManager.sessionAttributes } returns sessionAttributes
                every { attributesManager.sessionAttributes = any() } answers { 
                    sessionAttributes.putAll(firstArg<Map<String, Any>>())
                }
                
                val input = mockk<HandlerInput>()
                every { input.attributesManager } returns attributesManager
                
                val membersApi = mockk<MembersApi>()
                every { membersApi.getFriends(userId) } returns apiFriends
                
                val clientWrapper = mockk<ApiClientWrapper>()
                every { clientWrapper.members } returns membersApi
                
                // Act
                val result = FriendsListCache.get(input, clientWrapper)
                
                // Assert
                result shouldHaveSize 2
                result[0].memberId shouldBe 1001L
                result[0].name shouldBe "Dean Ellis"
                result[0].handicap shouldBe "8.5"
                result[1].memberId shouldBe 1002L
                result[1].name shouldBe "Jane Smith"
                result[1].handicap shouldBe "12.3"
                
                // Verify API was called
                verify(exactly = 1) { membersApi.getFriends(userId) }
                
                // Verify cache was populated with FriendInfo objects
                sessionAttributes.containsKey("friends_list") shouldBe true
                @Suppress("UNCHECKED_CAST")
                val cached = sessionAttributes["friends_list"] as List<FriendInfo>
                cached shouldHaveSize 2
            }
            
            it("should handle empty friends list from API") {
                // Arrange
                val userId = 12345L
                val userProfileSession = UserProfileSession(id = userId)
                val apiFriends = emptyList<Friend>()
                
                val sessionAttributes = mutableMapOf<String, Any>(
                    UserProfileInterceptor.USER_SESSION_KEY to userProfileSession
                )
                val attributesManager = mockk<AttributesManager>()
                every { attributesManager.sessionAttributes } returns sessionAttributes
                every { attributesManager.sessionAttributes = any() } answers {
                    sessionAttributes.putAll(firstArg<Map<String, Any>>())
                }
                
                val input = mockk<HandlerInput>()
                every { input.attributesManager } returns attributesManager
                
                val membersApi = mockk<MembersApi>()
                every { membersApi.getFriends(userId) } returns apiFriends
                
                val clientWrapper = mockk<ApiClientWrapper>()
                every { clientWrapper.members } returns membersApi
                
                // Act
                val result = FriendsListCache.get(input, clientWrapper)
                
                // Assert
                result.shouldBeEmpty()
                
                // Verify cache was populated with empty list
                sessionAttributes.containsKey("friends_list") shouldBe true
            }
            
            it("should only store memberId, name, and handicap, not full Friend DTO") {
                // Arrange
                val userId = 12345L
                val userProfileSession = UserProfileSession(id = userId)
                val apiFriends = listOf(
                    Friend(
                        individualId = 1001,
                        name = "Dean Ellis",
                        handicap = "8.5",
                        club = "Test Club",
                        region = "Test Region",
                        cardId = "12345",
                        gender = "M",
                        level = "Gold",
                        expiresOn = "2024-12-31"
                    )
                )
                
                val sessionAttributes = mutableMapOf<String, Any>(
                    UserProfileInterceptor.USER_SESSION_KEY to userProfileSession
                )
                val attributesManager = mockk<AttributesManager>()
                every { attributesManager.sessionAttributes } returns sessionAttributes
                every { attributesManager.sessionAttributes = any() } answers {
                    sessionAttributes.putAll(firstArg<Map<String, Any>>())
                }
                
                val input = mockk<HandlerInput>()
                every { input.attributesManager } returns attributesManager
                
                val membersApi = mockk<MembersApi>()
                every { membersApi.getFriends(userId) } returns apiFriends
                
                val clientWrapper = mockk<ApiClientWrapper>()
                every { clientWrapper.members } returns membersApi
                
                // Act
                FriendsListCache.get(input, clientWrapper)
                
                // Assert - verify only memberId, name, and handicap are stored as FriendInfo
                @Suppress("UNCHECKED_CAST")
                val cachedData = sessionAttributes["friends_list"] as List<FriendInfo>
                cachedData shouldHaveSize 1
                
                val firstFriend = cachedData[0]
                firstFriend.memberId shouldBe 1001L
                firstFriend.name shouldBe "Dean Ellis"
                firstFriend.handicap shouldBe "8.5"
            }
        }
        
        context("when friends list is already cached") {
            it("should return cached friends without calling API") {
                // Arrange
                val userId = 12345L
                val userProfileSession = UserProfileSession(id = userId)
                val cachedFriends = listOf(
                    FriendInfo(memberId = 1001L, name = "Dean Ellis", handicap = "8.5"),
                    FriendInfo(memberId = 1002L, name = "Jane Smith", handicap = "12.3")
                )
                
                val sessionAttributes = mutableMapOf<String, Any>(
                    UserProfileInterceptor.USER_SESSION_KEY to userProfileSession,
                    "friends_list" to cachedFriends
                )
                val attributesManager = mockk<AttributesManager>()
                every { attributesManager.sessionAttributes } returns sessionAttributes
                
                val input = mockk<HandlerInput>()
                every { input.attributesManager } returns attributesManager
                
                val membersApi = mockk<MembersApi>()
                val clientWrapper = mockk<ApiClientWrapper>()
                every { clientWrapper.members } returns membersApi
                
                // Act
                val result = FriendsListCache.get(input, clientWrapper)
                
                // Assert
                result shouldHaveSize 2
                result[0].memberId shouldBe 1001L
                result[0].name shouldBe "Dean Ellis"
                result[0].handicap shouldBe "8.5"
                result[1].memberId shouldBe 1002L
                result[1].name shouldBe "Jane Smith"
                result[1].handicap shouldBe "12.3"
                
                // Verify API was NOT called
                verify(exactly = 0) { membersApi.getFriends(any()) }
            }
        }
    }
    
    describe("FriendsListCache.fromFriends") {
        it("should convert Friend DTOs to FriendInfo objects") {
            // Arrange
            val friends = listOf(
                Friend(individualId = 1001, name = "Dean Ellis", handicap = "8.5"),
                Friend(individualId = 1002, name = "Jane Smith", handicap = "12.3")
            )
            
            // Act
            val result = FriendsListCache.fromFriends(friends)
            
            // Assert
            result shouldHaveSize 2
            result[0].memberId shouldBe 1001L
            result[0].name shouldBe "Dean Ellis"
            result[0].handicap shouldBe "8.5"
            result[1].memberId shouldBe 1002L
            result[1].name shouldBe "Jane Smith"
            result[1].handicap shouldBe "12.3"
        }
        
        it("should filter out friends with null id or name") {
            // Arrange
            val friends = listOf(
                Friend(individualId = null, name = null),
                Friend(individualId = 1001, name = null),
                Friend(individualId = null, name = "Missing ID"),
                Friend(individualId = 1002, name = "Jane Smith", handicap = "12.3")
            )
            
            // Act
            val result = FriendsListCache.fromFriends(friends)
            
            // Assert - only the friend with both id and name should be included
            result shouldHaveSize 1
            result[0].memberId shouldBe 1002L
            result[0].name shouldBe "Jane Smith"
            result[0].handicap shouldBe "12.3"
        }
        
        it("should handle empty friends list") {
            // Arrange
            val friends = emptyList<Friend>()
            
            // Act
            val result = FriendsListCache.fromFriends(friends)
            
            // Assert
            result.shouldBeEmpty()
        }
    }
})
