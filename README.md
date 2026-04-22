# FamilyPlanner

Local family week planner built with ASP.NET Core and LiteDB.

## Requirements

- .NET SDK 10
- Windows PowerShell for the included launcher and test scripts

## Run Locally

Preferred launcher:

```powershell
.\Launch-FamilyPlanner.cmd
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

Default local data path:

- `%LocalAppData%\FamilyPlanner`

Portable override:

```powershell
.\Launch-FamilyPlanner.ps1 -Portable
```

## Build

```powershell
dotnet build .\FamilyPlanner.csproj -c Debug
```

## UI Regression Tests

Install Playwright Chromium once:

```powershell
.\Install-UiRegressionBrowser.ps1
```

Run the suite:

```powershell
.\Run-UiRegression.ps1
```

Current verified baseline:

- `37 passed, 0 failed`

## Project Layout

- `Program.cs`: host setup and middleware
- `Endpoints/`: page, setup, and planner API routes
- `Services/Storage/`: LiteDB access and file storage paths
- `AppPages/`: server-served HTML pages
- `wwwroot/assets/`: split CSS and JavaScript assets for setup and planner surfaces
- `tests/FamilyPlanner.UiTests/`: Playwright regression suite

## Notes

- The application is now local-only; legacy live import has been removed.
- First run uses `/setup` to capture household and first family member, then the planner runs without account login.
- For workstation handoff and repo-specific operating guidance, see [AGENTS.md](AGENTS.md).
