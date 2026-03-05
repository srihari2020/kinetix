const { app, BrowserWindow } = require('electron');
const path = require('path');
const { spawn } = require('child_process');

let mainWindow;
let pythonProcess = null;

const isDev = !app.isPackaged;

function startPythonServer() {
    const pythonPath = 'python'; // or 'python3' based on the system

    // In dev, the server is at ../pc-server/server.py relative to the electron app
    // In prod, you'd typically bundle it with PyInstaller or copy the folder
    const serverScript = isDev
        ? path.join(__dirname, '../../pc-server/server.py')
        : path.join(process.resourcesPath, 'pc-server/server.py'); // Assuming it's copied to resources

    console.log('Starting Python server at:', serverScript);

    // Pass --no-tray flag to avoid double system tray icons if any
    pythonProcess = spawn(pythonPath, [serverScript, '--no-tray']);

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
