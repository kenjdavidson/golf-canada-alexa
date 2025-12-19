package kjd.golfcanada.alexa

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kjd.golfcanada.client.provider.MockApiClientProvider
import kjd.golfcanada.client.provider.mock.MockResourceLoader
import org.slf4j.LoggerFactory

/**
 * Handler tests for the Golf Canada Alexa Skill with mocked API responses.
 * 
 * These tests verify that the mocking infrastructure is correctly set up:
 * - Environment variables are configured properly
 * - Mock API client provider can be instantiated
 * - Mock resource files can be loaded
 * - The skill can be created with mock mode enabled
 * 
 * The tests use the MOCK_API environment variable to enable mock mode, which returns data
 * from JSON files in src/main/resources/client/ instead of making real API calls.
 * 
 * Note: These are not true integration tests as all external dependencies are mocked.
 */
class AlexaSkillHandlerTest : DescribeSpec({
    val logger = LoggerFactory.getLogger(AlexaSkillHandlerTest::class.java)
    
    describe("Mock Framework Integration Tests") {
        
        it("should verify MOCK_API environment variable is set") {
            val mockApiEnabled = System.getenv("MOCK_API")?.toBoolean() ?: false
            logger.info("MOCK_API environment variable: ${System.getenv("MOCK_API")}")
            mockApiEnabled shouldBe true
        }
        
        it("should verify required environment variables are set") {
            val skillId = System.getenv("SKILL_ID")
            val clientId = System.getenv("CLIENT_ID")
            val clientSecret = System.getenv("CLIENT_SECRET")
            
            logger.info("SKILL_ID: $skillId")
            logger.info("CLIENT_ID: $clientId")
            
            skillId shouldNotBe null
            clientId shouldNotBe null
            clientSecret shouldNotBe null
        }
        
        it("should create mock API client provider successfully") {
            val provider = MockApiClientProvider()
            val client = provider.getClient("test-token")
            
            client shouldNotBe null
            logger.info("Successfully created mock API client")
        }
        
        it("should load mock resource files successfully") {
            // Test loading a mock resource
            val mockData = MockResourceLoader.loadResource(
                serviceName = "auth",
                methodName = "getAuthToken",
                parameter = null,
                clazz = Map::class.java
            )
            
            mockData shouldNotBe null
            logger.info("Successfully loaded mock resource file")
        }
        
        it("should create Golf Canada Alexa Skill with mock mode enabled") {
            val skill = GolfCanadaAlexaSkill.getSkills()
            
            skill shouldNotBe null
            logger.info("Successfully created Golf Canada Alexa Skill")
        }
    }
})
