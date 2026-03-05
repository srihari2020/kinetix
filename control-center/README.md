# Kinetix Control Center

This is the Electron-based React dashboard for Kinetix 2.0.
It bundles the Python backend server and provides a graphical UI for monitoring connections, gamepad inputs, network latency, and server logs.

## Development

First, ensure you have the Python dependencies installed:
```bash
python -m pip install -r ../pc-server/requirements.txt
```

To run the React Dashboard and Electron App simultaneously (with auto-reload for React):
```bash
npm install
npm run electron:start
```

## Production Build

To package the Kinetix Control Center into a standalone `.exe` file (bundling the UI and the Python server):
```bash
npm run electron:build
```
The output executable will be placed in the `release/` directory.

> Note: The packaged app assumes `python` or `python3` is available in the user's system PATH. For a fully portable distribution, consider building the python server with `pyinstaller` before packaging with electron-builder, and updating `electron/main.cjs` to spawn the generated executable instead.
