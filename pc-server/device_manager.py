"""
device_manager.py – Multi-controller device manager.

Manages up to 4 simultaneous controller connections.  Each phone is
assigned a unique player slot (0–3) backed by its own virtual Xbox 360
gamepad via ViGEmBus / vgamepad.
"""

from __future__ import annotations

import time
from dataclasses import dataclass, field
from typing import Callable, Dict, Optional

import vgamepad as vg

from logger import get_logger

log = get_logger("device_mgr")

MAX_PLAYERS = 4


# ------------------------------------------------------------------ #
#  Device record                                                      #
# ------------------------------------------------------------------ #

@dataclass
class DeviceInfo:
    """Metadata for one connected controller device."""
    device_id: str
    device_name: str
    ip_address: str
    player_index: int  # 0-based

    connected_at: float = field(default_factory=time.time)
    last_packet_at: float = field(default_factory=time.time)
    packet_count: int = 0

    def packets_per_sec(self) -> float:
        elapsed = time.time() - self.connected_at
        return self.packet_count / elapsed if elapsed > 0 else 0.0


# ------------------------------------------------------------------ #
#  Manager                                                            #
# ------------------------------------------------------------------ #

class DeviceManager:
    """Allocates player slots and virtual gamepads for connected devices."""

    def __init__(
        self,
        on_device_changed: Optional[Callable[[], None]] = None,
    ) -> None:
        self._gamepads: list[Optional[vg.VX360Gamepad]] = [None] * MAX_PLAYERS
        self._devices: list[Optional[DeviceInfo]] = [None] * MAX_PLAYERS
        self._id_to_slot: Dict[str, int] = {}
        self._on_device_changed = on_device_changed

    # ── Query ─────────────────────────────────────────────────────────

    @property
    def connected_devices(self) -> list[DeviceInfo]:
        """Return a list of all currently connected devices."""
        return [d for d in self._devices if d is not None]

    def get_device(self, player_index: int) -> Optional[DeviceInfo]:
        if 0 <= player_index < MAX_PLAYERS:
            return self._devices[player_index]
        return None

    def get_gamepad(self, player_index: int) -> Optional[vg.VX360Gamepad]:
        if 0 <= player_index < MAX_PLAYERS:
            return self._gamepads[player_index]
        return None

    def slot_for_device(self, device_id: str) -> Optional[int]:
        return self._id_to_slot.get(device_id)

    @property
    def player_count(self) -> int:
        return len(self._id_to_slot)

    # ── Registration ──────────────────────────────────────────────────

    def register(
        self, device_id: str, device_name: str, ip_address: str
    ) -> Optional[int]:
        """Assign a player slot to *device_id*.  Returns the 0-based
        slot index, or ``None`` if all 4 slots are occupied.

        If the device is already registered, returns its existing slot.
        """
        # Already connected?
        if device_id in self._id_to_slot:
            slot = self._id_to_slot[device_id]
            log.info("Device %s already in slot %d – reconnecting", device_id, slot)
            info = self._devices[slot]
            if info is not None:
                info.ip_address = ip_address
                info.connected_at = time.time()
                info.last_packet_at = time.time()
                info.packet_count = 0
            return slot

        # Find a free slot
        for idx in range(MAX_PLAYERS):
            if self._devices[idx] is None:
                return self._allocate(idx, device_id, device_name, ip_address)
        log.warning("All %d slots occupied – rejecting %s", MAX_PLAYERS, device_id)
        return None

    def unregister(self, device_id: str) -> None:
        """Free the slot occupied by *device_id*."""
        slot = self._id_to_slot.pop(device_id, None)
        if slot is None:
            return
        gp = self._gamepads[slot]
        if gp is not None:
            try:
                gp.reset()
                gp.update()
            except Exception:
                pass
            self._gamepads[slot] = None
        dev = self._devices[slot]
        log.info(
            "Player %d (%s) disconnected after %d packets",
            slot + 1,
            dev.device_name if dev else "?",
            dev.packet_count if dev else 0,
        )
        self._devices[slot] = None
        self._notify()

    def unregister_all(self) -> None:
        for did in list(self._id_to_slot.keys()):
            self.unregister(did)

    # ── Input ─────────────────────────────────────────────────────────

    def record_packet(self, player_index: int) -> None:
        """Bump the packet counter for a player (call after update)."""
        dev = self._devices[player_index] if 0 <= player_index < MAX_PLAYERS else None
        if dev is not None:
            dev.packet_count += 1
            dev.last_packet_at = time.time()

    # ── Private ───────────────────────────────────────────────────────

    def _allocate(
        self, slot: int, device_id: str, name: str, ip: str
    ) -> int:
        gp = vg.VX360Gamepad()
        self._gamepads[slot] = gp
        self._devices[slot] = DeviceInfo(
            device_id=device_id,
            device_name=name,
            ip_address=ip,
            player_index=slot,
        )
        self._id_to_slot[device_id] = slot
        log.info("Player %d assigned → %s (%s)", slot + 1, name, ip)
        self._notify()
        return slot

    def _notify(self) -> None:
        if self._on_device_changed:
            try:
                self._on_device_changed()
            except Exception:
                pass
