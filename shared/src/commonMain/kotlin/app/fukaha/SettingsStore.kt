package app.fukaha

interface SettingsStore {
    suspend fun get(): FukahaSettings
    suspend fun update(transform: (FukahaSettings) -> FukahaSettings)
}
