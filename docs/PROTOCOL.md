# Protocol Specification v2.0

## Channels

Kinetix v2.0 uses **three** network channels:

| Channel | Transport | Port | Purpose |
|---|---|---|---|
| Control | WebSocket | 8765 | Registration, settings, rumble |
| Input   | UDP       | 5743 | Real-time controller state (120 Hz) |
| Discovery | UDP broadcast | 5742 | LAN auto-discovery |

---

## Discovery (UDP port 5742)

Server broadcasts every 2 seconds:

```
KINETIX_SERVER:<ip>:<ws_port>:<udp_port>
```

Example: `KINETIX_SERVER:192.168.1.5:8765:5743`

---

## Control Channel (WebSocket port 8765)

### Registration

```json
// Phone → Server
{"type": "register", "device_id": "uuid-string", "device_name": "Pixel 7"}

// Server → Phone (success)
{"type": "assigned", "player": 0, "udp_port": 5743, "status": "ok"}

// Server → Phone (slots full)
{"type": "assigned", "player": -1, "status": "full", "message": "All 4 controller slots are occupied."}
```

### Rumble Events

```json
// Server → Phone
{"type": "rumble", "player": 0, "small_motor": 128, "large_motor": 200, "duration_ms": 200}
```

### Settings Sync

```json
// Phone → Server
{"type": "settings", "gyro_enabled": true, "profile": "fps"}
```

### JSON Fallback Input

When UDP is unavailable, input can be sent as JSON over WebSocket (same format as v1.0):

```json
{"type": "input", "lx": 0.5, "ly": -0.3, "a": true, "dpad": "up"}
```

---

## Input Channel (UDP port 5743)

Binary-packed, little-endian, 17 bytes per packet at 120 Hz:

| Offset | Type | Field | Description |
|---|---|---|---|
| 0 | uint8 | player_id | Player slot (0–3) |
| 1 | uint8 | sequence | Wrapping counter |
| 2–3 | int16 | lx | Left stick X (-32768…32767) |
| 4–5 | int16 | ly | Left stick Y |
| 6–7 | int16 | rx | Right stick X |
| 8–9 | int16 | ry | Right stick Y |
| 10–11 | uint16 | lt | Left trigger (0…65535) |
| 12–13 | uint16 | rt | Right trigger |
| 14–15 | uint16 | buttons | Button bitfield |
| 16 | uint8 | dpad | D-pad direction |

### Button Bitfield

| Bit | Button |
|---|---|
| 0 | A |
| 1 | B |
| 2 | X |
| 3 | Y |
| 4 | LB |
| 5 | RB |
| 6 | Start |
| 7 | Select |
| 8 | LS (left stick click) |
| 9 | RS (right stick click) |

### D-pad Values

| Value | Direction |
|---|---|
| 0 | none |
| 1 | up |
| 2 | down |
| 3 | left |
| 4 | right |
| 5 | up-left |
| 6 | up-right |
| 7 | down-left |
| 8 | down-right |

---

## Connection Flow

```
1. Phone starts → listens for KINETIX_SERVER broadcasts (port 5742)
2. User selects server → phone opens WebSocket to ws://<ip>:8765
3. Phone sends {"type": "register", ...}
4. Server responds {"type": "assigned", "player": 0, "udp_port": 5743}
5. Phone starts UDP input at 120 Hz → <ip>:5743
6. Server maps UDP packets to virtual Xbox controller via ViGEmBus
7. Server sends rumble events back via WebSocket
```
