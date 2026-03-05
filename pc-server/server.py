#!/usr/bin/env python3
"""
Kinetix PC Server v2.0
======================
Hybrid networking server for the Kinetix phone-to-PC controller system.

*  **WebSocket** (port 8765) – control channel: registration, settings,
   rumble forwarding, and JSON-fallback input.
*  **UDP** (port 5743) – real-time binary controller input at 120 Hz.
*  **UDP broadcast** (port 5742) – LAN auto-discovery.

Supports up to 4 simultaneous controllers via ViGEmBus.

Usage:
    python server.py [--host 0.0.0.0] [--ws-port 8765] [--udp-port 5743] [--no-tray]
"""

from __future__ import annotations

import argparse
import asyncio
import json
import os
import signal
import socket
import sys
import threading
import time
from typing import Dict, Optional, Set

import websockets

from controller_mapper import update_from_json, parse_binary, update_from_binary
from device_manager import DeviceManager
from discovery import DiscoveryBroadcaster
from logger import get_logger, setup_logging
from plugin_manager import plugin_mgr
import webrtc_server

# ------------------------------------------------------------------ #
#  Globals                                                            #
# ------------------------------------------------------------------ #

log = get_logger("server")

device_mgr: DeviceManager | None = None
discovery: DiscoveryBroadcaster | None = None
webrtc_mgr: webrtc_server.WebRTCManager | None = None

# WebSocket → device_id mapping for cleanup
_ws_to_device: Dict[websockets.WebSocketServerProtocol, str] = {}

# Set of active WebSocket connections (for broadcasting rumble etc.)
_ws_clients: Set[websockets.WebSocketServerProtocol] = set()

# ------------------------------------------------------------------ #
#  Network helpers                                                    #
# ------------------------------------------------------------------ #


def get_local_ip() -> str:
    """Return the machine's LAN IP address (best-effort)."""
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "127.0.0.1"


def resource_path(relative: str) -> str:
    """PyInstaller-compatible resource path resolver."""
    base = getattr(sys, "_MEIPASS", os.path.dirname(os.path.abspath(__file__)))
    return os.path.join(base, relative)


# ------------------------------------------------------------------ #
#  WebSocket control channel                                          #
# ------------------------------------------------------------------ #


async def ws_handler(websocket):
    """Handle a single WebSocket client (control channel)."""
    remote = websocket.remote_address
    log.info("WebSocket connected: %s:%s", remote[0], remote[1])
    _ws_clients.add(websocket)

    try:
        async for message in websocket:
            try:
                packet = json.loads(message)
                await _process_ws_message(websocket, packet, remote)
            except json.JSONDecodeError:
                log.warning("Bad JSON from %s", remote[0])
            except Exception as exc:
                log.error("Error processing WS message: %s", exc)
    except websockets.exceptions.ConnectionClosed:
        pass
    finally:
        _ws_clients.discard(websocket)
        # Unregister device if it was registered through this WS
        device_id = _ws_to_device.pop(websocket, None)
        if device_id and device_mgr:
            device_mgr.unregister(device_id)
        log.info("WebSocket disconnected: %s:%s", remote[0], remote[1])


async def _process_ws_message(websocket, packet: dict, remote) -> None:
    """Route a parsed WebSocket JSON message."""
    msg_type = packet.get("type", "")

    if msg_type == "register":
        await _handle_register(websocket, packet, remote)
    elif msg_type == "settings":
        _handle_settings(packet)
    elif msg_type == "input":
        # JSON-fallback input (slower path for compat)
        _handle_json_input(packet)
    elif msg_type == "webrtc_offer":
        if webrtc_mgr:
            player_index = packet.get("player", 0)
            await webrtc_mgr.handle_offer(websocket, packet, player_index)
    else:
        # Legacy / bare input packet (no "type" field)
        if "lx" in packet or "a" in packet:
            _handle_json_input(packet)
        else:
            log.debug("Unknown WS message type: %s", msg_type)


async def _handle_register(websocket, packet: dict, remote) -> None:
    """Device registration: assign a player slot."""
    device_id = packet.get("device_id", f"unknown-{remote[0]}")
    device_name = packet.get("device_name", "Unknown Device")
    ip = remote[0]

    slot = device_mgr.register(device_id, device_name, ip)
    _ws_to_device[websocket] = device_id

    if slot is not None:
        plugin_mgr.notify_network_connect(device_id, ip)
        response = {
            "type": "assigned",
            "player": slot,  # 0-based
            "udp_port": _udp_port,
            "status": "ok",
        }
        log.info(
            "Registered %s as Player %d (device_id=%s)",
            device_name, slot + 1, device_id
        )
    else:
        response = {
            "type": "assigned",
            "player": -1,
            "status": "full",
            "message": "All 4 controller slots are occupied.",
        }
        log.warning("Registration rejected – slots full: %s", device_name)

    await websocket.send(json.dumps(response))


def _handle_settings(packet: dict) -> None:
    """Handle settings sync from a device."""
    log.info("Settings received: %s", packet)


def _handle_json_input(packet: dict) -> None:
    """Process a JSON controller-state packet (WebSocket fallback path)."""
    if device_mgr is None:
        return

    # Determine player slot from packet or use player 0
    device_id = packet.get("device_id")
    if device_id:
        slot = device_mgr.slot_for_device(device_id)
    else:
        slot = packet.get("player", 0)

    if slot is None:
        return

    # Process through plugins
    packet = plugin_mgr.process_input(slot, packet)

    gp = device_mgr.get_gamepad(slot)
    if gp is None:
        return

    update_from_json(gp, packet)
    device_mgr.record_packet(slot)
    
    try:
        import api_server
        api_server.push_live_update({"type": "input", "player": slot, "data": packet})
    except Exception:
        pass


def _on_webrtc_input(player_index: int, data: any, is_binary: bool) -> None:
    if device_mgr is None:
        return
        
    if is_binary:
        result = parse_binary(data)
        if result is None:
            return
        p_idx, vals = result
        seq = vals.get("seq", -1)
        vals = plugin_mgr.process_input(p_idx, vals)
        gp = device_mgr.get_gamepad(p_idx)
        if gp is None:
            return
        update_from_binary(gp, vals)
        device_mgr.record_packet(p_idx, seq)
    else:
        data = plugin_mgr.process_input(player_index, data)
        gp = device_mgr.get_gamepad(player_index)
        if gp is None:
            return
        update_from_json(gp, data)
        device_mgr.record_packet(player_index)
        
    try:
        import api_server
        api_server.push_live_update({"type": "input", "player": player_index, "data": vals if is_binary else data})
    except Exception:
        pass


# ------------------------------------------------------------------ #
#  Rumble forwarding                                                  #
# ------------------------------------------------------------------ #


async def send_rumble(player_index: int, small_motor: int, large_motor: int, duration_ms: int = 200) -> None:
    """Send a rumble event to the phone at *player_index*."""
    msg = json.dumps({
        "type": "rumble",
        "player": player_index,
        "small_motor": small_motor,
        "large_motor": large_motor,
        "duration_ms": duration_ms,
    })
    # Find the WS connection for this player's device
    if device_mgr is None:
        return
    dev = device_mgr.get_device(player_index)
    if dev is None:
        return
    for ws, did in _ws_to_device.items():
        if did == dev.device_id and ws in _ws_clients:
            try:
                await ws.send(msg)
            except Exception:
                pass
            break


# ------------------------------------------------------------------ #
#  UDP input server                                                   #
# ------------------------------------------------------------------ #


class UdpInputProtocol(asyncio.DatagramProtocol):
    """Asyncio protocol for receiving binary controller input via UDP."""

    def __init__(self) -> None:
        self._pkt_count = 0
        self._t_start = time.monotonic()

    def datagram_received(self, data: bytes, addr: tuple) -> None:
        if device_mgr is None:
            return

        result = parse_binary(data)
        if result is None:
            return

        player_index, vals = result
        seq = vals.get("seq", -1)
        
        # Process through plugins
        vals = plugin_mgr.process_input(player_index, vals)

        gp = device_mgr.get_gamepad(player_index)
        if gp is None:
            return

        update_from_binary(gp, vals)
        device_mgr.record_packet(player_index, seq)

        try:
            import api_server
            api_server.push_live_update({"type": "input", "player": player_index, "data": vals})
        except Exception:
            pass

        # Periodic throughput log
        self._pkt_count += 1
        elapsed = time.monotonic() - self._t_start
        if elapsed >= 5.0:
            hz = self._pkt_count / elapsed
            log.debug("UDP input: %.0f pkt/s", hz)
            try:
                import api_server
                api_server.update_network_stats({"packet_rate_in": hz})
            except Exception:
                pass
            self._pkt_count = 0
            self._t_start = time.monotonic()

    def error_received(self, exc: Exception) -> None:
        log.warning("UDP error: %s", exc)


# ------------------------------------------------------------------ #
#  Server lifecycle                                                   #
# ------------------------------------------------------------------ #

_udp_port: int = 5743
_ws_port: int = 8765

game_detector = None
telemetry_task = None

async def _telemetry_loop():
    """Periodically aggregates telemetry and sends to API server."""
    try:
        import api_server
    except Exception:
        return
        
    while True:
        await asyncio.sleep(0.5)
        if not device_mgr:
            continue
            
        devices = device_mgr.connected_devices
        if not devices:
            continue
            
        # Aggregate overall stats (avg jitter, sum rate, avg loss)
        total_rate = sum(d.packets_per_sec() for d in devices)
        avg_jitter = sum(d.jitter_ms for d in devices) / len(devices)
        avg_loss = sum(d.packet_loss_pct for d in devices) / len(devices)
        
        # We don't have true RTT without pinging, so we use jitter/2 as rough latency metric for UDP
        # Real WebRTC implementations would get this from getStats()
        avg_latency = avg_jitter / 2.0 + 2.0 # minimum baseline 2ms
        
        stats = {
            "latency_ms": avg_latency,
            "packet_rate_in": total_rate,
            "packet_loss_pct": avg_loss,
            "jitter_ms": avg_jitter
        }
        api_server.update_network_stats(stats)
        api_server.push_live_update({"type": "telemetry", "data": stats})


async def serve(host: str, ws_port: int, udp_port: int, stop_event: asyncio.Event) -> None:
    """Run both WebSocket and UDP servers until *stop_event* is set."""
    global device_mgr, discovery, _udp_port, _ws_port, game_detector
    _udp_port = udp_port
    _ws_port = ws_port

    local_ip = get_local_ip()

    # Device manager and plugins
    device_mgr = DeviceManager()
    plugin_mgr.load_plugins()

    global webrtc_mgr
    webrtc_mgr = webrtc_server.WebRTCManager(_on_webrtc_input)

    from game_detector import GameDetector
    def on_profile_switch(profile_data):
        msg = json.dumps({"type": "profile", "data": profile_data})
        for ws in list(_ws_clients):
            try:
                loop.call_soon_threadsafe(asyncio.create_task, ws.send(msg))
            except Exception:
                pass

    game_detector = GameDetector(on_profile_switch)
    game_detector.start()

    # Start API server for Control Center dashboard
    try:
        import api_server
        api_server.start_api_server_thread(port=8080)
        global telemetry_task
        telemetry_task = asyncio.create_task(_telemetry_loop())
    except ImportError:
        log.warning("FastAPI not installed; Control Center API disabled.")
    except Exception as exc:
        log.warning("Could not start API server: %s", exc)

    # Discovery broadcaster
    discovery = DiscoveryBroadcaster(local_ip, ws_port, udp_port)
    discovery.start()

    # UDP input server
    loop = asyncio.get_event_loop()
    transport, _protocol = await loop.create_datagram_endpoint(
        UdpInputProtocol,
        local_addr=(host, udp_port),
    )
    log.info("UDP input server listening on %s:%d", host, udp_port)

    # WebSocket server
    async with websockets.serve(ws_handler, host, ws_port):
        log.info("WebSocket server listening on %s:%d", host, ws_port)
        await stop_event.wait()

    # Cleanup
    if webrtc_mgr:
        await webrtc_mgr.cleanup()
    if game_detector:
        game_detector.stop()
    transport.close()
    discovery.stop()
    device_mgr.unregister_all()
    device_mgr = None
    plugin_mgr.unload_plugins()
    log.info("Server stopped")


def _run_server_thread(host: str, ws_port: int, udp_port: int, stop_event: asyncio.Event) -> asyncio.AbstractEventLoop:
    """Start the server on a background thread; returns the event loop."""
    loop = asyncio.new_event_loop()

    def thread_fn():
        asyncio.set_event_loop(loop)
        loop.run_until_complete(serve(host, ws_port, udp_port, stop_event))
        loop.close()

    t = threading.Thread(target=thread_fn, daemon=True, name="server")
    t.start()
    return loop


# ------------------------------------------------------------------ #
#  Entry points                                                       #
# ------------------------------------------------------------------ #


def main_with_tray(host: str, ws_port: int, udp_port: int) -> None:
    """Launch the server with a system tray icon (main thread)."""
    from tray_icon import TrayIcon

    setup_logging()
    local_ip = get_local_ip()
    _print_banner(host, ws_port, udp_port, local_ip)

    stop_event = asyncio.Event()
    loop = _run_server_thread(host, ws_port, udp_port, stop_event)

    def on_quit():
        loop.call_soon_threadsafe(stop_event.set)

    def get_devices():
        if device_mgr:
            return device_mgr.connected_devices
        return []

    tray = TrayIcon(
        ip_address=local_ip,
        ws_port=ws_port,
        udp_port=udp_port,
        on_quit=on_quit,
        get_devices=get_devices,
    )
    try:
        tray.run()
    except KeyboardInterrupt:
        on_quit()

    log.info("Bye!")


def main_headless(host: str, ws_port: int, udp_port: int) -> None:
    """Run in console-only mode (no tray)."""
    setup_logging()
    local_ip = get_local_ip()
    _print_banner(host, ws_port, udp_port, local_ip)

    stop_event = asyncio.Event()
    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)

    try:
        loop.add_signal_handler(signal.SIGINT, stop_event.set)
    except NotImplementedError:
        pass  # Windows fallback via KeyboardInterrupt

    try:
        loop.run_until_complete(serve(host, ws_port, udp_port, stop_event))
    except KeyboardInterrupt:
        stop_event.set()
        loop.run_until_complete(asyncio.sleep(0.5))
    finally:
        loop.close()


# ------------------------------------------------------------------ #
#  Helpers                                                            #
# ------------------------------------------------------------------ #


def _print_banner(host: str, ws_port: int, udp_port: int, local_ip: str) -> None:
    log.info("=" * 52)
    log.info("  🎮  Kinetix PC Server v2.0")
    log.info("=" * 52)
    log.info("  WebSocket  ws://%s:%d", local_ip, ws_port)
    log.info("  UDP Input  %s:%d", local_ip, udp_port)
    log.info("  Discovery  broadcast on port 5742")
    log.info("  Max players: 4")
    log.info("  Enter the IP in the Android app or use auto-discovery.")
    log.info("=" * 52)


# ------------------------------------------------------------------ #
#  CLI                                                                #
# ------------------------------------------------------------------ #


def parse_args():
    p = argparse.ArgumentParser(description="Kinetix PC Server v2.0")
    p.add_argument("--host", default="0.0.0.0", help="Bind address (default 0.0.0.0)")
    p.add_argument("--ws-port", type=int, default=8765, help="WebSocket port (default 8765)")
    p.add_argument("--udp-port", type=int, default=5743, help="UDP input port (default 5743)")
    p.add_argument("--no-tray", action="store_true", help="Console-only mode")
    return p.parse_args()


if __name__ == "__main__":
    args = parse_args()
    try:
        if args.no_tray:
            main_headless(args.host, args.ws_port, args.udp_port)
        else:
            main_with_tray(args.host, args.ws_port, args.udp_port)
    except KeyboardInterrupt:
        log.info("Interrupted – goodbye!")
        sys.exit(0)
