import json
import os
from typing import Dict, Any, List

from logger import get_logger

log = get_logger("profiles")

DEFAULT_PROFILES_FILE = "profiles.json"

class ProfileManager:
    def __init__(self, profiles_file: str = DEFAULT_PROFILES_FILE):
        self.profiles_file = profiles_file
        self.profiles: List[Dict[str, Any]] = []
        self.active_profile_name: str = "Default"
        
        # Mapping of game paths/names to profile names
        self.game_mappings: Dict[str, str] = {}
        
        self.load()

    def load(self):
        if not os.path.exists(self.profiles_file):
            self._create_default()
        else:
            try:
                with open(self.profiles_file, "r") as f:
                    data = json.load(f)
                    self.profiles = data.get("profiles", [])
                    self.game_mappings = data.get("game_mappings", {})
                    self.active_profile_name = data.get("active_profile", "Default")
                log.info(f"Loaded {len(self.profiles)} profiles.")
            except Exception as e:
                log.error(f"Error loading profiles: {e}")
                self._create_default()

    def save(self):
        try:
            with open(self.profiles_file, "w") as f:
                json.dump({
                    "active_profile": self.active_profile_name,
                    "profiles": self.profiles,
                    "game_mappings": self.game_mappings
                }, f, indent=4)
        except Exception as e:
            log.error(f"Error saving profiles: {e}")

    def _create_default(self):
        self.profiles = [
            {
                "name": "Default",
                "gyro_sensitivity": 1.0,
                "send_rate_hz": 120,
                "layout": {}
            },
            {
                "name": "FPS Mode",
                "gyro_sensitivity": 1.5,
                "send_rate_hz": 120,
                "layout": {}
            }
        ]
        self.active_profile_name = "Default"
        self.save()

    def get_active_profile(self) -> Dict[str, Any]:
        for p in self.profiles:
            if p["name"] == self.active_profile_name:
                return p
        return self.profiles[0] if self.profiles else {}

    def set_active_profile(self, name: str) -> bool:
        for p in self.profiles:
            if p["name"] == name:
                if self.active_profile_name != name:
                    self.active_profile_name = name
                    log.info(f"Active profile changed to: {name}")
                    self.save()
                return True
        log.warning(f"Profile {name} not found")
        return False

    def check_game_mapping(self, game_exe: str) -> str:
        """Returns the profile name if a game is mapped, or None."""
        # Find exact match or ends-with match (e.g., "cyberpunk2077.exe")
        for exe, profile_name in self.game_mappings.items():
            if exe.lower() in game_exe.lower():
                return profile_name
        return None

profile_mgr = ProfileManager()
