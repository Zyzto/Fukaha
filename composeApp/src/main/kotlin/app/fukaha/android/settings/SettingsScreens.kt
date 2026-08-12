package app.fukaha.android.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.fukaha.AppLanguage
import app.fukaha.AppTheme
import app.fukaha.EmbedCatalog
import app.fukaha.FukahaSettings
import app.fukaha.R
import app.fukaha.ShareAction
import app.fukaha.android.components.SettingsSection

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    padding: PaddingValues,
    settings: FukahaSettings,
    onChange: (FukahaSettings) -> Unit,
    onClearCache: () -> Unit,
) {
    val catalog = remember { EmbedCatalog.bundled() }
    var fixerPlatform by remember { mutableStateOf<String?>(null) }
    val platforms = remember {
        catalog.platformKeys().filter { catalog.activeServices(it).isNotEmpty() }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            SettingsSection(title = stringResource(R.string.default_action)) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.default_action)) },
                    supportingContent = {
                        Text(stringResource(R.string.default_action_hint))
                    },
                    leadingContent = {
                        Icon(Icons.Outlined.Share, contentDescription = null)
                    },
                    colors = transparentListColors(),
                )
                FlowRow(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ShareAction.entries.forEach { action ->
                        FilterChip(
                            selected = settings.defaultAction == action,
                            onClick = { onChange(settings.copy(defaultAction = action)) },
                            label = { Text(actionLabel(action)) },
                        )
                    }
                }
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.section_sharing)) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.resolve_short_links)) },
                    supportingContent = { Text(stringResource(R.string.resolve_short_links_hint)) },
                    leadingContent = { Icon(Icons.Outlined.Link, contentDescription = null) },
                    trailingContent = {
                        androidx.compose.material3.Switch(
                            checked = settings.resolveShortLinks,
                            onCheckedChange = { onChange(settings.copy(resolveShortLinks = it)) },
                        )
                    },
                    colors = transparentListColors(),
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text(stringResource(R.string.delete_cache)) },
                    supportingContent = { Text(stringResource(R.string.delete_cache_hint)) },
                    leadingContent = { Icon(Icons.Outlined.CleaningServices, contentDescription = null) },
                    trailingContent = {
                        androidx.compose.material3.Switch(
                            checked = settings.deleteCacheAfterShare,
                            onCheckedChange = { onChange(settings.copy(deleteCacheAfterShare = it)) },
                        )
                    },
                    colors = transparentListColors(),
                )
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.section_cobalt)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        value = settings.cobaltBaseUrl,
                        onValueChange = { onChange(settings.copy(cobaltBaseUrl = it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.cobalt_url)) },
                        supportingText = { Text(stringResource(R.string.cobalt_hint)) },
                        leadingIcon = { Icon(Icons.Outlined.Storage, contentDescription = null) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                    )
                    OutlinedTextField(
                        value = settings.cobaltApiKey,
                        onValueChange = { onChange(settings.copy(cobaltApiKey = it)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        label = { Text(stringResource(R.string.cobalt_api_key)) },
                        leadingIcon = { Icon(Icons.Outlined.Key, contentDescription = null) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                    )
                }
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.section_appearance)) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.language)) },
                    leadingContent = { Icon(Icons.Outlined.Language, contentDescription = null) },
                    colors = transparentListColors(),
                )
                FlowRow(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = settings.language == AppLanguage.English,
                        onClick = { onChange(settings.copy(language = AppLanguage.English)) },
                        label = { Text("English") },
                    )
                    FilterChip(
                        selected = settings.language == AppLanguage.Arabic,
                        onClick = { onChange(settings.copy(language = AppLanguage.Arabic)) },
                        label = { Text("العربية") },
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                ListItem(
                    headlineContent = { Text(stringResource(R.string.theme)) },
                    leadingContent = { Icon(Icons.Outlined.DarkMode, contentDescription = null) },
                    colors = transparentListColors(),
                )
                FlowRow(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AppTheme.entries.forEach { theme ->
                        FilterChip(
                            selected = settings.theme == theme,
                            onClick = { onChange(settings.copy(theme = theme)) },
                            label = { Text(themeLabel(theme)) },
                        )
                    }
                }
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.preferred_fixers)) {
                platforms.forEachIndexed { index, key ->
                    val platform = catalog.platform(key)
                    val current = settings.preferredFixers[key]
                        ?: catalog.defaultFixerHost(key).orEmpty()
                    ListItem(
                        headlineContent = { Text(platform?.name ?: key) },
                        supportingContent = { Text(current) },
                        leadingContent = {
                            Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null)
                        },
                        modifier = Modifier.clickable { fixerPlatform = key },
                        colors = transparentListColors(),
                    )
                    if (index != platforms.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.section_storage)) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.clear_cache)) },
                    supportingContent = { Text(stringResource(R.string.clear_cache_hint)) },
                    leadingContent = {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                    },
                    modifier = Modifier.clickable(onClick = onClearCache),
                    colors = transparentListColors(),
                )
            }
        }
    }

    fixerPlatform?.let { key ->
        val services = catalog.activeServices(key)
        AlertDialog(
            onDismissRequest = { fixerPlatform = null },
            icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
            title = { Text(catalog.platform(key)?.name ?: key) },
            text = {
                Column {
                    services.forEach { service ->
                        ListItem(
                            headlineContent = { Text(service.name) },
                            supportingContent = { Text(service.normalizedHost()) },
                            modifier = Modifier.clickable {
                                onChange(
                                    settings.copy(
                                        preferredFixers = settings.preferredFixers +
                                            (key to service.normalizedHost()),
                                    ),
                                )
                                fixerPlatform = null
                            },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { fixerPlatform = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
fun AboutScreen(padding: PaddingValues) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SettingsSection(title = stringResource(R.string.app_name)) {
                ListItem(
                    headlineContent = {
                        Text(
                            stringResource(R.string.app_name) + " · " +
                                stringResource(R.string.app_name_ar),
                        )
                    },
                    supportingContent = { Text(stringResource(R.string.about_body)) },
                    leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
                    colors = transparentListColors(),
                )
            }
        }
        item {
            SettingsSection(title = stringResource(R.string.credits_title)) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.credits)) },
                    colors = transparentListColors(),
                )
                Text(
                    text = "v0.1.0",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun actionLabel(action: ShareAction): String = when (action) {
    ShareAction.Ask -> stringResource(R.string.action_ask)
    ShareAction.Clean -> stringResource(R.string.action_clean)
    ShareAction.Embed -> stringResource(R.string.action_embed)
    ShareAction.Download -> stringResource(R.string.action_download)
}

@Composable
private fun themeLabel(theme: AppTheme): String = when (theme) {
    AppTheme.System -> stringResource(R.string.theme_system)
    AppTheme.Light -> stringResource(R.string.theme_light)
    AppTheme.Dark -> stringResource(R.string.theme_dark)
}

@Composable
private fun transparentListColors() = ListItemDefaults.colors(containerColor = Color.Transparent)
