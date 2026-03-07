# Architecture v2.0

## Overview

Kinetix is a professional phone-to-PC controller system with three components:

1. **Android Controller App** – renders a virtual Xbox gamepad, sends input via UDP at 120 Hz
2. **PC Server** – receives input and translates it into virtual Xbox 360 controllers via ViGEmBus
3. **Networking Layer** – hybrid UDP + WebSocket with LAN auto-discovery

## Data Flow

```
                        Wi-Fi (same LAN)
  Android App  ─────────────────────────────▸  PC Server
  ┌─────────────────────┐                     ┌────────────────────────────┐
  │ Touch / Gyro input  │                     │ Discovery broadcaster      │
  │        ↓            │  UDP broadcast      │   (port 5742, every 2s)    │
  │ JoystickView        │ ◂─────────────────  │                            │
  │ ControllerView      │                     │ WebSocket control channel  │
  │ GyroscopeManager    │  WebSocket ◂──────▸ │   (port 8765)              │
  │        ↓            │  register/rumble    │   ↕ DeviceManager (4 slots)│
  │ UdpInputSender      │                     │                            │
  │   (120 Hz binary)   │  UDP ────────────▸  │ UDP input server           │
  │        ↓            │  port 5743          │   (port 5743)              │
  │ WebSocketClient     │                     │   ↓ controller_mapper.py   │
  │ ServerDiscovery     │                     │   ↓ vgamepad (ViGEmBus)    │
  └─────────────────────┘                     │   ↓ Virtual Xbox 1–4      │
                                              └────────────────────────────┘
                                                        ↓
                                                  PC Games (XInput)
```

## Android App

| Class | Responsibility |
|---|---|
| `MainActivity` | Connection screen – auto-discovery list, manual IP, profile selector |
| `ControllerActivity` | Full-screen controller host, 120 Hz send loop, gyro, vibration |
| `JoystickView` | Custom analog stick with dead-zone and glow effects |
| `ControllerView` | Face buttons, bumpers, triggers, d-pad – multi-touch |
| `WebSocketClient` | OkHttp WebSocket for control channel (registration, rumble) |
| `UdpInputSender` | Binary UDP sender at 120 Hz on dedicated thread |
| `ServerDiscovery` | UDP broadcast listener for LAN auto-discovery |
| `GyroscopeManager` | Gyroscope sensor → right stick mapping |
| `ControllerProfile` | Settings persistence (gyro, vibration, send rate, layout) |
| `SettingsActivity` | UI for profile/settings management |
| `ControllerState` | Immutable data class matching the input schema |

## PC Server

| Module | Responsibility |
|---|---|
| `server.py` | Hybrid WebSocket + UDP server, registration, rumble forwarding |
| `controller_mapper.py` | Translates JSON or binary input → ViGEmBus API |
| `device_manager.py` | Multi-controller slot management (4 virtual gamepads) |
| `discovery.py` | UDP broadcast for LAN auto-discovery |
| `tray_icon.py` | System tray with device list, autostart, IP display |
| `logger.py` | Rotating file + console logging |

## Threading Model (PC)

```
Main Thread              Background Thread (asyncio)
───────────              ──────────────────────────
pystray event loop       ├─ websockets.serve() (port 8765)
(blocking)               ├─ UdpInputProtocol (port 5743)
                         └─ handler() per WS client

                         Discovery Thread
                         ──────────────────
                         UDP broadcast loop (port 5742, every 2s)
```

## Protocol

See [PROTOCOL.md](PROTOCOL.md).
