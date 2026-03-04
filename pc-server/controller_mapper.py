"""
controller_mapper.py – Translates JSON controller packets into ViGEmBus
virtual Xbox 360 controller inputs via the vgamepad library.
"""

import vgamepad as vg


# Map d-pad string values to vgamepad DPAD button constants
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

# Map JSON key → vgamepad button constant for face/shoulder buttons
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


class ControllerMapper:
    """Manages a single virtual Xbox 360 controller."""

    def __init__(self) -> None:
        self.gamepad = vg.VX360Gamepad()
        print("[ViGEm] Virtual Xbox 360 controller created.")

    # ------------------------------------------------------------------ #
    #  Public API                                                         #
    # ------------------------------------------------------------------ #

    def update(self, packet: dict) -> None:
        """
        Apply a full controller-state packet to the virtual gamepad.

        Expected keys (all optional – missing keys keep previous state):
            lx, ly          – left stick  (-1.0 … 1.0)
            rx, ry          – right stick (-1.0 … 1.0)
            lt, rt          – triggers    (0.0 … 1.0)
            a, b, x, y      – face buttons  (bool)
            lb, rb           – bumpers       (bool)
            start, select    – menu buttons  (bool)
            ls, rs           – stick clicks  (bool)
            dpad             – "up" | "down" | "left" | "right" |
                               "up-left" | … | "none"
        """
        self.gamepad.reset()

        # --- Analog sticks (float -1 … 1) ---
        self.gamepad.left_joystick_float(
            x_value_float=_clamp(packet.get("lx", 0.0)),
            y_value_float=_clamp(packet.get("ly", 0.0)),
        )
        self.gamepad.right_joystick_float(
            x_value_float=_clamp(packet.get("rx", 0.0)),
            y_value_float=_clamp(packet.get("ry", 0.0)),
        )

        # --- Triggers (float 0 … 1) ---
        self.gamepad.left_trigger_float(
            value_float=max(0.0, min(1.0, packet.get("lt", 0.0)))
        )
        self.gamepad.right_trigger_float(
            value_float=max(0.0, min(1.0, packet.get("rt", 0.0)))
        )

        # --- Face / shoulder / menu buttons ---
        for key, btn in _BUTTON_MAP.items():
            if packet.get(key, False):
                self.gamepad.press_button(button=btn)

        # --- D-pad ---
        dpad = packet.get("dpad", "none")
        dpad_btn = _DPAD_MAP.get(dpad)
        if dpad_btn is not None:
            self.gamepad.press_button(button=dpad_btn)

        # --- Commit ---
        self.gamepad.update()

    def close(self) -> None:
        """Release the virtual gamepad."""
        try:
            self.gamepad.reset()
            self.gamepad.update()
        except Exception:
            pass
        print("[ViGEm] Virtual controller released.")


# ------------------------------------------------------------------ #
#  Helpers                                                            #
# ------------------------------------------------------------------ #

def _clamp(value: float, lo: float = -1.0, hi: float = 1.0) -> float:
    """Clamp *value* between *lo* and *hi*."""
    return max(lo, min(hi, float(value)))
