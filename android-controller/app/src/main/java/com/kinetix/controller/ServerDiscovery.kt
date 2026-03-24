package com.kinetix.controller.v2

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Discovers Kinetix servers on the LAN by listening for UDP broadcasts.
 *
 * The PC server broadcasts `KINETIX_SERVER:<ip>:<ws_port>:<udp_port>`
 * on port 5742 every 2 seconds.
 */
class ServerDiscovery(
    private val context: Context,
    private val listener: DiscoveryListener
) {
    companion object {
        private const val TAG = "ServerDiscovery"
        private const val DISCOVERY_PORT = 5742
        private const val BUFFER_SIZE = 256
        private const val TIMEOUT_MS = 5000
    }

    data class ServerInfo(
        val ip: String,
        val wsPort: Int,
        val udpPort: Int,
        val displayName: String = "Kinetix-PC ($ip)"
    )

    interface DiscoveryListener {
        fun onServerFound(server: ServerInfo)
        fun onDiscoveryFinished()
        fun onDiscoveryError(error: String)
    }

    @Volatile
    private var scanning = false
    private var thread: Thread? = null

    fun startScan() {
        if (scanning) return
        scanning = true
        thread = Thread({
            scan()
        }, "server-discovery").also { it.isDaemon = true; it.start() }
    }

    fun stopScan() {
        scanning = false
        thread?.interrupt()
        thread = null
    }

    private fun scan() {
        var socket: DatagramSocket? = null
        // Acquire multicast lock to receive broadcasts on Wi-Fi
        val wifiMgr = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val lock = wifiMgr?.createMulticastLock("kinetix_discovery")
        lock?.setReferenceCounted(true)
        lock?.acquire()

        try {
            socket = DatagramSocket(DISCOVERY_PORT, InetAddress.getByName("0.0.0.0"))
            socket.broadcast = true
            socket.soTimeout = TIMEOUT_MS
            socket.reuseAddress = true

            Log.i(TAG, "Listening for server broadcasts on port $DISCOVERY_PORT")

            val seen = mutableSetOf<String>()
            val deadline = System.currentTimeMillis() + 8000  // scan for 8 seconds

            while (scanning && System.currentTimeMillis() < deadline) {
                try {
                    val buf = ByteArray(BUFFER_SIZE)
                    val packet = DatagramPacket(buf, buf.size)
                    socket.receive(packet)

                    val message = String(packet.data, 0, packet.length).trim()
                    Log.d(TAG, "Received: $message from ${packet.address.hostAddress}")

                    if (message.startsWith("KINETIX_SERVER:")) {
                        val parts = message.removePrefix("KINETIX_SERVER:").split(":")
                        if (parts.size >= 3) {
                            val ip = parts[0]
                            val wsPort = parts[1].toIntOrNull() ?: 8765
                            val udpPort = parts[2].toIntOrNull() ?: 5743

                            if (ip !in seen) {
                                seen.add(ip)
                                val server = ServerInfo(ip, wsPort, udpPort)
                                listener.onServerFound(server)
                            }
                        }
                    }
                } catch (e: java.net.SocketTimeoutException) {
                    // Normal timeout, continue scanning
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Discovery error: ${e.message}")
            listener.onDiscoveryError(e.message ?: "Unknown error")
        } finally {
            socket?.close()
            lock?.release()
            scanning = false
            listener.onDiscoveryFinished()
        }
    }
}
