import asyncio
import json
from aiortc import RTCPeerConnection, RTCSessionDescription, RTCDataChannel
from logger import get_logger

log = get_logger("webrtc")

class WebRTCManager:
    """Manages WebRTC Peer Connections and DataChannels for low-latency input."""
    
    def __init__(self, on_input_callback):
        self.pcs = set()
        self.on_input_callback = on_input_callback
        
    async def handle_offer(self, websocket, data: dict, player_index: int):
        """Processes an incoming WebRTC offer and returns the answer SDP via WebSocket."""
        offer = RTCSessionDescription(sdp=data["sdp"], type=data["type"])
        pc = RTCPeerConnection()
        self.pcs.add(pc)
        
        @pc.on("datachannel")
        def on_datachannel(channel: RTCDataChannel):
            log.info(f"[WebRTC] DataChannel established: {channel.label}")
            
            @channel.on("message")
            def on_message(message):
                # When input arrives via WebRTC
                try:
                    # Depending on if the message is bytes or str
                    if isinstance(message, bytes):
                        # Expected binary packet like UDP
                        self.on_input_callback(player_index, message, is_binary=True)
                    else:
                        packet = json.loads(message)
                        self.on_input_callback(player_index, packet, is_binary=False)
                except Exception as e:
                    log.error(f"Error handling WebRTC message: {e}")
                    
            @channel.on("open")
            def on_open():
                log.info("[WebRTC] DataChannel opened.")
                
            @channel.on("close")
            def on_close():
                log.info("[WebRTC] DataChannel closed.")

        @pc.on("connectionstatechange")
        async def on_connectionstatechange():
            log.info(f"[WebRTC] Connection state is {pc.connectionState}")
            if pc.connectionState == "failed" or pc.connectionState == "closed":
                await pc.close()
                self.pcs.discard(pc)
                
        # Set remote description
        await pc.setRemoteDescription(offer)
        
        # Create answer
        answer = await pc.createAnswer()
        await pc.setLocalDescription(answer)
        
        # Send answer back through WebSocket
        response = {
            "type": "webrtc_answer",
            "sdp": pc.localDescription.sdp,
            "webrtc_type": pc.localDescription.type
        }
        await websocket.send(json.dumps(response))
        log.info(f"[WebRTC] Sent answer to {websocket.remote_address}")

    async def cleanup(self):
        coros = [pc.close() for pc in self.pcs]
        await asyncio.gather(*coros)
        self.pcs.clear()

