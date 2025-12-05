# Development Roadmap

This document outlines the planned features and development milestones for the Golf Canada Alexa Skill.

## Current Status

### ✅ Completed

#### Phase 1: Authentication Infrastructure
- [x] OAuth wrapper for Golf Canada authentication
- [x] Lambda function with Function URL
- [x] Login page with Golf Canada branding
- [x] Token exchange flow
- [x] Token refresh flow
- [x] SSL certificate handling via Lambda layer
- [x] AWS SAM deployment configuration
- [x] Terms of Service and Privacy Policy

## Planned Development

### ✅ Phase 2: Account Information (Completed)

Account information retrieval functionality has been implemented and tested.

#### 2.1 Player Profile - Membership Intent
**Intent:** `GOLFCANADA.PlayerProfileMembership`

**Sample Utterances:**
- "what's my membership"
- "tell me about my membership"
- "when does my membership expire"
- "is my membership active"

**Implementation Status:**
- [x] Implement membership API endpoint in OpenAPI spec
- [x] Create `PlayerProfileMembershipIntentHandler` intent handler
- [x] Add response templates for membership status (English and French)
- [x] Handle expired membership scenarios
- [x] Add unit tests

**Actual Response:**
> "Your Golf Canada membership is active at the [level] level and expires on [date]."

#### 2.2 Player Profile - Handicap Intent
**Intent:** `GOLFCANADA.Handicap`

**Sample Utterances:**
- "what is my handicap"
- "what is my handicap index"
- "how is my handicap looking"
- "what is [friend name]'s handicap"

**Implementation Status:**
- [x] Implement handicap API endpoint in OpenAPI spec
- [x] Create `HandicapIntentRequestHandler` intent handler
- [x] Add response templates for handicap information (English and French)
- [x] Handle users without handicap index
- [x] Add unit tests
- [x] **Bonus:** Implement friend handicap lookup with name matching
- [x] **Bonus:** Add 10-minute TTL caching for user's own handicap via `HandicapLookupInterceptor`

**Actual Response (Own Handicap):**
> "Your current handicap index is [index]. Your low index is [low] and your high index is [high]."

**Actual Response (Friend's Handicap):**
> "[Friend name]'s current handicap index is [index]."

### 🚧 Phase 3: Round History (In Progress)

#### 3.1 Player Round History Intent
**Intent:** `GOLFCANADA.PlayerProfileHistory`

**Sample Utterances:**
- "tell me about my last round"
- "how did I play last"
- "tell me about my last [number] rounds"
- "how did I play in [year]"

**Implementation Status:**
- [x] Create `PlayerProfileHistoryIntentHandler` intent handler (stub)
- [x] Add response templates for round summaries (English and French)
- [x] Add unit tests
- [ ] **TODO:** Implement rounds history API endpoint in OpenAPI spec
- [ ] **TODO:** Implement full handler logic with API integration
- [ ] **TODO:** Add support for pagination and multiple rounds
- [ ] **TODO:** Add support for year filtering

**Expected Response (single round):**
> "Your last round was at [course] on [date]. You shot [score] with a differential of [diff]."

**Expected Response (multiple rounds):**
> "In your last [number] rounds, you've averaged [average] with scores ranging from [low] to [high]. Your best round was at [course]."

**Current Status:** Handler exists but returns placeholder response. API integration not yet implemented.

### 🚧 Phase 4: Friends/Favorites (Partially Complete)

#### 4.1 Favorite Player Handicap ✅ COMPLETE
**Intent:** `GOLFCANADA.Handicap` (with friend slots)

**Sample Utterances:**
- "what is [name]'s handicap"
- "tell me [friend name]'s handicap"

**Implementation Status:**
- [x] Implement friends/favorites API endpoint in OpenAPI spec
- [x] Create slots for friend names (FriendFullName, FriendFirstName)
- [x] Implement friend handicap lookup in `HandicapIntentRequestHandler`
- [x] Handle friend not found scenarios with appropriate error messages
- [x] Implement intelligent name matching via `FriendNameMatcher` utility
- [x] Handle multiple matching friends gracefully
- [x] Add unit tests

**Actual Response:**
> "[Friend name]'s current handicap index is [index]."

#### 4.2 Favorite Player History Intent
**Intent:** `GOLFCANADA.FavoritePlayerHistory`

**Sample Utterances:**
- "what was [playerName]'s last score"
- "how did [playerName] shoot last"
- "what are [playerName]'s last [numberOfRounds] scores"

**Implementation Status:**
- [x] Create `FavoritePlayerHistoryIntentHandler` intent handler (stub)
- [x] Add response templates (English and French)
- [x] Add unit tests
- [ ] **TODO:** Implement API endpoint for friend round history
- [ ] **TODO:** Implement full logic for friend history lookup with API integration
- [ ] **TODO:** Handle friend privacy settings
- [ ] **TODO:** Add friend name matching similar to handicap intent

**Current Status:** Handler exists but returns placeholder response. API integration and friend matching not yet implemented.

### 📋 Phase 5: Score Posting (Planned)

#### 5.1 Add Scorecard Intent
**Intent:** `GOLFCANADA.AddScorecard`

This is a complex conversational flow that will guide users through posting a new score.

**Sample Utterances:**
- "I want to add a score"
- "post a new round"
- "save my score"

**Planned Conversation Flow:**
1. "Where did you play?" → Course selection
2. "Which tees did you play from?" → Tee selection
3. "What was your score?" → Total score or hole-by-hole
4. Confirmation and posting

**Implementation Status:**
- [x] Create `AddScorecardIntentHandler` intent handler (stub)
- [x] Add response templates (English and French)
- [x] Add unit tests (basic)
- [ ] **TODO:** Implement score posting API endpoint in OpenAPI spec
- [ ] **TODO:** Implement course search API endpoint
- [ ] **TODO:** Create dialog model for multi-turn conversation
- [ ] **TODO:** Implement full dialog management logic in handler
- [ ] **TODO:** Add support for both total score and hole-by-hole entry
- [ ] **TODO:** Handle edge cases (9-hole rounds, incomplete rounds)
- [ ] **TODO:** Add confirmation prompts

**Current Status:** Handler exists with basic structure but returns placeholder response. Multi-turn dialog and API integration not yet implemented.

### 📋 Phase 6: Advanced Features

#### 6.1 Course Information
- Search for courses
- Get course rating and slope
- Get course contact information

#### 6.2 Statistics and Trends
- Handicap trend over time
- Scoring trends
- Best/worst courses

#### 6.3 Notifications (if supported)
- Handicap index updates
- Membership expiration reminders

## Technical Debt & Improvements

### High Priority
- [ ] Implement persistent token storage (DynamoDB)
- [ ] Add request validation and sanitization
- [ ] Improve error messages for users
- [ ] Add CloudWatch metrics and alarms

### Medium Priority
- [x] Add support for French language responses (English and French templates implemented)
- [x] Implement response caching (10-minute TTL for handicap data)
- [ ] Add APL (Alexa Presentation Language) support for screen devices
- [ ] Create CI/CD pipeline

### Low Priority
- [ ] Add skill card responses
- [ ] Implement progressive responses for long operations
- [ ] Add analytics tracking

## API Endpoints to Implement

Based on the Golf Canada API exploration:

| Endpoint | Status | Description |
|----------|--------|-------------|
| `/connect/token` | ✅ Complete | Authentication |
| `/api/player/profile` | ✅ Complete | Player membership info |
| `/api/player/handicap` | ✅ Complete | Handicap information |
| `/api/player/rounds` | ✅ Complete | Round history |
| `/api/player/favorites` | ✅ Complete | Friends/favorites list |
| `/api/member/{id}/handicap` | ✅ Complete | Friend handicap lookup |
| `/api/courses/search` | 📋 Planned | Course search |
| `/api/scores/post` | 📋 Planned | Post new score |

## Testing Strategy

### Unit Tests
- Intent handler logic
- API client calls (mocked)
- Response formatting

### Integration Tests
- Full request/response flow with local SAM
- API integration with Golf Canada (sandboxed)

### Manual Testing
- Alexa Developer Console
- Echo device testing
- Account linking flow

## Release Milestones

| Milestone | Target | Features |
|-----------|--------|----------|
| v1.0 | Completed | Authentication only |
| v1.1 | TBD | Membership + Handicap information |
| v1.2 | TBD | Round history |
| v1.3 | TBD | Friends/favorites |
| v2.0 | TBD | Score posting |

## Contributing

See [CONTRIBUTION.md](../CONTRIBUTION.md) for information on how to contribute to this project.

When working on features:
1. Check this roadmap for planned work
2. Open an issue to discuss the feature
3. Reference the issue in your PR
4. Update this roadmap when features are completed
