"""
controller_mapper.py – Translates controller input into ViGEmBus
virtual Xbox 360 controller commands via the vgamepad library.

Supports both JSON dict input (from WebSocket) and binary-packed
input (from UDP) for multi-gamepad sessions managed by DeviceManager.
"""

from __future__ import annotations

import struct
from typing import Optional

import vgamepad as vg

from logger import get_logger

log = get_logger("mapper")


# ------------------------------------------------------------------ #
#  D-pad / button maps                                                #
# ------------------------------------------------------------------ #

_DPAD_MAP = {
    "up":         vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_UP,
    "down":       vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_DOWN,
    "left":       vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_LEFT,
    "right":      vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_RIGHT,
    "up-left":    vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_UP | vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_LEFT,
    "up-right":   vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_UP | vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_RIGHT,
    "down-left":  vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_DOWN | vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_LEFT,
    "down-right": vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_DOWN | vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_RIGHT,
}

_BUTTON_MAP = {
    "a":      vg.XUSB_BUTTON.XUSB_GAMEPAD_A,
    "b":      vg.XUSB_BUTTON.XUSB_GAMEPAD_B,
    "x":      vg.XUSB_BUTTON.XUSB_GAMEPAD_X,
    "y":      vg.XUSB_BUTTON.XUSB_GAMEPAD_Y,
    "lb":     vg.XUSB_BUTTON.XUSB_GAMEPAD_LEFT_SHOULDER,
    "rb":     vg.XUSB_BUTTON.XUSB_GAMEPAD_RIGHT_SHOULDER,
    "start":  vg.XUSB_BUTTON.XUSB_GAMEPAD_START,
    "select": vg.XUSB_BUTTON.XUSB_GAMEPAD_BACK,
    "ls":     vg.XUSB_BUTTON.XUSB_GAMEPAD_LEFT_THUMB,
    "rs":     vg.XUSB_BUTTON.XUSB_GAMEPAD_RIGHT_THUMB,
}

# Binary button bitfield order (bit 0 = A, bit 1 = B, …)
_BUTTON_BITS = [
    vg.XUSB_BUTTON.XUSB_GAMEPAD_A,
    vg.XUSB_BUTTON.XUSB_GAMEPAD_B,
    vg.XUSB_BUTTON.XUSB_GAMEPAD_X,
    vg.XUSB_BUTTON.XUSB_GAMEPAD_Y,
    vg.XUSB_BUTTON.XUSB_GAMEPAD_LEFT_SHOULDER,
    vg.XUSB_BUTTON.XUSB_GAMEPAD_RIGHT_SHOULDER,
    vg.XUSB_BUTTON.XUSB_GAMEPAD_START,
    vg.XUSB_BUTTON.XUSB_GAMEPAD_BACK,
    vg.XUSB_BUTTON.XUSB_GAMEPAD_LEFT_THUMB,
    vg.XUSB_BUTTON.XUSB_GAMEPAD_RIGHT_THUMB,
]

# Binary d-pad lookup (value → vgamepad flag)
_DPAD_BINARY = {
    0: None,  # none
    1: vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_UP,
    2: vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_DOWN,
    3: vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_LEFT,
    4: vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_RIGHT,
    5: vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_UP | vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_LEFT,
    6: vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_UP | vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_RIGHT,
    7: vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_DOWN | vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_LEFT,
    8: vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_DOWN | vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_RIGHT,
}

# Binary packet format (little-endian):
#   B  player_id   (uint8)
#   B  sequence    (uint8)
#   h  lx          (int16  → -32768..32767)
#   h  ly          (int16)
#   h  rx          (int16)
#   h  ry          (int16)
#   H  lt          (uint16 → 0..65535)
#   H  rt          (uint16)
#   H  buttons     (uint16, bitfield)
#   B  dpad        (uint8)
# Total = 17 bytes minimum
_BINARY_FMT = "<BBhhhhHHHB"
_BINARY_SIZE = struct.calcsize(_BINARY_FMT)  # 17 bytes


# ------------------------------------------------------------------ #
#  Public API                                                         #
# ------------------------------------------------------------------ #

def update_from_json(gamepad: vg.VX360Gamepad, packet: dict) -> None:
    """Apply a full JSON controller-state *packet* to *gamepad*."""
    gamepad.reset()

    # Sticks
    gamepad.left_joystick_float(
        x_value_float=_clamp(packet.get("lx", 0.0)),
        y_value_float=_clamp(packet.get("ly", 0.0)),
    )
    gamepad.right_joystick_float(
        x_value_float=_clamp(packet.get("rx", 0.0)),
        y_value_float=_clamp(packet.get("ry", 0.0)),
    )

    # Triggers
    gamepad.left_trigger_float(value_float=_clamp01(packet.get("lt", 0.0)))
    gamepad.right_trigger_float(value_float=_clamp01(packet.get("rt", 0.0)))

    # Buttons
    for key, btn in _BUTTON_MAP.items():
        if packet.get(key, False):
            gamepad.press_button(button=btn)

    # D-pad
    dpad_btn = _DPAD_MAP.get(packet.get("dpad", "none"))
    if dpad_btn is not None:
        gamepad.press_button(button=dpad_btn)

    gamepad.update()


def parse_binary(data: bytes) -> Optional[tuple[int, dict]]:
    """Unpack a binary UDP input packet.

    Returns ``(player_index, values_dict)`` or ``None`` on error.
    The *values_dict* has the same keys used by `update_from_binary`.
    """
    if len(data) < _BINARY_SIZE:
        return None
    try:
        player_id, seq, lx, ly, rx, ry, lt, rt, buttons, dpad = struct.unpack_from(
            _BINARY_FMT, data
        )
    except struct.error:
        return None

    return player_id, {
        "seq": seq, "lx": lx, "ly": ly, "rx": rx, "ry": ry,
        "lt": lt, "rt": rt,
        "buttons": buttons, "dpad": dpad,
    }


def update_from_binary(gamepad: vg.VX360Gamepad, vals: dict) -> None:
    """Apply pre-parsed binary values to *gamepad*."""
    gamepad.reset()

    # Sticks (int16 → float)
    gamepad.left_joystick_float(
        x_value_float=_i16_to_float(vals["lx"]),
        y_value_float=_i16_to_float(vals["ly"]),
    )
    gamepad.right_joystick_float(
        x_value_float=_i16_to_float(vals["rx"]),
        y_value_float=_i16_to_float(vals["ry"]),
    )

    # Triggers (uint16 → float 0..1)
    gamepad.left_trigger_float(value_float=vals["lt"] / 65535.0)
    gamepad.right_trigger_float(value_float=vals["rt"] / 65535.0)

    # Buttons bitfield
    btn_bits = vals["buttons"]
    for i, vg_btn in enumerate(_BUTTON_BITS):
        if btn_bits & (1 << i):
            gamepad.press_button(button=vg_btn)

    # D-pad
    dpad_btn = _DPAD_BINARY.get(vals["dpad"])
    if dpad_btn is not None:
        gamepad.press_button(button=dpad_btn)

    gamepad.update()


# ------------------------------------------------------------------ #
#  Helpers                                                            #
# ------------------------------------------------------------------ #

def _clamp(v: float, lo: float = -1.0, hi: float = 1.0) -> float:
    return max(lo, min(hi, float(v)))


def _clamp01(v: float) -> float:
    return max(0.0, min(1.0, float(v)))


def _i16_to_float(v: int) -> float:
    """Convert a signed 16-bit int (-32768..32767) to float -1..1."""
    return max(-1.0, min(1.0, v / 32767.0))
