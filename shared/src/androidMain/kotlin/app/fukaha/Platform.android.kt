package app.fukaha

import java.io.File

actual object PlatformClock {
    actual fun epochMillis(): Long = System.currentTimeMillis()
}

actual fun writeCacheFile(cacheDirPath: String, fileName: String, bytes: ByteArray): String {
    val dir = File(cacheDirPath)
    if (!dir.exists()) dir.mkdirs()
    val file = File(dir, fileName)
    file.writeBytes(bytes)
    return file.absolutePath
}
