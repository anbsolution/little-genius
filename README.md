# Little Genius

Android educational learning app starter project for CBSE-aligned learning from Pre-Nursery through Class 5.

## Current implementation
- Kotlin + Jetpack Compose foundation
- Class selection for all 9 levels: Pre-Nursery, Nursery, LKG, UKG, Classes 1–5
- Student dashboard with subject cards
- Bottom navigation shell: Home, Learn, Practice, Progress, Profile
- Polished class selection and home dashboard with direct Practice, Library and Planner shortcuts
- Shared visual system with gradient learning cards, clearer page hierarchy and daily progress indicators
- On-device starter lessons and progress work offline
- Class-level learning games, puzzles, subject quizzes and practical guides with safety notes
- Game/puzzle answers and completed practical guides are saved on the device
- Study Library with official NCERT/ePathshala/CBSE source links, direct PDF downloads, saved-downloads management, and import of PDFs already downloaded to the device for offline opening
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
This is an early starter implementation, not a completed educational product. Bundled lessons, games, puzzles, practical guides and progress are available offline; they do not cover the full syllabus. The Study Library links to official NCERT/ePathshala and CBSE sources, downloads selected official PDFs to app-specific device storage, and can add a PDF already downloaded on the device using Android's document picker. Imported PDFs stay in their original folder; the app stores a permission-backed link rather than copying or republishing the file. Source files remain subject to their publisher's copyright and terms. AI, speech recognition, cloud sync, parent authentication and full academic content are not implemented. Content must be authored or sourced with appropriate permissions.
