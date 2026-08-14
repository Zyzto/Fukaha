package app.fukaha.android.settings

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.fukaha.BuildConfig
import app.fukaha.EmbedCatalog
import app.fukaha.EmbedHealthSnapshot
import app.fukaha.EmbedHealthStatus
import app.fukaha.FukahaSettings
import app.fukaha.R
import app.fukaha.ShareAction
import app.fukaha.UrlCleaner
import app.fukaha.android.ShareActivity
import app.fukaha.android.components.SettingsSection
import java.text.DateFormat
import java.util.Date

private const val TEST_SHARE_URL =
    "https://x.com/makkahregion/status/1902619525532512361" +
        "?utm_source=share&utm_medium=android_app&fbclid=IwAR0fukaha_test"

@Composable
fun SettingsScreen(
    padding: PaddingValues,
    settings: FukahaSettings,
    onChange: (FukahaSettings) -> Unit,
    onClearCache: () -> Unit,
    health: EmbedHealthSnapshot = EmbedHealthSnapshot(),
    healthChecking: Boolean = false,
    onRefreshHealth: () -> Unit = {},
) {
    val context = LocalContext.current
    val catalog = remember { EmbedCatalog.bundled() }
    var fixerPlatform by remember { mutableStateOf<String?>(null) }
    var cobaltExpanded by remember { mutableStateOf(false) }
    // Hoisted out of the lazy item so a typed link survives scrolling it off screen.
    var linkInput by rememberSaveable { mutableStateOf("") }
    val platforms = remember {
        catalog.platformKeys().filter { catalog.activeServices(it).isNotEmpty() }
    }
    val checkedAt = health.checkedAtEpochMs
    val lastCheckedLabel = when {
        healthChecking -> stringResource(R.string.embed_health_checking)
        checkedAt == null -> stringResource(R.string.embed_health_never)
        else -> stringResource(
            R.string.embed_health_last_checked,
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                .format(Date(checkedAt)),
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            QuickLinkSection(
                value = linkInput,
                onValueChange = { linkInput = it },
                onOpen = { context.openShareScreen(it) },
                onOpenSample = { context.openShareScreen(TEST_SHARE_URL) },
            )
        }

        item {
            SettingsSection(title = stringResource(R.string.default_action)) {
                Text(
                    text = stringResource(R.string.default_action_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                DefaultActionPicker(
                    settings = settings,
                    onChange = onChange,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val expandLabel = stringResource(
                    if (cobaltExpanded) {
                        R.string.section_cobalt_collapse
                    } else {
                        R.string.section_cobalt_expand
                    },
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .clickable { cobaltExpanded = !cobaltExpanded }
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .semantics { contentDescription = expandLabel },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.section_cobalt),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        imageVector = if (cobaltExpanded) {
                            Icons.Outlined.ExpandLess
                        } else {
                            Icons.Outlined.ExpandMore
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 0.dp,
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        if (!cobaltExpanded) {
                            ListItem(
                                headlineContent = {
                                    Text(stringResource(R.string.section_cobalt_collapsed_hint))
                                },
                                modifier = Modifier.clickable { cobaltExpanded = true },
                                colors = transparentListColors(),
                            )
                        }
                        AnimatedVisibility(visible = cobaltExpanded) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Text(
                                    text = stringResource(R.string.cobalt_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 12.dp),
                                )
                                OutlinedTextField(
                                    value = settings.cobaltBaseUrl,
                                    onValueChange = { url ->
                                        val next = settings.copy(cobaltBaseUrl = url)
                                        onChange(next.withDownloadClamped())
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text(stringResource(R.string.cobalt_url)) },
                                    placeholder = { Text(stringResource(R.string.cobalt_url_placeholder)) },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Storage, contentDescription = null)
                                    },
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
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Key, contentDescription = null)
                                    },
                                    singleLine = true,
                                    shape = MaterialTheme.shapes.medium,
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.preferred_fixers)) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.embed_health_refresh)) },
                    supportingContent = {
                        Text(
                            if (healthChecking) {
                                stringResource(R.string.embed_health_checking)
                            } else {
                                "$lastCheckedLabel\n${stringResource(R.string.embed_health_refresh_hint)}"
                            },
                        )
                    },
                    leadingContent = {
                        if (healthChecking) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Outlined.Refresh, contentDescription = null)
                        }
                    },
                    modifier = Modifier.clickable(enabled = !healthChecking, onClick = onRefreshHealth),
                    colors = transparentListColors(),
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                platforms.forEachIndexed { index, key ->
                    val platform = catalog.platform(key)
                    val currentHost = settings.preferredFixers[key]
                        ?: catalog.defaultFixerHost(key).orEmpty()
                    val currentService = catalog.activeServices(key).firstOrNull {
                        it.normalizedHost().equals(currentHost, ignoreCase = true)
                    }
                    PreferredFixerRow(
                        platformName = platform?.name ?: key,
                        serviceName = currentService?.name
                            ?: stringResource(R.string.preferred_fixer_unknown),
                        host = currentHost,
                        healthStatus = health.statusOf(currentHost),
                        infoUrl = currentService?.repo?.takeIf { it.isNotBlank() }
                            ?: currentHost.takeIf { it.startsWith("http") },
                        onClick = { fixerPlatform = key },
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
        val selectedHost = settings.preferredFixers[key]
            ?: catalog.defaultFixerHost(key).orEmpty()
        PreferredFixerPickerSheet(
            platformName = catalog.platform(key)?.name ?: key,
            services = services,
            selectedHost = selectedHost,
            health = health,
            onSelect = { service ->
                onChange(
                    settings.copy(
                        preferredFixers = settings.preferredFixers +
                            (key to service.normalizedHost()),
                    ),
                )
                fixerPlatform = null
            },
            onDismiss = { fixerPlatform = null },
        )
    }
}

/**
 * Lets the user run a link through Fukaha without sharing into it from another app.
 * The pasted text is handed to [ShareActivity] exactly like a system share would,
 * forcing the sheet so every option stays visible.
 */
@Composable
private fun QuickLinkSection(
    value: String,
    onValueChange: (String) -> Unit,
    onOpen: (String) -> Unit,
    onOpenSample: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val shareable = remember(value) { shareableLink(value) }
    val showInvalid = value.isNotBlank() && shareable == null

    SettingsSection(title = stringResource(R.string.section_quick_use)) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.quick_use_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.quick_use_field_label)) },
                placeholder = { Text(stringResource(R.string.quick_use_field_placeholder)) },
                leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null) },
                trailingIcon = {
                    if (value.isEmpty()) {
                        IconButton(
                            onClick = {
                                val pasted = clipboard.getText()?.text
                                if (pasted.isNullOrBlank()) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.quick_use_clipboard_empty),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                } else {
                                    onValueChange(pasted.trim())
                                }
                            },
                        ) {
                            Icon(
                                Icons.Outlined.ContentPaste,
                                contentDescription = stringResource(R.string.quick_use_paste),
                            )
                        }
                    } else {
                        IconButton(onClick = { onValueChange("") }) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.quick_use_clear),
                            )
                        }
                    }
                },
                supportingText = if (showInvalid) {
                    { Text(stringResource(R.string.quick_use_invalid)) }
                } else {
                    null
                },
                isError = showInvalid,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go,
                ),
                keyboardActions = KeyboardActions(onGo = { shareable?.let(onOpen) }),
                shape = MaterialTheme.shapes.medium,
            )
            // Only offer the CTA once there is something to open, so the section
            // never shows a large dead button.
            AnimatedVisibility(visible = shareable != null) {
                Button(
                    onClick = { shareable?.let(onOpen) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Icon(
                        Icons.Outlined.PlayCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.quick_use_open),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
            // The sample is a way out of the empty state; once there is a link of
            // their own to run it is just noise.
            AnimatedVisibility(
                visible = value.isEmpty(),
                modifier = Modifier.align(Alignment.Start),
            ) {
                TextButton(
                    onClick = onOpenSample,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Icon(
                        Icons.Outlined.PlayCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.quick_use_sample),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
internal fun DefaultActionPicker(
    settings: FukahaSettings,
    onChange: (FukahaSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    val downloadEnabled = settings.hasValidCobaltBaseUrl
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ShareAction.entries.forEach { action ->
            val enabled = action != ShareAction.Download || downloadEnabled
            DefaultActionOption(
                action = action,
                selected = settings.defaultAction == action && enabled,
                enabled = enabled,
                hintOverride = if (action == ShareAction.Download && !downloadEnabled) {
                    stringResource(R.string.action_download_disabled_hint)
                } else {
                    null
                },
                onClick = { onChange(settings.copy(defaultAction = action)) },
            )
        }
    }
}

private fun Context.openShareScreen(text: String) {
    startActivity(
        Intent(this, ShareActivity::class.java).apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(ShareActivity.EXTRA_FORCE_ASK, true)
        },
    )
}

/** Matches a scheme-less host with an optional path, e.g. `x.com/user/status/1`. */
private val bareHostRegex = Regex("""[\w-]+(\.[\w-]+)+(/\S*)?""")

/**
 * Returns text that [ShareActivity] can resolve to a link, or null when the input is
 * not usable yet. Typed input often lacks a scheme, so a bare host gets `https://`.
 */
private fun shareableLink(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    if (UrlCleaner.extractFirstUrl(trimmed) != null) return trimmed
    val firstToken = trimmed.substringBefore(' ')
    return if (bareHostRegex.matches(firstToken)) "https://$firstToken" else null
}

@Composable
fun AboutScreen(
    padding: PaddingValues,
    onOpenTutorial: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val siteUrl = stringResource(R.string.developer_site_url)
    val githubUrl = stringResource(R.string.github_url)
    val donateUrl = stringResource(R.string.donate_url)
    val creditsGistUrl = stringResource(R.string.credits_gist_url)
    val versionLabel = stringResource(R.string.version_value, BuildConfig.VERSION_NAME)

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
                        Text(stringResource(R.string.app_name))
                    },
                    supportingContent = { Text(stringResource(R.string.about_body)) },
                    leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
                    colors = transparentListColors(),
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text(stringResource(R.string.tutorial_open)) },
                    supportingContent = { Text(stringResource(R.string.tutorial_open_hint)) },
                    leadingContent = {
                        Icon(
                            Icons.AutoMirrored.Outlined.HelpOutline,
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable(onClick = onOpenTutorial),
                    colors = transparentListColors(),
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text(stringResource(R.string.version)) },
                    supportingContent = { Text(versionLabel) },
                    leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
                    colors = transparentListColors(),
                )
            }
        }
        item {
            SettingsSection(title = stringResource(R.string.developer_title)) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.developer_name)) },
                    supportingContent = { Text(stringResource(R.string.developer_site)) },
                    leadingContent = { Icon(Icons.Outlined.Person, contentDescription = null) },
                    trailingContent = {
                        Icon(
                            Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = stringResource(R.string.developer_site),
                        )
                    },
                    modifier = Modifier.clickable { uriHandler.openUri(siteUrl) },
                    colors = transparentListColors(),
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text(stringResource(R.string.github_title)) },
                    supportingContent = { Text(stringResource(R.string.github_subtitle)) },
                    leadingContent = { Icon(Icons.Outlined.Code, contentDescription = null) },
                    trailingContent = {
                        Icon(
                            Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = stringResource(R.string.github_title),
                        )
                    },
                    modifier = Modifier.clickable { uriHandler.openUri(githubUrl) },
                    colors = transparentListColors(),
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text(stringResource(R.string.donate)) },
                    supportingContent = { Text(stringResource(R.string.donate_subtitle)) },
                    leadingContent = { Icon(Icons.Outlined.FavoriteBorder, contentDescription = null) },
                    trailingContent = {
                        Icon(
                            Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = stringResource(R.string.donate),
                        )
                    },
                    modifier = Modifier.clickable { uriHandler.openUri(donateUrl) },
                    colors = transparentListColors(),
                )
            }
        }
        item {
            SettingsSection(title = stringResource(R.string.credits_title)) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.credits_headline)) },
                    supportingContent = { Text(stringResource(R.string.credits)) },
                    leadingContent = { Icon(Icons.Outlined.Link, contentDescription = null) },
                    colors = transparentListColors(),
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text(stringResource(R.string.credits_open_gist)) },
                    leadingContent = { Icon(Icons.Outlined.Code, contentDescription = null) },
                    trailingContent = {
                        Icon(
                            Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = stringResource(R.string.credits_open_gist),
                        )
                    },
                    modifier = Modifier.clickable { uriHandler.openUri(creditsGistUrl) },
                    colors = transparentListColors(),
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
private fun actionHint(action: ShareAction): String = when (action) {
    ShareAction.Ask -> stringResource(R.string.action_ask_hint)
    ShareAction.Clean -> stringResource(R.string.action_clean_hint)
    ShareAction.Embed -> stringResource(R.string.action_embed_hint)
    ShareAction.Download -> stringResource(R.string.action_download_hint)
}

@Composable
private fun DefaultActionOption(
    action: ShareAction,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    hintOverride: String? = null,
) {
    val icon = when (action) {
        ShareAction.Ask -> Icons.Outlined.TouchApp
        ShareAction.Clean -> Icons.Outlined.CleaningServices
        ShareAction.Embed -> Icons.Outlined.Visibility
        ShareAction.Download -> Icons.Outlined.Download
    }
    val container = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val border = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val titleColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        selected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val hintColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        selected -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val iconTint = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.72f)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (enabled) border else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.medium,
            )
            .clickable(enabled = enabled, onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = container,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = if (selected) Icons.Filled.CheckCircle else icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = actionLabel(action),
                    style = MaterialTheme.typography.titleSmall,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = hintOverride ?: actionHint(action),
                    style = MaterialTheme.typography.bodySmall,
                    color = hintColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (selected) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Text(
                        text = stringResource(R.string.fixer_selected),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun transparentListColors() = ListItemDefaults.colors(containerColor = Color.Transparent)

@Composable
fun HealthStatusDot(
    status: EmbedHealthStatus,
    modifier: Modifier = Modifier,
) {
    val color = when (status) {
        EmbedHealthStatus.Alive -> Color(0xFF2E7D32)
        EmbedHealthStatus.Dead -> MaterialTheme.colorScheme.error
        EmbedHealthStatus.Unknown -> MaterialTheme.colorScheme.outline
    }
    val label = when (status) {
        EmbedHealthStatus.Alive -> stringResource(R.string.embed_health_alive)
        EmbedHealthStatus.Dead -> stringResource(R.string.embed_health_dead)
        EmbedHealthStatus.Unknown -> stringResource(R.string.embed_health_unknown)
    }
    Box(
        modifier = modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
            .semantics { contentDescription = label },
    )
}

@Composable
fun HealthStatusChip(
    status: EmbedHealthStatus,
    modifier: Modifier = Modifier,
) {
    val accent = when (status) {
        EmbedHealthStatus.Alive -> Color(0xFF1B7A3D)
        EmbedHealthStatus.Dead -> MaterialTheme.colorScheme.error
        EmbedHealthStatus.Unknown -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val container = when (status) {
        EmbedHealthStatus.Alive -> Color(0xFF1B7A3D).copy(alpha = 0.14f)
        EmbedHealthStatus.Dead -> MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
        EmbedHealthStatus.Unknown -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val label = when (status) {
        EmbedHealthStatus.Alive -> stringResource(R.string.embed_health_alive)
        EmbedHealthStatus.Dead -> stringResource(R.string.embed_health_dead)
        EmbedHealthStatus.Unknown -> stringResource(R.string.embed_health_unknown)
    }

    Surface(
        modifier = modifier.semantics { contentDescription = label },
        color = container,
        shape = RoundedCornerShape(999.dp),
        contentColor = accent,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}
