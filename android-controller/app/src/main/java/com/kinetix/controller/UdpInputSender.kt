package com.kinetix.controller

import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * High-performance UDP sender for controller input.
 *
 * Binary-packs [ControllerState] into a compact 17-byte packet and
 * sends at up to 120 Hz using a dedicated thread with precise timing.
 *
 * Packet format (little-endian):
 * ```
 * Byte  0:     player_id  (uint8)
 * Byte  1:     sequence   (uint8, wrapping)
 * Bytes 2-3:   lx         (int16)
 * Bytes 4-5:   ly         (int16)
 * Bytes 6-7:   rx         (int16)
 * Bytes 8-9:   ry         (int16)
 * Bytes 10-11: lt         (uint16)
 * Bytes 12-13: rt         (uint16)
 * Bytes 14-15: buttons    (uint16, bitfield)
 * Byte  16:    dpad       (uint8)
 * ```
 */
class UdpInputSender(
    private val serverIp: String,
    private val serverPort: Int,
    private val playerId: Int = 0
) {
    companion object {
        private const val TAG = "UdpInputSender"
        private const val PACKET_SIZE = 17
    }

    @Volatile
    private var running = false
    private var thread: Thread? = null
    private var socket: DatagramSocket? = null
    private var sequence: Int = 0

    // Latest state to send (updated from UI thread)
    @Volatile
    var currentState: ControllerState = ControllerState()

    var sendRateHz: Int = 120

    fun start() {
        if (running) return
        running = true

        thread = Thread({
            sendLoop()
        }, "udp-input").also { it.isDaemon = true; it.start() }
        Log.i(TAG, "UDP sender started → $serverIp:$serverPort (player $playerId)")
    }

    fun stop() {
        running = false
        thread?.interrupt()
        thread = null
        try { socket?.close() } catch (_: Exception) {}
        socket = null
        Log.i(TAG, "UDP sender stopped")
    }

    private fun sendLoop() {
        try {
            socket = DatagramSocket()
            val address = InetAddress.getByName(serverIp)
            val intervalNanos = 1_000_000_000L / sendRateHz

            while (running) {
                val start = System.nanoTime()
                try {
                    val data = packState(currentState)
                    val packet = DatagramPacket(data, data.size, address, serverPort)
                    socket?.send(packet)
                } catch (e: Exception) {
                    if (running) Log.w(TAG, "Send failed: ${e.message}")
                }

                // Precise timing
                val elapsed = System.nanoTime() - start
                val sleepNanos = intervalNanos - elapsed
                if (sleepNanos > 0) {
                    Thread.sleep(sleepNanos / 1_000_000, (sleepNanos % 1_000_000).toInt())
                }
            }
        } catch (e: Exception) {
            if (running) Log.e(TAG, "UDP sender error: ${e.message}")
        }
    }

    private fun packState(state: ControllerState): ByteArray {
        val buf = ByteBuffer.allocate(PACKET_SIZE).order(ByteOrder.LITTLE_ENDIAN)

        buf.put(playerId.toByte())                           // player_id
        buf.put((sequence++ and 0xFF).toByte())             // sequence

        buf.putShort(floatToI16(state.lx))                  // lx
        buf.putShort(floatToI16(state.ly))                  // ly
        buf.putShort(floatToI16(state.rx))                  // rx
        buf.putShort(floatToI16(state.ry))                  // ry

        buf.putShort(floatToU16(state.lt))                  // lt
        buf.putShort(floatToU16(state.rt))                  // rt

        buf.putShort(encodeButtons(state).toShort())        // buttons
        buf.put(encodeDpad(state.dpad))                     // dpad

        return buf.array()
    }

    private fun floatToI16(v: Float): Short {
        return (v.coerceIn(-1f, 1f) * 32767f).toInt().toShort()
    }

    private fun floatToU16(v: Float): Short {
        return (v.coerceIn(0f, 1f) * 65535f).toInt().toShort()
    }

    private fun encodeButtons(s: ControllerState): Int {
        var bits = 0
        if (s.a)      bits = bits or (1 shl 0)
        if (s.b)      bits = bits or (1 shl 1)
        if (s.x)      bits = bits or (1 shl 2)
        if (s.y)      bits = bits or (1 shl 3)
        if (s.lb)     bits = bits or (1 shl 4)
        if (s.rb)     bits = bits or (1 shl 5)
        if (s.start)  bits = bits or (1 shl 6)
        if (s.select) bits = bits or (1 shl 7)
        if (s.ls)     bits = bits or (1 shl 8)
        if (s.rs)     bits = bits or (1 shl 9)
        return bits
    }

    private fun encodeDpad(dpad: String): Byte {
        return when (dpad) {
            "none"       -> 0
            "up"         -> 1
            "down"       -> 2
            "left"       -> 3
            "right"      -> 4
            "up-left"    -> 5
            "up-right"   -> 6
            "down-left"  -> 7
            "down-right" -> 8
            else         -> 0
        }.toByte()
    }
}
