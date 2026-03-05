@echo off
REM ──────────────────────────────────────────────────────────────────
REM  Kinetix PC Server v2.0 – Build Script
REM  Produces: dist\kinetix-server.exe  (single-file, no-console)
REM ──────────────────────────────────────────────────────────────────

echo.
echo  ============================================
echo   Kinetix PC Server v2.0 – Build
echo  ============================================
echo.

REM 1. Install / update dependencies
echo [1/3] Installing dependencies …
pip install -r requirements.txt
if errorlevel 1 (
    echo ERROR: pip install failed.
    pause
    exit /b 1
)

REM 2. Run PyInstaller
echo.
echo [2/3] Running PyInstaller …
pyinstaller ^
    --noconfirm ^
    --onefile ^
    --noconsole ^
    --name kinetix-server ^
    --hidden-import pystray._win32 ^
    --hidden-import PIL._tkinter_finder ^
    --collect-all vgamepad ^
    --add-data "controller_mapper.py;." ^
    --add-data "device_manager.py;." ^
    --add-data "discovery.py;." ^
    --add-data "logger.py;." ^
    --add-data "tray_icon.py;." ^
    server.py

if errorlevel 1 (
    echo ERROR: PyInstaller build failed.
    pause
    exit /b 1
)

REM 3. Done
echo.
echo [3/3] Build successful!
echo.
echo   Output:  dist\kinetix-server.exe
echo.
echo  You can now distribute dist\kinetix-server.exe
echo  It runs standalone — no Python installation required.
echo.
pause
