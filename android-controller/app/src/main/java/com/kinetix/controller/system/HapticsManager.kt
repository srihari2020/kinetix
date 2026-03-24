package com.kinetix.controller.v2.system

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

class HapticsManager(context: Context) {
    private val vibrator: Vibrator? = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    var enabled: Boolean = true
    var intensityMultipler: Float = 1.0f

    @Suppress("DEPRECATION")
    fun vibrate(pattern: Type) {
        if (!enabled || vibrator == null) return

        val mul = intensityMultipler.coerceIn(0f, 3f)

        when (pattern) {
            Type.LIGHT_TICK -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 10, 10, 15)
                    val amplitudes = intArrayOf(0, (40 * mul).toInt().coerceIn(1, 255), 0, (20 * mul).toInt().coerceIn(1, 255))
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    vibrator.vibrate(10)
                }
            }
            Type.MEDIUM_CLICK -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 15, 10, 20)
                    val amplitudes = intArrayOf(0, (120 * mul).toInt().coerceIn(1, 255), 0, (80 * mul).toInt().coerceIn(1, 255))
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    vibrator.vibrate(25)
                }
            }
            Type.STRONG_SHOOT -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 30, 20, 50)
                    val amplitudes = intArrayOf(0, (200 * mul).toInt().coerceIn(1, 255), 0, (255 * mul).toInt().coerceIn(1, 255))
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    vibrator.vibrate(longArrayOf(0, 30, 20, 50), -1)
                }
            }
            Type.DAMAGE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 80, 20, 120, 30, 80)
                    val amplitudes = intArrayOf(0, (255 * mul).toInt().coerceIn(1, 255), 0, (200 * mul).toInt().coerceIn(1, 255), 0, (255 * mul).toInt().coerceIn(1, 255))
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    vibrator.vibrate(300)
                }
            }
            // Xbox rubber-feel: sharp impulse simulating pressing into a rubber button
            Type.RUBBER_PRESS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 8, 5, 12)
                    val amplitudes = intArrayOf(0, (180 * mul).toInt().coerceIn(1, 255), 0, (100 * mul).toInt().coerceIn(1, 255))
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    vibrator.vibrate(15)
                }
            }
            // Xbox rubber-feel: softer spring-back release feedback
            Type.RUBBER_RELEASE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 5, 8, 6)
                    val amplitudes = intArrayOf(0, (60 * mul).toInt().coerceIn(1, 255), 0, (30 * mul).toInt().coerceIn(1, 255))
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    vibrator.vibrate(8)
                }
            }
        }
    }

    enum class Type {
        LIGHT_TICK, MEDIUM_CLICK, STRONG_SHOOT, DAMAGE, RUBBER_PRESS, RUBBER_RELEASE
    }
}
