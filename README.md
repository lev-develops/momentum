# Momentum

An offline-first Android habit tracker. Native Kotlin + Jetpack Compose, no backend, no
`INTERNET` permission anywhere in the manifest.

## Stack

- Kotlin, Jetpack Compose, Material 3 (fixed neutral palette, no dynamic color)
- Room for persistence, DataStore for widget bindings and small app prefs
- Glance for the home-screen widget
- WorkManager (periodic self-healing backstop) + AlarmManager (exact reminders and the
  local-midnight widget rollover)
- Single module, MVVM: `data/`, `domain/`, `ui/`, `widget/`, `reminder/`

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

## Building

```
./gradlew assembleDebug
./gradlew test
```

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

Everything outside `domain/` (Compose UI, Room, Glance, WorkManager/AlarmManager) was written
and carefully reviewed by hand but not compiler-verified. Run `./gradlew assembleDebug` and
`./gradlew test` in an environment with normal access to Google's Maven repository (Android
Studio, most CI) before treating it as done.
