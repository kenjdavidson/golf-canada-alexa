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
- `GolfCanadaAlexaSkill.kt` - Main skill entry point using Alexa Skills SDK
- `handler/` - Intent handlers for all supported voice commands
- `interceptor/` - Request/response interceptors for auth, user profile, and handicap caching
- `data/` - Session data models
- `exception/` - Custom exceptions for error handling
- `model/` - Response data models
- `util/` - Utility classes (e.g., friend name matching)

**Status:** ✅ Core functionality implemented and tested

**Key Features:**
- **Skill Builder Configuration** - Uses varargs methods (`addRequestHandlers`, `addRequestInterceptors`, `addExceptionHandlers`) for clean, maintainable configuration
- **Request Interceptors** - Pre-process requests to handle authentication, load user profiles, and cache handicap data with TTL
- **Exception Handlers** - Gracefully handle account linking issues, API errors, and missing user details
- **Template-based Responses** - FreeMarker templates for all responses with English and French localization

### 3. Golf Canada API Client

Generated client code for interacting with Golf Canada's API.

**Location:** `src/main/kotlin/kjd/golfcanada/client/` and `build/generated/openapi/`

**OpenAPI Spec:** `src/main/resources/client/golfcanada.yaml`

**Key Components:**
- **ApiClientProvider** - Manages API client instances with shared OkHttp client for connection pooling
- **ApiClientWrapper** - Wraps API clients with authentication token management
- **Token Management** - Automatically extracts and manages Golf Canada access tokens from Alexa account linking
- **API Endpoints** - Generated from OpenAPI spec including:
  - Authentication (token exchange)
  - Members API (profile, handicap, scores)
  - User management

**Status:** ✅ Implemented with connection pooling and token management

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
│                                        │    │                                        │
│  ┌──────────────────────────────────┐  │    │  ┌──────────────────────────────────┐  │
│  │    AuthenticationHandler         │  │    │  │    GolfCanadaAlexaSkill          │  │
│  │                                  │  │    │  │                                  │  │
│  │  /login  → Show login form       │  │    │  │  Request Handlers:               │  │
│  │  /code   → Authenticate user     │  │    │  │  - LaunchRequestHandler          │  │
│  │  /token  → Exchange/refresh      │  │    │  │  - HandicapIntentRequestHandler  │  │
│  └──────────────────────────────────┘  │    │  │  - PlayerProfileMembershipHandler│  │
│                                        │    │  │  - PlayerProfileHistoryHandler   │  │
│  ┌──────────────────────────────────┐  │    │  │  - FavoritePlayerHistoryHandler  │  │
│  │    TokenRepository (in-memory)   │  │    │  │  - (+ standard Alexa handlers)   │  │
│  │    Stores temp auth codes        │  │    │  └──────────────────────────────────┘  │
│  └──────────────────────────────────┘  │    │                                        │
│                                        │    │  ┌──────────────────────────────────┐  │
│                                        │    │  │  Request Interceptors:           │  │
│                                        │    │  │  - AuthenticationInterceptor     │  │
│                                        │    │  │  - UserProfileInterceptor        │  │
│                                        │    │  │  - HandicapLookupInterceptor     │  │
│                                        │    │  │    (with 10-min TTL cache)       │  │
│                                        │    │  └──────────────────────────────────┘  │
│                                        │    │                                        │
│                                        │    │  ┌──────────────────────────────────┐  │
│                                        │    │  │  ApiClientProvider               │  │
│                                        │    │  │  - Shared OkHttp client          │  │
│                                        │    │  │  - Token management per request  │  │
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
  - CLIENT_ID: !Ref ClientId (from SAM Parameters)
  - CLIENT_SECRET: !Ref ClientSecret (from SAM Parameters)
  - JAVA_TOOL_OPTIONS: SSL trust store configuration

Layers:
  - GolfCanadaAuthenticationCertLayer (SSL certificates)

Function URL:
  - AuthType: NONE (public endpoint for OAuth flow)
  - InvokeMode: BUFFERED
```

### Skill Handler Function

```yaml
Runtime: java21
Memory: 512 MB
Timeout: 20 seconds
Handler: kjd.golfcanada.alexa.GolfCanadaAlexaSkill::handleRequest

Environment Variables:
  - SKILL_ID: !Ref SkillId (from SAM Parameters)

Configuration:
  - Uses Alexa Skills SDK with custom skill builder
  - Varargs-based configuration for handlers and interceptors
  - Shared ApiClientProvider instance for HTTP client reuse
  - FreeMarker templates for response generation
  - Supports English and French localization

Invocation:
  - Invoked by Alexa Skills Kit via Lambda ARN
  - No public endpoint (secured by Alexa service)
```

**Status:** ✅ Implemented and added to template.yaml

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

- `CLIENT_ID`, `CLIENT_SECRET`, and `SKILL_ID` are now managed as SAM template Parameters
- Parameters can be provided during deployment via `sam deploy --guided` or `--parameter-overrides`
- In CI/CD pipelines, these can be sourced from GitHub Secrets and passed as parameters
- Parameter values with `NoEcho: true` are not logged or displayed in CloudFormation console
- Never commit these values to source control

## Data Flow

### Authentication Request Processing

1. **Request arrives** at Lambda Function URL
2. **Route matching** in `AuthenticationHandler.handleRequest()`
3. **Validation** of parameters using extension functions
4. **Business logic** execution
5. **Response building** with appropriate status and headers

### Skill Request Processing

1. **Alexa request arrives** at Skill Handler Lambda
2. **Request Interceptors** run in order:
   - **AuthenticationRequestInterceptor** - Validates access token is present
   - **UserProfileInterceptor** - Loads user profile and stores in session (with caching)
   - **HandicapLookupInterceptor** - Pre-loads user's handicap data with 10-minute TTL cache
3. **Intent Handler** processes the request using cached data from request attributes
4. **Response Generation** using FreeMarker templates with locale support
5. **Response returned** to Alexa

### Caching Strategy

**User Profile Caching:**
- Stored in Session Attributes for the duration of the session
- Loaded once per session to minimize API calls
- Contains: user ID, name, email, membership info

**Handicap Data Caching:**
- Stored in Session Attributes with 10-minute TTL
- Only cached for the current user (not for friend lookups)
- Reduces API calls for repeated handicap queries
- Automatically refreshed when TTL expires

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
