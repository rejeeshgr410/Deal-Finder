package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

val LocalLocaleManager = compositionLocalOf<LocaleManager?> { null }

class LocaleManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _currentLanguage = MutableStateFlow(getSavedLanguage(appContext))
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    fun setLanguage(langCode: String) {
        val normalized = normalizeCode(langCode)
        prefs.edit().putString(KEY_LANGUAGE, normalized).commit()
        _currentLanguage.value = normalized
        applyLocale(appContext, normalized)
    }

    val isArabic: Boolean
        get() = _currentLanguage.value == "ar"

    companion object {
        private const val PREFS_NAME = "deal_finder_locale_prefs"
        private const val KEY_LANGUAGE = "selected_language"

        fun normalizeCode(langCode: String): String = if (langCode == "ar") "ar" else "en"

        fun getSavedLanguage(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return normalizeCode(prefs.getString(KEY_LANGUAGE, "en") ?: "en")
        }

        fun applyLocale(context: Context, langCode: String): Context {
            val normalized = normalizeCode(langCode)
            val locale = Locale.forLanguageTag(normalized)
            Locale.setDefault(locale)
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            config.setLayoutDirection(locale)
            return context.createConfigurationContext(config)
        }
    }
}
