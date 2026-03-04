package com.kinetix.controller

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Connection screen – the user enters the PC server's IP and taps Connect.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var ipInput: EditText
    private lateinit var portInput: EditText
    private lateinit var connectBtn: Button
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ipInput = findViewById(R.id.ip_input)
        portInput = findViewById(R.id.port_input)
        connectBtn = findViewById(R.id.connect_btn)
        statusText = findViewById(R.id.status_text)

        // Restore last used IP from prefs
        val prefs = getSharedPreferences("kinetix", MODE_PRIVATE)
        ipInput.setText(prefs.getString("last_ip", ""))
        portInput.setText(prefs.getString("last_port", "8765"))

        connectBtn.setOnClickListener {
            val ip = ipInput.text.toString().trim()
            val port = portInput.text.toString().trim()
            if (ip.isEmpty()) {
                Toast.makeText(this, "Enter the server IP address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val portNum = port.toIntOrNull() ?: 8765

            // Save for next time
            prefs.edit().putString("last_ip", ip).putString("last_port", portNum.toString()).apply()

            // Launch controller
            val intent = Intent(this, ControllerActivity::class.java).apply {
                putExtra("server_url", "ws://$ip:$portNum")
            }
            startActivity(intent)
        }
    }
}
