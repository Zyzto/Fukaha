package app.fukaha

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.fukahaDataStore: DataStore<Preferences> by preferencesDataStore(name = "fukaha_settings")

class AndroidSettingsStore(private val context: Context) : SettingsStore {
    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val defaultAction = stringPreferencesKey("default_action")
        val preferredFixers = stringPreferencesKey("preferred_fixers")
        val cobaltBaseUrl = stringPreferencesKey("cobalt_base_url")
        val cobaltApiKey = stringPreferencesKey("cobalt_api_key")
        val resolveShortLinks = booleanPreferencesKey("resolve_short_links")
        val language = stringPreferencesKey("language")
        val theme = stringPreferencesKey("theme")
        val deleteCacheAfterShare = booleanPreferencesKey("delete_cache_after_share")
    }

    override suspend fun get(): FukahaSettings {
        return context.fukahaDataStore.data.map { prefs ->
            FukahaSettings(
                defaultAction = prefs[Keys.defaultAction]?.let { runCatching { ShareAction.valueOf(it) }.getOrNull() }
                    ?: ShareAction.Ask,
                preferredFixers = prefs[Keys.preferredFixers]?.let {
                    runCatching { json.decodeFromString<Map<String, String>>(it) }.getOrDefault(emptyMap())
                } ?: emptyMap(),
                cobaltBaseUrl = prefs[Keys.cobaltBaseUrl] ?: FukahaSettings.DEFAULT_COBALT_BASE_URL,
                cobaltApiKey = prefs[Keys.cobaltApiKey].orEmpty(),
                resolveShortLinks = prefs[Keys.resolveShortLinks] ?: true,
                language = prefs[Keys.language]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
                    ?: AppLanguage.English,
                theme = prefs[Keys.theme]?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() }
                    ?: AppTheme.System,
                deleteCacheAfterShare = prefs[Keys.deleteCacheAfterShare] ?: true,
            )
        }.first()
    }

    override suspend fun update(transform: (FukahaSettings) -> FukahaSettings) {
        val next = transform(get())
        context.fukahaDataStore.edit { prefs ->
            prefs[Keys.defaultAction] = next.defaultAction.name
            prefs[Keys.preferredFixers] = json.encodeToString(next.preferredFixers)
            prefs[Keys.cobaltBaseUrl] = next.cobaltBaseUrl
            prefs[Keys.cobaltApiKey] = next.cobaltApiKey
            prefs[Keys.resolveShortLinks] = next.resolveShortLinks
            prefs[Keys.language] = next.language.name
            prefs[Keys.theme] = next.theme.name
            prefs[Keys.deleteCacheAfterShare] = next.deleteCacheAfterShare
        }
    }

    override suspend fun setDefaultAction(action: ShareAction) {
        update { it.copy(defaultAction = action) }
    }

    override suspend fun setPreferredFixer(platformKey: String, fixerHost: String) {
        update { it.copy(preferredFixers = it.preferredFixers + (platformKey to fixerHost)) }
    }

    override suspend fun setCobaltBaseUrl(url: String) {
        update { it.copy(cobaltBaseUrl = url) }
    }

    override suspend fun setCobaltApiKey(key: String) {
        update { it.copy(cobaltApiKey = key) }
    }

    override suspend fun setResolveShortLinks(enabled: Boolean) {
        update { it.copy(resolveShortLinks = enabled) }
    }

    override suspend fun setLanguage(language: AppLanguage) {
        update { it.copy(language = language) }
    }

    override suspend fun setTheme(theme: AppTheme) {
        update { it.copy(theme = theme) }
    }

    override suspend fun setDeleteCacheAfterShare(enabled: Boolean) {
        update { it.copy(deleteCacheAfterShare = enabled) }
    }
}
