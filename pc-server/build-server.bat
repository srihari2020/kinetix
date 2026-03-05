@echo off
REM ──────────────────────────────────────────────────────────────────
REM  Kinetix PC Server v3.0 – Build Script
REM  Produces: dist\server.exe
REM ──────────────────────────────────────────────────────────────────

echo.
echo  ============================================
echo   Kinetix PC Server v3.0 – Build
echo  ============================================
echo.

echo [1/2] Installing dependencies …
python -m pip install -r requirements.txt pyinstaller
if errorlevel 1 (
    echo ERROR: pip install failed.
    exit /b 1
)

echo.
echo [2/2] Running PyInstaller …
python -m PyInstaller --clean --noconfirm server.spec

if errorlevel 1 (
    echo ERROR: PyInstaller build failed.
    exit /b 1
)

echo.
echo Build successful! Output in pc-server\dist\server.exe
echo.
