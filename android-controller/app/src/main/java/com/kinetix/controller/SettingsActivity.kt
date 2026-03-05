package com.kinetix.controller

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

/**
 * Settings screen for controller configuration.
 *
 * Allows the user to:
 *  - Toggle gyroscope aiming + adjust sensitivity
 *  - Toggle vibration feedback + adjust intensity
 *  - Set update rate (60 / 120 Hz)
 *  - Manage profiles
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var gyroSwitch: Switch
    private lateinit var gyroSensitivity: SeekBar
    private lateinit var gyroLabel: TextView
    private lateinit var vibrationSwitch: Switch
    private lateinit var vibrationIntensity: SeekBar
    private lateinit var vibrationLabel: TextView
    private lateinit var rateSpinner: Spinner
    private lateinit var profileSpinner: Spinner
    private lateinit var saveBtn: Button

    private var profiles = mutableListOf<ControllerProfile>()
    private var activeIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Bind views
        gyroSwitch = findViewById(R.id.gyro_switch)
        gyroSensitivity = findViewById(R.id.gyro_sensitivity)
        gyroLabel = findViewById(R.id.gyro_sensitivity_label)
        vibrationSwitch = findViewById(R.id.vibration_switch)
        vibrationIntensity = findViewById(R.id.vibration_intensity)
        vibrationLabel = findViewById(R.id.vibration_intensity_label)
        rateSpinner = findViewById(R.id.rate_spinner)
        profileSpinner = findViewById(R.id.profile_spinner)
        saveBtn = findViewById(R.id.save_btn)

        // Load profiles
        profiles = ControllerProfile.loadAll(this)
        val activeName = ControllerProfile.getActive(this).name
        activeIndex = profiles.indexOfFirst { it.name == activeName }.coerceAtLeast(0)

        setupProfileSpinner()
        setupRateSpinner()
        loadProfile(profiles[activeIndex])

        // Listeners
        gyroSensitivity.setOnSeekBarChangeListener(seekBarChanged { progress ->
            gyroLabel.text = "Sensitivity: ${(progress + 1) / 10f}"
        })
        vibrationIntensity.setOnSeekBarChangeListener(seekBarChanged { progress ->
            vibrationLabel.text = "Intensity: ${progress}%"
        })

        saveBtn.setOnClickListener { saveCurrentProfile() }

        val editLayoutBtn = findViewById<Button>(R.id.edit_layout_btn)
        editLayoutBtn?.setOnClickListener {
            val intent = android.content.Intent(this, LayoutEditorActivity::class.java)
            startActivity(intent)
        }

        // Back button in toolbar
        findViewById<View>(R.id.back_btn)?.setOnClickListener { finish() }
    }

    private fun setupProfileSpinner() {
        val names = profiles.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        profileSpinner.adapter = adapter
        profileSpinner.setSelection(activeIndex)

        profileSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                activeIndex = pos
                loadProfile(profiles[pos])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupRateSpinner() {
        val rates = listOf("60 Hz", "120 Hz")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, rates)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        rateSpinner.adapter = adapter
    }

    private fun loadProfile(p: ControllerProfile) {
        gyroSwitch.isChecked = p.gyroEnabled
        gyroSensitivity.progress = ((p.gyroSensitivity * 10) - 1).toInt().coerceIn(0, 99)
        gyroLabel.text = "Sensitivity: ${p.gyroSensitivity}"
        vibrationSwitch.isChecked = p.vibrationEnabled
        vibrationIntensity.progress = (p.vibrationIntensity * 100).toInt().coerceIn(0, 100)
        vibrationLabel.text = "Intensity: ${(p.vibrationIntensity * 100).toInt()}%"
        rateSpinner.setSelection(if (p.sendRateHz >= 120) 1 else 0)
    }

    private fun saveCurrentProfile() {
        val p = profiles[activeIndex].copy(
            gyroEnabled = gyroSwitch.isChecked,
            gyroSensitivity = (gyroSensitivity.progress + 1) / 10f,
            vibrationEnabled = vibrationSwitch.isChecked,
            vibrationIntensity = vibrationIntensity.progress / 100f,
            sendRateHz = if (rateSpinner.selectedItemPosition == 1) 120 else 60
        )
        profiles[activeIndex] = p
        ControllerProfile.saveAll(this, profiles)
        ControllerProfile.setActive(this, p.name)
        Toast.makeText(this, "Profile '${p.name}' saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun seekBarChanged(block: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) = block(progress)
        override fun onStartTrackingTouch(bar: SeekBar?) {}
        override fun onStopTrackingTouch(bar: SeekBar?) {}
    }
}
