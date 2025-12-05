# Agent Guidelines for Golf Canada Alexa Skill

This document provides guidelines for AI agents working on the Golf Canada Alexa Skill project.

## Project Overview

The Golf Canada Alexa Skill is an unofficial Alexa skill that provides Golf Canada membership functionality through voice commands. The project is built using:

- **Language**: Kotlin/JVM (Java 21)
- **Build Tool**: Gradle with Kotlin DSL
- **Infrastructure**: AWS SAM (Serverless Application Model)
- **Framework**: Amazon Alexa SDK
- **API Client**: OkHttp with Moshi for JSON serialization
- **Code Generation**: OpenAPI Generator for Golf Canada API client

### Architecture

The project consists of two main Lambda functions:

1. **Authentication Function** (`GolfCanadaAuthenticationFunction`)
   - OAuth wrapper for Golf Canada authentication
   - Enables Alexa Account Linking
   - Location: `src/main/kotlin/kjd/golfcanada/auth/`

2. **Alexa Skill Handler** (`GolfCanadaAlexaSkillFunction`)
   - Processes voice commands and intents
   - Location: `src/main/kotlin/kjd/golfcanada/alexa/`

## Key Project Components

### Intent Handlers
Located in `src/main/kotlin/kjd/golfcanada/alexa/handler/`

Each handler implements the `RequestHandler` interface and typically:
- Uses `canHandle()` to match specific intents
- Uses `handle()` to process requests and generate responses
- Returns responses using FreeMarker templates

### Request Interceptors
Located in `src/main/kotlin/kjd/golfcanada/alexa/interceptor/`

Interceptors run before intent handlers and can:
- Validate authentication
- Load and cache user data
- Pre-fetch commonly needed information
- Store data in Session or Request Attributes

### Response Templates
Located in `src/main/resources/kjd/golfcanada/alexa/responses/`

All responses use FreeMarker templates with localization:
- English: `*Response.ftl`
- French: `*Response_fr.ftl`

### Golf Canada API Client
- OpenAPI Spec: `src/main/resources/client/golfcanada.yaml`
- Generated code: `build/generated/openapi/src/main/kotlin/`
- Custom extensions: `src/main/kotlin/kjd/golfcanada/client/`

## Documentation Requirements

When adding or modifying features, **ALWAYS** update the following documentation:

### 1. README.md
Update the "Available Interactions" section with:
- Clear intent description
- Example utterances
- Expected behavior
- Any special notes or limitations

### 2. docs/AVAILABLE_FEATURES.md
Update with the new feature:
- Voice commands/utterances
- What the feature does
- Expected responses
- Feature status (Available/In Progress/Planned)

### 3. docs/ARCHITECTURE.md
Update if the change affects:
- System architecture
- Data flow
- New components or dependencies
- Caching strategy

### 4. Interaction Model
Update `model/model.json` with:
- New intents
- New slots
- New sample utterances
- Any changes to existing intents

## Adding a New Intent

When adding a new intent to the skill, follow this complete workflow:

### 1. Create the Intent Handler

Create a new file in `src/main/kotlin/kjd/golfcanada/alexa/handler/`:

```kotlin
package kjd.golfcanada.alexa.handler

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.dispatcher.request.handler.RequestHandler
import com.amazon.ask.model.Response
import com.amazon.ask.request.Predicates.intentName
import kjd.golfcanada.alexa.IntentName
import java.util.*

/**
 * Handles requests for [describe what this intent does].
 * 
 * Example utterances:
 * - "utterance example 1"
 * - "utterance example 2"
 */
class YourIntentHandler : RequestHandler {
    override fun canHandle(input: HandlerInput): Boolean =
        input.matches(intentName(IntentName.YOUR_INTENT.value))

    override fun handle(input: HandlerInput): Optional<Response> {
        // Implementation here
        return input.generateTemplateResponse("YourIntentResponse", dataModel)
    }
}
```

### 2. Create Response Templates

Create both English and French templates in `src/main/resources/kjd/golfcanada/alexa/responses/`:

**YourIntentResponse.ftl** (English):
```
Your English response with ${variables}.
```

**YourIntentResponse_fr.ftl** (French):
```
Votre réponse française avec ${variables}.
```

### 3. Register the Handler

Add the handler to `GolfCanadaAlexaSkill.kt` in the `addRequestHandlers()` call:

```kotlin
.addRequestHandlers(
    // ... existing handlers ...
    YourIntentHandler(),
    // ... other handlers ...
)
```

### 4. Update the Interaction Model

Add the intent to `model/model.json`:

```json
{
  "name": "GOLFCANADA.YourIntent",
  "slots": [
    {
      "name": "slotName",
      "type": "SLOT_TYPE"
    }
  ],
  "samples": [
    "utterance example 1",
    "utterance example 2",
    "utterance with {slotName}"
  ]
}
```

### 5. Add Unit Tests

Create comprehensive tests in `src/test/kotlin/kjd/golfcanada/alexa/handler/`:

```kotlin
class YourIntentHandlerTest : StringSpec({
    "should handle valid request" {
        // Test implementation
    }
    
    "should handle edge cases" {
        // Test edge cases
    }
})
```

### 6. Update Documentation

**README.md** - Add to "Available Interactions":
```markdown
### Section Name
- **Your Intent** - "utterance example 1" or "utterance example 2"
  - Describe the functionality
  - Any special notes
```

**docs/AVAILABLE_FEATURES.md** - Add the new feature:
```markdown
#### Your Feature Name
**Voice Commands:**
- "utterance example 1"
- "utterance example 2"

**What it does:**
- Describe the functionality
- Any special notes

**Response:**
> "Your actual response text here."
```

### 7. Build and Test

```bash
# Regenerate if OpenAPI spec changed
./gradlew openApiGenerate

# Build the project
./gradlew build

# Run tests
./gradlew test

# Build for SAM deployment
sam build
```

## Working with the Golf Canada API

### Updating the OpenAPI Specification

When adding new Golf Canada API endpoints:

1. Edit `src/main/resources/client/golfcanada.yaml`
2. Add the endpoint definition following OpenAPI 3.0 specification
3. Regenerate the client: `./gradlew openApiGenerate`
4. Review generated code in `build/generated/openapi/`
5. Add any necessary custom extensions in `src/main/kotlin/kjd/golfcanada/client/`

### API Client Usage Pattern

```kotlin
// Get access token from request
val accessToken = input.requestEnvelope.context?.system?.user?.accessToken
    ?: throw AccountLinkingException()

// Extract actual token (removes "Bearer " prefix if present)
val actualAccessToken = accessToken.extractAccessToken()

// Set token on ApiClient
org.openapitools.client.infrastructure.ApiClient.accessToken = actualAccessToken

// Create and use API client
val membersApi = MembersApi()
val response = membersApi.someEndpoint(parameters)
```

## Request Interceptors

Interceptors are powerful tools for preparing data before intent handlers run.

### Common Use Cases

1. **Authentication Validation** - Ensure access token is present
2. **User Profile Loading** - Load user data once per session
3. **Data Caching** - Pre-fetch commonly needed data with TTL

### Adding a New Interceptor

```kotlin
package kjd.golfcanada.alexa.interceptor

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.request.interceptor.RequestInterceptor

class YourInterceptor : RequestInterceptor {
    override fun process(input: HandlerInput) {
        // Load and cache data
        val data = fetchData()
        input.attributesManager.sessionAttributes["key"] = data
    }
}
```

Register in `GolfCanadaAlexaSkill.kt`:

```kotlin
.addRequestInterceptors(
    AuthenticationRequestInterceptor(),
    UserProfileInterceptor(),
    YourInterceptor(),  // Order matters!
)
```

## Testing Guidelines

### Unit Tests

- Use **Kotest** for test structure
- Use **MockK** for mocking dependencies
- Test both success and error paths
- Test edge cases and validation

### Integration Tests

Some tests make real API calls when `TEST_USERNAME` and `TEST_PASSWORD` environment variables are set. These are disabled by default.

### Local Testing with SAM

```bash
# Build
sam build

# Test authentication function
sam local invoke GolfCanadaAuthenticationFunction \
    -e events/AuthenticationLoginEvent.json \
    --parameter-overrides "ClientId=test ClientSecret=test SkillId=test"

# Test skill function
sam local invoke GolfCanadaAlexaSkillFunction \
    -e events/AlexaLaunchEvent.json
```

## Code Style and Conventions

### Kotlin Guidelines

- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful, descriptive names
- Prefer immutability (`val` over `var`)
- Use extension functions appropriately
- Add KDoc comments to public functions

### Project Patterns

- **Varargs Configuration**: Use varargs methods (`addRequestHandlers()`, `addRequestInterceptors()`) for clean configuration
- **Template Responses**: Always use `input.generateTemplateResponse()` for consistency
- **Exception Handling**: Use custom exception handlers for common error scenarios
- **Session vs Request Attributes**: 
  - Session Attributes: Data that persists across multiple turns in a session
  - Request Attributes: Data that only lives for one request (often with TTL)

### Logging

```kotlin
import org.slf4j.LoggerFactory

class YourHandler {
    private val logger = LoggerFactory.getLogger(YourHandler::class.java)
    
    fun process() {
        logger.info("Information message")
        logger.warn("Warning message")
        logger.error("Error message with context", exception)
    }
}
```

## Common Gotchas

### 1. SSL Certificate Issues

Golf Canada's SSL certificate requires custom trust store configuration. The project includes:
- Certificate layer: `layers/cacerts/`
- Lambda environment variable: `JAVA_TOOL_OPTIONS`
- Local development: Install certificate to Java keystore

### 2. OpenAPI Generated Code

Generated code is in `build/generated/openapi/`. Don't edit directly - changes will be overwritten. Instead:
- Add extensions in `src/main/kotlin/kjd/golfcanada/client/`
- Modify the OpenAPI spec and regenerate

### 3. Token Management

- Access tokens are passed by Alexa in the request
- Use `extractAccessToken()` extension function to clean the token
- Set on `ApiClient.accessToken` before making API calls
- Tokens are automatically refreshed by Alexa using the refresh endpoint

### 4. Caching Strategy

- **User Profile**: Cached in session, loaded once per session
- **Handicap Data**: Cached with 10-minute TTL for current user only
- **Friend Data**: Not cached, always fetched fresh

## Deployment

### Manual Deployment

```bash
# Build
./gradlew packageJar
sam build

# Deploy
sam deploy --guided
```

### GitHub Actions

The project uses GitHub Actions for automated deployment:
- Workflow: `.github/workflows/deploy.yml`
- Triggered manually with deployment target selection
- Requires secrets: AWS_ROLE_ARN, AWS_REGION, STACK_NAME, etc.

## Security Considerations

- Never commit credentials to source control
- Use SAM parameters for sensitive values
- Client credentials are NoEcho in CloudFormation
- Token storage is currently in-memory (consider DynamoDB for production)
- Always validate user input
- Use HTTPS for all external calls

## Getting Help

- Review existing code for patterns
- Check documentation in `docs/`
- Review test files for usage examples
- Open issues for questions or clarifications
- Email ken.j.davidson@live.ca for sensitive matters

## Summary Checklist for Adding Features

When adding a new intent or feature, ensure you:

- [ ] Create handler class with proper documentation
- [ ] Create English and French response templates
- [ ] Register handler in `GolfCanadaAlexaSkill.kt`
- [ ] Update interaction model (`model/model.json`)
- [ ] Add comprehensive unit tests
- [ ] Update README.md with examples
- [ ] Update docs/AVAILABLE_FEATURES.md with feature details
- [ ] Update docs/ARCHITECTURE.md if needed
- [ ] Build and test locally
- [ ] Verify documentation is consistent across all files
