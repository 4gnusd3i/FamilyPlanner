param(
    [string]$Version = "v0.1.0-android"
)

$ErrorActionPreference = "Stop"

$androidRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $androidRoot "..")
$branch = (& git -C $repoRoot branch --show-current).Trim()

if ($branch -notlike "release/android/v*") {
    throw "Android release packaging must run from a release/android/v* branch. Current branch: $branch"
}

$dirty = & git -C $repoRoot status --porcelain
if ($dirty) {
    throw "Android release packaging requires a clean worktree."
}

$signingProperties = Join-Path $androidRoot "signing\release.properties"
if (-not (Test-Path -LiteralPath $signingProperties)) {
    throw "Missing Android release signing file: $signingProperties. Copy android\release-signing.properties.example and use local secrets only."
}

$checksScript = Join-Path $androidRoot "Run-AndroidChecks.ps1"
& $checksScript -Tasks @("test", "lint", "assembleRelease")

$apkPath = Join-Path $androidRoot "app\build\outputs\apk\release\app-release.apk"
if (-not (Test-Path -LiteralPath $apkPath)) {
    throw "Expected signed release APK was not produced: $apkPath"
}

$sdkRoot = Join-Path $env:LOCALAPPDATA "Android\Sdk"
$apkSigner = Get-ChildItem -Path (Join-Path $sdkRoot "build-tools") -Recurse -Filter "apksigner.bat" |
    Sort-Object FullName -Descending |
    Select-Object -First 1

if (-not $apkSigner) {
    throw "Android SDK apksigner.bat was not found under $sdkRoot\build-tools."
}

& $apkSigner.FullName verify --verbose $apkPath
if ($LASTEXITCODE -ne 0) {
    throw "apksigner verification failed with exit code $LASTEXITCODE."
}

$releaseDir = Join-Path $repoRoot "releases"
New-Item -ItemType Directory -Force -Path $releaseDir | Out-Null

$artifactName = "FamilyPlanner-$Version.apk"
$artifactPath = Join-Path $releaseDir $artifactName
$checksumPath = "$artifactPath.sha256"

Copy-Item -LiteralPath $apkPath -Destination $artifactPath -Force

$hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $artifactPath).Hash.ToLowerInvariant()
$checksumLine = "$hash  $artifactName"
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($checksumPath, $checksumLine + [Environment]::NewLine, $utf8NoBom)

Write-Host "Created $artifactPath"
Write-Host "Created $checksumPath"
