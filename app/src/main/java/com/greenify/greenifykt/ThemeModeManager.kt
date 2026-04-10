package com.greenify.greenifykt

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeModeManager {
    private const val PREFS_NAME = "greenify_prefs"
    private const val KEY_DARK_MODE = "dark_mode_enabled"

    fun isDarkModeEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DARK_MODE, false)
    }

    fun setDarkMode(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
        AppCompatDelegate.setDefaultNightMode(
            if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    fun applySavedTheme(context: Context) {
        setDarkMode(context, isDarkModeEnabled(context))
    }
}
