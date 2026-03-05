package com.kinetix.controller

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

/**
 * Manages the gyroscope sensor for aiming. Maps phone rotation to
 * right-stick axes with configurable sensitivity and dead-zone.
 *
 * Enable via [start] / [stop]. Read current values via [gyroX] / [gyroY].
 */
class GyroscopeManager(context: Context) : SensorEventListener {

    companion object {
        private const val TAG = "GyroscopeManager"
        private const val DEFAULT_SENSITIVITY = 3.0f
        private const val DEAD_ZONE = 0.02f
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    var sensitivity: Float = DEFAULT_SENSITIVITY
    var enabled: Boolean = false
        private set

    // Output values (-1..1), thread-safe via @Volatile
    @Volatile var gyroX: Float = 0f; private set
    @Volatile var gyroY: Float = 0f; private set

    val isAvailable: Boolean get() = gyroscope != null

    fun start() {
        if (gyroscope == null) {
            Log.w(TAG, "Gyroscope not available on this device")
            return
        }
        sensorManager.registerListener(
            this, gyroscope, SensorManager.SENSOR_DELAY_GAME
        )
        enabled = true
        Log.i(TAG, "Gyroscope started (sensitivity=$sensitivity)")
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        enabled = false
        gyroX = 0f
        gyroY = 0f
        Log.i(TAG, "Gyroscope stopped")
    }

    fun reset() {
        gyroX = 0f
        gyroY = 0f
    }

    // ── SensorEventListener ──────────────────────────────────────────

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_GYROSCOPE) return

        // Gyroscope gives angular velocity in rad/s around X, Y, Z axes.
        // Map Y-axis rotation → horizontal aim (gyroX)
        // Map X-axis rotation → vertical aim (gyroY)
        val rawX = event.values[1] * sensitivity  // yaw  → horizontal
        val rawY = -event.values[0] * sensitivity // pitch → vertical (inverted)

        // Apply dead-zone
        gyroX = if (kotlin.math.abs(rawX) > DEAD_ZONE) rawX.coerceIn(-1f, 1f) else 0f
        gyroY = if (kotlin.math.abs(rawY) > DEAD_ZONE) rawY.coerceIn(-1f, 1f) else 0f
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
