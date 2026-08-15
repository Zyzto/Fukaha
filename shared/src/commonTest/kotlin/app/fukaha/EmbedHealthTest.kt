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
    fun prefersUnprobedOverKnownDead() {
        val preferred = service("https://dead.example")
        val unprobed = service("https://unprobed.example")
        val health = mapOf(
            EmbedHealthKeys.normalize(preferred.normalizedHost()) to EmbedHealthStatus.Dead,
        )
        val picked = EmbedHealthPolicy.pickService(
            services = listOf(preferred, unprobed),
            preferred = preferred,
            default = preferred,
            health = health,
        )
        assertEquals(unprobed.normalizedHost(), picked?.normalizedHost())
    }

    @Test
    fun effectiveServiceReportsTheStandInForADeadPick() {
        val services = catalog.activeServices("threads")
        assertTrue(services.size >= 2)
        val dead = services.first()
        val alive = services[1]
        val health = mapOf(
            EmbedHealthKeys.normalize(dead.normalizedHost()) to EmbedHealthStatus.Dead,
            EmbedHealthKeys.normalize(alive.normalizedHost()) to EmbedHealthStatus.Alive,
        )
        val effective = catalog.effectiveService("threads", dead.normalizedHost(), health)
        assertEquals(alive.normalizedHost(), effective?.normalizedHost())
        assertEquals(
            effective?.normalizedHost(),
            catalog.rewriteToEmbed(
                url = "https://threads.net/@a/post/1",
                preferredFixerHost = dead.normalizedHost(),
                health = health,
            )?.let { UrlCleaner.hostOf(it) }?.let { "https://$it" },
        )
    }

    @Test
    fun serviceForHostMatchesAlternateHosts() {
        val fx = catalog.serviceForHost("x", "https://fxtwitter.com")
        assertEquals("FxEmbed", fx?.name)
        assertEquals("https://fixupx.com", fx?.normalizedHost())
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
            EmbedHealthKeys.normalize("https://fixvx.com") to EmbedHealthStatus.Dead,
            EmbedHealthKeys.normalize("https://fixupx.com") to EmbedHealthStatus.Dead,
            EmbedHealthKeys.normalize("https://twitterez.com") to EmbedHealthStatus.Alive,
        )
        val services = catalog.activeServices("x")
        assertTrue(services.size >= 2)
        val embed = catalog.rewriteToEmbed(
            url = "https://twitter.com/foo/status/99",
            preferredFixerHost = "https://fixvx.com",
            health = health,
        )
        assertEquals("https://twitterez.com/foo/status/99", embed)
    }

    @Test
    fun runWithNoAliveHostIsNotUsable() {
        val allDead = mapOf(
            "https://a.example" to EmbedHealthStatus.Dead,
            "https://b.example" to EmbedHealthStatus.Dead,
        )
        assertFalse(EmbedHealthPolicy.isUsableResult(allDead))
        assertFalse(EmbedHealthPolicy.isUsableResult(emptyMap()))
        assertTrue(
            EmbedHealthPolicy.isUsableResult(
                allDead + ("https://c.example" to EmbedHealthStatus.Alive),
            ),
        )
    }

    @Test
    fun manualCheckCoolsDownAfterASavedRun() {
        val checkedAt = 10_000L
        assertEquals(
            EmbedHealthPolicy.MANUAL_REFRESH_COOLDOWN_MS,
            EmbedHealthPolicy.cooldownRemainingMs(checkedAt, nowEpochMs = checkedAt),
        )
        assertEquals(
            0L,
            EmbedHealthPolicy.cooldownRemainingMs(
                checkedAt,
                nowEpochMs = checkedAt + EmbedHealthPolicy.MANUAL_REFRESH_COOLDOWN_MS,
            ),
        )
        assertEquals(0L, EmbedHealthPolicy.cooldownRemainingMs(null, nowEpochMs = checkedAt))
    }

    @Test
    fun classifyProbeResult() {
        assertEquals(EmbedHealthStatus.Alive, EmbedHealthChecker.classifyProbeResult(true))
        assertEquals(EmbedHealthStatus.Dead, EmbedHealthChecker.classifyProbeResult(false))
    }

    @Test
    fun uniqueHostsAreCachedDistinctAndSorted() {
        val checker = EmbedHealthChecker.create(catalog)
        try {
            val hosts = checker.uniqueHosts()
            assertTrue(hosts.isNotEmpty())
            assertEquals(hosts, hosts.distinct())
            assertEquals(hosts.sorted(), hosts)
            assertTrue(hosts === checker.uniqueHosts())
            assertTrue(hosts.all { it.startsWith("https://") && !it.endsWith("/") })
        } finally {
            checker.close()
        }
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

    @Test
    fun progressShowsHostAndFraction() {
        val progress = EmbedHealthProgress(
            currentHost = "https://koutube.com/",
            currentIndex = 12,
            total = 40,
            aliveCount = 8,
            deadCount = 3,
        )
        assertEquals("koutube.com", progress.displayHost)
        assertEquals(11, progress.completedCount)
        assertEquals(11f / 40f, progress.fraction)
        assertEquals(2, EmbedHealthSnapshot(
            statuses = mapOf(
                "https://a.example" to EmbedHealthStatus.Alive,
                "https://b.example" to EmbedHealthStatus.Alive,
                "https://c.example" to EmbedHealthStatus.Dead,
            ),
        ).aliveCount)
    }
}
