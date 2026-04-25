[CmdletBinding(SupportsShouldProcess = $true, ConfirmImpact = 'High')]
param(
    [switch]$BuildOutput,
    [switch]$RegressionArtifacts,
    [switch]$SmokeData,
    [switch]$LocalAppData,
    [switch]$AllGenerated
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSCommandPath

function Get-RepoPath {
    param([Parameter(Mandatory = $true)][string]$RelativePath)

    return [System.IO.Path]::GetFullPath((Join-Path $repoRoot $RelativePath))
}

function Assert-UnderRepo {
    param([Parameter(Mandatory = $true)][string]$Path)

    $fullRepoRoot = [System.IO.Path]::GetFullPath($repoRoot).TrimEnd([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar)
    $fullPath = [System.IO.Path]::GetFullPath($Path).TrimEnd([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar)
    $prefix = "$fullRepoRoot$([System.IO.Path]::DirectorySeparatorChar)"

    if ($fullPath.Equals($fullRepoRoot, [System.StringComparison]::OrdinalIgnoreCase) -or
        -not $fullPath.StartsWith($prefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to remove path outside repository root: $fullPath"
    }
}

function Remove-CleanupTarget {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$Description,
        [switch]$RequireRepoPath
    )

    $fullPath = [System.IO.Path]::GetFullPath($Path)
    if ($RequireRepoPath) {
        Assert-UnderRepo -Path $fullPath
    }

    if (-not (Test-Path -LiteralPath $fullPath)) {
        Write-Host "Skip missing $Description`: $fullPath"
        return
    }

    if ($PSCmdlet.ShouldProcess($fullPath, "Remove $Description")) {
        Remove-Item -LiteralPath $fullPath -Recurse -Force
        Write-Host "Removed $Description`: $fullPath"
    }
}

$selectedAny = $BuildOutput -or $RegressionArtifacts -or $SmokeData -or $LocalAppData -or $AllGenerated
if (-not $selectedAny) {
    Write-Host "No cleanup switches selected. Use -BuildOutput, -RegressionArtifacts, -SmokeData, -LocalAppData, or -AllGenerated."
    Write-Host "This script intentionally preserves .dotnet and .playwright-browsers caches."
    exit 0
}

$repoTargets = [System.Collections.Generic.List[object]]::new()

if ($BuildOutput -or $AllGenerated) {
    $repoTargets.Add([pscustomobject]@{ Path = 'bin'; Description = 'app build output' })
    $repoTargets.Add([pscustomobject]@{ Path = 'obj'; Description = 'app intermediate output' })
    $repoTargets.Add([pscustomobject]@{ Path = 'tests/FamilyPlanner.UiTests/bin'; Description = 'UI test build output' })
    $repoTargets.Add([pscustomobject]@{ Path = 'tests/FamilyPlanner.UiTests/obj'; Description = 'UI test intermediate output' })
}

if ($RegressionArtifacts -or $AllGenerated) {
    $repoTargets.Add([pscustomobject]@{ Path = 'tests/FamilyPlanner.UiTests/.artifacts'; Description = 'UI regression artifacts' })
    $repoTargets.Add([pscustomobject]@{ Path = 'TestResults'; Description = 'test results' })
}

if ($SmokeData -or $AllGenerated) {
    $repoTargets.Add([pscustomobject]@{ Path = '.localdata'; Description = 'repo-local app data' })
    $repoTargets.Add([pscustomobject]@{ Path = '.smoketestdata'; Description = 'smoke-test data' })
    $repoTargets.Add([pscustomobject]@{ Path = '.smoketestdata-ui'; Description = 'UI smoke-test data' })
}

foreach ($target in $repoTargets) {
    Remove-CleanupTarget -Path (Get-RepoPath -RelativePath $target.Path) -Description $target.Description -RequireRepoPath
}

if ($LocalAppData) {
    $localAppDataRoot = [Environment]::GetFolderPath([Environment+SpecialFolder]::LocalApplicationData)
    $familyPlannerData = Join-Path $localAppDataRoot 'FamilyPlanner'
    Remove-CleanupTarget -Path $familyPlannerData -Description '%LocalAppData%\FamilyPlanner'
}

Write-Host "Cleanup complete. .dotnet and .playwright-browsers were not touched."
