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
   # On Linux/Mac
   sam build GolfCanadaAuthenticationFunction
   
   # On Windows PowerShell
   $env:JAVA_HOME="/path/to/java21"
   sam build GolfCanadaAuthenticationFunction
   ```

2. **Invoke locally with a test event:**
   ```shell
   sam local invoke GolfCanadaAuthenticationFunction \
       -e events/AuthenticationLoginEvent.json \
       --env-vars env.json
   ```
   
   Create an `env.json` file (do not commit):
   ```json
   {
     "GolfCanadaAuthenticationFunction": {
       "CLIENT_ID": "your-client-id",
       "CLIENT_SECRET": "your-client-secret"
     }
   }
   ```

3. **Start a local API:**
   ```shell
   sam local start-api
   ```

## AWS Deployment

### Prerequisites

- **AWS CLI** configured with appropriate credentials
- **AWS SAM CLI**
- **An S3 bucket** for deployment artifacts

### Authentication Function Deployment

The authentication function (`GolfCanadaAuthenticationFunction`) is deployed as an AWS Lambda with a Function URL.

1. **Build the deployment package:**
   ```shell
   ./gradlew packageJar
   sam build GolfCanadaAuthenticationFunction
   ```

2. **Deploy to AWS:**
   ```shell
   sam deploy --guided
   ```
   
   Follow the prompts to configure:
   - Stack name: `golf-canada-alexa`
   - AWS Region: `us-east-1` (recommended for Alexa skills)
   - Allow SAM to create IAM roles

3. **Configure environment variables in AWS Console:**
   - Navigate to the Lambda function
   - Set `CLIENT_ID` and `CLIENT_SECRET` environment variables
   - These credentials are used to validate Alexa account linking requests

4. **Note the Function URL:**
   After deployment, the Lambda Function URL will be displayed. This URL is needed for Alexa skill configuration:
   ```
   https://<function-id>.lambda-url.us-east-1.on.aws/
   ```

### Alexa Skill Configuration

1. In the Alexa Developer Console, configure Account Linking:
   - **Authorization URI**: `{function-url}/login`
   - **Access Token URI**: `{function-url}/authToken`
   - **Client ID**: Your configured `CLIENT_ID`
   - **Client Secret**: Your configured `CLIENT_SECRET`
   - **Client Authentication Scheme**: HTTP Basic
   - **Scope**: `address email offline_access openid phone profile roles`

2. Upload the interaction model from `model/model.json`

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
- [Contribution Guidelines](CONTRIBUTION.md)
- [Privacy Policy](PRIVACY_POLICY.md)
- [Terms of Service](TERMS_OF_SERVICE.md)
- [Amazon Alexa Account Linking](https://developer.amazon.com/en-US/docs/alexa/account-linking/add-account-linking.html)
- [AWS SAM Documentation](https://docs.aws.amazon.com/serverless-application-model/)