# FamilyPlanner

> `main` is the developer-facing branch. It contains source code, test suites,
> local development scripts, and release-preparation tooling. End users who want
> a packaged app should use GitHub Releases, not the `main` branch.

FamilyPlanner is a local family week planner for schedules, meals, shopping,
notes, budget items, and family members. It runs as an ASP.NET Core app on your
own computer and stores data locally with LiteDB.

## Branch And Release Policy

- `main`: active development baseline with tests, scripts, and contributor docs.
- `release/*`: trimmed release branches containing only source/package files for a specific release.
- `test/*`: disposable validation branches used for builds, regression runs, and manual checks.
- Packaged builds are published through GitHub Releases, for example `v0.1.0`.

## Packaged Releases

Users should download the latest package from GitHub Releases:

- <https://github.com/4gnusd3i/FamilyPlanner/releases>

The first package is:

- `FamilyPlanner-v0.1.0-win-x64.zip`
- `FamilyPlanner-v0.1.0-win-x64.sha256`

The v0.1.0 package is unsigned. Windows SmartScreen may show a warning until
FamilyPlanner has a code-signing certificate and signed release pipeline.

Default data path:

```text
%LocalAppData%\FamilyPlanner
```

That folder contains the LiteDB database and uploaded avatars, so app upgrades
can replace the extracted release folder without deleting family data.

## Build From Source

Requirements:

- .NET SDK 10
- Windows PowerShell
- Git

Preferred local launcher:

```powershell
.\Launch-FamilyPlanner.cmd
```

Useful launcher examples:

```powershell
.\Launch-FamilyPlanner.cmd nb
.\Launch-FamilyPlanner.cmd /en -Portable
.\Launch-FamilyPlanner.ps1 -Language en-US -NoBrowser
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

Run the full regression suite:

```powershell
.\Run-UiRegression.ps1
```

Current verified baseline:

```text
55 passed, 0 failed
```

Artifacts are written to:

```text
tests\FamilyPlanner.UiTests\.artifacts\
```

See [tests/FamilyPlanner.UiTests/README.md](tests/FamilyPlanner.UiTests/README.md)
for detailed test coverage.

## Release Preparation

Release packages are built from `release/v*` branches:

```powershell
.\Package-FamilyPlannerRelease.ps1
```

The script publishes a self-contained `win-x64` single-file build, adds the
package README, writes the zip under ignored `releases/`, and creates a SHA-256
checksum file. The release branch must be clean before packaging.

## Project Layout

- `Program.cs`: host startup, middleware, static files, setup routing, health.
- `Endpoints/`: page, setup, and planner API route groups.
- `Services/Storage/`: LiteDB access, maintenance, and avatar file storage.
- `Services/Localization/`: static language-pack loading and page rendering.
- `AppPages/`: server-rendered HTML templates.
- `wwwroot/assets/`: planner CSS and JavaScript.
- `tests/FamilyPlanner.UiTests/`: Playwright/NUnit regression suite.
- `Packaging/`: package README template used by release zips.

For repo-specific operating guidance, see
[AGENTS.md](AGENTS.md).
