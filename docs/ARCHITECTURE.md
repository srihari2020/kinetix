# Architecture

## Overview

Kinetix is a two-component system:

1. **Android Controller App** — runs on the phone, renders a virtual gamepad,
   and sends input state over WebSocket.
2. **PC Server** — receives packets, translates them into virtual Xbox 360
   controller inputs via ViGEmBus.

## Data Flow

```
                    Wi-Fi (same LAN)
  Android App  ─────────────────────────▸  PC Server
  ┌────────────────┐                      ┌──────────────────────────┐
  │ Touch events   │                      │ WebSocket handler        │
  │       ↓        │  JSON @ 60 Hz        │       ↓                  │
  │ JoystickView   │ ──────────────────▸  │ controller_mapper.py     │
  │ ControllerView │    Port 8765         │       ↓                  │
  │       ↓        │                      │ vgamepad (ViGEmBus API)  │
  │ WebSocketClient│                      │       ↓                  │
  └────────────────┘                      │ Virtual Xbox 360 gamepad │
                                          └──────────────────────────┘
                                                    ↓
                                              PC Games (XInput)
```

## Android App

| Class | Responsibility |
|---|---|
| `MainActivity` | Connection screen — IP input, saved preferences |
| `ControllerActivity` | Full-screen landscape host, 60 FPS send loop |
| `JoystickView` | Custom analog stick with dead-zone and glow effects |
| `ControllerView` | Face buttons, bumpers, triggers, d-pad — multi-touch |
| `WebSocketClient` | OkHttp WebSocket with exponential-backoff reconnect |
| `ControllerState` | Immutable data class matching the JSON schema |

## PC Server

| Module | Responsibility |
|---|---|
| `server.py` | Async WebSocket server, tray icon integration, CLI |
| `controller_mapper.py` | Translates JSON packets → ViGEmBus API calls |
| `tray_icon.py` | pystray system tray icon with menu |

## Threading Model (PC)

```
Main Thread          Background Thread
───────────         ──────────────────
pystray event       asyncio event loop
loop (blocking)     ├─ websockets.serve()
                    └─ handler() per client
```

`pystray` requires the main thread on Windows. The WebSocket server runs in a
daemon thread with its own `asyncio` event loop. Shutdown is coordinated via
`asyncio.Event`.

## Protocol

See [PROTOCOL.md](PROTOCOL.md).
