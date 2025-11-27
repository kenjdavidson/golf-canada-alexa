# Architecture Documentation

## Overview

The Golf Canada Alexa Skill is built using a serverless architecture on AWS, primarily utilizing AWS Lambda functions and the Amazon Alexa Skills Kit. This document describes the system architecture, components, and their interactions.

## System Components

### 1. Authentication Function (`GolfCanadaAuthenticationFunction`)

The authentication function is an AWS Lambda that implements an OAuth wrapper around Golf Canada's authentication system. This is necessary because Golf Canada doesn't provide standard OAuth features compatible with Alexa Account Linking.

**Location:** `src/main/kotlin/kjd/golfcanada/auth/AuthenticationHandler.kt`

**Endpoints:**
- `/login` (GET) - Displays the login form
- `/code` (POST) - Processes login and returns authorization code
- `/authToken` (POST) - Exchanges code for tokens or refreshes tokens

**Status:** ✅ Completed and deployed

### 2. Alexa Skill Handlers

The skill handlers process voice commands and return appropriate responses.

**Location:** `src/main/kotlin/kjd/golfcanada/alexa/`

**Components:**
- `GolfCanadaAlexaSkill.kt` - Main skill entry point
- `handler/` - Intent handlers
- `interceptor/` - Request/response interceptors

**Status:** 🚧 In development

### 3. Golf Canada API Client

Generated client code for interacting with Golf Canada's API.

**Location:** `src/main/kotlin/kjd/golfcanada/client/` and `build/generated/openapi/`

**OpenAPI Spec:** `src/main/resources/client/golfcanada.yaml`

## Architecture Diagram

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                               Amazon Alexa                                    │
│                                                                              │
│  ┌─────────────┐    ┌─────────────────┐    ┌───────────────────────────┐    │
│  │ User Device │───▶│ Alexa Voice     │───▶│ Alexa Skills Kit          │    │
│  │ (Echo, etc) │    │ Service         │    │ (Account Linking)         │    │
│  └─────────────┘    └─────────────────┘    └───────────────────────────┘    │
│                                                        │                     │
└────────────────────────────────────────────────────────│─────────────────────┘
                                                         │
                     ┌───────────────────────────────────┴──────────────────────┐
                     │                                                          │
                     ▼                                                          ▼
┌────────────────────────────────────────┐    ┌────────────────────────────────────────┐
│       AWS Lambda (Authentication)       │    │       AWS Lambda (Skill Handler)       │
│                                        │    │              (Planned)                  │
│  ┌──────────────────────────────────┐  │    │  ┌──────────────────────────────────┐  │
│  │    AuthenticationHandler         │  │    │  │    GolfCanadaAlexaSkill          │  │
│  │                                  │  │    │  │                                  │  │
│  │  /login  → Show login form       │  │    │  │  LaunchRequestHandler            │  │
│  │  /code   → Authenticate user     │  │    │  │  HelpIntentHandler               │  │
│  │  /token  → Exchange/refresh      │  │    │  │  PlayerProfileHandicapHandler    │  │
│  └──────────────────────────────────┘  │    │  │  (and more...)                   │  │
│                                        │    │  └──────────────────────────────────┘  │
│  ┌──────────────────────────────────┐  │    │                                        │
│  │    TokenRepository (in-memory)   │  │    │  ┌──────────────────────────────────┐  │
│  │    Stores temp auth codes        │  │    │  │    AuthenticationInterceptor     │  │
│  └──────────────────────────────────┘  │    │  │    Sets auth context per request │  │
│                                        │    │  └──────────────────────────────────┘  │
└────────────────────────────────────────┘    └────────────────────────────────────────┘
                     │                                           │
                     │                                           │
                     └───────────────────┬───────────────────────┘
                                         │
                                         ▼
                        ┌────────────────────────────────────────┐
                        │          Golf Canada API               │
                        │       https://scg.golfcanada.ca        │
                        │                                        │
                        │  /connect/token - Authentication       │
                        │  (Additional endpoints TBD)            │
                        └────────────────────────────────────────┘
```

## Authentication Flow

### Initial Account Linking

```
1. User enables skill in Alexa app
   └──▶ Alexa opens /login URL

2. /login endpoint (GET)
   ├── Validates client_id, response_type, state
   └── Returns HTML login form

3. User submits credentials
   └──▶ POST /code

4. /code endpoint (POST)
   ├── Validates request parameters
   ├── Calls Golf Canada API with credentials
   ├── Stores AuthToken with generated code
   └── Redirects to Alexa with code & state

5. Alexa exchanges code for tokens
   └──▶ POST /authToken (grant_type=authorization_code)

6. /authToken endpoint (POST)
   ├── Validates client_id, client_secret
   ├── Retrieves stored AuthToken by code/state
   └── Returns access_token, refresh_token
```

### Token Refresh

```
1. Alexa detects expired token
   └──▶ POST /authToken (grant_type=refresh_token)

2. /authToken endpoint (POST)
   ├── Validates client_id, client_secret
   ├── Calls Golf Canada API to refresh
   └── Returns new access_token, refresh_token
```

## Lambda Configuration

### Authentication Function

```yaml
Runtime: java21
Memory: 512 MB
Timeout: 120 seconds
Handler: kjd.golfcanada.auth.AuthenticationHandler::handleRequest

Environment Variables:
  - CLIENT_ID: Alexa account linking client ID
  - CLIENT_SECRET: Alexa account linking client secret
  - JAVA_TOOL_OPTIONS: SSL trust store configuration

Layers:
  - GolfCanadaAuthenticationCertLayer (SSL certificates)
```

### Skill Handler Function (Planned)

```yaml
Runtime: java21
Memory: 512 MB
Timeout: 20 seconds
Handler: kjd.golfcanada.alexa.GolfCanadaAlexaSkill

Environment Variables:
  - SKILL_ID: Alexa skill ID
```

## Security Considerations

### Token Storage

Currently, authentication tokens are stored in memory using a `ConcurrentHashMap`. This works for low-traffic scenarios where `/code` and `/authToken` requests hit the same Lambda instance. For higher traffic:

- Consider implementing DynamoDB storage
- Add TTL for automatic cleanup
- Implement cross-instance token sharing

### SSL Certificates

Golf Canada's SSL certificate requires a custom trust store due to Java's certificate chain not matching the Go Daddy root certificate. The Lambda layer provides:

- Custom keystore with Golf Canada certificate
- Configured via `JAVA_TOOL_OPTIONS`

### Client Credentials

- `CLIENT_ID` and `CLIENT_SECRET` are stored as Lambda environment variables
- These should be encrypted using AWS KMS in production
- Never commit these values to source control

## Data Flow

### Request Processing

1. **Request arrives** at Lambda Function URL
2. **Route matching** in `AuthenticationHandler.handleRequest()`
3. **Validation** of parameters using extension functions
4. **Business logic** execution
5. **Response building** with appropriate status and headers

### Error Handling

All errors are converted to OAuth-compatible error responses:

```kotlin
enum class ErrorCode(val code: Int, val errorResponse: String) {
    INVALID_CLIENT_ID(101, "unauthorized_client"),
    INVALID_SECRET(102, "unauthorized_client"),
    // ... more error codes
}
```

## Dependencies

### Runtime Dependencies

- `com.amazon.alexa:ask-sdk` - Alexa Skills Kit
- `com.amazonaws:aws-lambda-java-core` - Lambda runtime
- `com.squareup.okhttp3:okhttp` - HTTP client
- `com.squareup.moshi:moshi` - JSON serialization
- `org.apache.logging.log4j` - Logging

### Build Dependencies

- `org.openapi.generator` - API client generation
- Gradle Kotlin DSL - Build configuration
