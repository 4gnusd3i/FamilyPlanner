[CmdletBinding()]
param(
    [string]$Version = "0.1.0",
    [string]$Runtime = "win-x64",
    [string]$Configuration = "Release"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSCommandPath
$projectFile = Join-Path $repoRoot "FamilyPlanner.csproj"
$releaseBranch = "release/v$Version"
$packageName = "FamilyPlanner-v$Version-$Runtime"
$releasesRoot = Join-Path $repoRoot "releases"
$workRoot = Join-Path $releasesRoot ".work"
$publishDir = Join-Path $workRoot "publish"
$packageDir = Join-Path $workRoot $packageName
$zipPath = Join-Path $releasesRoot "$packageName.zip"
$checksumPath = Join-Path $releasesRoot "$packageName.sha256"

function Assert-UnderRepo {
    param([Parameter(Mandatory = $true)][string]$Path)

    $fullRepoRoot = [System.IO.Path]::GetFullPath($repoRoot).TrimEnd([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar)
    $fullPath = [System.IO.Path]::GetFullPath($Path).TrimEnd([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar)
    $prefix = "$fullRepoRoot$([System.IO.Path]::DirectorySeparatorChar)"

    if ($fullPath.Equals($fullRepoRoot, [System.StringComparison]::OrdinalIgnoreCase) -or
        -not $fullPath.StartsWith($prefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to use path outside repository root: $fullPath"
    }
}

function Invoke-Git {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments)

    $output = & git -C $repoRoot @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "git $($Arguments -join ' ') failed with exit code $LASTEXITCODE."
    }

    return $output
}

Assert-UnderRepo -Path $releasesRoot
Assert-UnderRepo -Path $workRoot
Assert-UnderRepo -Path $zipPath
Assert-UnderRepo -Path $checksumPath

$branch = (Invoke-Git branch --show-current).Trim()
if ($branch -ne $releaseBranch) {
    throw "Release packaging must run from $releaseBranch. Current branch: $branch"
}

$trackedStatus = Invoke-Git status --short --untracked-files=no
if ($trackedStatus) {
    throw "Tracked working tree changes are present. Commit or revert them before packaging."
}

$commit = (Invoke-Git rev-parse --short=12 HEAD).Trim()

if (Test-Path -LiteralPath $workRoot) {
    Remove-Item -LiteralPath $workRoot -Recurse -Force
}
if (Test-Path -LiteralPath $zipPath) {
    Remove-Item -LiteralPath $zipPath -Force
}
if (Test-Path -LiteralPath $checksumPath) {
    Remove-Item -LiteralPath $checksumPath -Force
}

New-Item -ItemType Directory -Path $publishDir -Force | Out-Null
New-Item -ItemType Directory -Path $packageDir -Force | Out-Null

$env:DOTNET_SKIP_FIRST_TIME_EXPERIENCE = "1"
$env:DOTNET_CLI_TELEMETRY_OPTOUT = "1"
$env:DOTNET_CLI_HOME = Join-Path $repoRoot ".dotnet"

$publishArgs = @(
    "publish", $projectFile,
    "-c", $Configuration,
    "-r", $Runtime,
    "--self-contained", "true",
    "-o", $publishDir,
    "-p:PublishSingleFile=true",
    "-p:PublishTrimmed=false",
    "-p:SourceRevisionId=$commit",
    "-p:DebugType=embedded"
)

& dotnet @publishArgs
if ($LASTEXITCODE -ne 0) {
    throw "dotnet publish failed with exit code $LASTEXITCODE."
}

Copy-Item -Path (Join-Path $publishDir "*") -Destination $packageDir -Recurse -Force
Copy-Item -Path (Join-Path $repoRoot "Packaging\Start-FamilyPlanner.cmd") -Destination $packageDir -Force
Copy-Item -Path (Join-Path $repoRoot "Packaging\Start-FamilyPlanner.ps1") -Destination $packageDir -Force
Copy-Item -Path (Join-Path $repoRoot "Packaging\README.txt") -Destination $packageDir -Force

$developmentSettings = Join-Path $packageDir "appsettings.Development.json"
if (Test-Path -LiteralPath $developmentSettings) {
    Remove-Item -LiteralPath $developmentSettings -Force
}

New-Item -ItemType Directory -Path $releasesRoot -Force | Out-Null
Compress-Archive -Path $packageDir -DestinationPath $zipPath -Force

$hash = Get-FileHash -Path $zipPath -Algorithm SHA256
"$($hash.Hash)  $(Split-Path -Leaf $zipPath)" | Set-Content -Path $checksumPath -Encoding ascii

Remove-Item -LiteralPath $workRoot -Recurse -Force

Write-Host "Created $zipPath"
Write-Host "Created $checksumPath"
