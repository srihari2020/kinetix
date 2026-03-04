<div align="center">

# 🎮 Kinetix

**Turn your Android phone into a wireless Xbox 360 controller for Windows PC games.**

[![License: MIT](https://img.shields.io/badge/License-MIT-E94560.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Windows%20%2B%20Android-0F3460.svg)](#)
[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84.svg?logo=android&logoColor=white)](#)
[![Python](https://img.shields.io/badge/Python-3.10%2B-3776AB.svg?logo=python&logoColor=white)](#)

[Getting Started](#-getting-started) · [Architecture](#-architecture) · [Building](#-building) · [Screenshots](#-screenshots) · [Contributing](#-contributing)

</div>

---

## ✨ Features

| Feature | Status |
|---|---|
| Dual analog sticks | ✅ |
| A / B / X / Y face buttons | ✅ |
| LB / RB bumpers | ✅ |
| LT / RT analog triggers | ✅ |
| D-pad (8-way) | ✅ |
| Start / Select | ✅ |
| Dark gaming UI | ✅ |
| Haptic feedback (vibration) | ✅ |
| Auto-reconnect with backoff | ✅ |
| 60 Hz input rate | ✅ |
| Multi-touch support | ✅ |
| System tray icon | ✅ |
| Single-file EXE (no Python needed) | ✅ |
| Windows installer (Inno Setup) | ✅ |
| Gyro support | 🔜 |
| Server auto-discovery | 🔜 |
| Customisable button layout | 🔜 |

---

## 🏗 Architecture

```
┌──────────────────┐   WebSocket (JSON @ 60 Hz)   ┌─────────────────────┐
│  Android Phone   │  ──────────────────────────▸  │  PC Python Server   │
│  (Kinetix App)   │        Port 8765              │  (server.py)        │
└──────────────────┘                               └────────┬────────────┘
                                                            │ ViGEmBus
                                                   ┌───────▼────────────┐
                                                   │  Virtual Xbox 360  │
                                                   │    Controller      │
                                                   └───────┬────────────┘
                                                            │
                                                   ┌───────▼────────────┐
                                                   │     PC Games       │
                                                   └────────────────────┘
```

| Layer | Tech |
|---|---|
| Transport | WebSocket on port `8765` |
| Payload | JSON — axes, buttons, triggers, d-pad |
| Latency target | < 20 ms on local Wi-Fi |
| Android app | Kotlin · OkHttp · Custom Views |
| PC server | Python · websockets · vgamepad · pystray |
| Controller emulation | ViGEmBus virtual Xbox 360 gamepad |

> See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for a deeper dive.

---

## 📋 Prerequisites

| Requirement | Version | Notes |
|---|---|---|
| Windows 10/11 | x64 | For the PC server |
| [ViGEmBus](https://github.com/nefarius/ViGEmBus/releases) driver | Latest | Reboot after install |
| Python | 3.10+ | Only needed if running from source |
| Android Studio | 2023.1+ | For building the app |
| Android device | API 26+ (8.0) | |

---

## 🚀 Getting Started

### 1 · Install the ViGEmBus driver

1. Download the latest MSI from **[ViGEmBus Releases](https://github.com/nefarius/ViGEmBus/releases)**.
2. Run `ViGEmBus_Setup_x64.msi` → follow the prompts.
3. **Reboot** your PC.

### 2 · Start the PC server

**Option A — Pre-built EXE** (no Python needed):

Download `kinetix-server.exe` from the [Releases](../../releases) page and double-click it.

**Option B — Run from source**:

```powershell
cd pc-server
pip install -r requirements.txt
python server.py
```

The server prints your LAN IP:

```
====================================================
  🎮  Kinetix PC Server
====================================================
  Listening on  ws://0.0.0.0:8765
  LAN address   ws://192.168.1.42:8765
  Enter this IP in the Android app to connect.
====================================================
```

<details>
<summary><strong>Server flags</strong></summary>

| Flag | Description |
|---|---|
| `--host 0.0.0.0` | Bind address (default `0.0.0.0`) |
| `--port 8765` | Port (default `8765`) |
| `--no-tray` | Console-only mode (no system tray) |

</details>

### 3 · Install the Android app

Download the APK from the [Releases](../../releases) page, or build it yourself (see [Building](#-building)).

### 4 · Connect

1. Ensure both devices are on the **same Wi-Fi network**.
2. Open **Kinetix** on your phone.
3. Enter the server IP → tap **CONNECT**.
4. Play! 🎮

> **Verify:** Windows Settings → Devices → *"Set up USB game controllers"* → the virtual Xbox 360 pad should appear.

---

## 🔨 Building

### Android APK

```bash
# Linux / macOS
scripts/build-android.sh

# Or manual
cd android-controller
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
```

### Windows EXE

```powershell
# One-click
scripts\build-server.bat

# Or manual
cd pc-server
pip install -r requirements.txt
pyinstaller kinetix-server.spec
# → dist\kinetix-server.exe
```

### Windows Installer (optional)

1. Build `kinetix-server.exe` first.
2. Place `ViGEmBus_Setup_x64.msi` in `installer/`.
3. Open `installer/kinetix-installer.iss` in [Inno Setup](https://jrsoftware.org/isinfo.php) → **Compile**.
4. Output: `installer/output/KinetixServerSetup.exe`.

---

## 📸 Screenshots

<!-- Add screenshots here -->
<!-- ![Connection Screen](docs/screenshots/connect.png) -->
<!-- ![Controller Screen](docs/screenshots/controller.png) -->

*Screenshots coming soon — PRs welcome!*

---

## 📦 Project Structure

```
kinetix/
├── android-controller/            # Android Studio project (Kotlin)
│   ├── app/
│   │   ├── build.gradle.kts
│   │   └── src/main/
│   │       ├── AndroidManifest.xml
│   │       └── java/com/kinetix/controller/
│   │           ├── MainActivity.kt
│   │           ├── ControllerActivity.kt
│   │           ├── ControllerView.kt
│   │           ├── JoystickView.kt
│   │           ├── ControllerState.kt
│   │           └── WebSocketClient.kt
│   ├── build.gradle.kts
│   └── settings.gradle.kts
│
├── pc-server/                     # Python WebSocket server
│   ├── server.py
│   ├── controller_mapper.py
│   ├── tray_icon.py
│   ├── requirements.txt
│   ├── build.bat
│   └── kinetix-server.spec
│
├── installer/                     # Inno Setup installer
│   └── kinetix-installer.iss
│
├── scripts/                       # Build helper scripts
│   ├── build-android.sh
│   └── build-server.bat
│
├── docs/                          # Documentation
│   ├── ARCHITECTURE.md
│   └── PROTOCOL.md
│
├── .github/workflows/             # CI / CD
│   ├── android-build.yml
│   ├── windows-build.yml
│   └── release.yml
│
├── .gitignore
├── CONTRIBUTING.md
├── CODE_OF_CONDUCT.md
├── LICENSE
└── README.md
```

---

## 🔌 Protocol

Each packet is a JSON object sent at ~60 Hz:

```json
{
  "lx": 0.0,  "ly": 0.0,
  "rx": 0.0,  "ry": 0.0,
  "a": false,  "b": false,
  "x": false,  "y": false,
  "lb": false, "rb": false,
  "lt": 0.0,   "rt": 0.0,
  "start": false, "select": false,
  "dpad": "none"
}
```

> See [docs/PROTOCOL.md](docs/PROTOCOL.md) for the full specification.

---

## 🐛 Troubleshooting

| Issue | Fix |
|---|---|
| `vgamepad` install fails | Make sure ViGEmBus driver is installed and PC is rebooted |
| App can't connect | Same Wi-Fi? Firewall allows port 8765? |
| High latency | Use 5 GHz band · reduce distance to router |
| Controller not detected in games | Check *"Set up USB game controllers"* in Windows |

---

## 🤝 Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a PR.

---

## 📄 License

This project is licensed under the **MIT License** — see [LICENSE](LICENSE) for details.

---

<div align="center">
<sub>Made with ❤️ by the Kinetix contributors</sub>
</div>
