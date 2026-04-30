# FamilyPlanner Android

This branch contains the staged native Android rewrite of FamilyPlanner.

The Android app is local-only: no ASP.NET Core host, no local webserver, no browser UI, and no network permission. The existing Windows/web app remains in this branch only as a temporary behavior reference until native parity is proven.

The planned branch name was `main/android`, but Git cannot store both `main` and `main/android` because branch refs share the same namespace. The Android development branch is therefore `android/main`. Future Android release branches use `release/android/v*`.

## Current Scope

- Kotlin, Jetpack Compose, Material 3, Hilt, Room, DataStore, Coroutines/Flow.
- App ID and namespace: `io.github.by4gnusd3i.familyplanner`.
- `compileSdk` and `targetSdk`: `36`.
- `minSdk`: `27`.
- Version name: `0.1.0-android`.
- Android cloud backup and device-transfer extraction are disabled for the first Android baseline.
- No `INTERNET` permission is declared.

Implemented parity slices:

- Setup-only local onboarding.
- Phone bottom navigation: `Oversikt`, `Uke`, `Måltider`, `Lister`, `Budsjett`.
- Tablet dashboard with quick actions, family row, budget, lists, week calendar, meal grid, and upcoming.
- Room-backed events, meals, budget months, expenses, notes, shopping items, family members, and household setup.
- Generated birthday events with leap-day handling.
- Upcoming filtering with one recurring occurrence per series in the upcoming window.
- Summary-first entry interaction for events, meals, expenses, notes, and shopping items.
- Language override using packaged `nb`/`nb-NO` and `en`/`en-US` resources.
- Currency setting via DataStore.
- Android Photo Picker avatar selection copied into app-private storage.
- Local data reset for Room data and stored avatars.
- Touch-friendly event creation by tapping a week day or long-pressing a tablet family chip to prefill responsible member.

Known remaining baseline work:

- Full native drag-and-drop gesture from avatar to calendar day.
- More Compose UI/instrumented coverage for screen flows.
- Android release signing, APK packaging, and release documentation.
- Manual tablet/phone smoke pass on real emulator/device.

## Toolchain

- JDK 17
- Android SDK platforms 35 and 36
- Gradle 9.4.1 wrapper
- Android Gradle Plugin 9.2.0
- Kotlin 2.3.21
- Jetpack Compose BOM 2026.04.01

## Validation

Run from the repository root:

```powershell
.\android\Run-AndroidChecks.ps1
```

Default tasks:

```powershell
.\gradlew.bat test lint assembleDebug
```

The helper sets `JAVA_HOME`, `ANDROID_HOME`, `ANDROID_SDK_ROOT`, `GRADLE_USER_HOME`, and `JAVA_TOOL_OPTIONS` for this workstation.

To run emulator/instrumented tests later:

```powershell
.\android\Run-AndroidChecks.ps1 -Tasks connectedDebugAndroidTest
```

Only run connected tests when an emulator or device is available. Current connected baseline is 4 tests: setup screen smoke, no-Internet manifest assertion, Room reset cleanup, and owner-deletion detach behavior.

## Data And Privacy

- Structured planner data is stored in the app-private Room database.
- Settings are stored in app-private DataStore preferences.
- Avatars selected through Android Photo Picker are copied into app-private `files/avatars`.
- No cloud sync, accounts, network access, or Android backup is enabled in this baseline.
- Reset local data from app settings to clear Room data and stored avatars.

## Release Policy

- `android/main` is the Android development branch.
- `release/android/v*` branches are for Android release preparation.
- First Android release tag format: `v0.1.0-android`.
- Do not commit keystores, passwords, generated APKs/AABs, emulator artifacts, build outputs, local DBs, or personal paths.
