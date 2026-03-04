package com.kinetix.controller

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Full-screen landscape controller screen.
 *
 * Hosts two [JoystickView]s and one [ControllerView], wires them to
 * [WebSocketClient], and sends the combined [ControllerState] at ~60 FPS.
 */
class ControllerActivity : AppCompatActivity(), WebSocketClient.ConnectionListener {

    private lateinit var wsClient: WebSocketClient
    private lateinit var leftJoystick: JoystickView
    private lateinit var rightJoystick: JoystickView
    private lateinit var controllerView: ControllerView
    private lateinit var connectionIndicator: TextView

    // Mutable state aggregated from the subviews
    private var lx = 0f; private var ly = 0f
    private var rx = 0f; private var ry = 0f
    private var buttonState = ControllerView.ButtonState()

    // Send loop
    private val handler = Handler(Looper.getMainLooper())
    private val sendIntervalMs = 16L   // ~60 FPS
    private val sendRunnable = object : Runnable {
        override fun run() {
            sendState()
            handler.postDelayed(this, sendIntervalMs)
        }
    }

    // ── Lifecycle ────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_controller)
        goImmersive()

        leftJoystick = findViewById(R.id.left_joystick)
        rightJoystick = findViewById(R.id.right_joystick)
        controllerView = findViewById(R.id.controller_view)
        connectionIndicator = findViewById(R.id.connection_indicator)

        // Wire joysticks
        leftJoystick.onPositionChanged = { x, y -> lx = x; ly = y }
        rightJoystick.onPositionChanged = { x, y -> rx = x; ry = y }

        // Wire buttons
        controllerView.onStateChanged = { state -> buttonState = state }

        // Connect
        val url = intent.getStringExtra("server_url") ?: "ws://192.168.1.100:8765"
        wsClient = WebSocketClient(url, this)
        wsClient.connect()
    }

    override fun onResume() {
        super.onResume()
        handler.post(sendRunnable)
        goImmersive()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(sendRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(sendRunnable)
        wsClient.disconnect()
    }

    // ── Send state ───────────────────────────────────────────────────

    private fun sendState() {
        val state = ControllerState(
            lx = lx, ly = ly,
            rx = rx, ry = ry,
            a = buttonState.a,
            b = buttonState.b,
            x = buttonState.x,
            y = buttonState.y,
            lb = buttonState.lb,
            rb = buttonState.rb,
            lt = buttonState.lt,
            rt = buttonState.rt,
            start = buttonState.start,
            select = buttonState.select,
            dpad = buttonState.dpad
        )
        wsClient.sendState(state)
    }

    // ── Connection callbacks ─────────────────────────────────────────

    override fun onConnected() {
        runOnUiThread {
            connectionIndicator.text = "● Connected"
            connectionIndicator.setTextColor(0xFF2ECC71.toInt())
            Toast.makeText(this, "Connected to server!", Toast.LENGTH_SHORT).show()
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
            connectionIndicator.text = "● Disconnected"
            connectionIndicator.setTextColor(0xFFE74C3C.toInt())
        }
    }

    // ── Immersive mode (API 24+ compatible) ──────────────────────────

    @Suppress("DEPRECATION")
    private fun goImmersive() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+
            window.insetsController?.let { ctrl ->
                ctrl.hide(WindowInsets.Type.systemBars())
                ctrl.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // API 24–29 fallback
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
