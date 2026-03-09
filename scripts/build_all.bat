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
pyinstaller --clean server.spec
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
call npx electron-packager . "Kinetix Control Center" --platform=win32 --arch=x64 --out=release --overwrite --icon=public/favicon.ico --asar --ignore="^^/node_modules/electron|^^/release"
copy /Y ..\pc-server\dist\server.exe "release\Kinetix Control Center-win32-x64\resources\server.exe"
cd ..
cd installer
call "C:\Program Files (x86)\Inno Setup 6\ISCC.exe" kinetix-installer.iss
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
