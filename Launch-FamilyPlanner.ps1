[CmdletBinding()]
param(
    [switch]$NoBrowser,
    [switch]$Portable,
    [string]$Language
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectFile = Join-Path $repoRoot "FamilyPlanner.csproj"
$launcherHome = Join-Path $repoRoot ".dotnet"
$portableDataRoot = Join-Path $repoRoot ".localdata"
$appUrl = "http://localhost:5080/"
$healthUrl = "${appUrl}health"

function Resolve-AppLanguage {
    param([string]$Value)

    if ([string]::IsNullOrWhiteSpace($Value)) {
        return $null
    }

    $normalized = $Value.Trim().Replace('_', '-').ToLowerInvariant()
    switch ($normalized) {
        { $_ -in @("no-nb", "nb-no", "nb", "no") } { return "no-NB" }
        { $_ -in @("en-us", "en") } { return "en-US" }
        default {
            throw "Ugyldig språk '$Value'. Bruk no-NB, nb-NO, nb, no, en-US eller en."
        }
    }
}

function Test-AppHealthy {
    param([string]$Url)

    try {
        $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -Method Get -TimeoutSec 2
        return $response.StatusCode -eq 200
    }
    catch {
        return $false
    }
}

function Wait-ForHealth {
    param(
        [string]$Url,
        [int]$MaxAttempts = 60
    )

    for ($attempt = 0; $attempt -lt $MaxAttempts; $attempt += 1) {
        if (Test-AppHealthy -Url $Url) {
            return $true
        }

        Start-Sleep -Milliseconds 500
    }

    return $false
}

if (-not (Get-Command dotnet -ErrorAction SilentlyContinue)) {
    throw "Fant ikke 'dotnet' i PATH. Installer .NET SDK 10 først."
}

if (-not (Test-Path $projectFile)) {
    throw "Fant ikke prosjektfilen: $projectFile"
}

$resolvedLanguage = Resolve-AppLanguage -Value $Language

if (Test-AppHealthy -Url $healthUrl) {
    Write-Host "FamilyPlanner kjører allerede på $appUrl"
    if ($resolvedLanguage) {
        Write-Warning "Språk kan ikke endres for en eksisterende prosess. Lukk FamilyPlanner og start på nytt for å bruke $resolvedLanguage."
    }
    if (-not $NoBrowser) {
        Start-Process $appUrl | Out-Null
    }
    exit 0
}

$runCommand = @(
    "`$env:DOTNET_SKIP_FIRST_TIME_EXPERIENCE='1'"
    "`$env:DOTNET_CLI_TELEMETRY_OPTOUT='1'"
    "`$env:DOTNET_CLI_HOME='$launcherHome'"
)

if ($Portable) {
    $runCommand += "`$env:FAMILYPLANNER_DATA_ROOT='$portableDataRoot'"
}

if ($resolvedLanguage) {
    $runCommand += "`$env:App__Language='$resolvedLanguage'"
}

$runCommand += "dotnet run --launch-profile http"

$commandText = ($runCommand -join "; ")

Write-Host "Starter FamilyPlanner..."
if ($Portable) {
    Write-Host "Datamappe: $portableDataRoot"
}
else {
    Write-Host "Datamappe: %LocalAppData%\FamilyPlanner"
}
if ($resolvedLanguage) {
    Write-Host "Språk: $resolvedLanguage"
}

Start-Process powershell.exe `
    -WorkingDirectory $repoRoot `
    -ArgumentList @(
        "-NoExit",
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-Command", $commandText
    ) | Out-Null

if (-not (Wait-ForHealth -Url $healthUrl)) {
    throw "FamilyPlanner svarte ikke på $healthUrl etter oppstart."
}

Write-Host "FamilyPlanner kjører på $appUrl"
if (-not $NoBrowser) {
    Start-Process $appUrl | Out-Null
}
