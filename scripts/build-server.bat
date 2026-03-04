@echo off
REM ──────────────────────────────────────────────────────────────────
REM  Kinetix — Build Windows Server EXE
REM  Run from the repository root: scripts\build-server.bat
REM ──────────────────────────────────────────────────────────────────

echo.
echo  ============================================
echo   Kinetix — Windows Server EXE Build
echo  ============================================
echo.

pushd "%~dp0..\pc-server"

REM 1. Create virtual environment (optional but recommended)
if not exist ".venv" (
    echo [1/4] Creating virtual environment…
    python -m venv .venv
)

echo [2/4] Activating virtual environment…
call .venv\Scripts\activate.bat

echo [3/4] Installing dependencies…
pip install -r requirements.txt
if errorlevel 1 (
    echo ERROR: pip install failed.
    popd
    pause
    exit /b 1
)

echo.
echo [4/4] Running PyInstaller…
pyinstaller --noconfirm kinetix-server.spec
if errorlevel 1 (
    echo ERROR: PyInstaller build failed.
    popd
    pause
    exit /b 1
)

echo.
echo  ============================================
echo   Build successful!
echo   Output: pc-server\dist\kinetix-server.exe
echo  ============================================
echo.

popd
pause
