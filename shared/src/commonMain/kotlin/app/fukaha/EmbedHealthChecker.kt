package app.fukaha

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.head
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

class EmbedHealthChecker private constructor(
    private val catalog: EmbedCatalog,
    private val httpClient: HttpClient,
) {
    /** The catalog never changes at runtime, so the probe list is built once. */
    private val hosts: List<String> by lazy {
        catalog.platformKeys()
            .flatMap { catalog.activeServices(it) }
            .map { EmbedHealthKeys.normalize(it.normalizedHost()) }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    fun uniqueHosts(): List<String> = hosts

    /**
     * Probes each unique fixer host sequentially with a pause between requests.
     * [onProgress] is invoked before each probe (so the UI can show the current host)
     * and again after the result is recorded.
     * Throws [CancellationException] if cancelled mid-run (partial results not saved by caller).
     */
    suspend fun refresh(
        onProgress: ((EmbedHealthProgress) -> Unit)? = null,
    ): Map<String, EmbedHealthStatus> {
        val hosts = uniqueHosts()
        val results = LinkedHashMap<String, EmbedHealthStatus>(hosts.size)
        var alive = 0
        var dead = 0
        for ((index, host) in hosts.withIndex()) {
            coroutineContext.ensureActive()
            val position = index + 1
            onProgress?.invoke(
                EmbedHealthProgress(
                    currentHost = host,
                    currentIndex = position,
                    total = hosts.size,
                    aliveCount = alive,
                    deadCount = dead,
                ),
            )
            val status = probe(host)
            results[host] = status
            if (status == EmbedHealthStatus.Alive) alive++ else dead++
            onProgress?.invoke(
                EmbedHealthProgress(
                    currentHost = host,
                    currentIndex = position,
                    total = hosts.size,
                    aliveCount = alive,
                    deadCount = dead,
                ),
            )
            if (index < hosts.lastIndex) {
                delay(EmbedHealthPolicy.INTER_HOST_DELAY_MS)
            }
        }
        return results
    }

    suspend fun probe(host: String): EmbedHealthStatus {
        val url = EmbedHealthKeys.probeUrl(host)
        return try {
            try {
                httpClient.head(url)
                EmbedHealthStatus.Alive
            } catch (headError: CancellationException) {
                throw headError
            } catch (_: Throwable) {
                httpClient.get(url)
                EmbedHealthStatus.Alive
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            EmbedHealthStatus.Dead
        }
    }

    fun close() {
        httpClient.close()
    }

    companion object {
        fun create(catalog: EmbedCatalog = EmbedCatalog.bundled()): EmbedHealthChecker =
            EmbedHealthChecker(catalog, probeHttpClient())

        fun probeHttpClient(): HttpClient = HttpClient {
            expectSuccess = false
            followRedirects = true
            install(HttpTimeout) {
                requestTimeoutMillis = EmbedHealthPolicy.PROBE_TIMEOUT_MS
                connectTimeoutMillis = EmbedHealthPolicy.PROBE_TIMEOUT_MS
                socketTimeoutMillis = EmbedHealthPolicy.PROBE_TIMEOUT_MS
            }
        }

        /** Pure helper for tests: HTTP layer succeeded → Alive, else Dead. */
        fun classifyProbeResult(succeeded: Boolean): EmbedHealthStatus =
            if (succeeded) EmbedHealthStatus.Alive else EmbedHealthStatus.Dead
    }
}
