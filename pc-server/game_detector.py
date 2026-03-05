import time
import threading
import psutil
from typing import Optional

from logger import get_logger
from profile_manager import profile_mgr

log = get_logger("game_detector")

class GameDetector:
    """Polls the active Windows foreground window to auto-switch controller profiles."""
    
    def __init__(self, callback):
        self.callback = callback
        self.running = False
        self.thread: Optional[threading.Thread] = None

    def start(self):
        if self.running: return
        self.running = True
        self.thread = threading.Thread(target=self._poll_loop, daemon=True, name="game_detector")
        self.thread.start()
        log.info("Game detector started.")

    def stop(self):
        self.running = False
        if self.thread:
            self.thread.join(timeout=1.0)
        log.info("Game detector stopped.")

    def _get_foreground_process_name(self) -> str:
        try:
            import ctypes
            # Get handle to the foreground window
            hwnd = ctypes.windll.user32.GetForegroundWindow()
            if not hwnd:
                return ""
            
            # Get the process ID
            pid = ctypes.c_ulong()
            ctypes.windll.user32.GetWindowThreadProcessId(hwnd, ctypes.byref(pid))
            
            # Get process name via psutil
            if pid.value > 0:
                p = psutil.Process(pid.value)
                return p.name()
        except Exception:
            pass
        return ""

    def _poll_loop(self):
        last_exe = ""
        while self.running:
            exe_name = self._get_foreground_process_name()
            if exe_name and exe_name != last_exe:
                last_exe = exe_name
                mapped_profile = profile_mgr.check_game_mapping(exe_name)
                if mapped_profile:
                    log.info(f"Detected game {exe_name}, switching to profile {mapped_profile}")
                    profile_mgr.set_active_profile(mapped_profile)
                    self.callback(profile_mgr.get_active_profile())
            time.sleep(2.0)
