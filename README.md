# 🎮 Kinetix

**Turn your Android phone into a wireless Xbox controller for PC games.**

Kinetix is a professional phone-to-PC controller system that transforms any Android phone into a full Xbox 360 gamepad — complete with analog sticks, triggers, bumpers, face buttons, d-pad, gyroscope aiming, and haptic feedback.

## ✨ Features

| Feature | Description |
|---|---|
| **Full Xbox Layout** | Dual analog sticks, A/B/X/Y, LB/RB, LT/RT, D-pad, Start/Select |
| **120 Hz Input** | Binary UDP packets for ultra-low latency |
| **4 Players** | Connect up to 4 phones simultaneously |
| **Auto Discovery** | Server automatically found on LAN — no IP needed |
| **Gyroscope Aiming** | Tilt phone to aim (right stick mapping) |
| **Vibration Feedback** | Rumble events forwarded from games to phone |
| **Controller Profiles** | Default, FPS Mode, Driving Mode (customizable) |
| **Custom Layout** | Drag buttons to reposition |
| **System Tray** | Server runs silently with tray icon |
| **Battery Indicator** | See phone battery while playing |
| **Start With Windows** | Optional autostart on boot |

## 📦 Components

```
kinetix/
├── android-controller/     Android app (Kotlin)
├── pc-server/              PC server (Python)
├── installer/              Windows installer (Inno Setup)
├── docs/                   Architecture & protocol docs
├── scripts/                Build scripts
└── README.md
```

## 🚀 Quick Start

### PC Server

1. Install [ViGEmBus driver](https://github.com/ViGEm/ViGEmBus/releases)
2. Install Python 3.10+
3. Run the server:
   ```bash
   cd pc-server
   pip install -r requirements.txt
   python server.py
   ```
4. The server shows your LAN IP — or the Android app finds it automatically.

### Android App

1. Open the project in Android Studio
2. Build and install on your phone
3. Launch Kinetix — it discovers the server automatically
4. Tap the server to connect
5. Play!

### Windows Installer

A one-click installer is available:

1. Build the server: `cd pc-server && build.bat`
2. Compile installer: open `installer/kinetix-installer.iss` in [Inno Setup](https://jrsoftware.org/isinfo.php)
3. Distribute `KinetixServerSetup.exe`

## 🌐 Network Ports

| Port | Protocol | Purpose |
|---|---|---|
| 5742 | UDP broadcast | Auto-discovery |
| 5743 | UDP | Controller input (120 Hz) |
| 8765 | WebSocket | Control channel |

## 🏗 Architecture

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the full system diagram.

## 📡 Protocol

See [docs/PROTOCOL.md](docs/PROTOCOL.md) for the binary packet format and WebSocket message types.

## 🎯 Requirements

**PC:**
- Windows 10/11 (64-bit)
- ViGEmBus driver installed
- Python 3.10+ (or use the standalone .exe)

**Android:**
- Android 7.0+ (API 24)
- Target SDK 34 (Android 14 compatible)
- Same Wi-Fi network as PC

## 🤝 Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## 📄 License

MIT License — see [LICENSE](LICENSE).
