package com.kinetix.controller.v2

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kinetix.controller.v2.system.HapticsManager
import com.kinetix.controller.v2.system.InputSender
import com.kinetix.controller.v2.ui.components.ABXYGroup
import com.kinetix.controller.v2.ui.components.BumperView
import com.kinetix.controller.v2.ui.components.CenterButtonsView
import com.kinetix.controller.v2.ui.components.DPadGroup
import com.kinetix.controller.v2.ui.components.JoystickView
import com.kinetix.controller.v2.ui.components.TriggerView
import android.view.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

/**
 * Controller Screen — FIXED UI. No edit mode. No dragging. No resizing.
 * Loads saved layout offsets from JSON and applies them on start.
 */
class ControllerActivity : AppCompatActivity(), WebSocketClient.ConnectionListener {

    private lateinit var haptics: HapticsManager
    private lateinit var inputSender: InputSender
    private lateinit var gyroManager: GyroscopeManager

    private var wsClient: WebSocketClient? = null
    private var udpSender: UdpInputSender? = null
    private var webRtcClient: WebRtcClient? = null
    private var serverIp = "192.168.1.100"
    private var wsPort = 8765
    private var udpPort = 5743
    private var playerIndex = 0

    private lateinit var leftJoystick: JoystickView
    private lateinit var rightJoystick: JoystickView
    private lateinit var dpadGroup: DPadGroup
    private lateinit var actionButtons: ABXYGroup
    private lateinit var triggerLT: TriggerView
    private lateinit var triggerRT: TriggerView
    private lateinit var bumperLB: BumperView
    private lateinit var bumperRB: BumperView
    private lateinit var centerBtns: CenterButtonsView

    private var inputLoopJob: Job? = null
    private var currentState = ControllerState()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_controller)
        goImmersive()

        serverIp = intent.getStringExtra("server_ip") ?: "192.168.1.100"
        wsPort = intent.getIntExtra("ws_port", 8765)
        udpPort = intent.getIntExtra("udp_port", 5743)

        haptics = HapticsManager(this)
        gyroManager = GyroscopeManager(this)

        val profile = ControllerProfile.getActive(this)
        haptics.enabled = profile.vibrationEnabled
        haptics.intensityMultipler = profile.vibrationIntensity
        if (profile.gyroEnabled && gyroManager.isAvailable) {
            gyroManager.sensitivity = profile.gyroSensitivity
            gyroManager.start()
        }

        // ── Bind Views ──
        leftJoystick = findViewById(R.id.left_joystick)
        rightJoystick = findViewById(R.id.right_joystick)
        dpadGroup = findViewById(R.id.dpad_view)
        actionButtons = findViewById(R.id.action_buttons)
        triggerLT = findViewById(R.id.trigger_lt)
        triggerRT = findViewById(R.id.trigger_rt)
        bumperLB = findViewById(R.id.bumper_lb)
        bumperRB = findViewById(R.id.bumper_rb)
        centerBtns = findViewById(R.id.center_buttons)

        bumperLB.label = "LB"
        bumperRB.label = "RB"

        // ── Pass Haptics ──
        leftJoystick.haptics = haptics
        rightJoystick.haptics = haptics
        dpadGroup.haptics = haptics
        actionButtons.haptics = haptics
        triggerLT.haptics = haptics
        triggerRT.haptics = haptics
        bumperLB.haptics = haptics
        bumperRB.haptics = haptics
        centerBtns.haptics = haptics

        // ── State Listeners ──
        leftJoystick.onPositionChanged = { x, y -> currentState.lx = x; currentState.ly = y }
        rightJoystick.onPositionChanged = { x, y ->
            if (!gyroManager.enabled) { currentState.rx = x; currentState.ry = y }
        }
        triggerLT.onValueChanged = { v -> currentState.lt = v }
        triggerRT.onValueChanged = { v -> currentState.rt = v }
        bumperLB.onStateChanged = { p -> currentState.lb = p }
        bumperRB.onStateChanged = { p -> currentState.rb = p }
        centerBtns.onStateChanged = { start, select -> currentState.start = start; currentState.select = select }
        dpadGroup.onDpadChanged = { dir -> currentState.dpad = dir }
        actionButtons.onStateChanged = { st ->
            currentState.a = st.a; currentState.b = st.b
            currentState.x = st.x; currentState.y = st.y
        }

        // ── Load saved layout offsets ──
        loadLayoutOffsets()

        // ── Start networking ──
        lifecycleScope.launch(Dispatchers.IO) { connectNetwork() }
    }

    // ── Load offsets saved by EditorActivity ──

    private fun loadLayoutOffsets() {
        val root = findViewById<View>(R.id.root_container)
        root.post {
            val prefs = getSharedPreferences("kinetix_layout", Context.MODE_PRIVATE)
            val jsonStr = prefs.getString("layout_offsets_v3", null) ?: return@post
            try {
                val json = JSONObject(jsonStr)
                val w = root.width.toFloat()
                val h = root.height.toFloat()
                if (w == 0f || h == 0f) return@post

                val views = mapOf(
                    "left" to leftJoystick, "right" to rightJoystick,
                    "dpad" to dpadGroup as View, "abxy" to actionButtons as View,
                    "lt" to triggerLT as View, "rt" to triggerRT as View,
                    "lb" to bumperLB as View, "rb" to bumperRB as View,
                    "center" to centerBtns as View
                )
                views.forEach { (id, v) ->
                    val obj = json.optJSONObject(id)
                    if (obj != null) {
                        v.translationX = obj.getDouble("tx").toFloat() * w
                        v.translationY = obj.getDouble("ty").toFloat() * h
                        v.scaleX = obj.getDouble("scaleX").toFloat()
                        v.scaleY = obj.getDouble("scaleY").toFloat()
                        // Respect editor visibility — hide removed components
                        val visible = obj.optBoolean("visible", true)
                        v.visibility = if (visible) View.VISIBLE else View.GONE
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // ── 120Hz Input Loop ──

    private fun startInputLoop() {
        inputLoopJob?.cancel()
        inputLoopJob = lifecycleScope.launch(Dispatchers.Default) {
            while (isActive) {
                if (gyroManager.enabled) {
                    currentState.rx = gyroManager.gyroX
                    currentState.ry = gyroManager.gyroY
                }
                sendState()
                delay(8) // ~120Hz
            }
        }
    }

    private fun sendState() {
        if (!this::inputSender.isInitialized) {
            inputSender = InputSender(udpSender, wsClient, webRtcClient)
        }
        inputSender.send(currentState)
    }

    // ── Networking ──

    private fun connectNetwork() {
        try {
            val wsUrl = "ws://$serverIp:$wsPort"
            val client = WebSocketClient(wsUrl, this)
            client.onMessage = { msg -> handleServerMessage(msg) }
            wsClient = client
            webRtcClient = WebRtcClient(this) { sdpJson -> wsClient?.sendRaw(sdpJson) }
            wsClient?.connect()
        } catch (_: Exception) {}
    }

    private fun handleServerMessage(message: String) {
        try {
            val json = JSONObject(message)
            when (json.optString("type")) {
                "assigned" -> {
                    playerIndex = json.optInt("player", 0)
                    val port = json.optInt("udp_port", udpPort)
                    if (json.optString("status") == "ok") {
                        lifecycleScope.launch(Dispatchers.Main) {
                            udpSender?.stop()
                            udpSender = UdpInputSender(serverIp, port, playerIndex)
                            udpSender?.start()
                            webRtcClient?.startCall(playerIndex)
                            inputSender = InputSender(udpSender, wsClient, webRtcClient)
                        }
                    }
                }
                "webrtc_answer" -> webRtcClient?.setRemoteDescription(json.optString("sdp"))
                "rumble" -> {
                    val duration = json.optLong("duration_ms", 200)
                    haptics.vibrate(if (duration > 300) HapticsManager.Type.DAMAGE else HapticsManager.Type.STRONG_SHOOT)
                }
            }
        } catch (_: Exception) {}
    }

    override fun onConnected() {
        val deviceId = getSharedPreferences("kinetix", MODE_PRIVATE)
            .getString("device_id", UUID.randomUUID().toString())
        val regMsg = JSONObject().apply {
            put("type", "register")
            put("device_id", deviceId)
            put("device_name", "${Build.MANUFACTURER} ${Build.MODEL}")
        }
        wsClient?.sendRaw(regMsg.toString())
        lifecycleScope.launch(Dispatchers.Main) {
            Toast.makeText(this@ControllerActivity, "Connected to Kinetix", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onDisconnected(reason: String) {}
    override fun onError(error: String) {}

    // ── Lifecycle ──

    override fun onResume() {
        super.onResume()
        startInputLoop()
        goImmersive()
        if (!gyroManager.enabled && ControllerProfile.getActive(this).gyroEnabled && gyroManager.isAvailable) {
            gyroManager.start()
        }
    }

    override fun onPause() {
        super.onPause()
        inputLoopJob?.cancel()
        if (gyroManager.enabled) gyroManager.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        inputLoopJob?.cancel()
        udpSender?.stop()
        webRtcClient?.close()
        wsClient?.disconnect()
    }

    private fun goImmersive() {
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        androidx.core.view.WindowInsetsControllerCompat(window, window.decorView).let { c ->
            c.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            c.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        supportActionBar?.hide()
    }
}
