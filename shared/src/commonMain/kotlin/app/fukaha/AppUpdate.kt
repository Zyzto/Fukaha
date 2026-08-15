package app.fukaha

/**
 * Latest GitHub release compared against the installed version.
 * Same source Obtainium / NewPipe-style checkers use: `/releases/latest`.
 */
data class AppRelease(
    val version: String,
    val tagName: String,
    val title: String,
    val changelog: String,
    val htmlUrl: String,
    val publishedAt: String? = null,
    val apkUrl: String? = null,
    val apkName: String? = null,
    val apkSizeBytes: Long? = null,
) {
    val canInstallInApp: Boolean get() = !apkUrl.isNullOrBlank()
}

sealed class UpdateCheckResult {
    data class Available(val release: AppRelease) : UpdateCheckResult()
    data object UpToDate : UpdateCheckResult()
    data class Failed(val message: String) : UpdateCheckResult()
}

object AppVersion {
    fun normalize(raw: String): String =
        raw.trim().removePrefix("v").removePrefix("V")

    fun core(raw: String): String {
        val normalized = normalize(raw)
        return normalized.substringBefore('-').substringBefore('+')
    }

    fun compare(left: String, right: String): Int {
        val a = parts(left)
        val b = parts(right)
        val n = maxOf(a.size, b.size)
        for (i in 0 until n) {
            val da = a.getOrElse(i) { 0 }
            val db = b.getOrElse(i) { 0 }
            if (da != db) return da.compareTo(db)
        }
        val leftHasSuffix = hasSuffix(left)
        val rightHasSuffix = hasSuffix(right)
        return when {
            leftHasSuffix == rightHasSuffix -> 0
            leftHasSuffix -> -1
            else -> 1
        }
    }

    fun isNewer(latest: String, current: String): Boolean = compare(latest, current) > 0

    private fun parts(raw: String): List<Int> =
        core(raw).split('.').map { it.toIntOrNull() ?: 0 }

    private fun hasSuffix(raw: String): Boolean {
        val normalized = normalize(raw)
        return normalized.contains('-') || normalized.contains('+')
    }
}

object ChangelogFormatter {
    private val fullChangelogLine = Regex(
        """^\s*(\*\*)?Full Changelog(\*\*)?:.*$""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE),
    )
    private val heading = Regex("""^#{1,6}\s+""")
    private val bold = Regex("""\*\*(.+?)\*\*|__(.+?)__""")

    fun displayNotes(body: String?): String {
        if (body.isNullOrBlank()) return ""
        val stripped = fullChangelogLine.replace(body, "").trim()
        return stripped.lineSequence()
            .map { line ->
                bold.replace(heading.replace(line, "")) { match ->
                    match.groupValues.drop(1).firstOrNull { it.isNotEmpty() } ?: ""
                }
            }
            .joinToString("\n")
            .trim()
    }
}

object AppUpdatePolicy {
    const val LAUNCH_CHECK_INTERVAL_MS: Long = 24L * 60L * 60L * 1000L

    fun isLaunchCheckDue(
        lastCheckEpochMs: Long,
        nowEpochMs: Long = PlatformClock.epochMillis(),
    ): Boolean {
        if (lastCheckEpochMs <= 0L) return true
        return nowEpochMs - lastCheckEpochMs >= LAUNCH_CHECK_INTERVAL_MS
    }

    fun shouldPrompt(
        release: AppRelease,
        skippedVersion: String,
    ): Boolean {
        if (skippedVersion.isBlank()) return true
        return AppVersion.compare(release.version, skippedVersion) != 0
    }
}
