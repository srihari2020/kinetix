package com.kinetix.controller

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Connection screen with auto-discovery + manual IP entry.
 *
 * On launch, scans the LAN for Kinetix servers via UDP broadcast.
 * Discovered servers appear in a list; the user taps to connect.
 * A manual IP fallback is always available.
 */
class MainActivity : AppCompatActivity(), ServerDiscovery.DiscoveryListener {

    private lateinit var serverList: RecyclerView
    private lateinit var scanProgress: ProgressBar
    private lateinit var scanStatus: TextView
    private lateinit var manualSection: LinearLayout
    private lateinit var ipInput: EditText
    private lateinit var portInput: EditText
    private lateinit var connectBtn: Button
    private lateinit var settingsBtn: ImageButton
    private lateinit var profileSpinner: Spinner
    private lateinit var statusText: TextView

    private val servers = mutableListOf<ServerDiscovery.ServerInfo>()
    private lateinit var adapter: ServerAdapter
    private var discovery: ServerDiscovery? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bind views
        serverList = findViewById(R.id.server_list)
        scanProgress = findViewById(R.id.scan_progress)
        scanStatus = findViewById(R.id.scan_status)
        manualSection = findViewById(R.id.manual_section)
        ipInput = findViewById(R.id.ip_input)
        portInput = findViewById(R.id.port_input)
        connectBtn = findViewById(R.id.connect_btn)
        settingsBtn = findViewById(R.id.settings_btn)
        profileSpinner = findViewById(R.id.profile_spinner)
        statusText = findViewById(R.id.status_text)

        // Server list
        adapter = ServerAdapter(servers) { server -> connectTo(server) }
        serverList.layoutManager = LinearLayoutManager(this)
        serverList.adapter = adapter

        // Restore prefs
        val prefs = getSharedPreferences("kinetix", MODE_PRIVATE)
        ipInput.setText(prefs.getString("last_ip", ""))
        portInput.setText(prefs.getString("last_port", "8765"))

        // Profile spinner
        setupProfileSpinner()

        // Manual connect
        connectBtn.setOnClickListener {
            val ip = ipInput.text.toString().trim()
            val port = portInput.text.toString().trim()
            if (ip.isEmpty()) {
                Toast.makeText(this, "Enter the server IP address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val portNum = port.toIntOrNull() ?: 8765
            prefs.edit()
                .putString("last_ip", ip)
                .putString("last_port", portNum.toString())
                .apply()
            connectTo(ServerDiscovery.ServerInfo(ip, portNum, portNum + 978))  // 8765 → 5743
        }

        // Settings
        settingsBtn.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Toggle manual section
        findViewById<View>(R.id.manual_toggle)?.setOnClickListener {
            manualSection.visibility =
                if (manualSection.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        // Start discovery
        startDiscovery()
    }

    override fun onResume() {
        super.onResume()
        setupProfileSpinner()  // Refresh after settings change
    }

    override fun onDestroy() {
        super.onDestroy()
        discovery?.stopScan()
    }

    // ── Discovery ────────────────────────────────────────────────────

    private fun startDiscovery() {
        servers.clear()
        adapter.notifyDataSetChanged()
        scanProgress.visibility = View.VISIBLE
        scanStatus.text = "Scanning for servers…"

        discovery = ServerDiscovery(this, this)
        discovery?.startScan()
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
            if (servers.isEmpty()) {
                scanStatus.text = "No servers found – use manual entry below"
                manualSection.visibility = View.VISIBLE
            } else {
                scanStatus.text = "${servers.size} server(s) found"
            }
        }
    }

    override fun onDiscoveryError(error: String) {
        handler.post {
            scanProgress.visibility = View.GONE
            scanStatus.text = "Discovery error: $error"
            manualSection.visibility = View.VISIBLE
        }
    }

    // ── Connect ──────────────────────────────────────────────────────

    private fun connectTo(server: ServerDiscovery.ServerInfo) {
        val profile = ControllerProfile.getActive(this)
        val intent = Intent(this, ControllerActivity::class.java).apply {
            putExtra("server_ip", server.ip)
            putExtra("ws_port", server.wsPort)
            putExtra("udp_port", server.udpPort)
            putExtra("server_url", "ws://${server.ip}:${server.wsPort}")
            putExtra("profile_name", profile.name)
        }
        startActivity(intent)
    }

    // ── Profiles ─────────────────────────────────────────────────────

    private fun setupProfileSpinner() {
        val profiles = ControllerProfile.loadAll(this)
        val names = profiles.map { it.name }
        val ad = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        profileSpinner.adapter = ad

        val active = ControllerProfile.getActive(this).name
        val idx = names.indexOf(active).coerceAtLeast(0)
        profileSpinner.setSelection(idx)
        profileSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                ControllerProfile.setActive(this@MainActivity, names[pos])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // ── Server list adapter ──────────────────────────────────────────

    private class ServerAdapter(
        private val items: List<ServerDiscovery.ServerInfo>,
        private val onClick: (ServerDiscovery.ServerInfo) -> Unit
    ) : RecyclerView.Adapter<ServerAdapter.VH>() {

        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(android.R.id.text1)
            val detail: TextView = view.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            // Style for dark theme
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
