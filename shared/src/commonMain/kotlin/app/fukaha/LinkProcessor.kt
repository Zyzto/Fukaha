package app.fukaha

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class LinkProcessor(
    private val catalog: EmbedCatalog = EmbedCatalog.bundled(),
    private val httpClient: HttpClient = defaultHttpClient(),
) {
    fun catalog(): EmbedCatalog = catalog

    fun extractAndDetect(sharedText: String): DetectedLink? {
        val url = UrlCleaner.extractFirstUrl(sharedText) ?: return null
        return catalog.detect(url)
    }

    suspend fun prepare(
        sharedText: String,
        settings: FukahaSettings,
    ): PreparedLink? {
        val extracted = UrlCleaner.extractFirstUrl(sharedText) ?: return null
        val resolved = if (settings.resolveShortLinks && UrlCleaner.isShortLink(extracted)) {
            resolveRedirect(extracted) ?: extracted
        } else {
            extracted
        }
        val detected = catalog.detect(resolved)
        val preferred = detected.platformKey?.let { settings.preferredFixers[it] }
        val embedUrl = catalog.rewriteToEmbed(detected.cleanedUrl, preferred)
        return PreparedLink(
            detected = detected,
            embedUrl = embedUrl,
        )
    }

    fun cleanUrl(url: String): String = UrlCleaner.clean(url)

    fun embedUrl(url: String, settings: FukahaSettings): String? {
        val detected = catalog.detect(url)
        val preferred = detected.platformKey?.let { settings.preferredFixers[it] }
        return catalog.rewriteToEmbed(detected.cleanedUrl, preferred)
    }

    suspend fun resolveRedirect(url: String): String? = runCatching {
        val response = httpClient.get(url) {
            // Ktor follows redirects by default; final URL is in request/response
        }
        response.call.request.url.toString()
    }.getOrNull()

    suspend fun downloadMedia(
        url: String,
        settings: FukahaSettings,
        cacheDirPath: String,
    ): MediaDownloadResult {
        val cobalt = CobaltClient(
            httpClient = httpClient,
            baseUrl = settings.cobaltBaseUrl.trimEnd('/'),
            apiKey = settings.cobaltApiKey,
        )
        return cobalt.download(url, cacheDirPath)
    }

    fun close() {
        httpClient.close()
    }

    companion object {
        fun defaultHttpClient(): HttpClient = HttpClient {
            expectSuccess = false
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 30_000
            }
            followRedirects = true
        }
    }
}

data class PreparedLink(
    val detected: DetectedLink,
    val embedUrl: String?,
)

sealed class MediaDownloadResult {
    data class Success(
        val filePath: String,
        val mimeType: String?,
        val fileName: String,
    ) : MediaDownloadResult()

    data class Failure(val message: String) : MediaDownloadResult()
}

class CobaltClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val apiKey: String = "",
) {
    suspend fun download(url: String, cacheDirPath: String): MediaDownloadResult {
        return try {
            val response = httpClient.post("$baseUrl/") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Accept, "application/json")
                if (apiKey.isNotBlank()) {
                    header(HttpHeaders.Authorization, "Api-Key $apiKey")
                }
                setBody(CobaltRequest(url = url))
            }
            if (response.status.value !in 200..299) {
                return MediaDownloadResult.Failure("Cobalt HTTP ${response.status.value}")
            }
            val body: CobaltResponse = response.body()
            when (body.status) {
                "error" -> MediaDownloadResult.Failure(
                    body.error?.code ?: body.text ?: "Cobalt error",
                )
                "picker" -> {
                    val first = body.picker?.firstOrNull()
                        ?: return MediaDownloadResult.Failure("No media in picker response")
                    saveRemoteFile(first.url, cacheDirPath, first.type)
                }
                "tunnel", "redirect" -> {
                    val mediaUrl = body.url
                        ?: return MediaDownloadResult.Failure("No media URL in Cobalt response")
                    saveRemoteFile(mediaUrl, cacheDirPath, guessExtensionFromUrl(mediaUrl))
                }
                "local-processing" -> {
                    // Best-effort: download the first tunnel stream (full client-side remux is out of scope).
                    val mediaUrl = body.tunnel?.firstOrNull()
                        ?: return MediaDownloadResult.Failure("No tunnel URL in local-processing response")
                    saveRemoteFile(mediaUrl, cacheDirPath, body.output?.type)
                }
                else -> {
                    val mediaUrl = body.url ?: body.tunnel?.firstOrNull()
                    if (mediaUrl != null) {
                        saveRemoteFile(mediaUrl, cacheDirPath, body.output?.type ?: guessExtensionFromUrl(mediaUrl))
                    } else {
                        MediaDownloadResult.Failure("Unexpected Cobalt status: ${body.status}")
                    }
                }
            }
        } catch (t: Throwable) {
            MediaDownloadResult.Failure(t.message ?: "Download failed")
        }
    }

    private suspend fun saveRemoteFile(
        mediaUrl: String,
        cacheDirPath: String,
        typeHint: String?,
    ): MediaDownloadResult {
        val response = httpClient.get(mediaUrl)
        if (response.status.value !in 200..299) {
            return MediaDownloadResult.Failure("Media HTTP ${response.status.value}")
        }
        val bytes: ByteArray = response.body()
        if (bytes.isEmpty()) {
            return MediaDownloadResult.Failure("Empty media response")
        }
        val ext = when {
            typeHint?.contains("video", ignoreCase = true) == true -> "mp4"
            typeHint?.contains("audio", ignoreCase = true) == true -> "mp3"
            typeHint?.contains("image", ignoreCase = true) == true || typeHint == "photo" -> "jpg"
            else -> guessExtensionFromUrl(mediaUrl) ?: "bin"
        }
        val mime = when (ext) {
            "mp4" -> "video/mp4"
            "mp3" -> "audio/mpeg"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "application/octet-stream"
        }
        val fileName = "fukaha_${PlatformClock.epochMillis()}.$ext"
        val path = writeCacheFile(cacheDirPath, fileName, bytes)
        return MediaDownloadResult.Success(path, mime, fileName)
    }

    private fun guessExtensionFromUrl(url: String): String? {
        val path = url.substringBefore('?').substringAfterLast('/')
        val dot = path.lastIndexOf('.')
        if (dot < 0 || dot == path.lastIndex) return null
        val ext = path.substring(dot + 1).lowercase()
        return ext.takeIf { it.length in 2..5 && it.all { c -> c.isLetterOrDigit() } }
    }
}

@Serializable
private data class CobaltRequest(
    val url: String,
    @SerialName("downloadMode") val downloadMode: String = "auto",
)

@Serializable
private data class CobaltResponse(
    val status: String? = null,
    val text: String? = null,
    val url: String? = null,
    val filename: String? = null,
    val tunnel: List<String>? = null,
    val output: CobaltOutput? = null,
    val error: CobaltError? = null,
    val picker: List<CobaltPickerItem>? = null,
)

@Serializable
private data class CobaltOutput(
    val type: String? = null,
    val filename: String? = null,
)

@Serializable
private data class CobaltError(
    val code: String? = null,
)

@Serializable
private data class CobaltPickerItem(
    val type: String? = null,
    val url: String,
)

expect object PlatformClock {
    fun epochMillis(): Long
}

expect fun writeCacheFile(cacheDirPath: String, fileName: String, bytes: ByteArray): String
