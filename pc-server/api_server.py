"""
api_server.py – FastAPI integration for the Kinetix Control Center dashboard.

Exposes REST APIs and a WebSocket for real-time monitoring of the Python backend.
Starts via uvicorn in a separate thread.
"""

import asyncio
import json
import threading
from typing import Any, Dict, List

import uvicorn
from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware

from logger import get_logger, mem_handler
import server as main_server

log = get_logger("api_server")

app = FastAPI(title="Kinetix Control Center API")

# Allow CORS for development (React UI from Vite)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ------------------------------------------------------------------ #
#  State & Broadcasting                                               #
# ------------------------------------------------------------------ #

class BroadcastState:
    def __init__(self):
        self.active_connections: List[WebSocket] = []
        self.network_stats = {
            "latency_ms": 0.0,
            "packet_rate_in": 0.0,
            "packet_loss_pct": 0.0
        }

    async def connect(self, websocket: WebSocket):
        await websocket.accept()
        self.active_connections.append(websocket)

    def disconnect(self, websocket: WebSocket):
        if websocket in self.active_connections:
            self.active_connections.remove(websocket)

    async def broadcast(self, message: str):
        for connection in list(self.active_connections):
            try:
                await connection.send_text(message)
            except Exception:
                self.disconnect(connection)

state = BroadcastState()

# We need a cross-thread mechanism to push real-time data from server.py's
# asyncio loop to the api_server's asyncio loop.
_api_loop: asyncio.AbstractEventLoop | None = None

def push_live_update(data: dict) -> None:
    """Called by server.py to push real-time updates to UI WebSockets."""
    if _api_loop and not _api_loop.is_closed() and state.active_connections:
        asyncio.run_coroutine_threadsafe(
            state.broadcast(json.dumps(data)), _api_loop
        )

def update_network_stats(stats: dict) -> None:
    """Updates the shared network statistics."""
    state.network_stats.update(stats)

# ------------------------------------------------------------------ #
#  Endpoints                                                          #
# ------------------------------------------------------------------ #

@app.get("/status")
def get_status() -> Dict[str, Any]:
    return {
        "status": "running",
        "ip": main_server.get_local_ip(),
        "ws_port": getattr(main_server, "_ws_port", 8765),
        "udp_port": getattr(main_server, "_udp_port", 5743),
        "api_port": 8080,
    }

@app.get("/devices")
def get_devices() -> List[Dict[str, Any]]:
    if not main_server.device_mgr:
        return []
    
    devices = []
    for d in main_server.device_mgr.connected_devices:
        devices.append({
            "device_id": d.device_id,
            "device_name": d.device_name,
            "ip_address": d.ip_address,
            "player_index": d.player_index,
            "packets_per_sec": float(d.packets_per_sec()),
        })
    return devices

@app.get("/network")
def get_network() -> Dict[str, Any]:
    return state.network_stats

@app.get("/logs")
def get_logs() -> List[str]:
    # Return the captured logs from memory handler
    return list(mem_handler.logs)

@app.post("/shutdown")
def shutdown_server() -> Dict[str, str]:
    import os
    import signal
    import threading
    def kill_soon():
        import time
        time.sleep(0.5)
        # Windows compatibility or generic SIGINT
        try:
            os.kill(os.getpid(), signal.CTRL_C_EVENT)
        except AttributeError:
            os.kill(os.getpid(), signal.SIGINT)
            
    threading.Thread(target=kill_soon).start()
    return {"status": "shutting down"}

@app.websocket("/ws/live")
async def websocket_endpoint(websocket: WebSocket):
    global _api_loop
    if _api_loop is None:
        _api_loop = asyncio.get_running_loop()
        
    await state.connect(websocket)
    try:
        while True:
            # We just keep connection open, client doesn't send much
            data = await websocket.receive_text()
    except WebSocketDisconnect:
        state.disconnect(websocket)


# ------------------------------------------------------------------ #
#  Lifecycle                                                          #
# ------------------------------------------------------------------ #

def run_api_server(port: int = 8080):
    """Starts the FastAPI server (blocking). Intended to be run in a thread."""
    log.info("Starting API Server on port %d", port)
    uvicorn.run(app, host="127.0.0.1", port=port, log_level="warning")

def start_api_server_thread(port: int = 8080) -> threading.Thread:
    """Returns the started thread for the API server."""
    t = threading.Thread(target=run_api_server, args=(port,), daemon=True, name="api_server")
    t.start()
    return t
