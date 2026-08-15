package app.fukaha.android.update

sealed class ApkUpdateUiState {
    data object Idle : ApkUpdateUiState()
    data class Downloading(val progress: Float) : ApkUpdateUiState()
    data object Installing : ApkUpdateUiState()
    data class Failed(val message: String) : ApkUpdateUiState()
}
