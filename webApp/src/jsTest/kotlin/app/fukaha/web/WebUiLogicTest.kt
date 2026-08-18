package app.fukaha.web

import app.fukaha.AppLanguage
import app.fukaha.ShareAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WebUiLogicTest {
    @Test
    fun languageTagsResolveSupportedRegionalFallbacksInBrowserOrder() {
        assertEquals(AppLanguage.Arabic, resolveLanguageTags(listOf("ar-SA")))
        assertEquals(AppLanguage.Japanese, resolveLanguageTags(listOf("ja-JP")))
        assertEquals(AppLanguage.SimplifiedChinese, resolveLanguageTags(listOf("zh-CN")))
        assertEquals(AppLanguage.SimplifiedChinese, resolveLanguageTags(listOf("zh-SG")))
        assertEquals(AppLanguage.SimplifiedChinese, resolveLanguageTags(listOf("zh-Hans-HK")))
        assertEquals(AppLanguage.Spanish, resolveLanguageTags(listOf("es-MX")))
        assertEquals(AppLanguage.English, resolveLanguageTags(listOf("fr-FR", "en-GB")))
        assertEquals(AppLanguage.English, resolveLanguageTags(listOf("fr-FR", "zh-TW")))
        assertEquals(AppLanguage.Arabic, resolveLanguageTags(listOf("fr-FR", "AR-EG", "en")))
        assertEquals(AppLanguage.Spanish, resolveLanguageTags(listOf("archive", "es-419")))
        assertEquals(AppLanguage.English, resolveLanguageTags(listOf("jargon", "enochian")))
    }

    @Test
    fun onlyArabicUsesRtlAndEveryLocaleHasTheExpectedTag() {
        val expectedTags = mapOf(
            AppLanguage.English to "en",
            AppLanguage.Arabic to "ar",
            AppLanguage.Japanese to "ja",
            AppLanguage.SimplifiedChinese to "zh-CN",
            AppLanguage.Spanish to "es",
        )

        expectedTags.forEach { (language, tag) ->
            assertEquals(tag, Strings.languageTag(language))
            assertEquals(language == AppLanguage.Arabic, Strings.isArabic(language))
        }
    }

    @Test
    fun languageMenuCoversEachNonSystemLocaleExactlyOnce() {
        assertEquals(
            AppLanguage.entries.filterNot { it == AppLanguage.System }.toSet(),
            LANGUAGE_OPTIONS.map { it.language }.toSet(),
        )
        assertEquals(LANGUAGE_OPTIONS.size, LANGUAGE_OPTIONS.map { it.language }.distinct().size)
        assertTrue(LANGUAGE_OPTIONS.all { it.flag.isNotBlank() && it.code.isNotBlank() })
    }

    @Test
    fun sampleUsesTheMakkahStatusAndTrackingParameters() {
        assertTrue(SAMPLE_LINK.startsWith("https://x.com/makkahregion/status/1902619525532512361"))
        assertTrue(SAMPLE_LINK.contains("utm_source=share"))
        assertTrue(SAMPLE_LINK.contains("fbclid="))
    }

    @Test
    fun askOpensOptionsWhileCleanAndEmbedChooseImmediateCopyTargets() {
        val cleaned = "https://x.com/user/status/1"
        val embed = "https://fixupx.com/user/status/1"

        assertNull(immediateHomeTarget(ShareAction.Ask, cleaned, embed))
        assertNull(immediateHomeTarget(ShareAction.Download, cleaned, embed))
        assertEquals(cleaned, immediateHomeTarget(ShareAction.Clean, cleaned, embed))
        assertEquals(embed, immediateHomeTarget(ShareAction.Embed, cleaned, embed))
        assertEquals(cleaned, immediateHomeTarget(ShareAction.Embed, cleaned, null))
    }

    @Test
    fun successfulCopyClearsOnlyTheSubmittedDraftAndFailureRetainsIt() {
        val submitted = "x.com/user/status/1"
        assertEquals("", draftAfterImmediateCopy(true, submitted, submitted))
        assertEquals(submitted, draftAfterImmediateCopy(false, submitted, submitted))
        assertEquals(
            "new draft",
            draftAfterImmediateCopy(true, "new draft", submitted),
            "an in-flight copy must not erase newer input",
        )
    }

    @Test
    fun unsupportedShareSheetNoticeIsAndroidOnly() {
        assertTrue(shouldShowAndroidInstallNotice(canPrompt = false, isAndroid = true, isIos = false))
        assertFalse(shouldShowAndroidInstallNotice(canPrompt = true, isAndroid = true, isIos = false))
        assertFalse(shouldShowAndroidInstallNotice(canPrompt = false, isAndroid = false, isIos = false))
        assertFalse(shouldShowAndroidInstallNotice(canPrompt = false, isAndroid = false, isIos = true))
    }

    @Test
    fun arbitraryNestedAndTrailingDocumentPathsCanonicalizeToRoot() {
        for (path in listOf("/foo", "/settings", "/nested/path", "/nested/path/", "/release.v2")) {
            val startup = resolveStartupNavigation(path, "", "")

            assertEquals("/", startup.canonicalUrl, path)
            assertTrue(startup.shouldReplace, path)
            assertNull(startup.sharedText, path)
        }
    }

    @Test
    fun canonicalizationPreservesQueryAndHashUntilStartupCanConsumeThem() {
        val startup = resolveStartupNavigation(
            pathname = "/nested/path",
            search = "?locale=ja&mode=compact",
            hash = "#install-help",
        )

        assertEquals("/?locale=ja&mode=compact#install-help", startup.canonicalUrl)
        assertNull(startup.sharedText)
        assertTrue(startup.shouldReplace)
    }

    @Test
    fun shareTargetFieldsAreCapturedOnceWhileOtherStartupStateSurvives() {
        val startup = resolveStartupNavigation(
            pathname = "/shared/from/browser",
            search = "?text=See+https%3A%2F%2Fx.com%2Fp%2F1&title=Post&locale=ar",
            hash = "#result",
        )

        assertEquals("See https://x.com/p/1 Post", startup.sharedText)
        assertEquals("/?locale=ar#result", startup.canonicalUrl)
        assertTrue(startup.shouldReplace)
    }

    @Test
    fun canonicalRootWithoutConsumedShareStateIsANoOp() {
        val startup = resolveStartupNavigation("/", "?locale=es", "#home")

        assertEquals("/?locale=es#home", startup.canonicalUrl)
        assertNull(startup.sharedText)
        assertFalse(startup.shouldReplace)
    }

    @Test
    fun appearanceTransitionsAreSerializedAndReleasedForCleanup() {
        val gate = AppearanceTransitionGate()
        assertTrue(gate.acquire())
        assertTrue(gate.isActive)
        assertFalse(gate.acquire())
        gate.release()
        assertFalse(gate.isActive)
        assertTrue(gate.acquire())
    }
}
