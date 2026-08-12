package app.fukaha

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EmbedHealthTest {
    private val catalog = EmbedCatalog.bundled()

    private fun service(host: String): EmbedService =
        EmbedService(name = host, host = host)

    @Test
    fun preferredAliveOrUnknownIsUsed() {
        val preferred = service("https://preferred.example")
        val other = service("https://other.example")
        val health = mapOf(
            EmbedHealthKeys.normalize(preferred.normalizedHost()) to EmbedHealthStatus.Unknown,
            EmbedHealthKeys.normalize(other.normalizedHost()) to EmbedHealthStatus.Alive,
        )
        val picked = EmbedHealthPolicy.pickService(
            services = listOf(preferred, other),
            preferred = preferred,
            default = other,
            health = health,
        )
        assertEquals(preferred.normalizedHost(), picked?.normalizedHost())
    }

    @Test
    fun skipsDeadPreferredForAliveAlternative() {
        val preferred = service("https://dead.example")
        val alive = service("https://alive.example")
        val health = mapOf(
            EmbedHealthKeys.normalize(preferred.normalizedHost()) to EmbedHealthStatus.Dead,
            EmbedHealthKeys.normalize(alive.normalizedHost()) to EmbedHealthStatus.Alive,
        )
        val picked = EmbedHealthPolicy.pickService(
            services = listOf(preferred, alive),
            preferred = preferred,
            default = preferred,
            health = health,
        )
        assertEquals(alive.normalizedHost(), picked?.normalizedHost())
    }

    @Test
    fun allDeadFallsBackToPreferred() {
        val preferred = service("https://dead-a.example")
        val other = service("https://dead-b.example")
        val health = mapOf(
            EmbedHealthKeys.normalize(preferred.normalizedHost()) to EmbedHealthStatus.Dead,
            EmbedHealthKeys.normalize(other.normalizedHost()) to EmbedHealthStatus.Dead,
        )
        val picked = EmbedHealthPolicy.pickService(
            services = listOf(preferred, other),
            preferred = preferred,
            default = other,
            health = health,
        )
        assertEquals(preferred.normalizedHost(), picked?.normalizedHost())
    }

    @Test
    fun rewriteSkipsDeadPreferredTwitterFixer() {
        val health = mapOf(
            EmbedHealthKeys.normalize("https://vxtwitter.com") to EmbedHealthStatus.Dead,
            EmbedHealthKeys.normalize("https://twitterez.com") to EmbedHealthStatus.Alive,
        )
        val services = catalog.activeServices("twitter")
        assertTrue(services.size >= 2)
        val embed = catalog.rewriteToEmbed(
            url = "https://twitter.com/foo/status/99",
            preferredFixerHost = "https://vxtwitter.com",
            health = health,
        )
        assertEquals("https://twitterez.com/foo/status/99", embed)
    }

    @Test
    fun classifyProbeResult() {
        assertEquals(EmbedHealthStatus.Alive, EmbedHealthChecker.classifyProbeResult(true))
        assertEquals(EmbedHealthStatus.Dead, EmbedHealthChecker.classifyProbeResult(false))
    }

    @Test
    fun autoRefreshDueWhenNeverChecked() {
        assertTrue(EmbedHealthPolicy.isDue(null, nowEpochMs = 1_000L))
        assertFalse(
            EmbedHealthPolicy.isDue(
                checkedAtEpochMs = 1_000L,
                nowEpochMs = 1_000L + EmbedHealthPolicy.AUTO_REFRESH_INTERVAL_MS - 1,
            ),
        )
        assertTrue(
            EmbedHealthPolicy.isDue(
                checkedAtEpochMs = 1_000L,
                nowEpochMs = 1_000L + EmbedHealthPolicy.AUTO_REFRESH_INTERVAL_MS,
            ),
        )
    }

    @Test
    fun normalizeHealthKeys() {
        assertEquals("https://fixvx.com", EmbedHealthKeys.normalize("https://fixvx.com/"))
        assertEquals("https://fixvx.com", EmbedHealthKeys.normalize("fixvx.com"))
        assertEquals("https://fixvx.com", EmbedHealthKeys.normalize("HTTPS://FixVX.com"))
    }
}
