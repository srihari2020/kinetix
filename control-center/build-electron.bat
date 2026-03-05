@echo off
REM ──────────────────────────────────────────────────────────────────
REM  Kinetix Control Center – Electron Build Script
REM  Builds the React app and packages the Electron app.
REM ──────────────────────────────────────────────────────────────────

echo.
echo  ============================================
echo   Kinetix Control Center – Build
echo  ============================================
echo.

echo [1/2] Installing Node dependencies …
call npm install
if errorlevel 1 (
    echo ERROR: npm install failed.
    exit /b 1
)

echo.
echo [2/2] Building Electron App …
call npm run electron:build
if errorlevel 1 (
    echo ERROR: Electron build failed.
    exit /b 1
)

echo.
echo Build successful! Output in control-center\release\
echo.
