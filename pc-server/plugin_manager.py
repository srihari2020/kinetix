import os
import sys
import importlib
import inspect
from typing import List, Dict, Any

from logger import get_logger
from kinetix_sdk import KinetixPlugin

log = get_logger("plugins")

class PluginManager:
    """Discovers and manages lifecycle of Kinetix SDK plugins."""
    
    def __init__(self, plugins_dir: str = "plugins"):
        self.plugins_dir = plugins_dir
        self.plugins: List[KinetixPlugin] = []
        
    def load_plugins(self):
        """Discovers and loads all plugins from the plugins directory."""
        if getattr(sys, 'frozen', False):
            # Running as compiled PyInstaller executable
            base_path = sys._MEIPASS
        else:
            base_path = os.getcwd()
            
        full_plugins_dir = os.path.join(base_path, self.plugins_dir)

        if not os.path.exists(full_plugins_dir):
            os.makedirs(full_plugins_dir, exist_ok=True)
            log.info(f"Created plugins directory: {full_plugins_dir}")
            return
            
        sys.path.insert(0, os.path.abspath(full_plugins_dir))
        
        for filename in os.listdir(full_plugins_dir):
            if filename.endswith(".py") and not filename.startswith("__"):
                module_name = filename[:-3]
                try:
                    module = importlib.import_module(module_name)
                    # Find all classes derived from KinetixPlugin
                    for name, obj in inspect.getmembers(module):
                        if inspect.isclass(obj) and issubclass(obj, KinetixPlugin) and obj is not KinetixPlugin:
                            plugin_instance = obj()
                            self.plugins.append(plugin_instance)
                            log.info(f"Loaded plugin: {plugin_instance.name} v{plugin_instance.version}")
                            try:
                                plugin_instance.on_load()
                            except Exception as e:
                                log.error(f"Plugin {plugin_instance.name} error on_load: {e}")
                except Exception as e:
                    log.error(f"Failed to load plugin module {module_name}: {e}")
                    
    def process_input(self, player_index: int, state: Dict[str, Any]) -> Dict[str, Any]:
        """Runs the input state through all registered plugins."""
        modified_state = state.copy()
        for plugin in self.plugins:
            try:
                modified_state = plugin.on_input(player_index, modified_state)
            except Exception as e:
                log.error(f"Plugin {plugin.name} error processing input: {e}")
        return modified_state

    def notify_network_connect(self, device_id: str, ip: str):
        """Notifies all plugins of a new network connection."""
        for plugin in self.plugins:
            try:
                plugin.on_network_connect(device_id, ip)
            except Exception as e:
                log.error(f"Plugin {plugin.name} error on connect hook: {e}")

    def unload_plugins(self):
        """Unloads all plugins and calls their on_unload hooks."""
        for plugin in self.plugins:
            try:
                plugin.on_unload()
                log.info(f"Unloaded plugin: {plugin.name}")
            except Exception as e:
                log.error(f"Plugin {plugin.name} error on unload: {e}")
        self.plugins.clear()

# Global manager instance
plugin_mgr = PluginManager()
