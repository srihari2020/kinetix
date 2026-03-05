from typing import Dict, Any
from kinetix_sdk import KinetixPlugin

class MacroEnginePlugin(KinetixPlugin):
    """
    A simple macro engine that watches for a specific button combo
    (like LB + RB) to trigger a sequence of actions.
    """
    
    @property
    def name(self) -> str:
        return "Macro Engine"
        
    @property
    def version(self) -> str:
        return "1.0.0"

    def on_load(self) -> None:
        print("[MacroEngine] Running!")
        
    def on_input(self, player_index: int, state: Dict[str, Any]) -> Dict[str, Any]:
        # Simple macro trigger: LB + RB pressed simultaneously
        
        # If input is JSON
        lb_pressed = state.get("lb", False)
        rb_pressed = state.get("rb", False)
        
        # If input is binary bitfield, we'd check bits:
        # bit 4 = LB, bit 5 = RB (based on controller_mapper.py)
        buttons = state.get("buttons", 0)
        if buttons > 0:
            lb_pressed = bool(buttons & (1 << 4))
            rb_pressed = bool(buttons & (1 << 5))
            
        if lb_pressed and rb_pressed:
            print(f"[MacroEngine] Player {player_index} triggered the macro!")
            # Could spawn a thread or task to send a rapid sequence of button presses
            # using pyvgamepad or by injecting directly into the DeviceManager.
            
        return state
