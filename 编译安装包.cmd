@echo off
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0build-apk.ps1"
if errorlevel 1 (echo Build failed. Please share this window with Codex.) else (echo Build complete. Open the APK folder.)
pause
