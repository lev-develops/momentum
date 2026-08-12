# Momentum

An offline-first Android habit tracker. Native Kotlin + Jetpack Compose, no backend required —
every screen reads and writes the local Room database directly and the app works fully with no
network at all. Cloud backup/multi-device sync (Firebase Auth + Firestore) is available as an
opt-in feature; see [Cloud sync](#cloud-sync-optional) below.

## Stack

- Kotlin, Jetpack Compose, Material 3 (fixed neutral palette, no dynamic color)
- Room for persistence, DataStore for widget bindings and small app prefs
- Glance for the home-screen widget
- WorkManager (periodic self-healing backstop, periodic cloud sync) + AlarmManager (exact
  reminders and the local-midnight widget rollover)
- Firebase Auth + Firestore for optional cloud backup/sync (`sync/`)
- Single module, MVVM: `data/`, `domain/`, `ui/`, `widget/`, `reminder/`, `sync/`

`minSdk 26`, `targetSdk 36`, `compileSdk 36` (dropped from the originally-specified 37, which
isn't an installable SDK platform yet — bump back to 37 once it's available). Gradle Kotlin DSL
with a version catalog
(`gradle/libs.versions.toml`).

## Structure

- `domain/` — pure Kotlin, no Android dependencies: habit/completion models, streak math
  (`StreakCalculator`), the contribution-grid level calculation (`ContributionGridBuilder`),
  and `InsightsCalculator`. Fully unit-testable on the plain JVM.
- `data/` — Room entities/DAOs/database, the repository, DataStore wrappers, and JSON
  export/import (via the system file picker, `kotlinx.serialization`).
- `ui/` — Compose screens (Today, Habit Detail, Add/Edit, Insights) and the shared
  `ui/theme/Theme.kt`, the single source of truth for every color in the app, including the
  six habit accent ramps and their dark-mode shift.
- `widget/` — the Glance widget, its configuration activity, and a bitmap renderer that reads
  the same `Theme.kt` palette so the widget's grid always matches the Detail screen's.
- `reminder/` — AlarmManager scheduling for per-habit reminders and the midnight rollover,
  plus a WorkManager worker that self-heals if an exact alarm gets dropped.
- `sync/` — optional Firebase Auth + Firestore cloud backup/sync (`AuthManager`,
  `CloudSyncRepository`, `SyncWorker`). Local Room stays the source of truth; sync only pushes
  and merges on a manual "Sync now" or the hourly background worker, never on the hot path of
  ticking a habit off.

## Building

```
./gradlew assembleDebug
./gradlew test
```

## Cloud sync (optional)

Cloud sync is off by default and the app never requires it. To turn it on:

1. Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com).
2. Add an Android app to it with package name `com.momentum.app`, then download its
   `google-services.json` and drop it in `app/google-services.json`.
3. In the Firebase console, enable **Authentication → Email/Password** and create a
   **Firestore** database (any region, start in production mode — the app only ever reads/writes
   under `users/{signed-in uid}/...`, so a rule like `allow read, write: if request.auth.uid ==
   uid;` on that path is sufficient).
4. Rebuild. `app/build.gradle.kts` only applies the `google-services` Gradle plugin when
   `google-services.json` is present, so the app builds and runs exactly as before if you skip
   all of this.

Without a `google-services.json`, the in-app "Cloud sync" screen (Today → ⋮ → Cloud sync) just
explains that sync isn't configured; nothing else in the app is affected.

**Known limitations of this v1 scaffold:**

- Habit edits merge last-write-wins by timestamp, but completions merge as a union (a date
  marked done on either device stays done after syncing). Unchecking a habit on one device while
  another device syncs from an older, still-checked state can resurrect that completion. A
  production version would want tombstoned deletes to close that gap.
- Habits and completions are keyed by Room's local autoincrement id, which is only unique
  *per device*. If two devices each create habits before ever syncing, they can land on the same
  id and get merged into one habit on first sync instead of staying separate. Safe for a single
  identity synced across devices from the start; not yet safe for merging two devices that
  already have independent history. Fixing this properly means switching the sync/merge key to a
  UUID assigned at habit creation instead of the local Room id.

## A note on this build

This project was written in a sandboxed environment whose network egress policy blocks
`dl.google.com` — the host both the Android SDK platform/build-tools and Gradle's `google()`
Maven repository (AndroidX, Compose, Room, Glance, WorkManager) are served from. That made it
impossible to run an actual Android Gradle build or instrumented test in that environment.

What *was* verified there: the entire `domain/` package (streak math, the contribution grid,
Insights) has zero Android dependencies, so it was copied into a throwaway plain-Kotlin/JVM
Gradle project (resolving only from Maven Central) and its unit tests were compiled and run for
real — all 23 pass, including the today-incomplete, gap, single-day, local-midnight-rollover,
and DST-transition cases. That run caught and fixed a real bug in the weekly-target streak
calculation.

Everything outside `domain/` (Compose UI, Room, Glance, WorkManager/AlarmManager, and — as of the
cloud sync work — `sync/` and the Firebase dependencies) was written and carefully reviewed by
hand but not compiler-verified. Run `./gradlew assembleDebug` and `./gradlew test` in an
environment with normal access to Google's Maven repository (Android Studio, most CI) before
treating it as done. The `com.google.gms.google-services` plugin is only ever applied when
`app/google-services.json` exists, so it won't block a build in an environment (like this one)
that never adds that file.
