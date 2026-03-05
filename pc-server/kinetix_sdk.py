from typing import Dict, Any

class KinetixPlugin:
    """Base class for all Kinetix PC Server plugins."""
    
    @property
    def name(self) -> str:
        return "Unnamed Plugin"
        
    @property
    def version(self) -> str:
        return "1.0.0"

    def on_load(self) -> None:
        """Called when the plugin is loaded."""
        pass
        
    def on_unload(self) -> None:
        """Called when the plugin is unloaded."""
        pass
        
    def on_input(self, player_index: int, state: Dict[str, Any]) -> Dict[str, Any]:
        """
        Intercept and modify controller input state.
        `state` is the parsed dictionary (JSON or binary).
        Return the modified state, or the original if no changes.
        """
        return state

    def on_network_connect(self, device_id: str, ip: str) -> None:
        """Called when a device connects."""
        pass
        
    def get_ui_components(self) -> Dict[str, Any]:
        """Return UI config if the plugin has configuration panels."""
        return {}
