"""
tray_icon.py – System tray icon for Kinetix PC Server.

Uses pystray + Pillow to show a gamepad-style tray icon with a
right-click menu: Show IP, Restart Server, Quit.
"""

import threading
from typing import Callable, Optional

from PIL import Image, ImageDraw, ImageFont
import pystray
from pystray import MenuItem as Item


def _create_icon_image(size: int = 64) -> Image.Image:
    """Generate a simple gamepad icon in-memory (no .ico file needed)."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Background circle
    pad = 2
    draw.ellipse([pad, pad, size - pad, size - pad], fill="#E94560")

    # Inner body (rounded rectangle imitating a controller)
    bx1, by1 = int(size * 0.18), int(size * 0.30)
    bx2, by2 = int(size * 0.82), int(size * 0.70)
    draw.rounded_rectangle([bx1, by1, bx2, by2], radius=6, fill="#FFFFFF")

    # Two "joystick" dots
    dot_r = int(size * 0.06)
    lcx, lcy = int(size * 0.35), int(size * 0.50)
    rcx, rcy = int(size * 0.65), int(size * 0.50)
    draw.ellipse([lcx - dot_r, lcy - dot_r, lcx + dot_r, lcy + dot_r], fill="#E94560")
    draw.ellipse([rcx - dot_r, rcy - dot_r, rcx + dot_r, rcy + dot_r], fill="#E94560")

    return img


class TrayIcon:
    """Manages the system tray icon lifecycle."""

    def __init__(
        self,
        ip_address: str,
        port: int,
        on_quit: Callable[[], None],
        on_restart: Optional[Callable[[], None]] = None,
    ) -> None:
        self.ip_address = ip_address
        self.port = port
        self._on_quit = on_quit
        self._on_restart = on_restart
        self._icon: Optional[pystray.Icon] = None

    # ── Public API ────────────────────────────────────────────────────

    def run(self) -> None:
        """Block the calling thread to run the tray icon message loop."""
        menu = pystray.Menu(
            Item(
                f"Kinetix  ·  ws://{self.ip_address}:{self.port}",
                None,
                enabled=False,
            ),
            pystray.Menu.SEPARATOR,
            Item("Show Server IP", self._show_ip),
            Item("Restart Server", self._restart, visible=self._on_restart is not None),
            pystray.Menu.SEPARATOR,
            Item("Quit", self._quit),
        )

        self._icon = pystray.Icon(
            name="KinetixServer",
            icon=_create_icon_image(),
            title=f"Kinetix Server – {self.ip_address}:{self.port}",
            menu=menu,
        )
        self._icon.run()

    def run_detached(self) -> threading.Thread:
        """Start the tray icon on a daemon thread and return the thread."""
        t = threading.Thread(target=self.run, daemon=True, name="tray-icon")
        t.start()
        return t

    def stop(self) -> None:
        if self._icon is not None:
            self._icon.stop()

    # ── Menu callbacks ────────────────────────────────────────────────

    def _show_ip(self, icon, item):  # noqa: ARG002
        """Show a desktop notification with the LAN IP."""
        if self._icon is not None:
            self._icon.notify(
                f"ws://{self.ip_address}:{self.port}",
                title="Kinetix Server IP",
            )

    def _restart(self, icon, item):  # noqa: ARG002
        if self._on_restart:
            self._on_restart()

    def _quit(self, icon, item):  # noqa: ARG002
        self._on_quit()
        self.stop()
