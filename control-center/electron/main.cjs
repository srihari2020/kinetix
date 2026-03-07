const { app, BrowserWindow } = require('electron');
const path = require('path');
const { spawn, exec } = require('child_process');
const net = require('net');

function checkServer(callback) {
    const socket = new net.Socket();
    const timeout = 1000;

    socket.setTimeout(timeout);

    socket.on('connect', () => {
        console.log("Server already running (Port 8765 is in use). Connecting to existing instance.");
        socket.destroy();
        callback(true);
    });

    socket.on('timeout', () => {
        socket.destroy();
        callback(false);
    });

    socket.on('error', (err) => {
        socket.destroy();
        callback(false);
    });

    socket.connect(8765, '127.0.0.1');
}

let mainWindow;
let pythonProcess = null;
let serverStartedByUs = false;

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
        title: "Kinetix Control Center"
    });

    if (process.env.NODE_ENV === "development") {
        // Wait for the React dev server to start
        mainWindow.loadURL("http://localhost:5173");
        mainWindow.webContents.openDevTools({ mode: 'detach' });
    } else {
        // In production, load the built React app
        mainWindow.loadFile(path.join(__dirname, "../dist/index.html"));
    }

    mainWindow.on('closed', () => {
        mainWindow = null;
    });
}

app.whenReady().then(() => {
    checkServer((isRunning) => {
        if (!isRunning) {
            serverStartedByUs = true;
            startPythonServer();
        }
        createWindow();
    });

    app.on('activate', () => {
        if (BrowserWindow.getAllWindows().length === 0) {
            createWindow();
        }
    });
});

app.on('before-quit', () => {
    if (serverStartedByUs && pythonProcess) {
        pythonProcess.kill();
    }
});

// Kill the python process when the electron app is killed
app.on('window-all-closed', () => {
    if (serverStartedByUs && pythonProcess) {
        pythonProcess.kill();
    }
    if (process.platform !== 'darwin') {
        app.quit();
    }
});
