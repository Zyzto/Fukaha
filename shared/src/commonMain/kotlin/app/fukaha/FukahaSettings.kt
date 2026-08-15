package app.fukaha

enum class ShareAction {
    Ask,
    Clean,
    Embed,
    Download,
}

enum class AppLanguage {
    System,
    English,
    Arabic,
}

enum class AppTheme {
    System,
    Light,
    Dark,
}

data class FukahaSettings(
    val defaultAction: ShareAction = ShareAction.Ask,
    val preferredFixers: Map<String, String> = emptyMap(),
    val cobaltBaseUrl: String = DEFAULT_COBALT_BASE_URL,
    val cobaltApiKey: String = "",
    val resolveShortLinks: Boolean = true,
    val language: AppLanguage = AppLanguage.System,
    val theme: AppTheme = AppTheme.System,
    val deleteCacheAfterShare: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val checkUpdatesOnLaunch: Boolean = true,
    val skippedUpdateVersion: String = "",
    val lastUpdateCheckEpochMs: Long = 0L,
) {
    /** True when a non-blank http(s) Cobalt instance URL is configured. */
    val hasValidCobaltBaseUrl: Boolean
        get() = isValidCobaltBaseUrl(cobaltBaseUrl)

    /**
     * Download requires a self-hosted Cobalt URL. If the setting is still Download
     * with an empty/invalid URL, treat it as Ask.
     */
    fun effectiveDefaultAction(): ShareAction =
        if (defaultAction == ShareAction.Download && !hasValidCobaltBaseUrl) {
            ShareAction.Ask
        } else {
            defaultAction
        }

    /** Clamp Download → Ask when Cobalt is not configured. */
    fun withDownloadClamped(): FukahaSettings =
        if (defaultAction == ShareAction.Download && !hasValidCobaltBaseUrl) {
            copy(defaultAction = ShareAction.Ask)
        } else {
            this
        }

    companion object {
        /** Empty until the user sets their own Cobalt instance. */
        const val DEFAULT_COBALT_BASE_URL = ""

        /** Former built-in default — public API is not usable by third-party apps. */
        const val LEGACY_PUBLIC_COBALT_BASE_URL = "https://api.cobalt.tools"

        fun isValidCobaltBaseUrl(url: String): Boolean {
            val trimmed = url.trim()
            if (trimmed.isEmpty()) return false
            return trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true)
        }

        fun isLegacyPublicCobaltBaseUrl(url: String): Boolean {
            val normalized = url.trim().trimEnd('/')
            return normalized.equals(LEGACY_PUBLIC_COBALT_BASE_URL, ignoreCase = true)
        }
    }
}
