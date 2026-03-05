package com.kinetix.controller

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.util.UUID

/**
 * Full-screen landscape controller screen with hybrid networking.
 *
 * Uses UDP for 120 Hz input and WebSocket for control messages
 * (registration, rumble events, settings).
 */
class ControllerActivity : AppCompatActivity(), WebSocketClient.ConnectionListener {

    // ── Networking ───────────────────────────────────────────────────
    private lateinit var wsClient: WebSocketClient
    private var udpSender: UdpInputSender? = null
    private var playerIndex: Int = 0

    // ── UI ───────────────────────────────────────────────────────────
    private lateinit var leftJoystick: JoystickView
    private lateinit var rightJoystick: JoystickView
    private lateinit var controllerView: ControllerView
    private lateinit var connectionIndicator: TextView
    private lateinit var playerIndicator: TextView
    private lateinit var batteryIndicator: TextView
    private lateinit var gyroToggle: ImageButton

    // ── Gyroscope ────────────────────────────────────────────────────
    private lateinit var gyroManager: GyroscopeManager
    private var gyroEnabled = false

    // ── Profile ──────────────────────────────────────────────────────
    private lateinit var profile: ControllerProfile

    // ── Input state ──────────────────────────────────────────────────
    private var lx = 0f; private var ly = 0f
    private var rx = 0f; private var ry = 0f
    private var buttonState = ControllerView.ButtonState()

    // ── Send loop ────────────────────────────────────────────────────
    private val handler = Handler(Looper.getMainLooper())
    private val sendRunnable = object : Runnable {
        override fun run() {
            sendState()
            handler.postDelayed(this, sendIntervalMs)
        }
    }
    private var sendIntervalMs = 8L  // ~120 Hz

    // ── Vibration ────────────────────────────────────────────────────
    private val vibrator by lazy { getSystemService(VIBRATOR_SERVICE) as? Vibrator }

    // ── Connection info ──────────────────────────────────────────────
    private var serverIp = ""
    private var wsPort = 8765
    private var udpPort = 5743
    private val deviceId by lazy {
        val prefs = getSharedPreferences("kinetix", MODE_PRIVATE)
        prefs.getString("device_id", null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString("device_id", it).apply()
        }
    }

    // ── Lifecycle ────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_controller)
        goImmersive()

        // Extract intent extras
        serverIp = intent.getStringExtra("server_ip") ?: "192.168.1.100"
        wsPort = intent.getIntExtra("ws_port", 8765)
        udpPort = intent.getIntExtra("udp_port", 5743)

        // Load profile
        val profileName = intent.getStringExtra("profile_name") ?: "Default"
        val profiles = ControllerProfile.loadAll(this)
        profile = profiles.find { it.name == profileName } ?: ControllerProfile.getActive(this)

        // Configure send rate
        sendIntervalMs = (1000L / profile.sendRateHz).coerceAtLeast(8L)

        // Bind views
        leftJoystick = findViewById(R.id.left_joystick)
        rightJoystick = findViewById(R.id.right_joystick)
        controllerView = findViewById(R.id.controller_view)
        connectionIndicator = findViewById(R.id.connection_indicator)
        playerIndicator = findViewById(R.id.player_indicator)
        batteryIndicator = findViewById(R.id.battery_indicator)
        gyroToggle = findViewById(R.id.gyro_toggle)

        // Wire joysticks
        leftJoystick.onPositionChanged = { x, y -> lx = x; ly = y }
        rightJoystick.onPositionChanged = { x, y -> rx = x; ry = y }

        // Wire buttons
        controllerView.onStateChanged = { state -> buttonState = state }

        // Gyroscope
        gyroManager = GyroscopeManager(this)
        gyroEnabled = profile.gyroEnabled
        gyroManager.sensitivity = profile.gyroSensitivity
        updateGyroButton()

        gyroToggle.setOnClickListener {
            gyroEnabled = !gyroEnabled
            updateGyroButton()
            if (gyroEnabled) gyroManager.start() else gyroManager.stop()
            Toast.makeText(this, if (gyroEnabled) "Gyro ON" else "Gyro OFF", Toast.LENGTH_SHORT).show()
        }

        // Hide gyro button if not available
        if (!gyroManager.isAvailable) {
            gyroToggle.visibility = View.GONE
        }

        // Battery indicator
        updateBattery()

        // Connect WebSocket (control channel)
        val wsUrl = "ws://$serverIp:$wsPort"
        wsClient = WebSocketClient(wsUrl, this)
        wsClient.onMessage = { msg -> handleServerMessage(msg) }
        wsClient.connect()
    }

    override fun onResume() {
        super.onResume()
        handler.post(sendRunnable)
        if (gyroEnabled) gyroManager.start()
        goImmersive()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(sendRunnable)
        gyroManager.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(sendRunnable)
        udpSender?.stop()
        wsClient.disconnect()
    }

    // ── Send state ───────────────────────────────────────────────────

    private fun sendState() {
        // Combine stick + gyro
        var finalRx = rx
        var finalRy = ry
        if (gyroEnabled && gyroManager.enabled) {
            finalRx = (rx + gyroManager.gyroX).coerceIn(-1f, 1f)
            finalRy = (ry + gyroManager.gyroY).coerceIn(-1f, 1f)
        }

        val state = ControllerState(
            lx = lx, ly = ly,
            rx = finalRx, ry = finalRy,
            a = buttonState.a, b = buttonState.b,
            x = buttonState.x, y = buttonState.y,
            lb = buttonState.lb, rb = buttonState.rb,
            lt = buttonState.lt, rt = buttonState.rt,
            start = buttonState.start, select = buttonState.select,
            dpad = buttonState.dpad
        )

        // Primary: UDP (fast path)
        udpSender?.let { it.currentState = state }

        // Fallback: WebSocket JSON (if UDP not yet started)
        if (udpSender == null && wsClient.isConnected) {
            wsClient.sendState(state)
        }
    }

    // ── Server messages ──────────────────────────────────────────────

    private fun handleServerMessage(message: String) {
        try {
            val json = JSONObject(message)
            when (json.optString("type")) {
                "assigned" -> {
                    playerIndex = json.optInt("player", 0)
                    val assignedUdpPort = json.optInt("udp_port", udpPort)
                    val status = json.optString("status", "ok")

                    runOnUiThread {
                        if (status == "ok") {
                            playerIndicator.text = "P${playerIndex + 1}"
                            playerIndicator.visibility = View.VISIBLE
                            // Start UDP sender
                            startUdpSender(assignedUdpPort)
                        } else {
                            Toast.makeText(this, json.optString("message", "Slot full"), Toast.LENGTH_LONG).show()
                        }
                    }
                }
                "rumble" -> {
                    val small = json.optInt("small_motor", 0)
                    val large = json.optInt("large_motor", 0)
                    val durationMs = json.optLong("duration_ms", 200)
                    if (profile.vibrationEnabled) {
                        triggerVibration(small, large, durationMs)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun startUdpSender(port: Int) {
        udpSender?.stop()
        udpSender = UdpInputSender(serverIp, port, playerIndex).apply {
            sendRateHz = profile.sendRateHz
            start()
        }
    }

    // ── Connection callbacks ─────────────────────────────────────────

    override fun onConnected() {
        // Send registration
        val regMsg = JSONObject().apply {
            put("type", "register")
            put("device_id", deviceId)
            put("device_name", "${Build.MANUFACTURER} ${Build.MODEL}")
        }
        wsClient.sendRaw(regMsg.toString())

        runOnUiThread {
            connectionIndicator.text = "● Connected"
            connectionIndicator.setTextColor(0xFF2ECC71.toInt())
        }
    }

    override fun onDisconnected(reason: String) {
        runOnUiThread {
            connectionIndicator.text = "● Reconnecting…"
            connectionIndicator.setTextColor(0xFFF39C12.toInt())
        }
    }

    override fun onError(error: String) {
        runOnUiThread {
            connectionIndicator.text = "● Connection failed"
            connectionIndicator.setTextColor(0xFFE74C3C.toInt())
            Toast.makeText(this, "Connection failed: $error", Toast.LENGTH_LONG).show()
        }
    }

    // ── Vibration ────────────────────────────────────────────────────

    @Suppress("DEPRECATION")
    private fun triggerVibration(small: Int, large: Int, durationMs: Long) {
        val intensity = ((small + large) / 2).coerceIn(0, 255)
        val scaledIntensity = (intensity * profile.vibrationIntensity).toInt().coerceIn(1, 255)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createOneShot(durationMs, scaledIntensity)
            )
        } else {
            vibrator?.vibrate(durationMs)
        }
    }

    // ── Gyro UI ──────────────────────────────────────────────────────

    private fun updateGyroButton() {
        gyroToggle.alpha = if (gyroEnabled) 1.0f else 0.4f
    }

    // ── Battery ──────────────────────────────────────────────────────

    private fun updateBattery() {
        val bm = getSystemService(BATTERY_SERVICE) as? android.os.BatteryManager
        val level = bm?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        if (level >= 0) {
            batteryIndicator.text = "🔋 $level%"
            batteryIndicator.visibility = View.VISIBLE
            batteryIndicator.setTextColor(
                when {
                    level <= 15 -> 0xFFE74C3C.toInt()
                    level <= 30 -> 0xFFF39C12.toInt()
                    else -> 0xFF2ECC71.toInt()
                }
            )
        }
        // Update every 60 seconds
        handler.postDelayed({ updateBattery() }, 60_000)
    }

    // ── Immersive mode ───────────────────────────────────────────────

    @Suppress("DEPRECATION")
    private fun goImmersive() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { ctrl ->
                ctrl.hide(WindowInsets.Type.systemBars())
                ctrl.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
        }
    }
}
