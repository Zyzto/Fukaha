package app.fukaha

/**
 * Live reachability of an embed-fixer host.
 * Untested hosts stay [Unknown] so they remain usable until probed.
 */
enum class EmbedHealthStatus {
    Alive,
    Dead,
    Unknown,
}

data class EmbedHealthSnapshot(
    val statuses: Map<String, EmbedHealthStatus> = emptyMap(),
    val checkedAtEpochMs: Long? = null,
) {
    val aliveCount: Int get() = statuses.values.count { it == EmbedHealthStatus.Alive }
    val deadCount: Int get() = statuses.values.count { it == EmbedHealthStatus.Dead }

    fun statusOf(host: String): EmbedHealthStatus {
        val key = EmbedHealthKeys.normalize(host)
        return statuses[key] ?: EmbedHealthStatus.Unknown
    }
}

/** Live progress of an embedder probe run. [currentIndex] counts completed probes. */
data class EmbedHealthProgress(
    val currentHost: String,
    val currentIndex: Int,
    val total: Int,
    val aliveCount: Int = 0,
    val deadCount: Int = 0,
) {
    val displayHost: String get() = EmbedHealthKeys.displayHost(currentHost)
    val completedCount: Int get() = (aliveCount + deadCount).coerceAtMost(total)
    val fraction: Float get() = if (total <= 0) 0f else completedCount.toFloat() / total.toFloat()
}

object EmbedHealthKeys {
    fun normalize(host: String): String {
        val trimmed = host.trim().trimEnd('/').lowercase()
        return when {
            trimmed.startsWith("https://") || trimmed.startsWith("http://") -> trimmed
            trimmed.isEmpty() -> trimmed
            else -> "https://$trimmed"
        }
    }

    fun probeUrl(host: String): String {
        val key = normalize(host)
        return if (key.endsWith("/")) key else "$key/"
    }

    fun displayHost(host: String): String =
        normalize(host)
            .removePrefix("https://")
            .removePrefix("http://")
            .trimEnd('/')
}

interface EmbedHealthStore {
    fun observe(): kotlinx.coroutines.flow.Flow<EmbedHealthSnapshot>

    suspend fun get(): EmbedHealthSnapshot

    suspend fun save(
        statuses: Map<String, EmbedHealthStatus>,
        checkedAtEpochMs: Long,
    )
}

object EmbedHealthPolicy {
    const val AUTO_REFRESH_INTERVAL_MS: Long = 6L * 60L * 60L * 1000L
    const val MANUAL_REFRESH_COOLDOWN_MS: Long = 10L * 60L * 1000L
    const val PROBE_CONCURRENCY: Int = 6
    const val PROBE_TIMEOUT_MS: Long = 5_000L

    fun isDue(checkedAtEpochMs: Long?, nowEpochMs: Long = PlatformClock.epochMillis()): Boolean {
        if (checkedAtEpochMs == null) return true
        return nowEpochMs - checkedAtEpochMs >= AUTO_REFRESH_INTERVAL_MS
    }

    /**
     * A run only counts when at least one host answered: everything failing means the
     * device is offline, not that the embedders died. Such runs keep the previous
     * snapshot and leave the manual check available for an immediate retry.
     */
    fun isUsableResult(statuses: Map<String, EmbedHealthStatus>): Boolean =
        statuses.values.any { it == EmbedHealthStatus.Alive }

    /** Time left before another manual check is allowed; 0 once it is available. */
    fun cooldownRemainingMs(
        checkedAtEpochMs: Long?,
        nowEpochMs: Long = PlatformClock.epochMillis(),
    ): Long {
        if (checkedAtEpochMs == null) return 0L
        val elapsed = nowEpochMs - checkedAtEpochMs
        if (elapsed < 0L) return 0L
        return (MANUAL_REFRESH_COOLDOWN_MS - elapsed).coerceAtLeast(0L)
    }

    /**
     * Preferred if Alive/Unknown; else the first Alive and then the first unprobed service,
     * searched from just after the pick and wrapping the list, or in default-then-catalogue
     * order when there is no pick; else preferred/default/first.
     */
    fun pickService(
        services: List<EmbedService>,
        preferred: EmbedService?,
        default: EmbedService?,
        health: Map<String, EmbedHealthStatus>,
    ): EmbedService? {
        if (services.isEmpty()) return null

        fun statusOf(service: EmbedService): EmbedHealthStatus {
            val key = EmbedHealthKeys.normalize(service.normalizedHost())
            return health[key] ?: EmbedHealthStatus.Unknown
        }

        if (preferred != null && statusOf(preferred) != EmbedHealthStatus.Dead) {
            return preferred
        }

        // A dead pick hands over to its neighbour rather than to the catalogue default:
        // the stand-in should sit next to the choice the user actually made. Without a
        // pick there is no position to stay near, so the default keeps leading.
        val anchor = preferred
            ?.let { EmbedHealthKeys.normalize(it.normalizedHost()) }
            ?.let { key ->
                services.indexOfFirst { EmbedHealthKeys.normalize(it.normalizedHost()) == key }
            }
            ?.takeIf { it >= 0 }

        val ordered = if (anchor != null) {
            List(services.size - 1) { services[(anchor + 1 + it) % services.size] }
        } else {
            buildList {
                default?.let { add(it) }
                addAll(services)
            }.distinctBy { EmbedHealthKeys.normalize(it.normalizedHost()) }
        }

        ordered.firstOrNull { statusOf(it) == EmbedHealthStatus.Alive }?.let { return it }
        ordered.firstOrNull { statusOf(it) == EmbedHealthStatus.Unknown }?.let { return it }

        return preferred ?: default ?: services.first()
    }
}
