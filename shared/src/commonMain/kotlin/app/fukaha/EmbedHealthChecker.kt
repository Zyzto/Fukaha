package app.fukaha

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.head
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.coroutineContext

internal enum class EmbedHealthRequestMethod {
    Head,
    Get,
}

class EmbedHealthChecker private constructor(
    private val catalog: EmbedCatalog,
    private val requestStatus: suspend (String, EmbedHealthRequestMethod) -> Int,
    private val closeRequest: () -> Unit,
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
     * Probes unique fixer hosts with a small bounded worker pool.
     * [onProgress] is invoked as each result is recorded.
     * Throws [CancellationException] if cancelled mid-run (partial results not saved by caller).
     */
    suspend fun refresh(
        onProgress: ((EmbedHealthProgress) -> Unit)? = null,
    ): Map<String, EmbedHealthStatus> = coroutineScope {
        val hosts = uniqueHosts()
        if (hosts.isEmpty()) return@coroutineScope emptyMap()

        val results = mutableMapOf<String, EmbedHealthStatus>()
        val cursorMutex = Mutex()
        val resultsMutex = Mutex()
        var nextIndex = 0
        var alive = 0
        var dead = 0

        suspend fun takeNextHost(): String? = cursorMutex.withLock {
            if (nextIndex >= hosts.size) {
                null
            } else {
                hosts[nextIndex++]
            }
        }

        List(minOf(EmbedHealthPolicy.PROBE_CONCURRENCY, hosts.size)) {
            async {
                while (true) {
                    coroutineContext.ensureActive()
                    val host = takeNextHost() ?: break
                    val status = probe(host)
                    resultsMutex.withLock {
                        results[host] = status
                        if (status == EmbedHealthStatus.Alive) alive++ else dead++
                        onProgress?.invoke(
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
            }
        }.awaitAll()

        hosts.associateWith { results.getValue(it) }
    }

    suspend fun probe(host: String): EmbedHealthStatus {
        val url = EmbedHealthKeys.probeUrl(host)
        val headStatus = requestStatusOrNull(url, EmbedHealthRequestMethod.Head)
        val status = when {
            headStatus == null -> requestStatusOrNull(url, EmbedHealthRequestMethod.Get)
            headStatus == HEAD_NOT_SUPPORTED || headStatus == METHOD_NOT_ALLOWED ->
                requestStatusOrNull(url, EmbedHealthRequestMethod.Get)
            else -> headStatus
        }
        return classifyHttpStatus(status)
    }

    private suspend fun requestStatusOrNull(
        url: String,
        method: EmbedHealthRequestMethod,
    ): Int? = try {
        requestStatus(url, method)
    } catch (error: CancellationException) {
        throw error
    } catch (_: Throwable) {
        null
    }

    fun close() {
        closeRequest()
    }

    companion object {
        fun create(catalog: EmbedCatalog = EmbedCatalog.bundled()): EmbedHealthChecker =
            probeHttpClient().let { client ->
                EmbedHealthChecker(
                    catalog = catalog,
                    requestStatus = { url, method ->
                        when (method) {
                            EmbedHealthRequestMethod.Head -> client.head(url).status.value
                            EmbedHealthRequestMethod.Get -> client.get(url).status.value
                        }
                    },
                    closeRequest = client::close,
                )
            }

        internal fun createForTests(
            catalog: EmbedCatalog,
            requestStatus: suspend (String, EmbedHealthRequestMethod) -> Int,
        ): EmbedHealthChecker = EmbedHealthChecker(catalog, requestStatus, {})

        internal fun classifyHttpStatus(statusCode: Int?): EmbedHealthStatus =
            if (statusCode in 200..299) EmbedHealthStatus.Alive else EmbedHealthStatus.Dead

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

        private const val HEAD_NOT_SUPPORTED = 501
        private const val METHOD_NOT_ALLOWED = 405
    }
}
