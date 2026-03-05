@echo off
REM ──────────────────────────────────────────────────────────────────
REM  Kinetix Control Center v3.0 – Installer Build Script
REM  Requires Inno Setup Compiler (iscc) in PATH.
REM ──────────────────────────────────────────────────────────────────

echo.
echo  ============================================
echo   Kinetix Installer – Build
echo  ============================================
echo.

echo [1/1] Running Inno Setup Compiler …
iscc kinetix-installer.iss

if errorlevel 1 (
    echo ERROR: Inno Setup build failed.
    echo Please ensure Inno Setup is installed and 'iscc' is in your PATH.
    exit /b 1
)

echo.
echo Build successful! Output in ..\dist\KinetixSetup.exe
echo.
