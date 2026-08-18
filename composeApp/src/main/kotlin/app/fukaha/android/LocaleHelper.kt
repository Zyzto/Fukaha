package app.fukaha.android

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import app.fukaha.AppLanguage
import java.util.Locale

object LocaleHelper {
    fun resolve(language: AppLanguage): AppLanguage = when (language) {
        AppLanguage.System -> resolveSystemLocale(Locale.getDefault())
        AppLanguage.English,
        AppLanguage.Arabic,
        AppLanguage.Japanese,
        AppLanguage.SimplifiedChinese,
        AppLanguage.Spanish,
        -> language
    }

    internal fun resolveSystemLocale(locale: Locale): AppLanguage = when (locale.language.lowercase()) {
        "ar" -> AppLanguage.Arabic
        "ja" -> AppLanguage.Japanese
        "zh" -> if (
            locale.script.equals("Hans", ignoreCase = true) ||
            locale.country.equals("CN", ignoreCase = true) ||
            locale.country.equals("SG", ignoreCase = true)
        ) {
            AppLanguage.SimplifiedChinese
        } else {
            AppLanguage.English
        }
        "es" -> AppLanguage.Spanish
        else -> AppLanguage.English
    }

    fun isRtl(language: AppLanguage): Boolean = resolve(language).isRtl

    fun languageTag(language: AppLanguage): String = when (resolve(language)) {
        AppLanguage.System -> error("System language must resolve to a concrete locale")
        AppLanguage.English -> "en"
        AppLanguage.Arabic -> "ar"
        AppLanguage.Japanese -> "ja"
        AppLanguage.SimplifiedChinese -> "zh-CN"
        AppLanguage.Spanish -> "es"
    }

    fun apply(language: AppLanguage) {
        val locales = when (language) {
            AppLanguage.System -> LocaleListCompat.getEmptyLocaleList()
            else -> LocaleListCompat.forLanguageTags(languageTag(language))
        }
        val currentTags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        val nextTags = locales.toLanguageTags()
        if (currentTags != nextTags) {
            AppCompatDelegate.setApplicationLocales(locales)
        }
    }
}
