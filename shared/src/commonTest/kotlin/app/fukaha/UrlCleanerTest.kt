package app.fukaha

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UrlCleanerTest {
    @Test
    fun extractsFirstUrl() {
        val text = "check this https://x.com/user/status/123?utm_source=share and more"
        assertEquals(
            "https://x.com/user/status/123?utm_source=share",
            UrlCleaner.extractFirstUrl(text),
        )
    }

    @Test
    fun stripsTrackingParams() {
        val dirty = "https://www.instagram.com/reel/ABC123/?igshid=xyz&utm_source=ig&utm_medium=social&si=1"
        val clean = UrlCleaner.clean(dirty)
        assertEquals("https://instagram.com/reel/ABC123", clean)
        assertFalse(clean.contains("utm_"))
        assertFalse(clean.contains("igshid"))
        assertFalse(clean.contains("si="))
    }

    @Test
    fun detectsShortLinks() {
        assertTrue(UrlCleaner.isShortLink("https://t.co/abcdef"))
        assertTrue(UrlCleaner.isShortLink("https://vm.tiktok.com/ZMxxxxx/"))
        assertFalse(UrlCleaner.isShortLink("https://tiktok.com/@user/video/1"))
    }
}

class EmbedCatalogTest {
    private val catalog = EmbedCatalog.bundled()

    @Test
    fun detectsTwitterAndRewrites() {
        val url = "https://twitter.com/foo/status/123456?utm_source=share"
        val detected = catalog.detect(url)
        assertEquals("twitter", detected.platformKey)
        assertEquals("https://twitter.com/foo/status/123456", detected.cleanedUrl)
        val embed = catalog.rewriteToEmbed(url)
        assertEquals("https://vxtwitter.com/foo/status/123456", embed)
    }

    @Test
    fun detectsXAndUsesFixvx() {
        val embed = catalog.rewriteToEmbed("https://x.com/foo/status/99")
        assertEquals("https://fixvx.com/foo/status/99", embed)
    }

    @Test
    fun prefersCustomFixer() {
        val embed = catalog.rewriteToEmbed(
            "https://tiktok.com/@a/video/1",
            preferredFixerHost = "https://vxtiktok.com",
        )
        assertEquals("https://vxtiktok.com/@a/video/1", embed)
    }

    @Test
    fun skipsBrokenTwitchServices() {
        assertTrue(catalog.activeServices("twitch").isEmpty())
        assertNull(catalog.rewriteToEmbed("https://twitch.tv/somechannel"))
    }

    @Test
    fun listsActiveInstagramServices() {
        val services = catalog.activeServices("instagram")
        assertTrue(services.any { it.name == "InstaFix" })
        assertNotNull(catalog.defaultFixerHost("instagram"))
    }
}
