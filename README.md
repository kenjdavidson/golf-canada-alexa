# Golf Canada Alexa Skill (Unofficial)

Provides basic Golf Canada membership functionality through an Amazon Alexa skill.

> This is an entirely unofficial application and may get shut down at any point due to the 
> nature in which it's interacting with the Golf Canada environment. For information (or if
> you're Golf Canada and would like to hire me) please email me at 
> **ken.j.davidson@live.ca**

## Table of Contents

- [Project Overview](#project-overview)
- [Architecture](#architecture)
- [Available Interactions](#available-interactions)
- [Future Interactions](#future-interactions)
- [Local Development](#local-development)
- [AWS Deployment](#aws-deployment)
- [Contributions](#contributions)
- [Related Documentation](#related-documentation)

## Project Overview

This project is an Alexa skill that connects to Golf Canada for the purpose of looking up and adding scoring information. The project consists of two main components:

1. **Authentication Function** - An AWS Lambda function that proxies Golf Canada OAuth services to enable Alexa Account Linking. ✅ Completed and deployed to AWS.
2. **Skill Handlers** - The Alexa skill handlers that process voice commands for handicap lookup, score history, and other Golf Canada features. ✅ Core functionality implemented and tested.

## Architecture

The project uses:

- **Kotlin/JVM (Java 21)** - Primary programming language
- **AWS SAM (Serverless Application Model)** - Infrastructure as code for Lambda deployment
- **Gradle** - Build tool with OpenAPI code generation
- **Amazon Alexa SDK** - For skill development
- **OkHttp/Moshi** - HTTP client and JSON serialization

### Authentication Flow

The authentication component implements an OAuth wrapper around Golf Canada's authentication:

```
Alexa App → /login → Display Golf Canada login form
User submits → /code → Authenticate with Golf Canada → Store token → Redirect with code
Alexa → /authToken → Exchange code for access/refresh tokens
Alexa → /authToken (refresh_token) → Refresh access token
```

For detailed architecture documentation, see [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Available Interactions

The following interactions are currently available:

### Authentication & Account Management
- **Account Linking** - Link your Golf Canada credentials through the Alexa app
- **Launch** - Start the skill with "Alexa, open Golf Canada"
- **Help** - Get information about available commands

### Handicap Information
- **Your Handicap** - "What's my handicap?" or "Tell me my handicap index"
- **Friend's Handicap** - "What is [friend name]'s handicap?"
  - Supports both full names and first names
  - Searches your Golf Canada friends list

### Membership Information
- **Membership Status** - "What's my membership?" or "When does my membership expire?"
  - View your membership level and expiration date
  - Check if your membership is active

### Score History
- **Your Recent Rounds** - "Tell me about my last round" or "How did I play last?"
- **Friend's Recent Rounds** - "What was [friend name]'s last score?"

> **Privacy Note**: This application is hosted on AWS and gains access to your Golf Canada
> account through Account Linking, which you can read about here:
> https://developer.amazon.com/en-US/docs/alexa/account-linking/add-account-linking.html
> No information is stored outside the Authentication information required by
> Alexa in order to facilitate account linking.

## Future Interactions

The following interactions are planned for future development:

- [ ] Year-specific score history ("How did I play in 2024?")
- [ ] Multiple rounds statistics ("Tell me about my last 5 rounds")
- [ ] Adding/saving new rounds through voice
- [ ] More detailed score breakdowns

For detailed planning on future work, see [docs/ROADMAP.md](docs/ROADMAP.md).

## Local Development

### Prerequisites

- **Java 21** (Amazon Corretto recommended)
- **Gradle** (wrapper included)
- **AWS SAM CLI** (for local Lambda testing)
- **Docker** (for SAM local invocation)

### Setting Up the Project

1. **Clone the repository:**
   ```shell
   git clone git@github.com:kenjdavidson/golf-canada-alexa.git
   cd golf-canada-alexa
   ```

2. **Configure Java:**
   ```shell
   # Ensure JAVA_HOME is set to Java 21
   export JAVA_HOME=/path/to/java21
   ```

3. **Install SSL Certificate:**
   
   Due to Java SSL certificate trust chain issues with Golf Canada's certificate, you need to add the certificate to your local JDK:
   ```shell
   # Add Certificate
   $JAVA_HOME/bin/keytool -importcert -file layers/cacerts/_.golfcanada.ca.crt \
       -keystore $JAVA_HOME/lib/security/cacerts \
       -alias "GolfCanadaCert" -noprompt -storepass changeit
   
   # Remove Certificate (when no longer needed)
   $JAVA_HOME/bin/keytool -delete -keystore $JAVA_HOME/lib/security/cacerts \
       -alias "GolfCanadaCert" -storepass changeit
   ```

4. **Build the project:**
   ```shell
   ./gradlew build
   ```

5. **Run tests:**
   ```shell
   ./gradlew test
   ```

### Integration Tests

Some tests in this project make real network calls to the Golf Canada API for integration testing. These tests are disabled by default and only run when both `TEST_USERNAME` and `TEST_PASSWORD` environment variables are set.

**Running integration tests locally:**
```shell
# Option 1: Set environment variables directly (will be in shell history)
export TEST_USERNAME="your-golf-canada-username"
export TEST_PASSWORD="your-golf-canada-password"
./gradlew test

# Option 2: Use a .env file (recommended to avoid shell history exposure)
# Create a .env file (ensure it's in .gitignore):
echo "export TEST_USERNAME='your-username'" >> .env
echo "export TEST_PASSWORD='your-password'" >> .env
source .env
./gradlew test
```

> **Security Note**: Be careful not to commit your credentials. If using a .env file, ensure it's listed in `.gitignore`.

**GitHub Actions integration:**

The GitHub Actions workflow is configured to run integration tests if the following repository secrets are set:
- `TEST_USERNAME`: Your Golf Canada username for testing
- `TEST_PASSWORD`: Your Golf Canada password for testing

If these secrets are not configured, the integration tests will be skipped, and only unit tests will run. This allows the CI pipeline to function without requiring access to the Golf Canada API.

To add these secrets:
1. Go to your repository Settings → Secrets and variables → Actions
2. Click "New repository secret"
3. Add `TEST_USERNAME` with your Golf Canada username as the value
4. Add `TEST_PASSWORD` with your Golf Canada password as the value

> **Note**: These credentials should be valid Golf Canada account credentials used only for testing purposes.

### Project Structure

```
golf-canada-alexa/
├── src/
│   ├── main/
│   │   ├── kotlin/kjd/golfcanada/
│   │   │   ├── auth/          # Authentication Lambda handler
│   │   │   ├── alexa/         # Alexa skill handlers
│   │   │   │   ├── handler/   # Intent and request handlers
│   │   │   │   ├── interceptor/ # Request/response interceptors
│   │   │   │   ├── data/      # Session data models
│   │   │   │   ├── exception/ # Custom exception classes
│   │   │   │   ├── model/     # Response data models
│   │   │   │   └── util/      # Utility classes (e.g., FriendNameMatcher)
│   │   │   ├── client/        # Golf Canada API client
│   │   │   │   └── provider/  # API client provider with token management
│   │   │   └── util/          # Utility functions
│   │   └── resources/
│   │       ├── client/        # OpenAPI spec for Golf Canada
│   │       └── kjd/golfcanada/
│   │           ├── auth/      # Login page HTML
│   │           └── alexa/     # Response templates (FreeMarker)
│   └── test/                  # Comprehensive unit tests
├── layers/                    # Lambda layers (SSL certificates)
├── model/                     # Alexa interaction model
├── events/                    # Test events for Lambda
├── template.yaml              # AWS SAM template
└── docs/                      # Additional documentation
```

### Running Locally with SAM

1. **Build the SAM application:**
   ```shell
   # Build all functions
   sam build
   
   # Or build a specific function
   sam build GolfCanadaAuthenticationFunction
   sam build GolfCanadaAlexaSkillFunction
   ```

2. **Invoke locally with a test event:**
   
   For the Authentication Function:
   ```shell
   sam local invoke GolfCanadaAuthenticationFunction \
       -e events/AuthenticationLoginEvent.json \
       --parameter-overrides "ClientId=your-client-id ClientSecret=your-client-secret SkillId=your-skill-id"
   ```
   
   For the Alexa Skill Function (create a test event based on Alexa request format):
   ```shell
   sam local invoke GolfCanadaAlexaSkillFunction \
       -e events/AlexaLaunchEvent.json \
       --parameter-overrides "ClientId=your-client-id ClientSecret=your-client-secret SkillId=your-skill-id"
   ```
   
   > **Note**: With SAM parameters, you no longer need to create an `env.json` file. Parameters are passed directly via the `--parameter-overrides` flag.

3. **Start a local API:**
   ```shell
   sam local start-api --parameter-overrides "ClientId=your-client-id ClientSecret=your-client-secret SkillId=your-skill-id"
   ```

## AWS Deployment

The project supports both automated deployment via GitHub Actions and manual deployment via AWS SAM CLI.

### Automated Deployment with GitHub Actions

The recommended approach is to use the GitHub Actions workflow for manual deployment:

1. Go to **Actions** tab in your GitHub repository
2. Select the **Deploy to AWS Lambda** workflow
3. Click **Run workflow**
4. Choose the deployment target from the dropdown:
   - **both** - Deploy both Lambda functions (default)
   - **authentication** - Deploy only the authentication function
   - **skill** - Deploy only the skill function
5. Click **Run workflow** to start the deployment

The workflow will build and deploy only the selected function(s) to AWS.

**Required GitHub Secrets:**
- `AWS_ROLE_ARN` - IAM role ARN for GitHub Actions to assume
- `AWS_REGION` - Target AWS region (e.g., `us-east-1`)
- `STACK_NAME` - CloudFormation stack name
- `GOLF_CANADA_CLIENT_ID` - OAuth Client ID
- `GOLF_CANADA_CLIENT_SECRET` - OAuth Client Secret
- `ALEXA_SKILL_ID` - Your Alexa Skill ID

For complete setup instructions and detailed information on configuring GitHub Secrets, see **[docs/DEPLOYMENT.md](docs/DEPLOYMENT.md)**.

### Manual Deployment

For manual deployment or local testing:

#### Prerequisites

- **AWS CLI** configured with appropriate credentials
- **AWS SAM CLI**

#### Deploying Both Functions

The project includes two Lambda functions that are deployed together:
1. **GolfCanadaAuthenticationFunction** - OAuth wrapper for account linking (with Function URL)
2. **GolfCanadaAlexaSkillFunction** - Alexa skill handler for voice interactions

1. **Build the deployment package:**
   ```shell
   ./gradlew packageJar
   sam build
   ```

2. **Deploy to AWS:**
   ```shell
   sam deploy --guided
   ```
   
   Follow the prompts to configure:
   - **Stack name**: `golf-canada-alexa`
   - **AWS Region**: `us-east-1` (recommended for Alexa skills)
   - **Parameter ClientId**: Your OAuth Client ID for Golf Canada authentication
   - **Parameter ClientSecret**: Your OAuth Client Secret for Golf Canada authentication
   - **Parameter SkillId**: Your Alexa Skill ID from the Alexa Developer Console
   - Allow SAM to create IAM roles

3. **Note the Function URL:**
   After deployment, the outputs will display:
   - **GolfCanadaAuthenticationFunctionUrl**: The authentication function URL needed for Alexa skill configuration
   - **GolfCanadaAlexaSkillFunctionArn**: The ARN to configure in the Alexa Developer Console as the skill endpoint
   
   Example:
   ```
   https://<function-id>.lambda-url.us-east-1.on.aws/
   ```

### Alexa Skill Configuration

1. **Configure the Skill Endpoint** in the Alexa Developer Console:
   - Navigate to your skill's endpoint configuration
   - Select **AWS Lambda ARN**
   - Enter the **GolfCanadaAlexaSkillFunctionArn** from the deployment outputs
   - For North America region, the ARN should look like: `arn:aws:lambda:us-east-1:ACCOUNT_ID:function:FUNCTION_NAME`

2. **Configure Account Linking** in the Alexa Developer Console:
   - **Authorization URI**: `{GolfCanadaAuthenticationFunctionUrl}/login`
   - **Access Token URI**: `{GolfCanadaAuthenticationFunctionUrl}/authToken`
   - **Client ID**: The same value you used for the `ClientId` parameter
   - **Client Secret**: The same value you used for the `ClientSecret` parameter
   - **Client Authentication Scheme**: HTTP Basic
   - **Scope**: `address email offline_access openid phone profile roles`

3. **Upload the interaction model** from `model/model.json`

### SSL Certificate Layer

The project includes a Lambda layer (`GolfCanadaAuthenticationCertLayer`) that provides the Golf Canada SSL certificate to the Lambda function. This is automatically deployed with the SAM template and configured via:
```yaml
JAVA_TOOL_OPTIONS: "-Djavax.net.ssl.trustStore=/opt/cacerts/cacertsclient_keystore_1.jks"
```

## Contributions

Please feel free to contribute with suggestions, issues found, pull requests or discussion updates. You should be able to tell from the current code what the expected styles and practices are... please continue with them.

See [CONTRIBUTION.md](CONTRIBUTION.md) for detailed contribution guidelines.

### Issues and Suggestions

Open an Issue on the project's GitHub page.

### Pull Requests

If you've worked on a feature or a fix, please open a well-documented pull request.

### Discussions

At this point I'm unsure whether the Wiki or Discussions will be available for this project, but if they are I'll always welcome help with documentation.

## Related Documentation

- [Architecture Documentation](docs/ARCHITECTURE.md)
- [Development Roadmap](docs/ROADMAP.md)
- [API Documentation](docs/API.md)
- [Deployment Guide](docs/DEPLOYMENT.md)
- [Contribution Guidelines](CONTRIBUTION.md)
- [Privacy Policy](PRIVACY_POLICY.md)
- [Terms of Service](TERMS_OF_SERVICE.md)
- [Amazon Alexa Account Linking](https://developer.amazon.com/en-US/docs/alexa/account-linking/add-account-linking.html)
- [AWS SAM Documentation](https://docs.aws.amazon.com/serverless-application-model/)