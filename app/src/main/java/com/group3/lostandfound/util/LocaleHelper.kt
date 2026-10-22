package com.group3.lostandfound.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * LocaleHelper manages in-app language switching and persists user preference.
 * Supports English ('en'), Sinhala ('si'), and Tamil ('ta').
 */
object LocaleHelper {
    private const val PREFS_NAME = "findit_prefs"
    private const val KEY_LANG = "selected_language_code"

    fun setLocale(context: Context, languageCode: String): Context {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANG, languageCode).apply()
        return updateResources(context, languageCode)
    }

    fun getLanguageCode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANG, "en") ?: "en"
    }

    fun getLanguageLabel(code: String): String {
        return when (code) {
            "si" -> "සිංහල (Sinhala)"
            "ta" -> "தமிழ் (Tamil)"
            else -> "English"
        }
    }

    fun applyLocale(context: Context): Context {
        val lang = getLanguageCode(context)
        return updateResources(context, lang)
    }

    @Suppress("DEPRECATION")
    private fun updateResources(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val resources = context.resources
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }
}
