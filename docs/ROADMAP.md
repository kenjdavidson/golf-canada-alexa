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

### 🚧 Phase 2: Account Information (In Progress)

The first skill functionality to be implemented is account information retrieval.

#### 2.1 Player Profile - Membership Intent
**Intent:** `PlayerProfileMembershipIntent`

**Sample Utterances:**
- "what's my membership"
- "tell me about my membership"
- "when does my membership expire"
- "is my membership active"

**Required Development:**
- [ ] Implement membership API endpoint in OpenAPI spec
- [ ] Create `PlayerProfileMembershipHandler` intent handler
- [ ] Add response templates for membership status
- [ ] Handle expired membership scenarios
- [ ] Add unit tests

**Expected Response:**
> "Your Golf Canada membership is active at the [level] level and expires on [date]."

#### 2.2 Player Profile - Handicap Intent
**Intent:** `PlayerProfileHandicapIntent`

**Sample Utterances:**
- "what is my handicap"
- "what is my handicap index"
- "how is my handicap looking"

**Required Development:**
- [ ] Implement handicap API endpoint in OpenAPI spec
- [ ] Create `PlayerProfileHandicapHandler` intent handler
- [ ] Add response templates for handicap information
- [ ] Handle users without handicap index
- [ ] Add unit tests

**Expected Response:**
> "Your current handicap index is [index]. Your low index is [low] and your high index is [high]."

### 📋 Phase 3: Round History

#### 3.1 Player Round History Intent
**Intent:** `PlayerProfileHistoryIntent`

**Sample Utterances:**
- "tell me about my last round"
- "how did I play last"
- "tell me about my last [number] rounds"
- "how did I play in [year]"

**Required Development:**
- [ ] Implement rounds history API endpoint in OpenAPI spec
- [ ] Create `PlayerProfileHistoryHandler` intent handler
- [ ] Add response templates for round summaries
- [ ] Handle pagination for multiple rounds
- [ ] Add support for year filtering
- [ ] Add unit tests

**Expected Response (single round):**
> "Your last round was at [course] on [date]. You shot [score] with a differential of [diff]."

**Expected Response (multiple rounds):**
> "In your last [number] rounds, you've averaged [average] with scores ranging from [low] to [high]. Your best round was at [course]."

### 📋 Phase 4: Friends/Favorites

#### 4.1 Favorite Player Handicap Intent
**Intent:** `FavoritePlayerHandicapIntent` (to be created)

**Sample Utterances:**
- "what is [name]'s handicap"
- "how is [name] playing"

**Required Development:**
- [ ] Implement favorites API endpoint in OpenAPI spec
- [ ] Create slot type for friend names
- [ ] Create `FavoritePlayerHandicapHandler` intent handler
- [ ] Handle friend not found scenarios
- [ ] Add unit tests

#### 4.2 Favorite Player History Intent
**Intent:** `FavoritePlayerHistoryIntent`

**Sample Utterances:**
- "how has [name] been playing"
- "what was [name]'s last score"
- "what are [name]'s last [number] scores"

**Required Development:**
- [ ] Create `FavoritePlayerHistoryHandler` intent handler
- [ ] Handle friend privacy settings
- [ ] Add unit tests

### 📋 Phase 5: Score Posting

#### 5.1 Add Scorecard Intent
**Intent:** `AddScorecardIntent`

This is a complex conversational flow that will guide users through posting a new score.

**Sample Utterances:**
- "I want to add a score"
- "post a new round"
- "save my score"

**Conversation Flow:**
1. "Where did you play?" → Course selection
2. "Which tees did you play from?" → Tee selection
3. "What was your score?" → Total score or hole-by-hole
4. Confirmation and posting

**Required Development:**
- [ ] Implement score posting API endpoint in OpenAPI spec
- [ ] Implement course search API endpoint
- [ ] Create dialog model for multi-turn conversation
- [ ] Create `AddScorecardHandler` with dialog management
- [ ] Add support for both total score and hole-by-hole entry
- [ ] Handle edge cases (9-hole rounds, incomplete rounds)
- [ ] Add confirmation prompts
- [ ] Add unit tests

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
- [ ] Add support for French language responses
- [ ] Implement response caching
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
| `/api/player/profile` | 📋 Planned | Player membership info |
| `/api/player/handicap` | 📋 Planned | Handicap information |
| `/api/player/rounds` | 📋 Planned | Round history |
| `/api/player/favorites` | 📋 Planned | Friends/favorites list |
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
