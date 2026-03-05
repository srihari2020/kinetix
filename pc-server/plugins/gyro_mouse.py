from typing import Dict, Any
from kinetix_sdk import KinetixPlugin

class GyroMousePlugin(KinetixPlugin):
    """
    Translates gyroscope input (which normally maps to rx, ry or similar) 
    into PC mouse movements if enabled.
    For this example, we assume gyro sends inputs in 'gyro_x' and 'gyro_y'
    or overrides the right stick (rx, ry). We'll watch for 'rx' and 'ry'.
    """
    
    def __init__(self):
        super().__init__()
        self.enabled = False
        
    @property
    def name(self) -> str:
        return "Gyro Mouse"
        
    @property
    def version(self) -> str:
        return "1.0.0"

    def on_load(self) -> None:
        print("[GyroMouse] Plugin loaded! (Not moving mouse for real in this example skeleton)")
        # In a real plugin, we would import `pyautogui` or `pynput` here.
        
    def on_input(self, player_index: int, state: Dict[str, Any]) -> Dict[str, Any]:
        """Intercept input and turn right-stick into mouse moving if enabled."""
        if not self.enabled:
            return state
            
        # Example logic: if plugin enabled, read 'rx' and 'ry' to move mouse,
        # and zero them out so they don't move the gamepad's camera.
        rx = state.get("rx", 0)
        ry = state.get("ry", 0)
        
        if rx != 0 or ry != 0:
            # e.g., move_mouse(rx * MOUSE_SENSITIVITY, -ry * MOUSE_SENSITIVITY)
            pass
            
        # Zero out right stick to prevent conflicting game camera movement
        state["rx"] = 0
        state["ry"] = 0
            
        return state
        
    def get_ui_components(self) -> Dict[str, Any]:
        return {
            "type": "toggle",
            "label": "Enable Gyro Mouse",
            "state_key": "enabled"
        }
