"""
discovery.py – LAN auto-discovery for Kinetix PC Server.

Broadcasts ``KINETIX_SERVER:<ip>:<ws_port>:<udp_port>`` on UDP port
5742 every 2 seconds so that Android clients can discover the server
without manual IP entry.
"""

from __future__ import annotations

import socket
import threading
import time

from logger import get_logger

log = get_logger("discovery")

DISCOVERY_PORT = 5742
BROADCAST_INTERVAL = 2.0  # seconds


class DiscoveryBroadcaster:
    """Periodically broadcasts the server's presence on the LAN."""

    def __init__(
        self,
        server_ip: str,
        ws_port: int = 8765,
        udp_port: int = 5743,
    ) -> None:
        self._message = f"KINETIX_SERVER:{server_ip}:{ws_port}:{udp_port}".encode()
        self._running = False
        self._thread: threading.Thread | None = None

    def start(self) -> None:
        if self._running:
            return
        self._running = True
        self._thread = threading.Thread(
            target=self._broadcast_loop, daemon=True, name="discovery"
        )
        self._thread.start()
        log.info(
            "Discovery broadcaster started on UDP port %d", DISCOVERY_PORT
        )

    def stop(self) -> None:
        self._running = False
        if self._thread is not None:
            self._thread.join(timeout=3)
            self._thread = None
        log.info("Discovery broadcaster stopped")

    # ── Private ───────────────────────────────────────────────────────

    def _broadcast_loop(self) -> None:
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
        sock.settimeout(1.0)

        while self._running:
            try:
                sock.sendto(self._message, ("<broadcast>", DISCOVERY_PORT))
            except OSError as exc:
                log.warning("Broadcast send failed: %s", exc)
            time.sleep(BROADCAST_INTERVAL)

        sock.close()
