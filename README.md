# Little Genius

Android educational learning app starter project for CBSE-aligned learning from Pre-Nursery through Class 5.

## Current implementation
- Kotlin + Jetpack Compose foundation
- Class selection for all 9 levels: Pre-Nursery, Nursery, LKG, UKG, Classes 1–5
- Student dashboard with subject cards
- Bottom navigation shell: Home, Learn, Practice, Progress, Profile
- Android CI workflow for debug APK and unit tests

## Planned modules
- Full curriculum-aligned lessons and chapter content
- Hindi/English voice learning
- Worksheets, quizzes and assessment engine
- Room-based offline learning and progress persistence
- Parent dashboard and timetable
- Secure, parent-controlled AI learning assistant

## Build
Requires JDK 17, Android SDK 35 and Gradle 8.9-compatible tooling.

```bash
gradle :app:assembleDebug
gradle :app:testDebugUnitTest
```

## Important
This is the Phase 1 starter implementation, not a completed educational product. AI, speech recognition, offline content packs, cloud sync, parent authentication and full academic content are planned modules and are not yet implemented. No real CBSE textbook content is bundled; content must be authored or sourced with appropriate permissions.
