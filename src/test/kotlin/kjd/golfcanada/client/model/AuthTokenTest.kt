package kjd.golfcanada.client.model

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class AuthTokenTest : DescribeSpec({
    describe("AuthToken") {
        it("should just work") {
            val authToken = AuthToken(
                "accessToken",
                "refreshToken",
                "idToken",
                expiresIn = 3600,
                tokenType = "bearer",
                user = null
            )

            authToken.accessToken shouldBe "accessToken"
            authToken.refreshToken shouldBe "refreshToken"
            authToken.idToken shouldBe "idToken"
            authToken.expiresIn shouldBe 3600
            authToken.user shouldBe null
        }
    }
    
    describe("withConcatenatedToken") {
        it("should concatenate access_token with id_token using delimiter") {
            val authToken = AuthToken(
                "accessToken123",
                "refreshToken456",
                "idToken789",
                expiresIn = 3600,
                tokenType = "bearer"
            )

            val concatenated = authToken.withConcatenatedToken()
            
            concatenated.accessToken shouldBe "accessToken123${TOKEN_DELIMITER}idToken789"
            concatenated.refreshToken shouldBe "refreshToken456"
            concatenated.idToken shouldBe "idToken789"
        }

        it("should not concatenate when idToken is null") {
            val authToken = AuthToken(
                "accessToken123",
                "refreshToken456",
                null,
                expiresIn = 3600,
                tokenType = "bearer"
            )

            val concatenated = authToken.withConcatenatedToken()
            
            concatenated.accessToken shouldBe "accessToken123"
        }

        it("should not concatenate when idToken is blank") {
            val authToken = AuthToken(
                "accessToken123",
                "refreshToken456",
                "",
                expiresIn = 3600,
                tokenType = "bearer"
            )

            val concatenated = authToken.withConcatenatedToken()
            
            concatenated.accessToken shouldBe "accessToken123"
        }
    }
    
    describe("extractAccessToken") {
        it("should extract access token from concatenated string") {
            val concatenated = "accessToken123${TOKEN_DELIMITER}idToken789"
            
            concatenated.extractAccessToken() shouldBe "accessToken123"
        }
        
        it("should return entire string if no delimiter present") {
            val token = "accessToken123"
            
            token.extractAccessToken() shouldBe "accessToken123"
        }
        
        it("should handle multiple delimiters by taking first part") {
            val token = "part1${TOKEN_DELIMITER}part2${TOKEN_DELIMITER}part3"
            
            token.extractAccessToken() shouldBe "part1"
        }
    }
    
    describe("extractIdToken") {
        it("should extract id token from concatenated string") {
            val concatenated = "accessToken123${TOKEN_DELIMITER}idToken789"
            
            concatenated.extractIdToken() shouldBe "idToken789"
        }
        
        it("should return null if no delimiter present") {
            val token = "accessToken123"
            
            token.extractIdToken() shouldBe null
        }
        
        it("should handle multiple delimiters by taking second part") {
            val token = "part1${TOKEN_DELIMITER}part2${TOKEN_DELIMITER}part3"
            
            token.extractIdToken() shouldBe "part2${TOKEN_DELIMITER}part3"
        }
    }
})
