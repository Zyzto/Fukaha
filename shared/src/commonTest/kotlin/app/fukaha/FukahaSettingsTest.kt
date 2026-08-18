package app.fukaha

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FukahaSettingsTest {
    @Test
    fun defaultsPreserveAskSystemThemeAndSystemLanguageSemantics() {
        val defaults = FukahaSettings()

        assertEquals(ShareAction.Ask, defaults.defaultAction)
        assertEquals(ShareAction.Ask, defaults.effectiveDefaultAction())
        assertEquals(AppLanguage.System, defaults.language)
        assertEquals(AppTheme.System, defaults.theme)
    }

    @Test
    fun languagesRoundTripPersistedEnumNames() {
        AppLanguage.entries.forEach { language ->
            assertEquals(language, AppLanguage.fromPersistedValue(language.name))
        }
    }

    @Test
    fun legacyLocaleTagsAndUnknownValuesDecodeSafely() {
        val expected = mapOf(
            "en" to AppLanguage.English,
            "en-US" to AppLanguage.English,
            "ar-SA" to AppLanguage.Arabic,
            "ja-JP" to AppLanguage.Japanese,
            "zh-CN" to AppLanguage.SimplifiedChinese,
            "zh_Hans_SG" to AppLanguage.SimplifiedChinese,
            "es-MX" to AppLanguage.Spanish,
        )
        expected.forEach { (stored, language) ->
            assertEquals(language, AppLanguage.fromPersistedValue(stored), stored)
        }
        assertEquals(AppLanguage.System, AppLanguage.fromPersistedValue("zh-TW"))
        assertEquals(AppLanguage.System, AppLanguage.fromPersistedValue("not-a-language"))
        assertEquals(AppLanguage.System, AppLanguage.fromPersistedValue(null))
    }

    @Test
    fun legacyEnglishDefaultMigratesOnceToSystemWithoutLosingExplicitLanguages() {
        assertEquals(AppLanguage.System.name, migrateLegacyLanguageValue(null))
        assertEquals(AppLanguage.System.name, migrateLegacyLanguageValue(AppLanguage.English.name))
        assertEquals(AppLanguage.Arabic.name, migrateLegacyLanguageValue(AppLanguage.Arabic.name))
        assertEquals(AppLanguage.Spanish.name, migrateLegacyLanguageValue("es-MX"))
    }

    @Test
    fun arabicIsTheOnlyRtlApplicationLanguage() {
        AppLanguage.entries.forEach { language ->
            assertEquals(language == AppLanguage.Arabic, language.isRtl, language.name)
        }
    }

    @Test
    fun themeCycleIsExhaustiveAndReturnsToSystem() {
        assertEquals(AppTheme.Light, AppTheme.System.next())
        assertEquals(AppTheme.Dark, AppTheme.Light.next())
        assertEquals(AppTheme.System, AppTheme.Dark.next())
        AppTheme.entries.forEach { theme ->
            assertEquals(theme, theme.next().next().next())
        }
    }

    @Test
    fun emptyAndLegacyPublicCobaltUrlsAreInvalid() {
        assertFalse(FukahaSettings.isValidCobaltBaseUrl(""))
        assertFalse(FukahaSettings.isValidCobaltBaseUrl("   "))
        assertTrue(FukahaSettings.isValidCobaltBaseUrl("https://cobalt.example"))
        assertTrue(FukahaSettings.isValidCobaltBaseUrl("http://localhost:9000"))
        assertTrue(FukahaSettings.isLegacyPublicCobaltBaseUrl("https://api.cobalt.tools"))
        assertTrue(FukahaSettings.isLegacyPublicCobaltBaseUrl("https://api.cobalt.tools/"))
        assertFalse(FukahaSettings.isLegacyPublicCobaltBaseUrl("https://cobalt.example"))
    }

    @Test
    fun downloadClampsToAskWithoutACobaltUrl() {
        val raw = FukahaSettings(defaultAction = ShareAction.Download)
        assertFalse(raw.hasValidCobaltBaseUrl)
        assertEquals(ShareAction.Ask, raw.effectiveDefaultAction())
        assertEquals(ShareAction.Ask, raw.withDownloadClamped().defaultAction)

        val configured = raw.copy(cobaltBaseUrl = "https://cobalt.example")
        assertTrue(configured.hasValidCobaltBaseUrl)
        assertEquals(ShareAction.Download, configured.effectiveDefaultAction())
        assertEquals(ShareAction.Download, configured.withDownloadClamped().defaultAction)
    }

    @Test
    fun otherActionsStayUnchangedWhenCobaltIsMissing() {
        ShareAction.entries.filter { it != ShareAction.Download }.forEach { action ->
            val settings = FukahaSettings(defaultAction = action)
            assertEquals(action, settings.effectiveDefaultAction())
            assertEquals(action, settings.withDownloadClamped().defaultAction)
        }
    }
}
