@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\maven.ps1" -B install
exit /b %errorlevel%
