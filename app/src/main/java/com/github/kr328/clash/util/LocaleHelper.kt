package com.github.kr328.clash.util

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object LocaleHelper {
    private const val KEY = "locale"
    private const val PREFS = "flowly_lang"

    fun getSavedLanguage(context: Context): String {
        if (Build.VERSION.SDK_INT >= 33) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            val locales = localeManager?.applicationLocales
            if (locales != null && !locales.isEmpty) {
                return locales.toLanguageTags().split(",").first()
            }
        }
        return context.getSharedPreferences(PREFS, 0).getString(KEY, "") ?: ""
    }

    fun saveLanguage(context: Context, code: String) {
        if (Build.VERSION.SDK_INT >= 33) {
            val lm = context.getSystemService(LocaleManager::class.java)
            val list = if (code.isBlank()) {
                LocaleList.getEmptyLocaleList()
            } else {
                LocaleList.forLanguageTags(code.replace('_', '-'))
            }
            lm?.setApplicationLocales(list)
        }
        context.getSharedPreferences(PREFS, 0).edit().putString(KEY, code).apply()
    }

    fun wrap(context: Context?): Context {
        context ?: return context as Context
        val code = getSavedLanguage(context)
        if (code.isBlank()) {
            return context
        }
        val locale = Locale.forLanguageTag(code.replace('_', '-'))
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    fun displayName(code: String): String {
        val lower = code.lowercase(Locale.ROOT)
        return when {
            lower.startsWith("zh") -> "中文"
            lower.startsWith("en") -> "English"
            lower.startsWith("ja") -> "日本語"
            lower.startsWith("ko") -> "한국어"
            lower.startsWith("vi") -> "Tiếng Việt"
            lower.startsWith("ru") -> "Русский"
            else -> "System"
        }
    }
}
