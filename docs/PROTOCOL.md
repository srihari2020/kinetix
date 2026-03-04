# Protocol Specification

## Transport

- **Protocol:** WebSocket (RFC 6455)
- **Default port:** `8765`
- **Direction:** Android → PC (unidirectional)
- **Rate:** ~60 packets per second (16 ms interval)

## Packet Format

Each WebSocket text frame contains a JSON object:

```json
{
  "lx":  0.0,
  "ly":  0.0,
  "rx":  0.0,
  "ry":  0.0,
  "a":   false,
  "b":   false,
  "x":   false,
  "y":   false,
  "lb":  false,
  "rb":  false,
  "lt":  0.0,
  "rt":  0.0,
  "start":  false,
  "select": false,
  "ls":  false,
  "rs":  false,
  "dpad": "none"
}
```

## Field Reference

| Field | Type | Range | Description |
|---|---|---|---|
| `lx` | float | -1.0 … 1.0 | Left stick horizontal axis |
| `ly` | float | -1.0 … 1.0 | Left stick vertical axis |
| `rx` | float | -1.0 … 1.0 | Right stick horizontal axis |
| `ry` | float | -1.0 … 1.0 | Right stick vertical axis |
| `a` | bool | | A button (green) |
| `b` | bool | | B button (red) |
| `x` | bool | | X button (blue) |
| `y` | bool | | Y button (yellow) |
| `lb` | bool | | Left bumper |
| `rb` | bool | | Right bumper |
| `lt` | float | 0.0 … 1.0 | Left trigger (analog) |
| `rt` | float | 0.0 … 1.0 | Right trigger (analog) |
| `start` | bool | | Start button |
| `select` | bool | | Select / Back button |
| `ls` | bool | | Left stick click |
| `rs` | bool | | Right stick click |
| `dpad` | string | see below | D-pad direction |

### D-pad Values

`"none"`, `"up"`, `"down"`, `"left"`, `"right"`, `"up-left"`, `"up-right"`, `"down-left"`, `"down-right"`

## Notes

- All fields are optional — missing fields default to `0` / `false` / `"none"`.
- Axis conventions: positive `lx`/`rx` = right, positive `ly`/`ry` = up.
- The server calls `gamepad.reset()` before each update, so every packet is a
  full snapshot (not a delta).
