package com.kinetix.controller

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Controller profile data class and persistence.
 *
 * Stores button layout positions, sensitivity settings, and feature
 * toggles.  Profiles are named (e.g. "Default", "FPS Mode", "Driving")
 * and persisted in SharedPreferences as JSON.
 */
data class ControllerProfile(
    val name: String = "Default",
    val gyroEnabled: Boolean = false,
    val gyroSensitivity: Float = 3.0f,
    val vibrationEnabled: Boolean = true,
    val vibrationIntensity: Float = 1.0f,
    val sendRateHz: Int = 120,
    val buttonPositions: Map<String, FloatArray> = emptyMap()  // id → [x%, y%]
) {
    companion object {
        private const val PREFS_NAME = "kinetix_profiles"
        private const val KEY_PROFILES = "profiles"
        private const val KEY_ACTIVE = "active_profile"

        private val gson = Gson()

        /** Built-in profiles. */
        val DEFAULTS = listOf(
            ControllerProfile(name = "Default"),
            ControllerProfile(
                name = "FPS Mode",
                gyroEnabled = true,
                gyroSensitivity = 4.0f,
                sendRateHz = 120
            ),
            ControllerProfile(
                name = "Driving Mode",
                gyroEnabled = false,
                vibrationIntensity = 0.7f,
                sendRateHz = 60
            )
        )

        fun loadAll(ctx: Context): MutableList<ControllerProfile> {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_PROFILES, null)
            if (json != null) {
                try {
                    val type = object : TypeToken<MutableList<ControllerProfile>>() {}.type
                    return gson.fromJson(json, type)
                } catch (_: Exception) {}
            }
            return DEFAULTS.toMutableList()
        }

        fun saveAll(ctx: Context, profiles: List<ControllerProfile>) {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_PROFILES, gson.toJson(profiles)).apply()
        }

        fun getActive(ctx: Context): ControllerProfile {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val name = prefs.getString(KEY_ACTIVE, "Default") ?: "Default"
            val profiles = loadAll(ctx)
            return profiles.find { it.name == name } ?: profiles.firstOrNull() ?: DEFAULTS[0]
        }

        fun setActive(ctx: Context, name: String) {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_ACTIVE, name).apply()
        }
    }
}
