package com.kinetix.controller.v2.system

import com.kinetix.controller.v2.ControllerState
import com.kinetix.controller.v2.UdpInputSender
import com.kinetix.controller.v2.WebSocketClient
import com.kinetix.controller.v2.WebRtcClient

class InputSender(
    private val udpSender: UdpInputSender?,
    private val wsClient: WebSocketClient?,
    private val webRtcClient: WebRtcClient?
) {
    fun send(state: ControllerState) {
        // Broadcast input state robustly across active transports
        
        udpSender?.let {
            it.currentState = state
            if (webRtcClient != null && webRtcClient.isConnected) {
                // Send fast binary data via WebRTC Datachannel
                val packet = it.packState(state)
                webRtcClient.send(packet)
            }
        }

        // Fallback or secondary reporting via WS if UDP is not active
        if (udpSender == null && wsClient?.isConnected == true) {
            wsClient.sendState(state)
        }
    }
}
