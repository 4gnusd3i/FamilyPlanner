# FamilyPlanner Android

This branch contains the staged native Android rewrite of FamilyPlanner.

The Android app is local-only and must not depend on the ASP.NET Core host, a
local webserver, browser UI, or network access. The existing web app remains in
the branch only as a temporary behavior reference until native parity is proven.

The planned branch name was `main/android`, but Git cannot store both `main`
and `main/android` because branch refs share the same namespace. The Android
development branch is therefore `android/main`.

## Toolchain

- JDK 17
- Android SDK platforms 35 and 36
- Gradle 9.4.1 wrapper
- Android Gradle Plugin 9.2.0
- Kotlin 2.3.21
- Jetpack Compose BOM 2026.04.01

In the Codex sandbox, Gradle must be launched with a real user home:

```powershell
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot'
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
$env:GRADLE_USER_HOME="$env:USERPROFILE\.gradle"
$env:JAVA_TOOL_OPTIONS="-Duser.home=$env:USERPROFILE"
.\gradlew.bat test
```

Or run the checked-in helper from the repository root:

```powershell
.\android\Run-AndroidChecks.ps1
```

## Current Scope

- Native project scaffold under `android/`.
- Room/DataStore/Hilt/Compose architecture baseline.
- `targetSdk` and `compileSdk` are `36`, and `minSdk` is `27`.
- The first milestone is feature parity with the v0.1.0 local planner before
  Android-specific UX polish.
