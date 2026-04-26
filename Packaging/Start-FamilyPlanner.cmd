@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "SCRIPT_DIR=%~dp0"
set "ARGS="

:parse
if "%~1"=="" goto run

if /I "%~1"=="/?" goto help
if /I "%~1"=="-?" goto help
if /I "%~1"=="--help" goto help

if /I "%~1"=="nb" (
  set "ARGS=!ARGS! -Language no-NB"
  shift
  goto parse
)
if /I "%~1"=="nb-no" (
  set "ARGS=!ARGS! -Language no-NB"
  shift
  goto parse
)
if /I "%~1"=="no-nb" (
  set "ARGS=!ARGS! -Language no-NB"
  shift
  goto parse
)
if /I "%~1"=="no" (
  set "ARGS=!ARGS! -Language no-NB"
  shift
  goto parse
)
if /I "%~1"=="/nb" (
  set "ARGS=!ARGS! -Language no-NB"
  shift
  goto parse
)

if /I "%~1"=="en" (
  set "ARGS=!ARGS! -Language en-US"
  shift
  goto parse
)
if /I "%~1"=="en-us" (
  set "ARGS=!ARGS! -Language en-US"
  shift
  goto parse
)
if /I "%~1"=="/en" (
  set "ARGS=!ARGS! -Language en-US"
  shift
  goto parse
)

set "ARGS=!ARGS! %~1"
shift
goto parse

:run
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%Start-FamilyPlanner.ps1" %ARGS%
exit /b %ERRORLEVEL%

:help
echo FamilyPlanner packaged launcher
echo.
echo Usage:
echo   Start-FamilyPlanner.cmd [nb^|/nb^|en^|/en] [-Portable] [-NoBrowser] [-Language ^<code^>]
echo.
echo Language shortcuts:
echo   nb, /nb, nb-no, no-nb, no   = Norwegian Bokmal
echo   en, /en, en-us              = English (United States)
echo.
echo Examples:
echo   Start-FamilyPlanner.cmd nb
echo   Start-FamilyPlanner.cmd /en -Portable
echo   Start-FamilyPlanner.cmd -Language no-NB -NoBrowser
exit /b 0
