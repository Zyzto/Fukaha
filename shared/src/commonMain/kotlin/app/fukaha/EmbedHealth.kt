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
    fun statusOf(host: String): EmbedHealthStatus {
        val key = EmbedHealthKeys.normalize(host)
        return statuses[key] ?: EmbedHealthStatus.Unknown
    }
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
    const val INTER_HOST_DELAY_MS: Long = 750L
    const val PROBE_TIMEOUT_MS: Long = 5_000L

    fun isDue(checkedAtEpochMs: Long?, nowEpochMs: Long = PlatformClock.epochMillis()): Boolean {
        if (checkedAtEpochMs == null) return true
        return nowEpochMs - checkedAtEpochMs >= AUTO_REFRESH_INTERVAL_MS
    }

    /** Preferred if Alive/Unknown; else first Alive; else preferred/default/first. */
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

        val ordered = buildList {
            default?.let { add(it) }
            addAll(services)
        }.distinctBy { EmbedHealthKeys.normalize(it.normalizedHost()) }

        ordered.firstOrNull { statusOf(it) == EmbedHealthStatus.Alive }?.let { return it }

        return preferred ?: default ?: services.first()
    }
}
