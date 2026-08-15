package app.fukaha.android.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.fukaha.AppRelease
import app.fukaha.R
import app.fukaha.android.update.ApkUpdateUiState

@Composable
fun UpdateAvailableDialog(
    release: AppRelease,
    state: ApkUpdateUiState,
    onUpdate: () -> Unit,
    onCancelDownload: () -> Unit,
    onViewRelease: () -> Unit,
    onLater: () -> Unit,
    onSkip: () -> Unit,
) {
    val busy = state is ApkUpdateUiState.Downloading || state is ApkUpdateUiState.Installing
    AlertDialog(
        onDismissRequest = {
            if (state is ApkUpdateUiState.Downloading) onCancelDownload() else onLater()
        },
        icon = { Icon(Icons.Outlined.SystemUpdate, contentDescription = null) },
        title = { Text(stringResource(R.string.update_available_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = stringResource(R.string.update_available_version, release.version),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                release.apkSizeBytes?.takeIf { it > 0 }?.let { bytes ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.update_size, formatApkSize(bytes)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = release.changelog.ifBlank {
                        stringResource(R.string.update_no_notes)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                when (state) {
                    is ApkUpdateUiState.Downloading -> {
                        Spacer(Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(
                                R.string.update_downloading,
                                (state.progress * 100).toInt(),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    is ApkUpdateUiState.Installing -> {
                        Spacer(Modifier.height(16.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.update_installing),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    is ApkUpdateUiState.Failed -> {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    ApkUpdateUiState.Idle -> Unit
                }
                if (!busy) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onSkip) {
                        Text(stringResource(R.string.update_skip))
                    }
                    TextButton(onClick = onViewRelease) {
                        Text(stringResource(R.string.update_view_release))
                    }
                }
            }
        },
        confirmButton = {
            when {
                state is ApkUpdateUiState.Downloading -> TextButton(onClick = onCancelDownload) {
                    Text(stringResource(R.string.update_cancel))
                }
                state is ApkUpdateUiState.Installing -> Unit
                else -> TextButton(onClick = onUpdate) {
                    Text(
                        if (release.canInstallInApp) {
                            stringResource(R.string.update_now)
                        } else {
                            stringResource(R.string.update_view_release)
                        },
                    )
                }
            }
        },
        dismissButton = {
            if (!busy) {
                TextButton(onClick = onLater) {
                    Text(stringResource(R.string.update_later))
                }
            }
        },
    )
}

private fun formatApkSize(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return "%.1f MB".format(mb)
}
