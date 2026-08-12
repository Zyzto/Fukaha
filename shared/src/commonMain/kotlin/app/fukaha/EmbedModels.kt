package app.fukaha

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EmbedPlatform(
    val href: String? = null,
    val name: String? = null,
    val services: List<EmbedService> = emptyList(),
)

@Serializable
data class EmbedService(
    val name: String,
    val host: String,
    val author: String? = null,
    val description: String? = null,
    val repo: String? = null,
    @SerialName("alternate_hosts")
    val alternateHosts: List<String> = emptyList(),
    @SerialName("brokenSince")
    val brokenSince: String? = null,
) {
    val isBroken: Boolean get() = !brokenSince.isNullOrBlank()

    fun normalizedHost(): String = host.trim().trimEnd('/')
}

data class DetectedLink(
    val originalUrl: String,
    val cleanedUrl: String,
    val platformKey: String?,
    val platformName: String?,
)
