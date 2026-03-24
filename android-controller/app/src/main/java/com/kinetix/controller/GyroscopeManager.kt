package com.kinetix.controller.v2

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

class GyroscopeManager(context: Context) : SensorEventListener {

    companion object {
        private const val TAG = "GyroscopeManager"
        private const val DEFAULT_SENSITIVITY = 3.0f
        private const val DEAD_ZONE = 0.02f
        private const val ALPHA = 0.8f // Smoothing factor
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    var sensitivity: Float = DEFAULT_SENSITIVITY
    var enabled: Boolean = false
        private set

    @Volatile var gyroX: Float = 0f; private set
    @Volatile var gyroY: Float = 0f; private set
    
    private var lastRawX = 0f
    private var lastRawY = 0f

    val isAvailable: Boolean get() = gyroscope != null

    fun start() {
        if (gyroscope == null) {
            Log.w(TAG, "Gyroscope not available on this device")
            return
        }
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME)
        enabled = true
        Log.i(TAG, "Gyroscope started (sensitivity=$sensitivity)")
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        enabled = false
        reset()
        Log.i(TAG, "Gyroscope stopped")
    }

    fun reset() {
        gyroX = 0f
        gyroY = 0f
        lastRawX = 0f
        lastRawY = 0f
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_GYROSCOPE) return

        val currentRawX = event.values[1] * sensitivity
        val currentRawY = -event.values[0] * sensitivity

        // Low Pass Filter
        lastRawX = ALPHA * lastRawX + (1 - ALPHA) * currentRawX
        lastRawY = ALPHA * lastRawY + (1 - ALPHA) * currentRawY

        // Dead-zone
        gyroX = if (kotlin.math.abs(lastRawX) > DEAD_ZONE) lastRawX.coerceIn(-1f, 1f) else 0f
        gyroY = if (kotlin.math.abs(lastRawY) > DEAD_ZONE) lastRawY.coerceIn(-1f, 1f) else 0f
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
