package com.kinetix.controller

import android.util.Log
import com.google.gson.Gson
import okhttp3.*
import java.util.concurrent.TimeUnit

/**
 * WebSocket client for the Kinetix control channel.
 *
 * Handles:
 * - Device registration and player slot assignment
 * - Receiving rumble events from the PC server
 * - Settings synchronisation
 * - JSON-fallback input (when UDP is unavailable)
 * - Auto-reconnect with exponential backoff
 */
class WebSocketClient(
    private val serverUrl: String,
    private val listener: ConnectionListener
) {
    companion object {
        private const val TAG = "WebSocketClient"
        private const val CLOSE_NORMAL = 1000
    }

    interface ConnectionListener {
        fun onConnected()
        fun onDisconnected(reason: String)
        fun onError(error: String)
    }

    // ── State ────────────────────────────────────────────────────────

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(5, TimeUnit.SECONDS)
        .build()

    private var ws: WebSocket? = null
    @Volatile private var connected = false
    @Volatile private var shouldReconnect = true
    private var backoffMs = 1000L

    /** Callback for incoming messages from the server. */
    var onMessage: ((String) -> Unit)? = null

    // ── Public API ───────────────────────────────────────────────────

    fun connect() {
        shouldReconnect = true
        backoffMs = 1000L
        openSocket()
    }

    fun disconnect() {
        shouldReconnect = false
        ws?.close(CLOSE_NORMAL, "User disconnected")
        ws = null
        connected = false
    }

    /** Send a [ControllerState] as JSON (fallback path). */
    fun sendState(state: ControllerState) {
        if (!connected) return
        val json = gson.toJson(state)
        ws?.send(json)
    }

    /** Send a raw JSON string (for registration, settings, etc.). */
    fun sendRaw(json: String) {
        if (!connected) return
        ws?.send(json)
    }

    val isConnected: Boolean get() = connected

    // ── Internal ─────────────────────────────────────────────────────

    private fun openSocket() {
        Log.i(TAG, "Attempting connection to $serverUrl")
        val request = Request.Builder().url(serverUrl).build()
        ws = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "Connected to $serverUrl")
                connected = true
                backoffMs = 1000L
                listener.onConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Received: $text")
                onMessage?.invoke(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Connection failed: ${t.message}")
                connected = false
                listener.onError(t.message ?: "Unknown error")
                scheduleReconnect()
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(CLOSE_NORMAL, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "Connection closed: $reason")
                connected = false
                listener.onDisconnected(reason)
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        if (!shouldReconnect) return
        Log.i(TAG, "Reconnecting in ${backoffMs}ms …")
        Thread {
            Thread.sleep(backoffMs)
            backoffMs = (backoffMs * 2).coerceAtMost(10_000L)
            if (shouldReconnect) openSocket()
        }.start()
    }
}
