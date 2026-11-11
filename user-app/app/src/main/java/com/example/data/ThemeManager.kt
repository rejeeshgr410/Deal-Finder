package com.example.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val value: Int) {
    AUTO(0), LIGHT(1), DARK(2);
    
    companion object {
        fun fromValue(value: Int) = entries.find { it.value == value } ?: AUTO
    }
}

class ThemeManager(context: Context) {
    private val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow<ThemeMode>(ThemeMode.fromValue(prefs.getInt("theme_mode", 0)))
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit(commit = true) {
            putInt("theme_mode", mode.value)
        }
        _themeMode.value = mode
    }
}
