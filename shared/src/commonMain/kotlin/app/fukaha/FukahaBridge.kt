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
    ): String? = catalog.rewriteToEmbed(url, preferredFixerHost)

    fun defaultFixer(platformKey: String): String? = catalog.defaultFixerHost(platformKey)

    fun platformKeys(): List<String> = catalog.platformKeys()

    fun servicesFor(platformKey: String): List<EmbedService> = catalog.activeServices(platformKey)

    suspend fun prepare(text: String, settings: FukahaSettings): PreparedLink? =
        processor.prepare(text, settings)

    suspend fun download(
        url: String,
        settings: FukahaSettings,
        cacheDirPath: String,
    ): MediaDownloadResult = processor.downloadMedia(url, settings, cacheDirPath)

    fun close() = processor.close()
}
