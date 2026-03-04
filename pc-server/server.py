#!/usr/bin/env python3
"""
Kinetix PC Server
=================
Async WebSocket server that receives controller-state JSON packets from the
Kinetix Android app and feeds them into a virtual Xbox 360 controller via
ViGEmBus.

Usage:
    python server.py [--host 0.0.0.0] [--port 8765] [--no-tray]
"""

import argparse
import asyncio
import json
import os
import signal
import socket
import sys
import threading
import time

import websockets

from controller_mapper import ControllerMapper

# ------------------------------------------------------------------ #
#  PyInstaller helper                                                  #
# ------------------------------------------------------------------ #

def resource_path(relative: str) -> str:
    """Return the absolute path to a bundled resource (works both in
    dev and when frozen by PyInstaller)."""
    base = getattr(sys, "_MEIPASS", os.path.dirname(os.path.abspath(__file__)))
    return os.path.join(base, relative)


# ------------------------------------------------------------------ #
#  Globals                                                            #
# ------------------------------------------------------------------ #

mapper: ControllerMapper | None = None
_server_stop_event: asyncio.Event | None = None


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


# ------------------------------------------------------------------ #
#  WebSocket handler                                                  #
# ------------------------------------------------------------------ #

async def handler(websocket):
    """Handle a single controller client connection."""
    global mapper

    remote = websocket.remote_address
    print(f"\n[+] Controller connected: {remote[0]}:{remote[1]}")

    # Create a fresh virtual controller for this session
    if mapper is None:
        mapper = ControllerMapper()

    packets = 0
    t_start = time.monotonic()

    try:
        async for message in websocket:
            try:
                packet = json.loads(message)
                mapper.update(packet)
                packets += 1

                # Periodic throughput log (every ~5 s)
                elapsed = time.monotonic() - t_start
                if elapsed >= 5.0:
                    hz = packets / elapsed
                    print(f"    ↳ {hz:.0f} packets/s from {remote[0]}", end="\r")
                    packets = 0
                    t_start = time.monotonic()

            except json.JSONDecodeError:
                print(f"[!] Bad JSON from {remote[0]}")
            except Exception as exc:
                print(f"[!] Error processing packet: {exc}")

    except websockets.exceptions.ConnectionClosed:
        pass
    finally:
        print(f"\n[-] Controller disconnected: {remote[0]}:{remote[1]}")


# ------------------------------------------------------------------ #
#  Server lifecycle                                                   #
# ------------------------------------------------------------------ #

async def serve(host: str, port: int, stop_event: asyncio.Event) -> None:
    """Run the WebSocket server until *stop_event* is set."""
    global mapper

    async with websockets.serve(handler, host, port):
        await stop_event.wait()

    # Cleanup
    if mapper is not None:
        mapper.close()
        mapper = None

    print("\n[*] Server stopped.")


def _run_server_thread(host: str, port: int, stop_event: asyncio.Event) -> None:
    """Entry point for the server background thread."""
    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)
    try:
        loop.run_until_complete(serve(host, port, stop_event))
    finally:
        loop.close()


# ------------------------------------------------------------------ #
#  Main (with optional tray)                                          #
# ------------------------------------------------------------------ #

def main_with_tray(host: str, port: int) -> None:
    """Launch the server on a background thread and run the system tray
    icon on the main thread (required by pystray on Windows)."""
    from tray_icon import TrayIcon

    local_ip = get_local_ip()
    stop_event = asyncio.Event()

    # Print banner to the console / log
    _print_banner(host, port, local_ip)

    # Start server in background thread
    loop = asyncio.new_event_loop()

    def server_thread():
        asyncio.set_event_loop(loop)
        loop.run_until_complete(serve(host, port, stop_event))
        loop.close()

    srv_t = threading.Thread(target=server_thread, daemon=True, name="ws-server")
    srv_t.start()

    # Quit callback for tray
    def on_quit():
        loop.call_soon_threadsafe(stop_event.set)

    # Run tray on main thread (blocks until Quit)
    tray = TrayIcon(ip_address=local_ip, port=port, on_quit=on_quit)
    try:
        tray.run()
    except KeyboardInterrupt:
        on_quit()

    srv_t.join(timeout=3)
    print("[*] Bye!")


def main_headless(host: str, port: int) -> None:
    """Run the server in console-only mode (no tray)."""
    local_ip = get_local_ip()
    _print_banner(host, port, local_ip)

    stop_event = asyncio.Event()

    # Graceful shutdown on Ctrl+C / SIGINT
    def _signal_handler():
        if not stop_event.is_set():
            stop_event.set()

    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)

    try:
        loop.add_signal_handler(signal.SIGINT, _signal_handler)
    except NotImplementedError:
        pass  # Windows — falls back to KeyboardInterrupt

    try:
        loop.run_until_complete(serve(host, port, stop_event))
    except KeyboardInterrupt:
        stop_event.set()
        loop.run_until_complete(serve(host, port, stop_event))
    finally:
        loop.close()


# ------------------------------------------------------------------ #
#  Helpers                                                            #
# ------------------------------------------------------------------ #

def _print_banner(host: str, port: int, local_ip: str) -> None:
    print("=" * 52)
    print("  🎮  Kinetix PC Server")
    print("=" * 52)
    print(f"  Listening on  ws://{host}:{port}")
    print(f"  LAN address   ws://{local_ip}:{port}")
    print(f"  Enter this IP in the Android app to connect.")
    print("=" * 52)
    print("  Press Ctrl+C or use the tray icon to stop.\n")


# ------------------------------------------------------------------ #
#  Entry point                                                        #
# ------------------------------------------------------------------ #

def parse_args():
    parser = argparse.ArgumentParser(description="Kinetix PC Server")
    parser.add_argument("--host", default="0.0.0.0", help="Bind address (default 0.0.0.0)")
    parser.add_argument("--port", type=int, default=8765, help="Port (default 8765)")
    parser.add_argument(
        "--no-tray",
        action="store_true",
        help="Run in console-only mode without the system tray icon",
    )
    return parser.parse_args()


if __name__ == "__main__":
    args = parse_args()
    try:
        if args.no_tray:
            main_headless(args.host, args.port)
        else:
            main_with_tray(args.host, args.port)
    except KeyboardInterrupt:
        print("\n[*] Interrupted. Bye!")
        sys.exit(0)
