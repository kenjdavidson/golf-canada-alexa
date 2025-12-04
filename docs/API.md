# API Documentation

This document describes the API endpoints exposed by the Golf Canada Alexa Skill and the Golf Canada API integration.

## Authentication Lambda API

The authentication Lambda function is exposed via AWS Lambda Function URL and provides OAuth-compatible endpoints for Alexa Account Linking.

### Base URL

```
https://<function-id>.lambda-url.<region>.on.aws/
```

### Endpoints

#### GET /login

Displays the login form for Golf Canada authentication.

**Query Parameters:**

| Parameter | Required | Description |
|-----------|----------|-------------|
| `client_id` | Yes | Must match configured CLIENT_ID |
| `redirect_uri` | Yes | Alexa callback URL |
| `response_type` | Yes | Must be "code" |
| `scope` | Yes | OAuth scopes |
| `state` | Yes | Random state value for CSRF protection |

**Response:**

- `200 OK` - HTML login form
- `400 Bad Request` - Invalid parameters

**Example:**
```
GET /login?client_id=xxx&redirect_uri=https://alexa.amazon.com/callback&response_type=code&scope=openid&state=abc123
```

---

#### POST /code

Processes the login form submission and authenticates with Golf Canada.

**Content-Type:** `application/x-www-form-urlencoded`

**Form Parameters:**

| Parameter | Required | Description |
|-----------|----------|-------------|
| `client_id` | Yes | Must match configured CLIENT_ID |
| `redirect_uri` | Yes | Alexa callback URL |
| `response_type` | Yes | Must be "code" |
| `scope` | Yes | OAuth scopes |
| `state` | Yes | State from /login request |
| `username` | Yes | Golf Canada username |
| `password` | Yes | Golf Canada password |

**Response:**

- `301 Redirect` - On success, redirects to `redirect_uri?code=xxx&state=xxx`
  - Sets `x-csrf-token` cookie for state validation
- `400 Bad Request` - Invalid credentials or parameters

**Error Response Body:**
```json
{
  "error": "invalid_request",
  "code": "109"
}
```

---

#### POST /authToken

Exchanges authorization code for access token or refreshes an existing token.

**Content-Type:** `application/x-www-form-urlencoded`

**Authentication:**
- HTTP Basic Auth with client_id:client_secret, OR
- Include `client_id` and `client_secret` in form body

**For Authorization Code Grant:**

| Parameter | Required | Description |
|-----------|----------|-------------|
| `grant_type` | Yes | Must be "authorization_code" |
| `code` | Yes | Code from /code redirect |
| `state` | Yes | Original state value |
| `client_id` | Yes* | Required if not using Basic Auth |
| `client_secret` | Yes* | Required if not using Basic Auth |

**For Token Refresh:**

| Parameter | Required | Description |
|-----------|----------|-------------|
| `grant_type` | Yes | Must be "refresh_token" |
| `refresh_token` | Yes | The refresh token |
| `client_id` | Yes* | Required if not using Basic Auth |
| `client_secret` | Yes* | Required if not using Basic Auth |

**Success Response (200 OK):**
```json
{
  "access_token": "eyJ...",
  "refresh_token": "abc123...",
  "id_token": "eyJ...",
  "expires_in": 3600,
  "token_type": "Bearer",
  "user": {
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "handicap": "12.3",
    "membershipLevel": "Full",
    "expirationDate": "2025-12-31T00:00:00"
  }
}
```

**Error Response (400 Bad Request):**
```json
{
  "error": "unauthorized_client",
  "code": "101"
}
```

---

### Error Codes

| Code | Error Response | Description |
|------|----------------|-------------|
| 1 | invalid_request | General client error |
| 100 | invalid_request | Invalid API call |
| 101 | unauthorized_client | Invalid client ID |
| 102 | unauthorized_client | Invalid client secret |
| 103 | invalid_request | Code not found |
| 104 | invalid_request | Invalid state |
| 105 | invalid_request | Invalid code |
| 106 | invalid_request | Invalid response type |
| 107 | invalid_request | Invalid redirect URI |
| 108 | invalid_scope | Invalid scope |
| 109 | invalid_request | Invalid username |
| 110 | invalid_request | Invalid password |
| 111 | invalid_grant | Invalid grant type |
| 112 | invalid_request | Invalid code body |
| 113 | invalid_request | Invalid refresh token |

---

## Golf Canada API

The Golf Canada API is used internally by the Lambda functions.

### Base URL

```
https://scg.golfcanada.ca
```

### Currently Implemented Endpoints

#### POST /connect/token

Authenticates with Golf Canada and retrieves tokens.

**Content-Type:** `application/x-www-form-urlencoded`

**For Password Grant:**

| Parameter | Required | Description |
|-----------|----------|-------------|
| `grant_type` | Yes | "password" |
| `scope` | Yes | "address email offline_access openid phone profile roles" |
| `username` | Yes | Golf Canada username/email |
| `password` | Yes | Golf Canada password |

**For Refresh Token Grant:**

| Parameter | Required | Description |
|-----------|----------|-------------|
| `grant_type` | Yes | "refresh_token" |
| `scope` | Yes | "address email offline_access openid phone profile roles" |
| `refresh_token` | Yes | Refresh token from previous auth |

**Response:**
```json
{
  "access_token": "eyJ...",
  "refresh_token": "abc123...",
  "id_token": "eyJ...",
  "expires_in": 3600,
  "token_type": "Bearer",
  "user": {
    "allowScorePosting": true,
    "authUserId": 12345,
    "clubManagementGroupId": 1,
    "email": "user@example.com",
    "expirationDate": "2025-12-31T00:00:00",
    "firstName": "John",
    "fullName": "John Doe",
    "golfCanadaCardId": "123456789",
    "handicap": "12.3",
    "id": 12345,
    "lastName": "Doe",
    "membershipLevel": "Full",
    "networkId": "abc123",
    "termsAndConditionsDate": "2024-01-01T00:00:00",
    "username": "user@example.com",
    "scoreDefaults": {
      "facilityName": "Home Club",
      "nationalAssociation": "Golf Canada",
      "postHoleByHole": false
    }
  }
}
```

---

## Alexa Skill Handler API

The Alexa Skill Handler receives requests from the Alexa service and returns voice responses. It uses the Alexa Skills SDK and is deployed as a Lambda function.

### Supported Intents

#### Standard Alexa Intents

| Intent | Handler | Description |
|--------|---------|-------------|
| `LaunchRequest` | `LaunchRequestHandler` | Handles skill launch |
| `AMAZON.HelpIntent` | `HelpIntentHandler` | Provides help information |
| `AMAZON.CancelIntent` | `CancelAndStopIntentHandler` | Cancels current action |
| `AMAZON.StopIntent` | `CancelAndStopIntentHandler` | Stops the skill |
| `AMAZON.NavigateHomeIntent` | `NavigateHomeIntentHandler` | Returns to home |
| `AMAZON.FallbackIntent` | `FallbackIntentHandler` | Handles unrecognized requests |
| `SessionEndedRequest` | `SessionEndedRequestHandler` | Cleans up on session end |

#### Custom Golf Canada Intents

| Intent | Handler | Slots | Description |
|--------|---------|-------|-------------|
| `GOLFCANADA.Handicap` | `HandicapIntentRequestHandler` | `FriendFullName`, `FriendFirstName` | Get handicap for self or friend |
| `GOLFCANADA.PlayerProfileMembership` | `PlayerProfileMembershipIntentHandler` | None | Get membership information |
| `GOLFCANADA.PlayerProfileHistory` | `PlayerProfileHistoryIntentHandler` | `numberOfRounds`, `year` | Get score history |
| `GOLFCANADA.FavoritePlayerHistory` | `FavoritePlayerHistoryIntentHandler` | `playerName`, `numberOfRounds` | Get friend's score history |
| `GOLFCANADA.AddScorecard` | `AddScorecardIntentHandler` | None | Add a new score (stub) |

### Request Interceptors

Request interceptors run before intent handlers and prepare data:

1. **AuthenticationRequestInterceptor** - Validates access token presence
2. **UserProfileInterceptor** - Loads user profile and caches in session (1 API call per session)
3. **HandicapLookupInterceptor** - Pre-loads handicap data with 10-minute TTL cache

### Exception Handlers

Custom exception handlers provide user-friendly error messages:

- **AccountLinkingExceptionHandler** - Prompts user to link account
- **NoUserDetailsExceptionHandler** - Handles missing user profile
- **GolfCanadaApiExceptionHandler** - Handles Golf Canada API errors

### Response Templating

All responses use FreeMarker templates with locale support:
- Templates located in `src/main/resources/kjd/golfcanada/alexa/responses/`
- English templates: `*Response.ftl`
- French templates: `*Response_fr.ftl`

### Caching Strategy

**Handicap Data Caching:**
```kotlin
// Cached for 10 minutes in session attributes
data class CachedHandicap(
    val data: HandicapSummaryData,
    val timestamp: Long,
    val ttlMillis: Long = 600_000  // 10 minutes
)
```

**User Profile Caching:**
- Stored in session attributes for entire session duration
- No TTL (refreshes on new session)

---

## OpenAPI Specification

The Golf Canada API client is generated from an OpenAPI specification located at:

```
src/main/resources/client/golfcanada.yaml
```

To regenerate the client after updating the spec:

```shell
./gradlew openApiGenerate
```

Generated code is placed in:
```
build/generated/openapi/src/main/kotlin/
```

---

## Data Models

### AuthToken

```kotlin
data class AuthToken(
    val access_token: String?,
    val refresh_token: String?,
    val id_token: String?,
    val expires_in: Int?,
    val token_type: String?,
    val user: User?
)
```

### User

```kotlin
data class User(
    val allowScorePosting: Boolean?,
    val authUserId: Long?,
    val clubManagementGroupId: Int?,
    val email: String?,
    val expirationDate: String?,  // ISO 8601 format
    val firstName: String?,
    val fullName: String?,
    val golfCanadaCardId: String?,
    val handicap: String?,
    val id: Long?,
    val lastName: String?,
    val membershipLevel: String?,
    val networkId: String?,
    val termsAndConditionsDate: String?,  // ISO 8601 format
    val username: String?,
    val scoreDefaults: ScoreDefaults?
)
```

### ScoreDefaults

```kotlin
data class ScoreDefaults(
    val facilityName: String?,
    val nationalAssociation: String?,
    val postHoleByHole: Boolean?
)
```

---

## Rate Limiting

Golf Canada API rate limits are not publicly documented. The Lambda function does not implement any rate limiting. If you experience `429 Too Many Requests` errors, implement exponential backoff.

## Authentication Notes

- Access tokens expire in 3600 seconds (1 hour)
- Refresh tokens should be stored securely by Alexa
- The authentication Lambda stores temporary codes in memory (single instance limitation)
