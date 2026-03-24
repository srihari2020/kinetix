package com.kinetix.controller.v2

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.kinetix.controller.v2.ServerDiscovery

/**
 * Home Screen — DroidJoy style.
 * 4 neon circular buttons: Gamepad, Customize, Connect, Settings.
 */
class MainActivity : AppCompatActivity(), ServerDiscovery.DiscoveryListener {

    private lateinit var connectOverlay: View
    private lateinit var serverList: RecyclerView
    private lateinit var scanProgress: ProgressBar
    private lateinit var scanStatus: TextView
    private lateinit var ipInput: EditText
    private lateinit var portInput: EditText

    private val servers = mutableListOf<ServerDiscovery.ServerInfo>()
    private lateinit var adapter: ServerAdapter
    private var discovery: ServerDiscovery? = null
    private var testWsClient: WebSocketClient? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        connectOverlay = findViewById(R.id.connect_overlay)
        serverList = findViewById(R.id.server_list)
        scanProgress = findViewById(R.id.scan_progress)
        scanStatus = findViewById(R.id.scan_status)
        ipInput = findViewById(R.id.ip_input)
        portInput = findViewById(R.id.port_input)

        // ── Neon Buttons ──
        setupNeonButton(findViewById(R.id.btn_gamepad)) {
            val prefs = getSharedPreferences("kinetix", MODE_PRIVATE)
            val ip = prefs.getString("last_ip", "")
            if (ip.isNullOrEmpty()) {
                Toast.makeText(this, "Please Connect to a server first", Toast.LENGTH_SHORT).show()
                showConnectOverlay()
            } else {
                openController()
            }
        }
        setupNeonButton(findViewById(R.id.btn_customize)) {
            openEditor()
        }
        setupNeonButton(findViewById(R.id.btn_connect)) {
            showConnectOverlay()
        }
        setupNeonButton(findViewById(R.id.btn_settings)) {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // ── Connect Overlay ──
        findViewById<Button>(R.id.close_overlay_btn).setOnClickListener { hideConnectOverlay() }
        findViewById<Button>(R.id.connect_btn).setOnClickListener {
            val ip = ipInput.text.toString().trim()
            val portText = portInput.text.toString().trim()
            if (ip.isEmpty() || !isValidIp(ip)) {
                Toast.makeText(this, "Valid IP required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val portNum = portText.toIntOrNull() ?: 8765
            testServerConnection(ip, portNum)
        }

        adapter = ServerAdapter(servers) { server ->
            testServerConnection(server.ip, server.wsPort)
        }
        serverList.layoutManager = LinearLayoutManager(this)
        serverList.adapter = adapter

        val prefs = getSharedPreferences("kinetix", MODE_PRIVATE)
        ipInput.setText(prefs.getString("last_ip", ""))
        portInput.setText(prefs.getString("last_port", "8765"))
        
        updateStatusText()
    }
    
    private fun updateStatusText() {
        val prefs = getSharedPreferences("kinetix", MODE_PRIVATE)
        val ip = prefs.getString("last_ip", "")
        val statusText = findViewById<TextView>(R.id.status_text)
        if (ip.isNullOrEmpty()) {
            statusText.text = "Tap Connect to find your PC • Customize to edit layout"
            statusText.setTextColor(0xFF5A5A7E.toInt())
        } else {
            statusText.text = "🟢 Connected to $ip • Tap Gamepad to play"
            statusText.setTextColor(0xFF2ECC71.toInt())
        }
    }

    // ── Navigation ──

    private fun openController() {
        val prefs = getSharedPreferences("kinetix", MODE_PRIVATE)
        val ip = prefs.getString("last_ip", "192.168.1.100") ?: "192.168.1.100"
        val port = prefs.getString("last_port", "8765")?.toIntOrNull() ?: 8765
        val profile = ControllerProfile.getActive(this)
        startActivity(Intent(this, ControllerActivity::class.java).apply {
            putExtra("server_ip", ip)
            putExtra("ws_port", port)
            putExtra("udp_port", port + 978)
            putExtra("profile_name", profile.name)
        })
    }

    private fun openEditor() {
        startActivity(Intent(this, EditorActivity::class.java))
    }

    // ── Neon button touch ──

    @SuppressLint("ClickableViewAccessibility")
    private fun setupNeonButton(view: View, onClick: () -> Unit) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN ->
                    v.animate().scaleX(0.90f).scaleY(0.90f).setDuration(80).setInterpolator(OvershootInterpolator()).start()
                MotionEvent.ACTION_UP -> {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).setInterpolator(OvershootInterpolator()).start()
                    onClick()
                }
                MotionEvent.ACTION_CANCEL ->
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).setInterpolator(OvershootInterpolator()).start()
            }
            true
        }
    }

    // ── Connect Overlay ──

    private fun showConnectOverlay() {
        connectOverlay.visibility = View.VISIBLE
        connectOverlay.alpha = 0f
        connectOverlay.animate().alpha(1f).setDuration(200).start()
        startDiscovery()
    }

    private fun hideConnectOverlay() {
        discovery?.stopScan()
        connectOverlay.animate().alpha(0f).setDuration(200).withEndAction {
            connectOverlay.visibility = View.GONE
        }.start()
    }

    private fun saveLastConnection(ip: String, port: Int) {
        getSharedPreferences("kinetix", MODE_PRIVATE).edit()
            .putString("last_ip", ip)
            .putString("last_port", port.toString())
            .apply()
        updateStatusText()
    }
    
    private fun testServerConnection(ip: String, port: Int) {
        scanStatus.text = "Connecting to $ip..."
        scanProgress.visibility = View.VISIBLE
        
        testWsClient?.disconnect()
        testWsClient = WebSocketClient("ws://$ip:$port", object : WebSocketClient.ConnectionListener {
            override fun onConnected() {
                handler.post {
                    saveLastConnection(ip, port)
                    hideConnectOverlay()
                    Toast.makeText(this@MainActivity, "Connected to $ip successfully!", Toast.LENGTH_SHORT).show()
                    testWsClient?.disconnect()
                    testWsClient = null
                }
            }

            override fun onDisconnected(reason: String) {
                // Ignore, handled by error mostly
            }

            override fun onError(error: String) {
                handler.post {
                    scanStatus.text = "Failed to connect to $ip"
                    scanProgress.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "Connection failed: $error", Toast.LENGTH_SHORT).show()
                    testWsClient?.disconnect()
                    testWsClient = null
                }
            }
        })
        testWsClient?.connect()
    }

    // ── Discovery ──

    private fun startDiscovery() {
        servers.clear()
        adapter.notifyDataSetChanged()
        scanProgress.visibility = View.VISIBLE
        scanStatus.text = "Scanning for servers…"
        discovery = ServerDiscovery(this, this)
        discovery?.startScan()
    }

    private fun isValidIp(ip: String): Boolean {
        val parts = ip.split(".")
        if (parts.size != 4) return false
        return try { parts.all { it.toInt() in 0..255 } } catch (_: Exception) { false }
    }

    override fun onServerFound(server: ServerDiscovery.ServerInfo) {
        handler.post {
            if (servers.none { it.ip == server.ip }) {
                servers.add(server)
                adapter.notifyItemInserted(servers.size - 1)
                scanStatus.text = "${servers.size} server(s) found"
            }
        }
    }
    override fun onDiscoveryFinished() {
        handler.post {
            scanProgress.visibility = View.GONE
            scanStatus.text = if (servers.isEmpty()) "No servers found." else "${servers.size} server(s) found"
        }
    }
    override fun onDiscoveryError(error: String) {
        handler.post {
            scanProgress.visibility = View.GONE
            scanStatus.text = "Error: $error"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        discovery?.stopScan()
        testWsClient?.disconnect()
    }

    // ── Adapter ──

    private class ServerAdapter(
        private val items: List<com.kinetix.controller.v2.ServerDiscovery.ServerInfo>,
        private val onClick: (com.kinetix.controller.v2.ServerDiscovery.ServerInfo) -> Unit
    ) : RecyclerView.Adapter<ServerAdapter.VH>() {
        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(android.R.id.text1)
            val detail: TextView = view.findViewById(android.R.id.text2)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
            view.setBackgroundResource(android.R.color.transparent)
            return VH(view)
        }
        override fun onBindViewHolder(holder: VH, position: Int) {
            val server = items[position]
            holder.name.text = server.displayName
            holder.name.setTextColor(0xFFFFFFFF.toInt())
            holder.detail.text = "ws://${server.ip}:${server.wsPort}"
            holder.detail.setTextColor(0xFF7A7A9E.toInt())
            holder.itemView.setOnClickListener { onClick(server) }
        }
        override fun getItemCount() = items.size
    }
}
