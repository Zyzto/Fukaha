package app.fukaha

interface SettingsStore {
    suspend fun get(): FukahaSettings
    suspend fun update(transform: (FukahaSettings) -> FukahaSettings)
    suspend fun setDefaultAction(action: ShareAction)
    suspend fun setPreferredFixer(platformKey: String, fixerHost: String)
    suspend fun setCobaltBaseUrl(url: String)
    suspend fun setCobaltApiKey(key: String)
    suspend fun setResolveShortLinks(enabled: Boolean)
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setTheme(theme: AppTheme)
    suspend fun setDeleteCacheAfterShare(enabled: Boolean)
}
