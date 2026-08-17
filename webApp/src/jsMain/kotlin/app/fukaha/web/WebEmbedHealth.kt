package app.fukaha.web

import app.fukaha.EmbedHealthKeys
import app.fukaha.EmbedHealthPolicy
import app.fukaha.EmbedHealthProgress
import app.fukaha.EmbedHealthSnapshot
import app.fukaha.EmbedHealthStatus
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.coroutines.async
import kotlinx.coroutines.await
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.js.Promise

/**
 * Reachability probing for embed-fixer hosts, from inside a browser.
 *
 * CORS stops us reading a third-party response, but it does not stop us learning whether one
 * arrived: a `no-cors` fetch resolves with an opaque response for any HTTP status and rejects
 * only on a network-level failure (DNS, refused connection, bad TLS, timeout). That is the same
 * distinction [app.fukaha.EmbedHealthChecker] draws on Android, where the Ktor client runs with
 * `expectSuccess = false` and treats any status as alive — so a 404 counts as reachable in both.
 *
 * An `<iframe>` cannot do this job: `onload` fires for error pages too, and hosts sending
 * `X-Frame-Options` produce a blocked frame that is indistinguishable from a dead one.
 */
object WebEmbedHealth {
    /** Different host per request, so a small pool is polite and finishes in seconds, not a minute. */
    private const val CONCURRENCY = 6

    private val abortSignal: dynamic = js("AbortSignal")

    suspend fun probe(host: String): EmbedHealthStatus {
        val url = EmbedHealthKeys.probeUrl(host)
        // Some hosts reject HEAD outright; fall back to GET exactly as the Android probe does.
        val reachable = request(url, "HEAD") || request(url, "GET")
        return if (reachable) EmbedHealthStatus.Alive else EmbedHealthStatus.Dead
    }

    /**
     * Probes every host once, reporting progress as results land. Order of completion is not the
     * order of [hosts], so progress counts completed probes rather than a position in the list.
     */
    suspend fun refresh(
        hosts: List<String>,
        onProgress: (EmbedHealthProgress) -> Unit,
    ): Map<String, EmbedHealthStatus> = coroutineScope {
        val results = LinkedHashMap<String, EmbedHealthStatus>(hosts.size)
        var alive = 0
        var dead = 0
        // Coroutines on Kotlin/JS run on one thread, so a plain cursor needs no synchronisation.
        var next = 0

        List(minOf(CONCURRENCY, hosts.size)) {
            async {
                while (true) {
                    val index = next
                    if (index >= hosts.size) break
                    next = index + 1

                    val host = hosts[index]
                    val status = probe(host)
                    results[host] = status
                    if (status == EmbedHealthStatus.Alive) alive++ else dead++
                    onProgress(
                        EmbedHealthProgress(
                            currentHost = host,
                            currentIndex = results.size,
                            total = hosts.size,
                            aliveCount = alive,
                            deadCount = dead,
                        ),
                    )
                }
            }
        }.awaitAll()

        results
    }

    private suspend fun request(url: String, method: String): Boolean {
        val init: dynamic = js("({})")
        init.method = method
        init.mode = "no-cors"
        init.cache = "no-store"
        init.redirect = "follow"
        init.referrerPolicy = "no-referrer"
        init.signal = abortSignal.timeout(EmbedHealthPolicy.PROBE_TIMEOUT_MS.toInt())
        return runCatching {
            (window.asDynamic().fetch(url, init) as Promise<*>).await()
        }.isSuccess
    }
}

/** localStorage-backed snapshot, mirroring what `EmbedHealthStore` persists on Android. */
class WebHealthStore {
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class Stored(
        val statuses: Map<String, String> = emptyMap(),
        val checkedAt: Long? = null,
    )

    fun get(): EmbedHealthSnapshot {
        val raw = localStorage.getItem(KEY) ?: return EmbedHealthSnapshot()
        val stored = runCatching { json.decodeFromString<Stored>(raw) }.getOrNull()
            ?: return EmbedHealthSnapshot()
        return EmbedHealthSnapshot(
            statuses = stored.statuses.mapNotNull { (host, status) ->
                EmbedHealthStatus.entries.firstOrNull { it.name == status }?.let { host to it }
            }.toMap(),
            checkedAtEpochMs = stored.checkedAt,
        )
    }

    fun save(statuses: Map<String, EmbedHealthStatus>, checkedAtEpochMs: Long) {
        val stored = Stored(
            statuses = statuses.mapValues { it.value.name },
            checkedAt = checkedAtEpochMs,
        )
        localStorage.setItem(KEY, json.encodeToString(stored))
    }

    private companion object {
        const val KEY = "fukaha_embed_health"
    }
}
