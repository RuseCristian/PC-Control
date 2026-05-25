package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PrefsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("pc_control_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveProfiles(profiles: List<PcProfile>) {
        val json = gson.toJson(profiles)
        prefs.edit().putString("profiles", json).apply()
    }

    fun loadProfiles(): List<PcProfile> {
        val json = prefs.getString("profiles", null) ?: return emptyList()
        val type = object : TypeToken<List<PcProfile>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveAccentColor(color: Color) {
        prefs.edit().putInt("accent_color", color.toArgb()).apply()
    }

    fun loadAccentColor(): Color {
        val argb = prefs.getInt("accent_color", Color(0xFF00F5A0).toArgb())
        return Color(argb)
    }

    fun saveIsDarkTheme(isDark: Boolean) {
        prefs.edit().putBoolean("is_dark_theme", isDark).apply()
    }

    fun loadIsDarkTheme(): Boolean {
        return prefs.getBoolean("is_dark_theme", true)
    }

    fun saveBackgroundColor(color: Color?) {
        if (color == null) {
            prefs.edit().remove("bg_color").apply()
        } else {
            prefs.edit().putInt("bg_color", color.toArgb()).apply()
        }
    }

    fun loadBackgroundColor(): Color? {
        if (!prefs.contains("bg_color")) return null
        return Color(prefs.getInt("bg_color", 0))
    }
}
