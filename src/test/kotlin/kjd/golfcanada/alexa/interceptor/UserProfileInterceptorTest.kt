package kjd.golfcanada.alexa.interceptor

import com.amazon.ask.attributes.AttributesManager
import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.model.Context
import com.amazon.ask.model.LaunchRequest
import com.amazon.ask.model.RequestEnvelope
import com.amazon.ask.model.User
import com.amazon.ask.model.interfaces.system.SystemState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.Base64

class UserProfileInterceptorTest : DescribeSpec({

    val interceptor = UserProfileInterceptor()

    /**
     * Creates a sample JWT with the given claims.
     */
    fun createJwt(claims: Map<String, Any>): String {
        val header = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("""{"alg":"HS256","typ":"JWT"}""".toByteArray())
        
        val claimsJson = buildString {
            append("{")
            append(claims.entries.joinToString(",") { (key, value) ->
                val escapedKey = key.replace("\"", "\\\"")
                val valueStr = when (value) {
                    is String -> "\"${value.replace("\"", "\\\"")}\""
                    is Boolean -> value.toString()
                    is Number -> value.toString()
                    else -> "\"$value\""
                }
                "\"$escapedKey\":$valueStr"
            })
            append("}")
        }
        
        val payload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(claimsJson.toByteArray())
        
        val signature = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("signature".toByteArray())
        
        return "$header.$payload.$signature"
    }

    context("parseJwtToUser") {
        it("should parse valid JWT and extract user information") {
            val claims = mapOf(
                "sub" to "1538533",
                "name" to "KENJDAVIDSON",
                "http://schemas.golfnet.com/authuserid" to "1673106",
                "http://schemas.golfnet.com/networkid" to "3234132",
                "http://schemas.golfnet.com/golfcanadacardid" to "5200043264",
                "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname" to "Ken",
                "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname" to "Davidson",
                "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress" to "ken.j.davidson@live.ca",
                "http://schemas.golfnet.com/handicap" to "8.9",
                "http://schemas.golfnet.com/membershiplevel" to "Gold",
                "http://schemas.golfnet.com/clubmanagementgroupid" to "8",
                "http://schemas.golfnet.com/allowscoreposting" to "True",
                "http://schemas.golfnet.com/defaultfacilityname" to "Blue Springs Golf Club",
                "http://schemas.golfnet.com/defaultnationalassociation" to "RCGA",
                "http://schemas.golfnet.com/postholebyhole" to "True"
            )
            
            val jwt = createJwt(claims)
            val user = interceptor.parseJwtToUser(jwt)

            user.id shouldBe 1538533L
            user.username shouldBe "KENJDAVIDSON"
            user.authUserId shouldBe 1673106L
            user.networkId shouldBe "3234132"
            user.golfCanadaCardId shouldBe "5200043264"
            user.firstName shouldBe "Ken"
            user.lastName shouldBe "Davidson"
            user.email shouldBe "ken.j.davidson@live.ca"
            user.handicap shouldBe "8.9"
            user.membershipLevel shouldBe "Gold"
            user.clubManagementGroupId shouldBe 8
            user.allowScorePosting shouldBe true
            user.scoreDefaults shouldNotBe null
            user.scoreDefaults?.facilityName shouldBe "Blue Springs Golf Club"
            user.scoreDefaults?.nationalAssociation shouldBe "RCGA"
            user.scoreDefaults?.postHoleByHole shouldBe true
        }

        it("should handle missing optional claims") {
            val claims = mapOf(
                "sub" to "1538533",
                "name" to "TESTUSER"
            )
            
            val jwt = createJwt(claims)
            val user = interceptor.parseJwtToUser(jwt)

            user.id shouldBe 1538533L
            user.username shouldBe "TESTUSER"
            user.authUserId shouldBe null
            user.firstName shouldBe null
            user.handicap shouldBe null
        }

        it("should throw exception for invalid JWT format") {
            val invalidJwt = "invalid-jwt-format"
            
            val exception = runCatching { interceptor.parseJwtToUser(invalidJwt) }.exceptionOrNull()
            exception shouldNotBe null
        }
    }

    context("mapClaimsToUser") {
        it("should correctly map boolean string 'True' to true (case-insensitive)") {
            val claims = mapOf(
                "http://schemas.golfnet.com/allowscoreposting" to "True"
            )
            
            val user = interceptor.mapClaimsToUser(claims)
            
            user.allowScorePosting shouldBe true
        }

        it("should correctly map boolean string 'true' to true") {
            val claims = mapOf(
                "http://schemas.golfnet.com/allowscoreposting" to "true"
            )
            
            val user = interceptor.mapClaimsToUser(claims)
            
            user.allowScorePosting shouldBe true
        }

        it("should correctly map boolean string 'false' to false") {
            val claims = mapOf(
                "http://schemas.golfnet.com/allowscoreposting" to "false"
            )
            
            val user = interceptor.mapClaimsToUser(claims)
            
            user.allowScorePosting shouldBe false
        }

        it("should correctly map boolean string 'False' to false (case-insensitive)") {
            val claims = mapOf(
                "http://schemas.golfnet.com/allowscoreposting" to "False"
            )
            
            val user = interceptor.mapClaimsToUser(claims)
            
            user.allowScorePosting shouldBe false
        }
    }

    context("process") {
        it("should not process when access token is null") {
            val attributesManager = mockk<AttributesManager>(relaxed = true)
            val input = mockk<HandlerInput>(relaxed = true)
            
            every { input.requestEnvelope } returns RequestEnvelope.builder()
                .withRequest(LaunchRequest.builder().build())
                .withContext(
                    Context.builder()
                        .withSystem(
                            SystemState.builder()
                                .withUser(User.builder().withAccessToken(null).build())
                                .build()
                        )
                        .build()
                )
                .build()
            
            interceptor.process(input)
            
            verify(exactly = 0) { attributesManager.sessionAttributes }
        }

        it("should not process when access token is blank") {
            val attributesManager = mockk<AttributesManager>(relaxed = true)
            val input = mockk<HandlerInput>(relaxed = true)
            
            every { input.requestEnvelope } returns RequestEnvelope.builder()
                .withRequest(LaunchRequest.builder().build())
                .withContext(
                    Context.builder()
                        .withSystem(
                            SystemState.builder()
                                .withUser(User.builder().withAccessToken("").build())
                                .build()
                        )
                        .build()
                )
                .build()
            
            interceptor.process(input)
            
            verify(exactly = 0) { attributesManager.sessionAttributes }
        }

        it("should skip when user profile already exists in session") {
            val existingProfile = kjd.golfcanada.client.model.User(id = 123L)
            val sessionAttributes = mutableMapOf<String, Any>(
                UserProfileInterceptor.USER_SESSION_KEY to existingProfile
            )
            
            val attributesManager = mockk<AttributesManager>(relaxed = true)
            every { attributesManager.sessionAttributes } returns sessionAttributes
            
            val claims = mapOf("sub" to "1538533", "name" to "TESTUSER")
            val jwt = createJwt(claims)
            
            val input = mockk<HandlerInput>(relaxed = true)
            every { input.requestEnvelope } returns RequestEnvelope.builder()
                .withRequest(LaunchRequest.builder().build())
                .withContext(
                    Context.builder()
                        .withSystem(
                            SystemState.builder()
                                .withUser(User.builder().withAccessToken(jwt).build())
                                .build()
                        )
                        .build()
                )
                .build()
            every { input.attributesManager } returns attributesManager
            
            interceptor.process(input)
            
            // Session attributes should not be modified
            sessionAttributes[UserProfileInterceptor.USER_SESSION_KEY] shouldBe existingProfile
        }

        it("should skip user profile extraction when access token has no delimiter") {
            val sessionAttributes = mutableMapOf<String, Any>()
            
            val attributesManager = mockk<AttributesManager>(relaxed = true)
            every { attributesManager.sessionAttributes } returns sessionAttributes
            
            // Plain access token without delimiter (not a JWT, just a string)
            val plainAccessToken = "someAccessTokenString"
            
            val input = mockk<HandlerInput>(relaxed = true)
            every { input.requestEnvelope } returns RequestEnvelope.builder()
                .withRequest(LaunchRequest.builder().build())
                .withContext(
                    Context.builder()
                        .withSystem(
                            SystemState.builder()
                                .withUser(User.builder().withAccessToken(plainAccessToken).build())
                                .build()
                        )
                        .build()
                )
                .build()
            every { input.attributesManager } returns attributesManager
            
            interceptor.process(input)
            
            // User profile should NOT be stored because there's no id_token
            sessionAttributes.containsKey(UserProfileInterceptor.USER_SESSION_KEY) shouldBe false
        }

        it("should extract id_token from concatenated access token and parse user profile") {
            val sessionAttributes = mutableMapOf<String, Any>()
            val capturedAttributes = slot<MutableMap<String, Any>>()
            
            val attributesManager = mockk<AttributesManager>(relaxed = true)
            every { attributesManager.sessionAttributes } returns sessionAttributes
            every { attributesManager.sessionAttributes = capture(capturedAttributes) } answers { }
            
            val claims = mapOf(
                "sub" to "1538533",
                "name" to "KENJDAVIDSON",
                "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname" to "Ken"
            )
            val idToken = createJwt(claims)
            
            // Simulate concatenated token: access_token#id_token
            val concatenatedToken = "someAccessToken123#$idToken"
            
            val input = mockk<HandlerInput>(relaxed = true)
            every { input.requestEnvelope } returns RequestEnvelope.builder()
                .withRequest(LaunchRequest.builder().build())
                .withContext(
                    Context.builder()
                        .withSystem(
                            SystemState.builder()
                                .withUser(User.builder().withAccessToken(concatenatedToken).build())
                                .build()
                        )
                        .build()
                )
                .build()
            every { input.attributesManager } returns attributesManager
            
            interceptor.process(input)
            
            sessionAttributes shouldContainKey UserProfileInterceptor.USER_SESSION_KEY
            
            val storedUser = sessionAttributes[UserProfileInterceptor.USER_SESSION_KEY] as kjd.golfcanada.client.model.User
            storedUser.id shouldBe 1538533L
            storedUser.username shouldBe "KENJDAVIDSON"
            storedUser.firstName shouldBe "Ken"
        }

        it("should handle invalid JWT gracefully without throwing exception") {
            val sessionAttributes = mutableMapOf<String, Any>()
            
            val attributesManager = mockk<AttributesManager>(relaxed = true)
            every { attributesManager.sessionAttributes } returns sessionAttributes
            
            val invalidJwt = "invalid.jwt"
            
            val input = mockk<HandlerInput>(relaxed = true)
            every { input.requestEnvelope } returns RequestEnvelope.builder()
                .withRequest(LaunchRequest.builder().build())
                .withContext(
                    Context.builder()
                        .withSystem(
                            SystemState.builder()
                                .withUser(User.builder().withAccessToken(invalidJwt).build())
                                .build()
                        )
                        .build()
                )
                .build()
            every { input.attributesManager } returns attributesManager
            
            // Should not throw exception
            interceptor.process(input)
            
            // User profile should not be stored due to invalid JWT
            sessionAttributes.containsKey(UserProfileInterceptor.USER_SESSION_KEY) shouldBe false
        }
    }
})
