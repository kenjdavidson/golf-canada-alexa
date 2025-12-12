# Contribution Guide

Contributions are always welcome! This guide will help you get set up for development and understand the contribution process.

## Table of Contents

- [Getting Started](#getting-started)
- [Development Environment](#development-environment)
- [Project Structure](#project-structure)
- [Development Workflow](#development-workflow)
- [Testing](#testing)
- [Pull Request Process](#pull-request-process)
- [Code Style](#code-style)
- [Releases and Versioning](#releases-and-versioning)

## Getting Started

### Prerequisites

- **Java 21** (Amazon Corretto recommended)
- **Gradle** (wrapper included in project)
- **AWS SAM CLI** (for local Lambda testing)
- **Docker** (required for SAM local invocation)
- **Git**

### Clone the Project

```shell
git clone git@github.com:kenjdavidson/golf-canada-alexa.git
cd golf-canada-alexa
```

### Initial Setup

1. **Configure Java:**
   ```shell
   # Verify Java version
   java -version  # Should show Java 21
   
   # Set JAVA_HOME if needed
   export JAVA_HOME=/path/to/java21
   ```

2. **Install SSL Certificate (Required for Golf Canada API calls):**
   ```shell
   sudo $JAVA_HOME/bin/keytool -importcert \
       -file layers/cacerts/_.golfcanada.ca.crt \
       -keystore $JAVA_HOME/lib/security/cacerts \
       -alias "GolfCanadaCert" \
       -noprompt \
       -storepass changeit
   ```

3. **Build the project:**
   ```shell
   ./gradlew build
   ```

4. **Run tests:**
   ```shell
   ./gradlew test
   ```

## Development Environment

### IDE Setup

**IntelliJ IDEA (Recommended):**
1. Open the project directory
2. IntelliJ should automatically detect the Gradle project
3. Install the AWS Toolkit plugin for Lambda development
4. Mark `build/generated/openapi/src/main/kotlin` as a Generated Sources Root

**VS Code:**
1. Install the Kotlin Language extension
2. Install the AWS Toolkit extension
3. Open the project folder

### AWS SAM Local Development

1. **Install SAM CLI:**
   - Follow [AWS SAM CLI installation guide](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/install-sam-cli.html)

2. **Build the SAM application:**
   ```shell
   sam build GolfCanadaAuthenticationFunction
   ```

3. **Create environment file (`env.json`):**
   ```json
   {
     "GolfCanadaAuthenticationFunction": {
       "CLIENT_ID": "test-client-id",
       "CLIENT_SECRET": "test-client-secret"
     }
   }
   ```
   
   > ⚠️ Do not commit `env.json` to version control!

4. **Invoke locally with test event:**
   ```shell
   sam local invoke GolfCanadaAuthenticationFunction \
       -e events/AuthenticationLoginEvent.json \
       --env-vars env.json
   ```

5. **Start local API for testing:**
   ```shell
   sam local start-api --env-vars env.json
   ```

### Known Issues

**IntelliJ AWS Toolkit Issue:**
The IntelliJ AWS Toolkit may generate code in `build/generated/src/main/kotlin/.aws-sam` which can cause duplicate class errors. If this happens:
- Delete the `.aws-sam` directory
- Rebuild with `./gradlew clean build`

## Project Structure

```
golf-canada-alexa/
├── src/main/kotlin/kjd/golfcanada/
│   ├── auth/           # Authentication Lambda handler
│   ├── alexa/          # Alexa skill handlers (in development)
│   ├── client/         # Golf Canada API client extensions
│   └── util/           # Utility functions
├── src/main/resources/
│   ├── client/         # OpenAPI specification
│   └── kjd/golfcanada/ # Templates and HTML resources
├── src/test/           # Test files
├── layers/             # Lambda layers (SSL certificates)
├── model/              # Alexa interaction model
├── events/             # Test events for Lambda
├── docs/               # Documentation
├── template.yaml       # AWS SAM template
└── build.gradle.kts    # Gradle build configuration
```

## Development Workflow

### Adding a New Feature

1. **Check available features:** See `docs/AVAILABLE_FEATURES.md` for current and planned features
2. **Open an issue:** Discuss the feature before implementing
3. **Create a branch:**
   ```shell
   git checkout -b feature/your-feature-name
   ```
4. **Implement the feature:**
   - Write tests first (TDD encouraged)
   - Follow existing code patterns
   - Update documentation as needed
5. **Test thoroughly:**
   ```shell
   ./gradlew test
   ```
6. **Open a pull request**

### Adding a New Intent Handler

1. Create handler class in `src/main/kotlin/kjd/golfcanada/alexa/handler/`
2. Implement `RequestHandler` interface with `canHandle()` and `handle()` methods
3. Add response templates (both English and French) in `src/main/resources/kjd/golfcanada/alexa/responses/`
   - Format: `YourIntentResponse.ftl` and `YourIntentResponse_fr.ftl`
4. Register handler in `GolfCanadaAlexaSkill.kt` using `addRequestHandlers()` varargs method:
   ```kotlin
   .addRequestHandlers(
       // ... existing handlers ...
       YourNewIntentHandler(),
       // ... other handlers ...
   )
   ```
5. Add comprehensive unit tests using Kotest and MockK
6. Update the interaction model in `model/model.json` if adding new intents or slots
7. **Update documentation** (See [Documentation Requirements](#documentation-requirements))

### Adding a New Request Interceptor

Request interceptors run before intent handlers and can pre-load data or validate requests.

1. Create interceptor class in `src/main/kotlin/kjd/golfcanada/alexa/interceptor/`
2. Implement `RequestInterceptor` interface with `process()` method
3. Register interceptor in `GolfCanadaAlexaSkill.kt` using `addRequestInterceptors()` varargs method:
   ```kotlin
   .addRequestInterceptors(
       // ... existing interceptors ...
       YourNewInterceptor(),
       // ... other interceptors ...
   )
   ```
4. Consider caching strategy - use Session Attributes for data that should persist across turns
5. Add unit tests

**Note:** Interceptors run in the order they are registered.

### Updating the Golf Canada API Client

1. Modify `src/main/resources/client/golfcanada.yaml`
2. Regenerate the client:
   ```shell
   ./gradlew openApiGenerate
   ```
3. Update any custom extensions in `src/main/kotlin/kjd/golfcanada/client/`

## Testing

### Running Tests

```shell
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "kjd.golfcanada.auth.AuthenticationHandlerTest"

# Run with verbose output
./gradlew test --info
```

### Test Structure

- Tests use **Kotest** for assertions
- Tests use **MockK** for mocking
- Test files mirror source structure in `src/test/kotlin/`

### Writing Tests

```kotlin
class MyHandlerTest : StringSpec({
    "should handle valid request" {
        val handler = MyHandler()
        val result = handler.handleRequest(mockEvent, mockContext)
        result.statusCode shouldBe 200
    }
})
```

### Local Integration Testing

1. Start local API:
   ```shell
   sam local start-api --env-vars env.json
   ```

2. Test with curl:
   ```shell
   curl "http://localhost:3000/login?client_id=test&redirect_uri=http://localhost&response_type=code&state=test123"
   ```

## Documentation Requirements

When adding a new intent or feature, **always** update the following documentation files:

### 1. README.md
Add to the "Available Interactions" section with:
- Intent description
- Example utterances
- Expected behavior
- Any limitations or special notes

### 2. docs/AVAILABLE_FEATURES.md
Update with the new feature details:
- Voice commands/utterances
- What the feature does
- Expected responses
- Feature status (Available/In Progress/Planned)

### 3. docs/ARCHITECTURE.md
Update if your changes affect:
- System architecture or components
- Data flow or request processing
- New dependencies
- Caching strategy

### 4. model/model.json
Update the Alexa interaction model with:
- New intents
- New slots and slot types
- Sample utterances

For detailed guidance on adding features, see [.github/AGENT.md](../.github/AGENT.md).

## Pull Request Process

1. **Ensure tests pass:**
   ```shell
   ./gradlew test
   ```

2. **Verify documentation is updated:**
   - Review all files listed in [Documentation Requirements](#documentation-requirements)
   - Ensure consistency across all documentation
   - Add inline code comments where helpful

3. **Create PR with:**
   - Clear description of changes
   - Reference to related issue(s)
   - Screenshots for UI changes
   - Test evidence
   - Documentation checklist

4. **PR Review:**
   - Address review feedback
   - Keep commits clean and focused
   - Squash if requested

## Code Style

### Kotlin Guidelines

- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful names for functions and variables
- Prefer immutability (`val` over `var`)
- Use extension functions where appropriate

### Formatting

The project follows standard Kotlin formatting. Consider using:
- IntelliJ's built-in Kotlin formatter
- ktlint for command-line formatting

### Documentation

- Add KDoc comments to public functions
- Include `@param` and `@return` tags
- Document exceptions with `@throws`

### Example

```kotlin
/**
 * Handles the login request from Alexa Account Linking.
 *
 * @param event API Gateway event containing query parameters
 * @return Response with login form HTML or error
 * @throws AuthenticationException if validation fails
 */
private fun handleLoginRequest(event: APIGatewayV2HTTPEvent): APIGatewayProxyResponseEvent {
    // Implementation
}
```

## Releases and Versioning

### Creating a Release

Releases are created manually using a GitHub Actions workflow. The process is:

1. **Tag the release:**
   ```shell
   # Create an annotated tag with semantic versioning
   git tag -a v1.0.0 -m "Release version 1.0.0"
   
   # Push the tag to GitHub
   git push origin v1.0.0
   ```

2. **Create the release via GitHub Actions:**
   - Go to the **Actions** tab in the GitHub repository
   - Select the **Create Release** workflow
   - Click **Run workflow**
   - Enter the tag name (e.g., `v1.0.0`) in the input field
   - Click **Run workflow** to start the release process

3. **Automated release generation:**
   - The workflow validates that the tag exists
   - Generates a changelog from commits since the last tag
   - Creates a GitHub release with the changelog and release notes
   - Pre-release tags (containing `alpha`, `beta`, or `rc`) are automatically marked as pre-releases

### Versioning Convention

This project follows [Semantic Versioning](https://semver.org/) (SemVer):

- **MAJOR.MINOR.PATCH** (e.g., `v1.2.3`)
  - **MAJOR**: Breaking changes or major feature releases
  - **MINOR**: New features that are backward-compatible
  - **PATCH**: Bug fixes and minor improvements

**Examples:**
- `v1.0.0` - Initial stable release
- `v1.1.0` - Added new intent handler
- `v1.1.1` - Fixed bug in authentication
- `v2.0.0` - Breaking API changes
- `v1.2.0-beta.1` - Pre-release beta version

### Changelog Generation

The changelog is automatically categorized by commit message patterns:

- **Features** - Commits starting with `feat:`, `feature:`, `Add`, or `Implement`
- **Bug Fixes** - Commits starting with `fix:`, `bug:`, or `Fix`
- **Documentation** - Commits starting with `docs:`, `doc:`, or updating markdown files
- **Other Changes** - All other commits

**Tips for better changelogs:**
- Use clear, descriptive commit messages
- Follow [Conventional Commits](https://www.conventionalcommits.org/) when possible
- Examples:
  - `feat: Add handicap lookup intent handler`
  - `fix: Correct authentication token refresh logic`
  - `docs: Update deployment guide with new steps`

## Questions?

- Open an issue for questions
- Check existing issues and documentation first
- Email ken.j.davidson@live.ca for sensitive matters