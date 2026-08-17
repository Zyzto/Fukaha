package app.fukaha

import kotlin.js.Date

actual object PlatformClock {
    actual fun epochMillis(): Long = Date.now().toLong()
}

/**
 * Browsers have no filesystem, so downloaded bytes stay in memory keyed by file name
 * and a caller turns them into a Blob. Nothing populates this until media download
 * reaches the web build.
 */
object WebDownloads {
    private val files = mutableMapOf<String, ByteArray>()

    fun put(fileName: String, bytes: ByteArray) {
        files[fileName] = bytes
    }

    fun take(fileName: String): ByteArray? = files.remove(fileName)

    fun clear() = files.clear()
}

actual fun writeCacheFile(cacheDirPath: String, fileName: String, bytes: ByteArray): String {
    WebDownloads.put(fileName, bytes)
    return fileName
}
