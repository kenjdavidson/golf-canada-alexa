# Mocking Framework and Handler Testing

This document describes the local mocking framework and handler testing infrastructure for the Golf Canada Alexa Skill.

## Overview

The mocking framework allows the Alexa Skill and Authentication handlers to run locally without a live connection to the Golf Canada API. This is achieved through:

1. **IApiClientProvider Interface**: An abstraction layer that allows switching between production and mock API client providers
2. **MockApiClientProvider**: A mock implementation that can load responses from local JSON resource files
3. **MockResourceLoader**: A utility that loads mock data from JSON files following a specific naming convention
4. **Handler Test Suite**: Gradle-based tests that verify the mocking infrastructure with mocked dependencies
5. **GitHub Actions Workflow**: Automated CI pipeline for running handler tests

## Architecture

### Interface-Based Design

The `IApiClientProvider` interface defines the contract for providing API client instances:

```kotlin
interface IApiClientProvider {
    fun getClient(accessToken: String): ApiClientWrapper
}
```

Two implementations exist:
- `ApiClientProvider`: Production implementation that makes real HTTP calls to Golf Canada API
- `MockApiClientProvider`: Mock implementation (currently a placeholder due to limitations)

### Environment Variable Control

The `MOCK_API` environment variable controls which provider is used:

```kotlin
val apiClientProvider: IApiClientProvider = if (System.getenv("MOCK_API")?.toBoolean() == true) {
    MockApiClientProvider()
} else {
    ApiClientProvider()
}
```

## Mock Resource Files

Mock responses are stored as JSON files in `src/main/resources/client/` following this structure:

```
src/main/resources/client/
├── auth/
│   └── getAuthToken.json
├── members/
│   ├── getFriends.json
│   ├── getCourseList.json
│   ├── getHandicapHistory.json
│   └── getSnapshot.json
├── scores/
│   ├── getHandicapCalculation.json
│   └── getScoreData.json
├── courses/
│   └── getCourseHandicapInfo.json
└── facilities/
    └── searchFacilities.json
```

### Naming Convention

The `MockResourceLoader` follows this naming convention:

1. **Parameter-specific**: `/resources/client/{serviceName}/{methodName}_{parameter}.json`
   - Example: `members/getSnapshot_12345.json` for user ID 12345
   
2. **Generic fallback**: `/resources/client/{serviceName}/{methodName}.json`
   - Example: `members/getFriends.json` for any user

## Handler Tests

### Running Handler Tests Locally

```bash
# Run with mock API enabled
./gradlew alexaSkillHandlerTest -PMOCK_API=true

# Or set environment variable
MOCK_API=true ./gradlew alexaSkillHandlerTest
```

### Test Structure

Handler tests are located in `src/alexaSkillHandlerTest/kotlin/` and verify:
- Environment variables are configured correctly
- Mock API client provider can be instantiated
- Mock resource files can be loaded
- The skill can be created with mock mode enabled

Note: These are not true integration tests as all external dependencies (API calls) are mocked.

### Test Events

Test Alexa events are stored in `src/alexaSkillHandlerTest/resources/events/`:
- `LaunchRequest.json`: Basic launch request event

## GitHub Actions Integration

The `.github/workflows/integration-tests.yml` workflow:
1. Sets up Java 21, Python, and Docker
2. Installs AWS SAM CLI
3. Builds the application with Gradle
4. Builds the SAM application
5. Runs handler tests with `MOCK_API=true`
6. Uploads test results and logs as artifacts

## Current Limitations

### OpenAPI Generator Constraints

The current implementation has limitations due to the OpenAPI-generated API classes:

1. **Final Classes**: Generated API classes (`MembersApi`, `ScoresApi`, etc.) are final by default in Kotlin and cannot be extended
2. **No Interfaces**: The OpenAPI generator doesn't create interfaces for the API classes
3. **Tight Coupling**: Direct instantiation of concrete API classes throughout the codebase

### Mock Implementation Status

The `MockApiClientProvider` is currently a **placeholder** that returns a standard `ApiClientProvider` instance. Full mocking requires one of:

1. **HTTP Interceptor Approach**: Implement mocking at the OkHttp interceptor level to intercept and return mock responses
2. **Local Mock Server**: Use a tool like WireMock to create a local mock server that returns predefined responses
3. **OpenAPI Generator Configuration**: Configure the generator to create open classes or interfaces (requires generator customization)

### Recommended Next Steps

For full mocking functionality, consider:

1. **Implement HTTP-level mocking**:
   - Create a `MockInterceptor` that checks for mock mode
   - Parse the request URL and method
   - Load corresponding JSON resource file
   - Return mock response with proper status codes

2. **Enhance MockResourceLoader**:
   - Support more sophisticated parameter matching
   - Handle query parameters and request bodies
   - Add response delay simulation
   - Support error scenarios

3. **Expand test coverage**:
   - Add integration tests that actually invoke handlers
   - Test error handling paths
   - Verify response formats match production

## Usage Examples

### Enabling Mock Mode

```bash
# For local development
export MOCK_API=true
sam local start-api

# For handler tests
MOCK_API=true ./gradlew alexaSkillHandlerTest

# For SAM local invoke
sam local invoke GolfCanadaAlexaSkillFunction -e events/AlexaLaunchEvent.json --env-vars env.json
```

### Adding New Mock Responses

1. Create a JSON file in the appropriate service directory:
   ```bash
   # For a new endpoint
   echo '{"result": "mock data"}' > src/main/resources/client/members/getNewEndpoint.json
   ```

2. For parameter-specific responses:
   ```bash
   # For specific user ID
   echo '{"userId": 123, "name": "Test User"}' > src/main/resources/client/members/getSnapshot_123.json
   ```

3. The `MockResourceLoader` will automatically find and use these files

### Writing Handler Tests

```kotlin
class MyHandlerTest : DescribeSpec({
    describe("My Feature") {
        it("should work with mock API") {
            val provider = MockApiClientProvider()
            val client = provider.getClient("test-token")
            
            // Your test code here
        }
    }
})
```

## Benefits

1. **Local Development**: Test skill functionality without API access
2. **Fast Feedback**: No network latency or rate limiting
3. **Reliable Tests**: Consistent mock data eliminates flakiness
4. **Offline Development**: Work without internet connection
5. **Cost Savings**: Reduce API calls during development and testing
6. **CI/CD Ready**: Automated handler tests in GitHub Actions

## Future Enhancements

- [ ] Implement HTTP-level mocking for full functionality
- [ ] Add more sophisticated mock response matching
- [ ] Support dynamic mock responses based on request parameters
- [ ] Add mock error scenarios for testing error handling
- [ ] Create SAM local testing examples
- [ ] Add handler tests that invoke full skill handlers
- [ ] Document OAuth/Authentication mocking patterns
