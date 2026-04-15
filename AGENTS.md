# AGENTS.md

## Purpose

This repository is a local rebuild of the FamilyPlanner family week planner. It is an ASP.NET Core 10 web app with LiteDB storage, setup-only onboarding, a launcher for local use, and a Playwright-based functional regression suite.

This file is intended to let another workstation pick up work quickly without rediscovering the project structure or the current baseline.

## Current State

- Primary working branch: `feature/frontend-redesign`
- Current expected regression result: `13 passed, 0 failed`
- Legacy live-import functionality has been removed from setup, UI, API, and storage code
- Frontend assets and backend planner/storage code are now split into smaller feature files for safer maintenance

## Tech Stack

- .NET SDK: `10.x`
- App project: `FamilyPlanner.csproj`
- Web framework: ASP.NET Core minimal hosting
- Storage: LiteDB `5.0.21`
- Setup: one-time local household initialization without account login
- Frontend: static HTML/CSS/JS served by the app
- Browser tests: NUnit + Playwright
- Scripts: Windows PowerShell / `.cmd`

## Repository Layout

- `Program.cs`
  - host startup, setup redirect logic, static files, `/health`
- `Endpoints/`
  - `PageEndpoints.cs`: `/`, `/setup`
  - `SetupEndpoints.cs`: `POST /setup`
  - `PlannerApiEndpoints*.cs`: `/api/*`, split by feature area
- `Services/Storage/`
  - `PlannerStore*.cs`: LiteDB collections and CRUD logic, split by feature area
  - `StoragePaths.cs`: local data-root resolution
  - `AvatarStorageService.cs`: avatar upload/delete
- `Models/`
  - entity and command models
- `AppPages/`
  - server-served HTML pages: `index.html`, `setup.html`
- `wwwroot/assets/`
  - `css/base.css`
  - `css/planner.css`
  - `js/auth-feedback.js`
  - `js/planner-*.js`
- `wwwroot/pwa/`
  - manifest and icons
- `tests/FamilyPlanner.UiTests/`
  - Playwright functional regression suite

## Runtime and Data

Default local data root:

- `%LocalAppData%\FamilyPlanner`

Override for portable/test runs:

- environment variable `FAMILYPLANNER_DATA_ROOT`

Expected contents under the data root:

- `familyplanner.db`
- `uploads\avatars\`

Important note:

- user-uploaded avatars are served from `/uploads/...`

## First-Time Workstation Setup

Minimum prerequisites:

1. Install .NET SDK 10.
2. Install Git and make sure your signing setup works on that workstation.
3. If you intend to run browser regression tests, install Playwright Chromium via the provided script.

Validation commands:

```powershell
dotnet --version
git config --global --get commit.gpgsign
```

If browser testing is needed:

```powershell
.\Install-UiRegressionBrowser.ps1
```

## Running the App

Preferred local launcher:

```powershell
.\Launch-FamilyPlanner.cmd
```

Useful launcher options:

```powershell
.\Launch-FamilyPlanner.ps1 -Portable
.\Launch-FamilyPlanner.ps1 -NoBrowser
```

Manual run:

```powershell
$env:DOTNET_SKIP_FIRST_TIME_EXPERIENCE='1'
$env:DOTNET_CLI_TELEMETRY_OPTOUT='1'
$env:DOTNET_CLI_HOME="$PWD\.dotnet"
dotnet run --launch-profile http
```

Default URL:

- `http://localhost:5080`

## Setup Flow

- On a brand-new data root with no household profile, browser routes redirect to `/setup`.
- Submitting `/setup` stores household name and first family member.
- After setup exists, `/setup` redirects to `/`.
- API routes are open locally after setup and return `409` with `setup_required` before setup.

## API Surface

Routes under `/api`:

- `GET/POST /api/events`
- `GET/POST /api/meals`
- `GET/POST /api/budget`
- `GET/POST /api/family`
- `GET/POST /api/family/assignments`
- `GET/POST /api/medicines`
- `GET/POST /api/notes`
- `GET/POST /api/shopping`

Conventions:

- forms are used for most create/update requests
- JSON is used for toggles, deletes, and budget actions
- field names intentionally mirror the current frontend contract, for example:
  - `event_date`
  - `day_of_week`
  - `meal_type`
  - `owner_id`
  - `source_meal_id`
  - `taken`
  - `done`

## Data Model Summary

LiteDB collections:

- `householdProfile`
- `familyMembers`
- `events`
- `meals`
- `budgetMonths`
- `expenses`
- `medicines`
- `notes`
- `shoppingItems`
- `familyAssignments`

## Functional Regression Baseline

Regression suite location:

- `tests/FamilyPlanner.UiTests`

Run it with:

```powershell
.\Run-UiRegression.ps1
```

What it currently covers:

- setup routing after initialization
- kiosk layout above threshold and stacked layout below threshold
- horizontal overflow checks
- overlap and viewport-fit checks for major UI regions
- touch-target sizing checks for primary interactive controls
- event create/edit/delete
- meal create/edit/delete and meal-to-shopping
- budget update and expense add/delete
- shopping create/toggle/edit/delete
- medicine create/toggle/view/edit/delete
- note create/view/edit/delete
- family member create/profile edit/delete
- family assignment add/remove through drag/drop path

Artifacts are written to:

- `tests\FamilyPlanner.UiTests\.artifacts\`

Important testing notes:

- the suite starts its own app instance on a free localhost port
- each test gets a fresh data root under the artifacts directory
- Playwright browsers are stored in `.playwright-browsers`
- there is no `.sln` file; use the project-level scripts and `.csproj` commands

## Build Commands

App:

```powershell
dotnet build .\FamilyPlanner.csproj -c Debug
```

UI tests:

```powershell
dotnet build .\tests\FamilyPlanner.UiTests\FamilyPlanner.UiTests.csproj -c Debug
```

## Version Control Expectations

- Commits are expected to be signed.
- On the current machine, commit signing is configured globally and happens implicitly.
- On a new workstation, verify signing is actually working before you assume it is.
- Keep generated output out of Git. The current `.gitignore` already excludes:
  - `.dotnet/`
  - `.localdata/`
  - `.smoketestdata/`
  - `.smoketestdata-ui/`
  - `.playwright-browsers/`
  - `TestResults/`
  - `tests/FamilyPlanner.UiTests/.artifacts/`
  - `bin/`
  - `obj/`
- `todo.md` is currently untracked and should stay that way unless explicitly requested otherwise.

## Project-Specific Working Rules

- Do not degrade testing quality just because the environment is constrained. If proper browser automation or package installation is needed, do that properly.
- If required software or packages are missing, ask before installing them rather than silently taking a worse fallback.
- Keep responsive behavior explicit with media queries for kiosk and stacked layouts.
- When UI behavior changes, update the Playwright regression suite in the same change set.
- Prefer using the existing launcher and test scripts instead of inventing new entrypoints.

## Known Product Direction

Items already called out for future work in `todo.md`:

- continue UI/UX cleanup and responsive polishing
- refine color theme
- trim superfluous text in the interface
- general cleanup and optimization
- prepare a Windows release branch
- later evaluate backup/restore work
- later evaluate an Android/native branch

## Fast Resume Checklist

On a fresh workstation, the fastest safe resume path is:

1. Verify `.NET 10`, Git, and commit signing.
2. Clone the repo and checkout `feature/frontend-redesign`.
3. Run `.\Launch-FamilyPlanner.cmd` and confirm `http://localhost:5080/health`.
4. Run `.\Install-UiRegressionBrowser.ps1` once if Playwright is not set up yet.
5. Run `.\Run-UiRegression.ps1` and confirm the suite is green before changing behavior.
6. Make changes.
7. Re-run the regression suite before committing.
