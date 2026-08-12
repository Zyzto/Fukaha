package app.fukaha.android

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import app.fukaha.AppLanguage
import java.util.Locale

object LocaleHelper {
    fun resolve(language: AppLanguage): AppLanguage = when (language) {
        AppLanguage.System ->
            if (Locale.getDefault().language.startsWith("ar", ignoreCase = true)) {
                AppLanguage.Arabic
            } else {
                AppLanguage.English
            }
        AppLanguage.English, AppLanguage.Arabic -> language
    }

    fun apply(language: AppLanguage) {
        // Explicit EN/AR only in the UI; System means follow device until the user picks.
        val locales = when (language) {
            AppLanguage.System -> LocaleListCompat.getEmptyLocaleList()
            AppLanguage.English -> LocaleListCompat.forLanguageTags("en")
            AppLanguage.Arabic -> LocaleListCompat.forLanguageTags("ar")
        }
        val currentTags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        val nextTags = locales.toLanguageTags()
        if (currentTags != nextTags) {
            AppCompatDelegate.setApplicationLocales(locales)
        }
    }
}
