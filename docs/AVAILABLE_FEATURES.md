# Available Features

This document lists all currently available and planned features for the Golf Canada Alexa Skill, including voice commands and their expected behavior.

## Currently Available Features

### Account Information

#### ✅ Handicap Lookup
**Voice Commands:**
- "What is my handicap?"
- "What is my handicap index?"
- "How is my handicap looking?"
- "What is [friend name]'s handicap?"
- "Tell me [friend name]'s handicap"

**What it does:**
- Looks up your current handicap index
- Shows your low and high index values
- Can also look up handicap for friends on your Golf Canada friends list
- Uses intelligent name matching (supports both full names and first names)
- Results are cached for 10 minutes to improve performance

**Response:**
> "Your current handicap index is [index]. Your low index is [low] and your high index is [high]."

or for a friend:
> "[Friend name]'s current handicap index is [index]."

#### ✅ Membership Status
**Voice Commands:**
- "What's my membership?"
- "Tell me about my membership"
- "When does my membership expire?"
- "Is my membership active?"

**What it does:**
- Checks your current Golf Canada membership level
- Shows when your membership expires
- Confirms if your membership is currently active

**Response:**
> "Your Golf Canada membership is active at the [level] level and expires on [date]."

## Planned Features

The following features are planned for future development:

### Score History

#### 📋 Your Recent Rounds
**Voice Commands:**
- "Tell me about my last round"
- "How did I play last?"
- "Tell me about my last [number] rounds"
- "How did I play in [year]?"

**What it will do:**
- Retrieve information about your most recent rounds
- Show scores, differentials, and course information
- Support looking up multiple rounds
- Filter by specific years

**Expected Response:**
> "Your last round was at [course] on [date]. You shot [score] with a differential of [diff]."

#### 📋 Friend's Recent Rounds
**Voice Commands:**
- "What was [friend name]'s last score?"
- "How did [friend name] shoot last?"
- "What are [friend name]'s last [number] scores?"

**What it will do:**
- Look up your friend's recent round history
- Show their scores and performance

**Expected Response:**
> "[Friend name]'s last round was at [course] on [date]. They shot [score]."

### Score Posting

#### 📋 Add a New Scorecard
**Voice Commands:**
- "I want to add a score"
- "Post a new round"
- "Save my score"

**What it will do:**
- Guide you through posting a new score via voice
- Ask for course, tees, and score information
- Support both total score and hole-by-hole entry
- Confirm details before posting

**Expected Flow:**
1. "Where did you play?" → Course selection
2. "Which tees did you play from?" → Tee selection
3. "What was your score?" → Score entry
4. Confirmation and posting

### Advanced Features (Future)

The following features are being considered for future releases:

- 📋 **Course Information** - Search for courses, get ratings and contact info
- 📋 **Statistics and Trends** - Handicap trends, scoring patterns, best/worst courses
- 📋 **Notifications** - Handicap updates, membership expiration reminders (if supported by Alexa)

## Feature Status Legend

- ✅ **Available** - Feature is implemented and working
- 🚧 **In Progress** - Feature is partially implemented
- 📋 **Planned** - Feature is planned but not yet started

## Contributing

Have ideas for new features? Want to help implement planned features? See [CONTRIBUTION.md](../CONTRIBUTION.md) for information on how to contribute to this project.
