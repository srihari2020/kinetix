"""
tray_icon.py – Enhanced system tray icon for Kinetix PC Server v2.0.

Menu items:
  • Server IP (notification)
  • Connected Devices (submenu)
  • Start With Windows (toggle)
  • Restart Server
  • Exit
"""

from __future__ import annotations

import sys
import threading
import winreg
from typing import Callable, List, Optional

from PIL import Image, ImageDraw
import pystray
from pystray import MenuItem as Item

from logger import get_logger

log = get_logger("tray")

_AUTOSTART_KEY = r"Software\Microsoft\Windows\CurrentVersion\Run"
_AUTOSTART_VALUE = "KinetixServer"


def _create_icon_image(size: int = 64) -> Image.Image:
    """Generate a gamepad icon in-memory."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    pad = 2
    draw.ellipse([pad, pad, size - pad, size - pad], fill="#E94560")
    bx1, by1 = int(size * 0.18), int(size * 0.30)
    bx2, by2 = int(size * 0.82), int(size * 0.70)
    draw.rounded_rectangle([bx1, by1, bx2, by2], radius=6, fill="#FFFFFF")
    dot_r = int(size * 0.06)
    lcx, lcy = int(size * 0.35), int(size * 0.50)
    rcx, rcy = int(size * 0.65), int(size * 0.50)
    draw.ellipse([lcx - dot_r, lcy - dot_r, lcx + dot_r, lcy + dot_r], fill="#E94560")
    draw.ellipse([rcx - dot_r, rcy - dot_r, rcx + dot_r, rcy + dot_r], fill="#E94560")
    return img


# ------------------------------------------------------------------ #
#  Autostart helpers                                                  #
# ------------------------------------------------------------------ #


def _is_autostart_enabled() -> bool:
    try:
        key = winreg.OpenKey(winreg.HKEY_CURRENT_USER, _AUTOSTART_KEY, 0, winreg.KEY_READ)
        winreg.QueryValueEx(key, _AUTOSTART_VALUE)
        winreg.CloseKey(key)
        return True
    except FileNotFoundError:
        return False
    except Exception:
        return False


def _set_autostart(enable: bool) -> None:
    try:
        key = winreg.OpenKey(winreg.HKEY_CURRENT_USER, _AUTOSTART_KEY, 0, winreg.KEY_SET_VALUE)
        if enable:
            exe = sys.executable if not getattr(sys, "frozen", False) else sys.argv[0]
            winreg.SetValueEx(key, _AUTOSTART_VALUE, 0, winreg.REG_SZ, f'"{exe}"')
            log.info("Autostart enabled: %s", exe)
        else:
            try:
                winreg.DeleteValue(key, _AUTOSTART_VALUE)
            except FileNotFoundError:
                pass
            log.info("Autostart disabled")
        winreg.CloseKey(key)
    except Exception as exc:
        log.warning("Failed to set autostart: %s", exc)


# ------------------------------------------------------------------ #
#  TrayIcon                                                           #
# ------------------------------------------------------------------ #


class TrayIcon:
    """System tray icon with enhanced menu."""

    def __init__(
        self,
        ip_address: str,
        ws_port: int = 8765,
        udp_port: int = 5743,
        on_quit: Optional[Callable[[], None]] = None,
        on_restart: Optional[Callable[[], None]] = None,
        get_devices: Optional[Callable[[], list]] = None,
    ) -> None:
        self.ip_address = ip_address
        self.ws_port = ws_port
        self.udp_port = udp_port
        self._on_quit = on_quit or (lambda: None)
        self._on_restart = on_restart
        self._get_devices = get_devices or (lambda: [])
        self._icon: Optional[pystray.Icon] = None

    # ── Public API ────────────────────────────────────────────────────

    def run(self) -> None:
        """Block the calling thread to run the tray icon message loop."""
        menu = pystray.Menu(
            Item(
                f"Kinetix v2.0  ·  {self.ip_address}",
                None,
                enabled=False,
            ),
            pystray.Menu.SEPARATOR,
            Item("Show Server IP", self._show_ip),
            Item("Connected Devices", pystray.Menu(self._build_devices_menu)),
            pystray.Menu.SEPARATOR,
            Item(
                "Start With Windows",
                self._toggle_autostart,
                checked=lambda item: _is_autostart_enabled(),
            ),
            Item(
                "Restart Server",
                self._restart,
                visible=self._on_restart is not None,
            ),
            pystray.Menu.SEPARATOR,
            Item("Quit", self._quit),
        )

        self._icon = pystray.Icon(
            name="KinetixServer",
            icon=_create_icon_image(),
            title=f"Kinetix Server – {self.ip_address}:{self.ws_port}",
            menu=menu,
        )
        self._icon.run()

    def run_detached(self) -> threading.Thread:
        t = threading.Thread(target=self.run, daemon=True, name="tray-icon")
        t.start()
        return t

    def stop(self) -> None:
        if self._icon is not None:
            self._icon.stop()

    # ── Menu callbacks ────────────────────────────────────────────────

    def _show_ip(self, icon, item):
        if self._icon is not None:
            self._icon.notify(
                f"WebSocket: ws://{self.ip_address}:{self.ws_port}\n"
                f"UDP Input: {self.ip_address}:{self.udp_port}",
                title="Kinetix Server",
            )

    def _build_devices_menu(self) -> list:
        """Dynamically build the devices submenu."""
        devices = self._get_devices()
        if not devices:
            return [Item("No devices connected", None, enabled=False)]
        items = []
        for dev in devices:
            hz = f"{dev.packets_per_sec():.0f} Hz"
            label = f"Player {dev.player_index + 1} – {dev.device_name} ({hz})"
            items.append(Item(label, None, enabled=False))
        return items

    def _toggle_autostart(self, icon, item):
        _set_autostart(not _is_autostart_enabled())

    def _restart(self, icon, item):
        if self._on_restart:
            self._on_restart()

    def _quit(self, icon, item):
        self._on_quit()
        self.stop()
