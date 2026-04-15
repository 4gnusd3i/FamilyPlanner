$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$env:DOTNET_SKIP_FIRST_TIME_EXPERIENCE = "1"
$env:DOTNET_CLI_TELEMETRY_OPTOUT = "1"
$env:DOTNET_CLI_HOME = Join-Path $repoRoot ".dotnet"
$env:PLAYWRIGHT_BROWSERS_PATH = Join-Path $repoRoot ".playwright-browsers"

$testProject = Join-Path $repoRoot "tests\FamilyPlanner.UiTests\FamilyPlanner.UiTests.csproj"

dotnet build $testProject -c Debug
if ($LASTEXITCODE -ne 0) {
    throw "dotnet build feilet."
}

$playwrightScript = Join-Path $repoRoot "tests\FamilyPlanner.UiTests\bin\Debug\net10.0\playwright.ps1"
if (-not (Test-Path $playwrightScript)) {
    throw "Fant ikke Playwright-installasjonsskriptet: $playwrightScript"
}

powershell.exe -NoProfile -ExecutionPolicy Bypass -File $playwrightScript install chromium
if ($LASTEXITCODE -ne 0) {
    throw "Playwright browserinstallasjon feilet."
}
