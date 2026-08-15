package app.fukaha

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FukahaSettingsTest {
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
