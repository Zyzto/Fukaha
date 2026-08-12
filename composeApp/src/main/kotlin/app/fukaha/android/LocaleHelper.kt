package app.fukaha.android

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import app.fukaha.AppLanguage

object LocaleHelper {
    fun apply(language: AppLanguage) {
        val locales = when (language) {
            AppLanguage.System -> LocaleListCompat.getEmptyLocaleList()
            AppLanguage.English -> LocaleListCompat.forLanguageTags("en")
            AppLanguage.Arabic -> LocaleListCompat.forLanguageTags("ar")
        }
        val current = AppCompatDelegate.getApplicationLocales()
        if (current.toLanguageTags() != locales.toLanguageTags()) {
            AppCompatDelegate.setApplicationLocales(locales)
        }
    }
}
