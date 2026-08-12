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
    fun uniqueHosts(): List<String> =
        catalog.platformKeys()
            .flatMap { catalog.activeServices(it) }
            .map { EmbedHealthKeys.normalize(it.normalizedHost()) }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

    /**
     * Probes each unique fixer host sequentially with a pause between requests.
     * Throws [CancellationException] if cancelled mid-run (partial results not saved by caller).
     */
    suspend fun refresh(): Map<String, EmbedHealthStatus> {
        val hosts = uniqueHosts()
        val results = LinkedHashMap<String, EmbedHealthStatus>(hosts.size)
        for ((index, host) in hosts.withIndex()) {
            coroutineContext.ensureActive()
            results[host] = probe(host)
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
