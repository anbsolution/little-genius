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
- Worksheet Center supports untimed practice and timed tests with score summaries and missed-answer explanations
- Game/puzzle answers and completed practical guides are saved on the device
- Favorite lessons are saved locally in Room and can be reopened from More → Favorite Lessons
- Lesson language preference is saved locally with DataStore (English/Hindi)
- Parent-PIN-protected child profiles keep favorites and progress separate; the original default profile continues using its existing progress preferences
- Homework is saved per child in Room with deadlines, completion tracking and optional on-device reminders
- Offline badge milestones and a locally generated PDF learning certificate are based on each child's saved progress
- Parent dashboard can create and restore a local JSON backup of profiles, favorites, homework, learning progress, class selection and language preference
- Study Library with official NCERT/ePathshala/CBSE source links, direct PDF downloads, saved-downloads management, and import of PDFs already downloaded to the device for offline opening
- Android CI workflow for debug APK and unit tests

## Planned modules
- Full curriculum-aligned lessons and chapter content
- Speech recognition, pronunciation practice and voice questions
- Expanded CBSE question banks, revision worksheets and assessment reports
- Room-backed structured lesson/progress records (progress is currently stored in per-profile preferences)
- Revision planner and timetable customization
- Parent voice announcements
- Secure, parent-controlled AI learning assistant

## Build
Requires JDK 17, Android SDK 35 and Gradle 8.9-compatible tooling.

```bash
gradle :app:assembleDebug
gradle :app:testDebugUnitTest
```

## Important
This is an early starter implementation, not a completed educational product. Bundled lessons, games, puzzles, practical guides, worksheet questions and progress are available offline; they do not cover the full syllabus. Worksheet answers and total progress are saved locally; timed session scores are shown on completion, while individual answer outcomes feed the existing progress counters. Favorites and homework use Room; homework reminders use device-local WorkManager and Android notification permission. Badge milestones are derived from each child's saved progress; a PDF certificate is generated locally for completing five lessons and is clearly marked as a personal milestone rather than an official school or CBSE certificate. Lesson language/profile preference uses DataStore. Existing default-profile progress continues to use its original local preferences so the profile upgrade does not discard user data; additional child profiles receive separate local progress stores. Parent-gated backup creates a portable JSON file through Android's document picker. Restoring replaces local profiles, favorites, homework and supported learning progress; the current parent PIN is retained. Downloaded PDFs and imported document links are not part of that backup and must be selected again on another device. The Study Library links to official NCERT/ePathshala and CBSE sources, downloads selected official PDFs to app-specific device storage, and can add a PDF already downloaded on the device using Android's document picker. Imported PDFs stay in their original folder; the app stores a permission-backed link rather than copying or republishing the file. Source files remain subject to their publisher's copyright and terms. Full Classes 1–12 curriculum, AI, speech recognition, cloud sync and parent announcements are not implemented yet. Content must be authored or sourced with appropriate permissions.
