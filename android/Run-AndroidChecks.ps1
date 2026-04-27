param(
    [string[]]$Tasks = @("test", "lint", "assembleDebug")
)

$ErrorActionPreference = "Stop"

$androidRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$jdkRoot = "C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot"
$sdkRoot = Join-Path $env:LOCALAPPDATA "Android\Sdk"

if (-not (Test-Path -LiteralPath $jdkRoot)) {
    throw "JDK 17 not found at $jdkRoot."
}

if (-not (Test-Path -LiteralPath $sdkRoot)) {
    throw "Android SDK not found at $sdkRoot."
}

$env:JAVA_HOME = $jdkRoot
$env:ANDROID_HOME = $sdkRoot
$env:ANDROID_SDK_ROOT = $sdkRoot
$env:GRADLE_USER_HOME = Join-Path $env:USERPROFILE ".gradle"
$env:JAVA_TOOL_OPTIONS = "-Duser.home=$env:USERPROFILE"
$env:PATH = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\cmdline-tools\latest\bin;$env:ANDROID_HOME\platform-tools;$env:PATH"

Push-Location $androidRoot
try {
    & .\gradlew.bat @Tasks
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle failed with exit code $LASTEXITCODE."
    }
}
finally {
    Pop-Location
}
