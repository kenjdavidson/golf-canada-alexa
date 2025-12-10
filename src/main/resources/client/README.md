# OpenAPI Specification Files

This directory contains the modular OpenAPI specification files for the Golf Canada API client.

## Structure

The API specification has been split into module components by feature/tag:

- **`authapi.yaml`** - Authentication endpoints and models
  - Endpoints: `/connect/token`
  - Models: AuthRequest, AuthToken, User, ScoreDefaults

- **`membersapi.yaml`** - Member-related endpoints and models
  - Endpoints: `/api/members/{userId}/getSnapshot`, `/api/members/{userId}/getFriends`, etc.
  - Models: MemberSnapshot, Friend, HandicapHistoryEntry, Course, Score

- **`scoresapi.yaml`** - Score-related endpoints and models
  - Endpoints: `/api/scores/getHandicapCalculation`, `/api/scores/getScoreData`
  - Models: HandicapCalculation, HandicapScore, ScoreData, ScoreDetail, Facility, CourseDetail, Tee, Hole, HoleScore

- **`gametrackerapi.yaml`** - Game tracker endpoints and models
  - Endpoints: `/api/gameTracker/getSummary`
  - Models: GameTrackerSummary, SummaryScore, HoleStatistic

- **`golfcanada-main.yaml`** - Main specification file that references all modules

## Build Process

The build process automatically bundles these separate module files into a single `golfcanada.yaml` file:

1. The Gradle task `bundleOpenApiSpec` uses `@redocly/cli` to bundle all module files
2. The bundled `golfcanada.yaml` file is generated at build time (not tracked in git)
3. The OpenAPI Generator plugin uses the bundled file to generate client code

## Editing the Specifications

When making changes:

1. Edit the appropriate module file (`authapi.yaml`, `membersapi.yaml`, etc.)
2. Run `./gradlew bundleOpenApiSpec` to regenerate the bundled file
3. Run `./gradlew openApiGenerate` to regenerate the client code
4. The full build process (`./gradlew build`) handles all of this automatically

## Benefits

- **Modularity**: Each API module is self-contained with its own endpoints and models
- **Maintainability**: Easier to find and update specific features
- **No Duplication**: Models are kept within their respective API modules
- **Single Source**: The bundled file ensures code generation works seamlessly
