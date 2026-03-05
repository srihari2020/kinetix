package com.kinetix.controller

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.webrtc.*
import java.nio.ByteBuffer

class WebRtcClient(
    private val context: Context,
    private val sendRawSignaling: (String) -> Unit
) {
    companion object {
        private const val TAG = "WebRtcClient"
    }

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var dataChannel: DataChannel? = null

    @Volatile
    var isConnected = false

    interface Listener {
        fun onDataChannelStateChange(state: DataChannel.State)
        fun onMessage(message: DataChannel.Buffer)
    }

    var listener: Listener? = null

    init {
        val initializationOptions = PeerConnectionFactory.InitializationOptions.builder(context)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(initializationOptions)

        val options = PeerConnectionFactory.Options()
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .createPeerConnectionFactory()
    }

    fun startCall(playerId: Int) {
        val rtcConfig = PeerConnection.RTCConfiguration(emptyList())
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN

        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(newState: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "ICE state: $newState")
            }
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidate(candidate: IceCandidate?) {}
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(dc: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {}
        })

        val dcInit = DataChannel.Init()
        dcInit.ordered = false
        dcInit.maxRetransmits = 0
        
        dataChannel = peerConnection?.createDataChannel("input_channel", dcInit)
        dataChannel?.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(previousAmount: Long) {}
            override fun onStateChange() {
                val state = dataChannel?.state()
                isConnected = (state == DataChannel.State.OPEN)
                state?.let { listener?.onDataChannelStateChange(it) }
                Log.i(TAG, "DataChannel state changed to $state")
            }
            override fun onMessage(buffer: DataChannel.Buffer) {
                listener?.onMessage(buffer)
            }
        })

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                peerConnection?.setLocalDescription(this, desc)
                desc?.let {
                    val json = JSONObject()
                    json.put("type", "webrtc_offer")
                    json.put("player", playerId)
                    json.put("sdp", it.description)
                    sendRawSignaling(json.toString())
                }
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(reason: String?) {}
            override fun onSetFailure(reason: String?) {}
        }, MediaConstraints())
    }

    fun setRemoteDescription(sdp: String) {
        val desc = SessionDescription(SessionDescription.Type.ANSWER, sdp)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
        }, desc)
    }

    fun send(data: ByteArray) {
        if (isConnected) {
            try {
                // Must allocate direct byte buffer for WebRTC
                val buffer = ByteBuffer.allocateDirect(data.size)
                buffer.put(data)
                buffer.flip()
                dataChannel?.send(DataChannel.Buffer(buffer, true))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send over WebRTC", e)
            }
        }
    }

    fun close() {
        dataChannel?.close()
        peerConnection?.close()
        peerConnectionFactory?.dispose()
        isConnected = false
    }
}
