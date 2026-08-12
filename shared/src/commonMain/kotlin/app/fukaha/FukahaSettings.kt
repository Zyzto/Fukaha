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
) {
    companion object {
        /** Replace with your own Cobalt instance — public api.cobalt.tools is bot-protected. */
        const val DEFAULT_COBALT_BASE_URL = "https://api.cobalt.tools"
    }
}
