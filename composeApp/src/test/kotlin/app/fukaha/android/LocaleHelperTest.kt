package app.fukaha.android

import app.fukaha.AppLanguage
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocaleHelperTest {
    @Test
    fun explicitLanguagesResolveToThemselvesWithExhaustiveTags() {
        val tags = mapOf(
            AppLanguage.English to "en",
            AppLanguage.Arabic to "ar",
            AppLanguage.Japanese to "ja",
            AppLanguage.SimplifiedChinese to "zh-CN",
            AppLanguage.Spanish to "es",
        )

        assertEquals(AppLanguage.entries.filterNot { it == AppLanguage.System }.toSet(), tags.keys)
        tags.forEach { (language, tag) ->
            assertEquals(language, LocaleHelper.resolve(language))
            assertEquals(tag, LocaleHelper.languageTag(language))
        }
    }

    @Test
    fun systemLocaleResolutionSupportsRegionsAndFallsBackToEnglish() {
        val expected = mapOf(
            "en" to AppLanguage.English,
            "en-GB" to AppLanguage.English,
            "ar" to AppLanguage.Arabic,
            "ar-EG" to AppLanguage.Arabic,
            "ja" to AppLanguage.Japanese,
            "ja-JP" to AppLanguage.Japanese,
            "zh-CN" to AppLanguage.SimplifiedChinese,
            "zh-SG" to AppLanguage.SimplifiedChinese,
            "zh-Hans-TW" to AppLanguage.SimplifiedChinese,
            "zh-TW" to AppLanguage.English,
            "zh-Hant-HK" to AppLanguage.English,
            "es" to AppLanguage.Spanish,
            "es-419" to AppLanguage.Spanish,
            "fr-FR" to AppLanguage.English,
        )

        expected.forEach { (tag, language) ->
            assertEquals(language, LocaleHelper.resolveSystemLocale(Locale.forLanguageTag(tag)), tag)
        }
    }

    @Test
    fun onlyResolvedArabicUsesRtl() {
        AppLanguage.entries.filterNot { it == AppLanguage.System }.forEach { language ->
            assertEquals(language == AppLanguage.Arabic, LocaleHelper.isRtl(language), language.name)
        }

        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG"))
            assertTrue(LocaleHelper.isRtl(AppLanguage.System))
            Locale.setDefault(Locale.forLanguageTag("es-MX"))
            assertFalse(LocaleHelper.isRtl(AppLanguage.System))
        } finally {
            Locale.setDefault(original)
        }
    }
}
