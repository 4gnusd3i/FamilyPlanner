$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$env:DOTNET_SKIP_FIRST_TIME_EXPERIENCE = "1"
$env:DOTNET_CLI_TELEMETRY_OPTOUT = "1"
$env:DOTNET_CLI_HOME = Join-Path $repoRoot ".dotnet"
$env:PLAYWRIGHT_BROWSERS_PATH = Join-Path $repoRoot ".playwright-browsers"

$settingsPath = Join-Path $repoRoot "tests\FamilyPlanner.UiTests\Playwright.runsettings"
$testProject = Join-Path $repoRoot "tests\FamilyPlanner.UiTests\FamilyPlanner.UiTests.csproj"

dotnet build $testProject -c Debug
if ($LASTEXITCODE -ne 0) {
    throw "dotnet build feilet."
}

dotnet test $testProject -c Debug --no-build --settings $settingsPath
if ($LASTEXITCODE -ne 0) {
    throw "dotnet test feilet."
}
