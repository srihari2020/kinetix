# Kinetix Installer Build Instructions

You are ready to build your zero-dependency single-executable installer for Kinetix!

## Prerequisites setup

1. Ensure the compiled React web app is already in `dist/`.
2. Ensure you have the `server.exe` compiled in `../pc-server/dist/server.exe`.
3. **IMPORTANT**: Place the `ViGEmBus_Setup_x64.exe` natively downloaded from Nefarius into `c:\Projects\Kinetix\control-center\installer\ViGEmBus_Setup_x64.exe`.

*The `installer/` directory has been created for you.*

## Build Command

Run the following command in `c:\Projects\Kinetix\control-center`:

```cmd
npm run electron:build
```

**What will happen during the build:**
1. Electron-builder will compile the NSIS installer.
2. It will bundle `dist/`, `electron/`, the `server.exe`, the plugins, AND the `installer/ViGEmBus_Setup_x64.exe` inside it.
3. The custom `build/installer.nsh` script will be embedded during the installer creation.
4. Once completed, a `.exe` setup file will appear in the `release/` folder.

**When the user executes the Setup file:**
1. It requests admin privileges (needed to install drivers and place files natively).
2. It uses our NSIS script to dynamically read the registry for "ViGEm Bus Driver".
3. If missing, it silently (`/q /norestart`) installs the driver, which takes seconds.
4. It installs the UI, python app, etc. to `AppData/Local/Programs` or standard x64 location.
5. It runs the Kinetix Control Center executable.

**When Kinetix Starts:**
1. The UI boots immediately to avoid black screens and show loading state.
2. The Electron `checkServer` probes port `8765`.
3. If no duplicate server is running, the standalone Python `server.exe` launches safely using `process.resourcesPath`, with gracefully handled fallbacks if an error prevents it from running.
