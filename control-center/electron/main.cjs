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
let serverProcess = null;
let serverStartedByUs = false;

function startServer() {
    try {
        if (!app.isPackaged) {
            const pythonPath = 'python'; // or 'python3' based on the system
            const serverScript = path.join(__dirname, '../../pc-server/server.py');
            console.log('Starting Python server at:', serverScript);
            serverProcess = spawn(pythonPath, [serverScript, '--no-tray']);
        } else {
            const serverExe = path.join(process.resourcesPath, 'server.exe');
            console.log('Starting standalone Python server at:', serverExe);
            serverProcess = spawn(serverExe, ['--no-tray']);
        }

        serverProcess.stdout.on('data', (data) => {
            console.log(`Python: ${data.toString()}`);
        });

        serverProcess.stderr.on('data', (data) => {
            console.error(`Python Error: ${data.toString()}`);
        });

        serverProcess.on('close', (code) => {
            console.log(`Python process exited with code ${code}`);
        });
    } catch (error) {
        console.error("Failed to start backend server:", error);
    }
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

    if (app.isPackaged) {
        // In production, load the built React app from local files
        mainWindow.loadFile(path.join(__dirname, "../dist/index.html"));
    } else {
        // In development, load the Vite dev server
        mainWindow.loadURL("http://localhost:5173");
        mainWindow.webContents.openDevTools({ mode: 'detach' });
    }

    mainWindow.on('closed', () => {
        mainWindow = null;
    });
}

const gotTheLock = app.requestSingleInstanceLock()

if (!gotTheLock) {
    app.quit()
} else {
    app.on('second-instance', (event, commandLine, workingDirectory) => {
        // Someone tried to run a second instance, we should focus our window.
        if (mainWindow) {
            if (mainWindow.isMinimized()) mainWindow.restore()
            mainWindow.focus()
        }
    })

    app.whenReady().then(() => {
        // Create the UI immediately so we don't show a long white/black screen
        createWindow();

        // Then check and start the backend server in the background
        checkServer((isRunning) => {
            if (!isRunning) {
                serverStartedByUs = true;
                startServer();
            }
        });

        app.on('activate', () => {
            if (BrowserWindow.getAllWindows().length === 0) {
                createWindow();
            }
        });
    });
}

// Ensure we fully terminate the spawned server process on exit to avoid
// "address already in use" (WinError 10048) issues on restart.
app.on('will-quit', () => {
    if (serverStartedByUs && serverProcess && serverProcess.pid && process.platform === 'win32') {
        const pid = serverProcess.pid;
        console.log(`Killing server process tree with taskkill /PID ${pid}`);
        try {
            exec(`taskkill /F /T /PID ${pid}`, (error, stdout, stderr) => {
                if (error) {
                    console.error('Failed to taskkill server process:', error);
                }
                if (stdout) console.log(stdout.toString());
                if (stderr) console.error(stderr.toString());
            });
        } catch (err) {
            console.error('Error while attempting to kill server process:', err);
        }
    } else if (serverStartedByUs && serverProcess) {
        // Fallback for non-Windows platforms
        serverProcess.kill();
    }
});

// Standard Electron behavior: quit when all windows are closed (except on macOS)
app.on('window-all-closed', () => {
    if (process.platform !== 'darwin') {
        app.quit();
    }
});
