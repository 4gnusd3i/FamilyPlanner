# FamilyPlanner

FamilyPlanner is a local family week planner for schedules, meals, shopping,
notes, budget items, and family members. It runs as a small ASP.NET Core app on
your own computer and stores data locally with LiteDB.

The app is currently prepared for Windows-first local use and packaged releases.
It does not require an account or an external service.

## Download And Run

The first packaged release is planned as:

- `FamilyPlanner-v0.1.0-win-x64.zip`
- `FamilyPlanner-v0.1.0-win-x64.sha256`

To use the packaged app:

1. Extract the zip file.
2. Double-click `Start-FamilyPlanner.cmd`.
3. Open `http://localhost:5080` if the browser does not open automatically.
4. Complete the first-run setup.

The v0.1.0 Windows package is unsigned. Windows SmartScreen may show a warning
until FamilyPlanner has a code-signing certificate and signed release pipeline.

## Data And Privacy

Default data is stored here:

```text
%LocalAppData%\FamilyPlanner
```

That folder contains the LiteDB database and uploaded avatars. Keeping data in
`%LocalAppData%` means upgrades can replace the app folder without deleting
family data.

Portable mode is available for advanced use:

```powershell
.\Start-FamilyPlanner.cmd -Portable
```

Portable mode stores data beside the extracted app. Back up that folder before
deleting or replacing the extracted release.

## Language Options

FamilyPlanner ships static language packs for Norwegian Bokmål and English.
The app uses the configured launch language first, then system/browser language
fallbacks.

Packaged app examples:

```powershell
.\Start-FamilyPlanner.cmd nb
.\Start-FamilyPlanner.cmd en
.\Start-FamilyPlanner.cmd -Language no-NB
.\Start-FamilyPlanner.cmd -Language en-US
```

Source launcher examples:

```powershell
.\Launch-FamilyPlanner.cmd nb
.\Launch-FamilyPlanner.cmd /en -Portable
.\Launch-FamilyPlanner.ps1 -Language en-US -NoBrowser
```

## Build From Source

Requirements:

- .NET SDK 10
- Windows PowerShell
- Git

Preferred source launcher:

```powershell
.\Launch-FamilyPlanner.cmd
```

Manual source run:

```powershell
$env:DOTNET_SKIP_FIRST_TIME_EXPERIENCE='1'
$env:DOTNET_CLI_TELEMETRY_OPTOUT='1'
$env:DOTNET_CLI_HOME="$PWD\.dotnet"
dotnet run --launch-profile http
```

Default URL:

```text
http://localhost:5080
```

Build:

```powershell
dotnet build .\FamilyPlanner.csproj -c Debug
```

## Testing

Install Playwright Chromium once:

```powershell
.\Install-UiRegressionBrowser.ps1
```

Run the full UI regression suite:

```powershell
.\Run-UiRegression.ps1
```

Current verified baseline:

```text
55 passed, 0 failed
```

Regression artifacts are written to:

```text
tests\FamilyPlanner.UiTests\.artifacts\
```

See [tests/FamilyPlanner.UiTests/README.md](tests/FamilyPlanner.UiTests/README.md)
for the detailed test coverage map.

## Release And Branch Policy

- `main` is the clean developer-facing baseline.
- `release/*` branches are used for package preparation.
- `test/*` branches are disposable validation branches.
- Build outputs, package outputs, browser caches, databases, logs, and test
  artifacts stay out of Git.

Release packaging for v0.1.0 is run from `release/v0.1.0`:

```powershell
.\Package-FamilyPlannerRelease.ps1
```

The script creates ignored local files under `releases/`, including the zip and
SHA-256 checksum.

## Project Layout

- `Program.cs`: host startup, middleware, static files, setup routing, health.
- `Endpoints/`: page, setup, and planner API route groups.
- `Services/Storage/`: LiteDB access, maintenance, and avatar file storage.
- `Services/Localization/`: static language-pack loading and page rendering.
- `AppPages/`: server-rendered HTML templates.
- `wwwroot/assets/`: planner CSS and JavaScript.
- `Packaging/`: launcher templates included in release zips.
- `tests/FamilyPlanner.UiTests/`: Playwright/NUnit regression suite.

For workstation handoff and repo-specific operating guidance, see
[AGENTS.md](AGENTS.md).
