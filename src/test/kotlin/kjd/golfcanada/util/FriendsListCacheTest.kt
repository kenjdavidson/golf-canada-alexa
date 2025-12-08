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
import kjd.golfcanada.client.api.MembersApi
import kjd.golfcanada.client.model.Friend
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
                val apiFriends = listOf(
                    Friend(individualId = 1001, name = "Dean Ellis", handicap = "8.5"),
                    Friend(individualId = 1002, name = "Jane Smith", handicap = "12.3")
                )
                
                val sessionAttributes = mutableMapOf<String, Any>()
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
                val result = FriendsListCache.get(input, clientWrapper, userId)
                
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
                
                // Verify cache was populated
                sessionAttributes.containsKey("friends_list_cache") shouldBe true
            }
            
            it("should handle empty friends list from API") {
                // Arrange
                val userId = 12345L
                val apiFriends = emptyList<Friend>()
                
                val sessionAttributes = mutableMapOf<String, Any>()
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
                val result = FriendsListCache.get(input, clientWrapper, userId)
                
                // Assert
                result.shouldBeEmpty()
                
                // Verify cache was populated with empty list
                sessionAttributes.containsKey("friends_list_cache") shouldBe true
            }
            
            it("should only store memberId, name, and handicap, not full Friend DTO") {
                // Arrange
                val userId = 12345L
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
                
                val sessionAttributes = mutableMapOf<String, Any>()
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
                FriendsListCache.get(input, clientWrapper, userId)
                
                // Assert - verify only memberId, name, and handicap are stored
                @Suppress("UNCHECKED_CAST")
                val cachedData = sessionAttributes["friends_list_cache"] as List<Map<String, Any?>>
                cachedData shouldHaveSize 1
                
                val firstFriend = cachedData[0]
                firstFriend.keys shouldHaveSize 3
                firstFriend.containsKey("memberId") shouldBe true
                firstFriend.containsKey("name") shouldBe true
                firstFriend.containsKey("handicap") shouldBe true
                firstFriend.containsKey("club") shouldBe false
            }
        }
        
        context("when friends list is already cached") {
            it("should return cached friends without calling API") {
                // Arrange
                val userId = 12345L
                val cachedFriends = listOf(
                    mapOf("memberId" to 1001L, "name" to "Dean Ellis", "handicap" to "8.5"),
                    mapOf("memberId" to 1002L, "name" to "Jane Smith", "handicap" to "12.3")
                )
                
                val sessionAttributes = mutableMapOf<String, Any>(
                    "friends_list_cache" to cachedFriends
                )
                val attributesManager = mockk<AttributesManager>()
                every { attributesManager.sessionAttributes } returns sessionAttributes
                
                val input = mockk<HandlerInput>()
                every { input.attributesManager } returns attributesManager
                
                val membersApi = mockk<MembersApi>()
                val clientWrapper = mockk<ApiClientWrapper>()
                every { clientWrapper.members } returns membersApi
                
                // Act
                val result = FriendsListCache.get(input, clientWrapper, userId)
                
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
            
            it("should handle cached friends with numeric memberId as Number") {
                // Arrange - simulate how session attributes might deserialize numbers
                val userId = 12345L
                val cachedFriends = listOf(
                    mapOf("memberId" to 1001, "name" to "Dean Ellis", "handicap" to "8.5"), // Int instead of Long
                    mapOf("memberId" to 1002.0, "name" to "Jane Smith", "handicap" to "12.3") // Double instead of Long
                )
                
                val sessionAttributes = mutableMapOf<String, Any>(
                    "friends_list_cache" to cachedFriends
                )
                val attributesManager = mockk<AttributesManager>()
                every { attributesManager.sessionAttributes } returns sessionAttributes
                
                val input = mockk<HandlerInput>()
                every { input.attributesManager } returns attributesManager
                
                val clientWrapper = mockk<ApiClientWrapper>()
                
                // Act
                val result = FriendsListCache.get(input, clientWrapper, userId)
                
                // Assert
                result shouldHaveSize 2
                result[0].memberId shouldBe 1001L
                result[1].memberId shouldBe 1002L
            }
            
            it("should handle cached friends with memberId as String") {
                // Arrange - handle edge case where memberId might be stored as string
                val userId = 12345L
                val cachedFriends = listOf(
                    mapOf("memberId" to "1001", "name" to "Dean Ellis", "handicap" to "8.5")
                )
                
                val sessionAttributes = mutableMapOf<String, Any>(
                    "friends_list_cache" to cachedFriends
                )
                val attributesManager = mockk<AttributesManager>()
                every { attributesManager.sessionAttributes } returns sessionAttributes
                
                val input = mockk<HandlerInput>()
                every { input.attributesManager } returns attributesManager
                
                val clientWrapper = mockk<ApiClientWrapper>()
                
                // Act
                val result = FriendsListCache.get(input, clientWrapper, userId)
                
                // Assert
                result shouldHaveSize 1
                result[0].memberId shouldBe 1001L
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
        
        it("should handle friends with null fields") {
            // Arrange
            val friends = listOf(
                Friend(individualId = null, name = null),
                Friend(individualId = 1002, name = "Jane Smith")
            )
            
            // Act
            val result = FriendsListCache.fromFriends(friends)
            
            // Assert
            result shouldHaveSize 2
            result[0].memberId shouldBe null
            result[0].name shouldBe null
            result[1].memberId shouldBe 1002L
            result[1].name shouldBe "Jane Smith"
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
