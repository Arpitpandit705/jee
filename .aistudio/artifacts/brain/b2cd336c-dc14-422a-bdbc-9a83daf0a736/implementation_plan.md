# FocusStudy AI: Multi-Step Study Onboarding, 2-Hour Firestore Worker & Smart Batch Lecture Launcher

FocusStudy AI expands with a guided 3-step study onboarding wizard, automated 2-hour WorkManager background sync of student progress, lecture status, and DPP completion data to Firebase Firestore, and a Smart Batch Lecture Launcher with dynamic AI playback speed recommendations (1.25x - 1.75x) and direct lecture URL/API key integration.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The architectural plan below directly reflects all of your confirmed answers:
> 1. **3-Step Study Onboarding Wizard**:
>    - **Step 1: Daily Target & Lectures Quota**: Choose daily lecture volume (e.g., 2–6 lectures/day), subjects, and expected duration.
>    - **Step 2: DPP (Daily Practice Problem) Mapping**: Identify which lectures have associated DPPs, set problem counts, and establish minimum solving requirements.
>    - **Step 3: Study Reminders & Batch API Setup**: Configure daily morning check-in time, lock-in time, 2-hour sync alerts, and enter your batch API key/batch lecture URL with start bookmarks.
> 2. **WorkManager 2-Hour Background Sync**:
>    - Android `PeriodicWorkRequest` running every 2 hours with network constraints.
>    - Persists student study telemetry, verified lecture counts, DPP completion status, and active focus logs to Firebase Firestore.
> 3. **Smart Batch Lecture Launcher with Dynamic Speed (1.25x - 1.75x)**:
>    - Direct batch lecture launcher: input batch API key, course URL, or lecture ID to launch directly into your video lectures.
>    - AI Dynamic Speed Advisor: Analyzes topic complexity (e.g. Theoretical Derivations -> 1.25x; Standard Lecture -> 1.5x; Revision/Practice -> 1.75x) and calculates exact minutes saved per study session.

---

### 1. Overview & Core Concept

Students following intensive coaching batches (JEE, NEET, UPSC, University) require seamless access from their daily planner directly to their batch lectures without getting lost in extraneous apps, while ensuring their study records and DPP completion sync steadily to the cloud.

- **Primary Persona**: Batch students managing daily online video lectures, homework DPPs, and structured schedules.
- **Key Workflow**:
  1. Student configures daily lecture capacity, DPP tracking, and reminder times in a smooth 3-step wizard.
  2. Student inputs their Batch API key / portal URL and current lecture bookmark.
  3. Clicking "Start Lecture" calculates the recommended playback speed (e.g. 1.5x for Kinematics), locks down distracting apps, and launches directly into the video lecture.
  4. WorkManager quietly pushes lecture completion and DPP data to Firestore every 2 hours in the background.

---

### 2. User Experience & Visual Design

#### Flow 1: 3-Step Study Onboarding Wizard (`StudyOnboardingWizard`)
- **Step 1 — Lecture Quota & Capacity**:
  - Interactive stepper/chips selecting daily target lectures (e.g. 3 lectures/day).
  - Target daily study hours slider (e.g. 3 to 8 hours).
  - Subject tags (Physics, Chemistry, Maths, Biology, etc.).
- **Step 2 — DPP & Problem Mapping**:
  - Toggle: "Auto-pair DPP sheet with every conceptual lecture".
  - Target daily practice problem quota (e.g. 15 DPP questions/day).
  - Option to designate specific revision cycles.
- **Step 3 — Reminders & Batch Portal Integration**:
  - Daily Morning Check-In time picker (defaults to 7:00 AM).
  - Auto-Lockdown Start time picker (defaults to 8:00 AM).
  - Batch API Key & Lecture Portal URL input field with bookmark tracker: *"Where do you want to start?"* (e.g. Lecture 12, Chapter 4).

#### Flow 2: Smart Batch Lecture Launcher (`BatchLectureLauncherCard`)
- Displays current lecture card with:
  - Subject emblem, lecture title, and chapter name.
  - **Dynamic Speed Advisor**:
    - High-difficulty / Math-heavy topic -> **1.25x** (Max retention, prevents re-watching).
    - Moderate / Standard Concept -> **1.5x** (Optimal balance, saves 20 mins per 60m lecture).
    - Revision / Practice Problem -> **1.75x - 2.0x** (Rapid review, saves 30 mins).
  - Calculated "Time Saved" pill (e.g. "Saves 25 mins today!").
  - **One-Tap "Launch Lecture" Button**: Launches the batch URL/intent directly with the active token/API key, activates the focus blocker, and tracks progress.

#### Flow 3: 2-Hour WorkManager Sync (`StudentProgressSyncWorker`)
- Background worker executing every 2 hours:
  - Bundles today's completed lectures, solved DPPs, total focus minutes, and blocked distraction attempts.
  - Pushes payload to `/users/{userId}/daily_plans/` and `/users/{userId}/sync_logs/` in Firestore.
  - If a student falls behind their daily lecture quota, generates an encouraging reminder alert.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Batch API Key & Video Launching**:
  - *Chosen Approach*: Support flexible batch launching through a dedicated Batch Portal Configuration in settings/onboarding. The app stores the API Key and Batch Portal URL, automatically constructing the authenticated query or deep link. If the user provides an external batch URL or API endpoint, clicking "Start" launches directly via custom tab or platform intent while maintaining the study lock.
  - *Why*: Allows students from any educational batch (PhysicsWallah, Unacademy, Allen, Coursera, YouTube batch playlists, or custom college portals) to integrate their exact batch without proprietary vendor lock-in.
- **Decision 2: Dynamic Playback Speed Algorithm**:
  - *Chosen Approach*: Rule-based + AI recommendation based on subject type and lecture category (Theory, Derivation, DPP Solving, Revision) that displays clear guidance and timer adjustments.
  - *Why*: Watching lectures too fast (2x on new theory) causes poor retention, while 1x on revision wastes valuable study hours. Dynamic speed guidance optimizes academic efficiency.
- **Decision 3: WorkManager Background Sync Guarantee**:
  - *Chosen Approach*: `PeriodicWorkRequestBuilder` with 2-hour interval, `NetworkType.CONNECTED` constraint, and local fallback caching in SharedPreferences.
  - *Why*: Ensures student data persists to Firebase Firestore without requiring the app to be open or active in the foreground.

---

### 4. Technical Architecture & Component Hierarchy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        FocusStudy AI Enhancements                      │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                        UI Layer (Compose)                        │  │
│  │  - StudyOnboardingWizard (3 Steps: Lectures -> DPPs -> Reminders)│  │
│  │  - BatchLauncherCard (Direct link, API key, Speed Advisor)       │  │
│  │  - SpeedRecommendationBadge (1.25x / 1.5x / 1.75x calculation)   │  │
│  │  - TodayPlannerSection (Updated with batch launch button)        │  │
│  └──────────────────┬─────────────────────────────┬─────────────────┘  │
│                     │                             │                    │
│  ┌──────────────────▼──────────────────┐  ┌───────▼─────────────────┐  │
│  │       Background WorkManager        │  │       Data & Prefs      │  │
│  │  - StudentProgressSyncWorker        │  │  - BatchPortalConfig    │  │
│  │    (Every 2 Hours to Firestore)     │  │    (API Key, Start Lec) │  │
│  │  - SyncNotificationManager          │  │  - OnboardingPreferences│  │
│  └──────────────────┬──────────────────┘  └───────┬─────────────────┘  │
│                     │                             │                    │
│  ┌──────────────────▼─────────────────────────────▼─────────────────┐  │
│  │                  Firebase Firestore Cloud Database               │  │
│  │  - users/{userId}/daily_plans/{date} (lectures + DPP completion) │  │
│  │  - users/{userId}/sync_logs/{logId} (2-hour study telemetry)     │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

#### New Data Entities & Preferences
1. **BatchPortalConfig**:
   - `apiKey: String`, `batchName: String`, `portalUrl: String`, `currentLectureIndex: Int`, `defaultSpeed: Float`.
2. **OnboardingSetup**:
   - `dailyLectureQuota: Int`, `targetDailyMinutes: Int`, `dppsEnabled: Boolean`, `checkInTime7Am: String`, `blockerTime8Am: String`, `isCompleted: Boolean`.
3. **SpeedRecommendation**:
   - `recommendedSpeed: Float` (1.25f, 1.5f, 1.75f), `reason: String`, `minutesSaved: Int`.

---

### Step-by-Step Implementation Sequence

1. **Batch Portal & Onboarding Models**:
   - Define data models and preference keys for batch API key, portal URL, starting lecture bookmark, and 3-step wizard state.
2. **Multi-Step Onboarding Screen (`StudyOnboardingWizard`)**:
   - Create 3-step wizard with step indicators, interactive chips, DPP toggles, reminder time selectors, and Batch API Key configuration.
3. **Smart Batch Lecture Launcher with Dynamic Speed Advisor**:
   - Implement `SpeedAdvisor` computing 1.25x, 1.5x, or 1.75x based on topic category with time saved calculation.
   - Build `BatchLauncherCard` that launches the lecture directly via browser/deep-link and engages focus lock.
4. **Enhanced 2-Hour WorkManager Sync (`StudentProgressSyncWorker`)**:
   - Sync daily progress, lecture status, and DPP completion data to Firestore every 2 hours with network constraints.
5. **Integration with `MainActivity` & `TodayPlannerSection`**:
   - Connect onboarding flow, batch launch triggers, and verify with tests and compilation.
