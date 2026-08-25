package com.aivectorgame.app.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal object ThemeController {
    private const val PREFS = "ai_vector_ui"
    private const val KEY_LIGHT = "light_mode"

    var isLight by mutableStateOf(false)
        private set

    private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        isLight = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_LIGHT, false)
        loaded = true
    }

    fun toggle(context: Context) {
        isLight = !isLight
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_LIGHT, isLight)
            .apply()
    }
}
