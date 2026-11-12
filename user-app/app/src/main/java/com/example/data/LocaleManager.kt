package com.example.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

val LocalLocaleManager = compositionLocalOf<LocaleManager?> { null }

class LocaleManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("deal_finder_locale_prefs", Context.MODE_PRIVATE)

    private val _currentLanguage = MutableStateFlow(getInitialLanguage())
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private fun getInitialLanguage(): String {
        return prefs.getString("selected_language", "en") ?: "en"
    }

    fun setLanguage(langCode: String) {
        val normalized = if (langCode == "ar") "ar" else "en"
        prefs.edit().putString("selected_language", normalized).apply()
        _currentLanguage.value = normalized
        applyLocale(context, normalized)
    }

    val isArabic: Boolean
        get() = _currentLanguage.value == "ar"

    companion object {
        fun applyLocale(context: Context, langCode: String): Context {
            val locale = if (langCode == "ar") Locale("ar") else Locale("en")
            Locale.setDefault(locale)
            val config = context.resources.configuration
            config.setLocale(locale)
            config.setLayoutDirection(locale)
            return context.createConfigurationContext(config)
        }
    }
}
