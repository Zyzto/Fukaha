package app.fukaha

/**
 * Facade used by Android and the iOS Share Extension.
 */
class FukahaBridge(
    private val processor: LinkProcessor = LinkProcessor(),
) {
    private val catalog = processor.catalog()

    fun extractUrl(text: String): String? = UrlCleaner.extractFirstUrl(text)

    fun clean(url: String): String = UrlCleaner.clean(url)

    fun detectPlatform(url: String): String? = catalog.detectPlatformKey(url)

    fun embed(
        url: String,
        preferredFixerHost: String? = null,
        health: Map<String, EmbedHealthStatus> = emptyMap(),
    ): String? = catalog.rewriteToEmbed(url, preferredFixerHost, health)

    fun defaultFixer(platformKey: String): String? = catalog.defaultFixerHost(platformKey)

    fun serviceForHost(platformKey: String, host: String?): EmbedService? =
        catalog.serviceForHost(platformKey, host)

    /** Fixer [embed] would really route through, so a UI can show it without guessing. */
    fun effectiveService(
        platformKey: String,
        preferredFixerHost: String? = null,
        health: Map<String, EmbedHealthStatus> = emptyMap(),
    ): EmbedService? = catalog.effectiveService(platformKey, preferredFixerHost, health)

    fun platformKeys(): List<String> = catalog.platformKeys()

    fun servicesFor(platformKey: String): List<EmbedService> = catalog.activeServices(platformKey)

    suspend fun prepare(
        text: String,
        settings: FukahaSettings,
        health: Map<String, EmbedHealthStatus> = emptyMap(),
    ): PreparedLink? = processor.prepare(text, settings, health)

    suspend fun download(
        url: String,
        settings: FukahaSettings,
        cacheDirPath: String,
    ): MediaDownloadResult = processor.downloadMedia(url, settings, cacheDirPath)

    fun close() = processor.close()
}
