package kjd.golfcanada.alexa.util

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kjd.golfcanada.client.model.Friend

/**
 * Tests for the FriendNameMatcher utility class.
 * 
 * These tests verify that the FriendNameMatcher utility correctly implements
 * the fuzzy matching algorithm used for friend name lookups.
 * 
 * The matching algorithm uses a 4-tier approach:
 * 1. Exact match (case-insensitive)
 * 2. Starts with query (case-insensitive)
 * 3. Contains query (case-insensitive)
 * 4. Query contains any part of the friend's name (case-insensitive)
 */
class FriendNameMatcherTest : DescribeSpec({
    
    val testFriends = listOf(
        Friend(individualId = 1001, name = "Dean Ellis", handicap = "8.5"),
        Friend(individualId = 1002, name = "Jane Smith", handicap = "12.3"),
        Friend(individualId = 1003, name = "Bob Johnson", handicap = "15.7"),
        Friend(individualId = 1004, name = "Michael Williams", handicap = "6.2"),
        Friend(individualId = 1005, name = "Sarah Davis", handicap = "18.1")
    )
    
    describe("FriendNameMatcher.findMatches") {
        context("exact match") {
            it("should match exact name (case-insensitive)") {
                val matches = FriendNameMatcher.findMatches(testFriends, "Dean Ellis")
                matches shouldHaveSize 1
                matches.first().name shouldBe "Dean Ellis"
            }
            
            it("should match name with different case") {
                val matches = FriendNameMatcher.findMatches(testFriends, "dean ellis")
                matches shouldHaveSize 1
                matches.first().name shouldBe "Dean Ellis"
            }
            
            it("should match name with extra whitespace") {
                val matches = FriendNameMatcher.findMatches(testFriends, "  Dean Ellis  ")
                matches shouldHaveSize 1
                matches.first().name shouldBe "Dean Ellis"
            }
            
            it("should match uppercase name") {
                val matches = FriendNameMatcher.findMatches(testFriends, "JANE SMITH")
                matches shouldHaveSize 1
                matches.first().name shouldBe "Jane Smith"
            }
        }
        
        context("starts with match") {
            it("should match when query is start of first name") {
                val matches = FriendNameMatcher.findMatches(testFriends, "Dean")
                matches shouldHaveSize 1
                matches.first().name shouldBe "Dean Ellis"
            }
            
            it("should match first and last name start") {
                val matches = FriendNameMatcher.findMatches(testFriends, "Jane Sm")
                matches shouldHaveSize 1
                matches.first().name shouldBe "Jane Smith"
            }
            
            it("should match partial first name") {
                val matches = FriendNameMatcher.findMatches(testFriends, "Mic")
                matches shouldHaveSize 1
                matches.first().name shouldBe "Michael Williams"
            }
        }
        
        context("contains match") {
            it("should match when name contains query (last name)") {
                val matches = FriendNameMatcher.findMatches(testFriends, "Ellis")
                matches shouldHaveSize 1
                matches.first().name shouldBe "Dean Ellis"
            }
            
            it("should match middle of full name") {
                val matches = FriendNameMatcher.findMatches(testFriends, "Smith")
                matches shouldHaveSize 1
                matches.first().name shouldBe "Jane Smith"
            }
            
            it("should match substring in last name") {
                val matches = FriendNameMatcher.findMatches(testFriends, "ohn")
                matches shouldHaveSize 1
                matches.first().name shouldBe "Bob Johnson"
            }
        }
        
        context("no match") {
            it("should return empty list for non-matching query") {
                val matches = FriendNameMatcher.findMatches(testFriends, "Nonexistent Person")
                matches.shouldBeEmpty()
            }
            
            it("should return empty list for empty query") {
                val matches = FriendNameMatcher.findMatches(testFriends, "")
                matches.shouldBeEmpty()
            }
            
            it("should return empty list for whitespace-only query") {
                val matches = FriendNameMatcher.findMatches(testFriends, "   ")
                matches.shouldBeEmpty()
            }
            
            it("should return empty list for very short non-matching query") {
                val matches = FriendNameMatcher.findMatches(testFriends, "X")
                matches.shouldBeEmpty()
            }
        }
        
        context("multiple matches") {
            it("should return all friends starting with same letter") {
                // Create test data with multiple J names
                val friendsWithJ = listOf(
                    Friend(individualId = 1, name = "Jane Smith", handicap = "12.3"),
                    Friend(individualId = 2, name = "John Doe", handicap = "10.5"),
                    Friend(individualId = 3, name = "Bob Johnson", handicap = "15.7")
                )
                
                val matches = FriendNameMatcher.findMatches(friendsWithJ, "J")
                matches shouldHaveSize 2
                matches.map { it.name } shouldContain "Jane Smith"
                matches.map { it.name } shouldContain "John Doe"
            }
        }
        
        context("edge cases") {
            it("should handle friends with null names") {
                val friendsWithNull = listOf(
                    Friend(individualId = 1, name = null, handicap = "8.5"),
                    Friend(individualId = 2, name = "Jane Smith", handicap = "12.3")
                )
                
                val matches = FriendNameMatcher.findMatches(friendsWithNull, "Jane")
                matches shouldHaveSize 1
                matches.first().name shouldBe "Jane Smith"
            }
            
            it("should handle special characters in query") {
                val matches = FriendNameMatcher.findMatches(testFriends, "Dean's")
                // The algorithm treats "Dean's" as containing "dean" which matches "Dean Ellis"
                // This is acceptable behavior for fuzzy matching
                matches shouldHaveSize 1
                matches.first().name shouldBe "Dean Ellis"
            }
            
            it("should prioritize exact match over starts with") {
                val ambiguousFriends = listOf(
                    Friend(individualId = 1, name = "Dean", handicap = "8.5"),
                    Friend(individualId = 2, name = "Dean Ellis", handicap = "12.3")
                )
                
                val matches = FriendNameMatcher.findMatches(ambiguousFriends, "Dean")
                matches shouldHaveSize 1
                matches.first().name shouldBe "Dean"
            }
        }
    }
    
    describe("FriendNameMatcher.createPredicate") {
        context("predicate functionality") {
            it("should create a predicate that filters friends") {
                val predicate = FriendNameMatcher.createPredicate("Dean")
                val matches = testFriends.filter(predicate)
                matches shouldHaveSize 1
                matches.first().name shouldBe "Dean Ellis"
            }
            
            it("should create a predicate that returns false for empty query") {
                val predicate = FriendNameMatcher.createPredicate("")
                val matches = testFriends.filter(predicate)
                matches.shouldBeEmpty()
            }
            
            it("should create a predicate that matches multiple friends") {
                val friendsWithJ = listOf(
                    Friend(individualId = 1, name = "Jane Smith", handicap = "12.3"),
                    Friend(individualId = 2, name = "John Doe", handicap = "10.5"),
                    Friend(individualId = 3, name = "Bob Johnson", handicap = "15.7")
                )
                
                val predicate = FriendNameMatcher.createPredicate("J")
                val matches = friendsWithJ.filter(predicate)
                // Note: The predicate uses OR logic, so it matches all friends containing "j"
                // This includes "Jane", "John", and "Bob Johnson" (contains "Johnson")
                matches shouldHaveSize 3
                matches.map { it.name } shouldContain "Jane Smith"
                matches.map { it.name } shouldContain "John Doe"
                matches.map { it.name } shouldContain "Bob Johnson"
            }
        }
    }
})
