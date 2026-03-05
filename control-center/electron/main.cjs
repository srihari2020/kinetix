const { app, BrowserWindow } = require('electron');
const path = require('path');
const { spawn } = require('child_process');

let mainWindow;
let pythonProcess = null;

const isDev = !app.isPackaged;

function startPythonServer() {
    if (isDev) {
        const pythonPath = 'python'; // or 'python3' based on the system
        const serverScript = path.join(__dirname, '../../pc-server/server.py');
        console.log('Starting Python server at:', serverScript);
        pythonProcess = spawn(pythonPath, [serverScript, '--no-tray']);
    } else {
        const serverExe = path.join(process.resourcesPath, 'server.exe');
        console.log('Starting standalone Python server at:', serverExe);
        pythonProcess = spawn(serverExe, ['--no-tray']);
    }

    pythonProcess.stdout.on('data', (data) => {
        console.log(`Python: ${data.toString()}`);
    });

    pythonProcess.stderr.on('data', (data) => {
        console.error(`Python Error: ${data.toString()}`);
    });

    pythonProcess.on('close', (code) => {
        console.log(`Python process exited with code ${code}`);
    });
}

function createWindow() {
    mainWindow = new BrowserWindow({
        width: 1200,
        height: 800,
        minWidth: 1000,
        minHeight: 600,
        backgroundColor: '#0f1115',
        webPreferences: {
            nodeIntegration: true,
            contextIsolation: false,
        },
        autoHideMenuBar: true,
    });

    if (isDev) {
        // Wait for the React dev server to start
        mainWindow.loadURL('http://localhost:5173');
        mainWindow.webContents.openDevTools({ mode: 'detach' });
    } else {
        // In production, load the built React app
        mainWindow.loadFile(path.join(__dirname, '../dist/index.html'));
    }

    mainWindow.on('closed', () => {
        mainWindow = null;
    });
}

app.whenReady().then(() => {
    startPythonServer();
    createWindow();

    app.on('activate', () => {
        if (BrowserWindow.getAllWindows().length === 0) {
            createWindow();
        }
    });
});

// Kill the python process when the electron app is killed
app.on('window-all-closed', () => {
    if (pythonProcess) {
        pythonProcess.kill();
    }
    if (process.platform !== 'darwin') {
        app.quit();
    }
});
