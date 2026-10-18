@echo off
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\test-project.ps1"
if errorlevel 1 (echo Checks failed. Please share this window with Codex.) else (echo Project checks passed.)
pause
