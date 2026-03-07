@echo off
setlocal
echo ===========================================
echo Kinetix Automated Build Pipeline
echo ===========================================

REM --- 1. Build Python Back-End ---
echo.
echo [1/3] Building Python Server...
cd pc-server
if not exist "venv" (
    echo Setting up Python VENV...
    python -m venv venv
)
call venv\Scripts\activate.bat
pip install -r requirements.txt
pip install pyinstaller
pyinstaller --onefile --noconsole --name server server.py
copy /Y dist\server.exe ..\control-center\resources\server.exe
call deactivate
cd ..

REM --- 2. Build Electron Dashboard ---
echo.
echo [2/3] Building Control Center...
cd control-center
echo Installing NPM dependencies...
call npm install
echo Building React / Electron App...
call npm run build
call npm run dist
copy /Y dist\*.exe ..\installer\KinetixSetup.exe
cd ..

REM --- 3. Build Web Preview (Optional test) ---
echo.
echo [3/3] Building Website...
cd website
call npm i
call npm run build
cd ..

echo.
echo ===========================================
echo Build Pipeline Complete!
echo Windows Installer created at installer\KinetixSetup.exe
echo ===========================================
endlocal
exit /b 0
