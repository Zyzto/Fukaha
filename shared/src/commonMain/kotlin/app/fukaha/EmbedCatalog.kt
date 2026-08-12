package app.fukaha

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

class EmbedCatalog(
    private val platforms: Map<String, EmbedPlatform>,
) {
    fun platformKeys(): List<String> = platforms.keys.sorted()

    fun platform(key: String): EmbedPlatform? = platforms[key]

    fun activeServices(platformKey: String): List<EmbedService> =
        platforms[platformKey]?.services?.filterNot { it.isBroken }.orEmpty()

    fun detectPlatformKey(url: String): String? {
        val host = UrlCleaner.hostOf(url) ?: return null
        return hostToPlatform[host] ?: hostToPlatform.entries
            .firstOrNull { (pattern, _) -> host == pattern || host.endsWith(".$pattern") }
            ?.value
    }

    fun detect(url: String): DetectedLink {
        val cleaned = UrlCleaner.clean(url)
        val key = detectPlatformKey(cleaned)
        val name = key?.let { platforms[it]?.name ?: it.replaceFirstChar { c -> c.uppercase() } }
        return DetectedLink(
            originalUrl = url,
            cleanedUrl = cleaned,
            platformKey = key,
            platformName = name,
        )
    }

    fun rewriteToEmbed(
        url: String,
        preferredFixerHost: String? = null,
    ): String? {
        val cleaned = UrlCleaner.clean(url)
        val key = detectPlatformKey(cleaned) ?: return null
        val services = activeServices(key)
        if (services.isEmpty()) return null

        val preferred = preferredFixerHost?.let { pref ->
            val normalized = pref.trim().trimEnd('/')
            services.firstOrNull {
                it.normalizedHost().equals(normalized, ignoreCase = true) ||
                    it.alternateHosts.any { alt ->
                        alt.trim().trimEnd('/').equals(normalized, ignoreCase = true)
                    } ||
                    it.name.equals(normalized, ignoreCase = true)
            }
        }

        val service = preferred ?: defaultService(key, services) ?: services.first()
        return replaceHost(cleaned, service.normalizedHost())
    }

    fun defaultFixerHost(platformKey: String): String? {
        val services = activeServices(platformKey)
        return defaultService(platformKey, services)?.normalizedHost()
    }

    private fun defaultService(platformKey: String, services: List<EmbedService>): EmbedService? {
        val preferredHost = DEFAULT_FIXERS[platformKey] ?: return services.firstOrNull()
        return services.firstOrNull {
            it.normalizedHost().equals(preferredHost, ignoreCase = true)
        } ?: services.firstOrNull()
    }

    private fun replaceHost(url: String, newHostWithScheme: String): String {
        val schemeSplit = url.indexOf("://")
        if (schemeSplit < 0) return url
        val rest = url.substring(schemeSplit + 3)
        val pathIdx = rest.indexOfAny(charArrayOf('/', '?', '#')).let { if (it < 0) rest.length else it }
        val pathAndAfter = rest.substring(pathIdx)

        val target = newHostWithScheme.trim().trimEnd('/')
        return if (target.contains("://")) {
            "$target$pathAndAfter"
        } else {
            "https://$target$pathAndAfter"
        }
    }

    companion object {
        val DEFAULT_FIXERS: Map<String, String> = mapOf(
            "bluesky" to "https://bskx.app",
            "instagram" to "https://ddinstagram.com",
            "reddit" to "https://rxddit.com",
            "threads" to "https://fixthreads.net",
            "tiktok" to "https://tnktok.com",
            "tumblr" to "https://tpmblr.com",
            "twitter" to "https://vxtwitter.com",
            "x" to "https://fixvx.com",
            "pixiv" to "https://phixiv.net",
        )

        private val hostToPlatform: Map<String, String> = mapOf(
            "bsky.app" to "bluesky",
            "instagram.com" to "instagram",
            "instagr.am" to "instagram",
            "pixiv.net" to "pixiv",
            "www.pixiv.net" to "pixiv",
            "reddit.com" to "reddit",
            "old.reddit.com" to "reddit",
            "new.reddit.com" to "reddit",
            "redd.it" to "reddit",
            "threads.net" to "threads",
            "threads.com" to "threads",
            "tiktok.com" to "tiktok",
            "www.tiktok.com" to "tiktok",
            "tumblr.com" to "tumblr",
            "www.tumblr.com" to "tumblr",
            "twitch.tv" to "twitch",
            "www.twitch.tv" to "twitch",
            "twitter.com" to "twitter",
            "mobile.twitter.com" to "twitter",
            "x.com" to "x",
            "mobile.x.com" to "x",
        )

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        fun fromJson(raw: String): EmbedCatalog {
            val root = json.parseToJsonElement(raw) as JsonObject
            val platforms = root.mapValues { (_, value) ->
                json.decodeFromJsonElement<EmbedPlatform>(value)
            }
            return EmbedCatalog(platforms)
        }

        fun bundled(): EmbedCatalog = fromJson(ServicesJson.RAW)
    }
}
