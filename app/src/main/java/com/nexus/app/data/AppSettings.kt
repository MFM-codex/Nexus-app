package com.nexus.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow

// Settings saved on this phone (not in Firebase).
object AppSettings {
    private const val PREFS = "nexus_prefs"

    // "system" (follow the phone), "light" or "dark"
    val theme = MutableStateFlow("system")

    // When on, videos wait for a tap instead of downloading automatically.
    val dataSaver = MutableStateFlow(false)

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        theme.value = prefs.getString("theme", "system") ?: "system"
        dataSaver.value = prefs.getBoolean("data_saver", false)
    }

    fun setTheme(context: Context, value: String) {
        theme.value = value
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("theme", value).apply()
    }

    fun setDataSaver(context: Context, value: Boolean) {
        dataSaver.value = value
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("data_saver", value).apply()
    }
}
