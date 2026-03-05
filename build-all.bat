@echo off
REM ──────────────────────────────────────────────────────────────────
REM  Kinetix Control Center v3.0 – Master Build Script
REM  Builds the Python Server, Electron Dashboard, and Windows Installer
REM ──────────────────────────────────────────────────────────────────

echo =================================================================
echo.
echo    Kinetix Control Center v3.0 - Full Build Pipeline
echo.
echo =================================================================

REM 1. Build Python Server
echo.
echo --- STEP 1: Building Python Server ---
cd pc-server
call build-server.bat
if errorlevel 1 (
    echo [FATAL] Python server build failed. Aborting.
    cd ..
    exit /b 1
)
cd ..

REM 2. Build Electron UI
echo.
echo --- STEP 2: Building Electron App ---
cd control-center
call build-electron.bat
if errorlevel 1 (
    echo [FATAL] Electron app build failed. Aborting.
    cd ..
    exit /b 1
)
cd ..

REM 3. Build Installer
echo.
echo --- STEP 3: Building Windows Installer ---
cd installer
call build-installer.bat
if errorlevel 1 (
    echo [FATAL] Installer build failed. Aborting.
    cd ..
    exit /b 1
)
cd ..

echo.
echo =================================================================
echo.
echo    SUCCESS! Kinetix Installer created at:
echo    installer\output\KinetixSetup.exe
echo.
echo =================================================================
pause
