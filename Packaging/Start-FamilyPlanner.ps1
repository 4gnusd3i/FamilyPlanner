[CmdletBinding()]
param(
    [switch]$NoBrowser,
    [switch]$Portable,
    [string]$Language
)

$ErrorActionPreference = "Stop"

$packageRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$appExe = Join-Path $packageRoot "FamilyPlanner.exe"
$portableDataRoot = Join-Path $packageRoot "data"
$appUrl = "http://localhost:5080/"
$healthUrl = "${appUrl}health"

function Resolve-AppLanguage {
    param([string]$Value)

    if ([string]::IsNullOrWhiteSpace($Value)) {
        return $null
    }

    $normalized = $Value.Trim().Replace("_", "-").ToLowerInvariant()
    switch ($normalized) {
        { $_ -in @("no-nb", "nb-no", "nb", "no") } { return "no-NB" }
        { $_ -in @("en-us", "en") } { return "en-US" }
        default {
            throw "Invalid language '$Value'. Use no-NB, nb-NO, nb, no, en-US, or en."
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

if (-not (Test-Path -LiteralPath $appExe)) {
    throw "Could not find FamilyPlanner.exe in $packageRoot"
}

$resolvedLanguage = Resolve-AppLanguage -Value $Language

if (Test-AppHealthy -Url $healthUrl) {
    Write-Host "FamilyPlanner is already running at $appUrl"
    if ($resolvedLanguage) {
        Write-Warning "Language cannot be changed for an already running process. Close FamilyPlanner and start again to use $resolvedLanguage."
    }
    if (-not $NoBrowser) {
        Start-Process $appUrl | Out-Null
    }
    exit 0
}

$env:ASPNETCORE_ENVIRONMENT = "Production"
$env:App__Urls = "http://localhost:5080"
if ($Portable) {
    $env:FAMILYPLANNER_DATA_ROOT = $portableDataRoot
}
elseif (Test-Path Env:\FAMILYPLANNER_DATA_ROOT) {
    Remove-Item Env:\FAMILYPLANNER_DATA_ROOT
}
if ($resolvedLanguage) {
    $env:App__Language = $resolvedLanguage
}
elseif (Test-Path Env:\App__Language) {
    Remove-Item Env:\App__Language
}

Write-Host "Starting FamilyPlanner..."
if ($Portable) {
    Write-Host "Data folder: $portableDataRoot"
}
else {
    Write-Host "Data folder: %LocalAppData%\FamilyPlanner"
}
if ($resolvedLanguage) {
    Write-Host "Language: $resolvedLanguage"
}

$process = Start-Process -FilePath $appExe -WorkingDirectory $packageRoot -PassThru

if (-not (Wait-ForHealth -Url $healthUrl)) {
    if ($process.HasExited) {
        throw "FamilyPlanner exited during startup with code $($process.ExitCode)."
    }

    throw "FamilyPlanner did not respond at $healthUrl after startup."
}

Write-Host "FamilyPlanner is running at $appUrl"
if (-not $NoBrowser) {
    Start-Process $appUrl | Out-Null
}
