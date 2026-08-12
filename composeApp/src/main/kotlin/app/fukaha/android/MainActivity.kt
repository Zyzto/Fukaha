package app.fukaha.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import app.fukaha.AppLanguage
import app.fukaha.AppTheme
import app.fukaha.FukahaSettings
import app.fukaha.R
import app.fukaha.android.settings.AboutScreen
import app.fukaha.android.settings.SettingsScreen
import app.fukaha.android.theme.FukahaTheme
import app.fukaha.fukaha
import java.io.File
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application.fukaha()

        setContent {
            var settings by remember { mutableStateOf(FukahaSettings()) }
            var tab by remember { mutableIntStateOf(0) }
            val scope = rememberCoroutineScope()
            val snackbar = remember { SnackbarHostState() }
            val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

            fun persist(next: FukahaSettings) {
                val languageChanged = next.language != settings.language
                settings = next
                scope.launch {
                    app.settingsStore.update { next }
                    if (languageChanged) {
                        LocaleHelper.apply(next.language)
                    }
                }
            }

            LaunchedEffect(Unit) {
                settings = app.settingsStore.get()
                LocaleHelper.apply(settings.language)
            }

            FukahaTheme(theme = settings.theme) {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    containerColor = MaterialTheme.colorScheme.surface,
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    if (tab == 0) stringResource(R.string.settings)
                                    else stringResource(R.string.about),
                                )
                            },
                            actions = {
                                LanguageMenuButton(
                                    language = settings.language,
                                    onSelect = { persist(settings.copy(language = it)) },
                                )
                                ThemeCycleButton(
                                    theme = settings.theme,
                                    onSelect = { persist(settings.copy(theme = it)) },
                                )
                            },
                            scrollBehavior = scrollBehavior,
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            ),
                        )
                    },
                    bottomBar = {
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                            NavigationBarItem(
                                selected = tab == 0,
                                onClick = { tab = 0 },
                                icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                                label = { Text(stringResource(R.string.settings)) },
                            )
                            NavigationBarItem(
                                selected = tab == 1,
                                onClick = { tab = 1 },
                                icon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                                label = { Text(stringResource(R.string.about)) },
                            )
                        }
                    },
                    snackbarHost = { SnackbarHost(snackbar) },
                ) { padding ->
                    when (tab) {
                        0 -> SettingsScreen(
                            padding = padding,
                            settings = settings,
                            onChange = { persist(it) },
                            onClearCache = {
                                File(cacheDir, "fukaha").listFiles()?.forEach { it.delete() }
                                scope.launch {
                                    snackbar.showSnackbar(getString(R.string.cache_cleared))
                                }
                            },
                        )
                        else -> AboutScreen(padding = padding)
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageMenuButton(
    language: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(
                Icons.Outlined.Language,
                contentDescription = stringResource(R.string.language),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            LanguageMenuItem(
                label = stringResource(R.string.language_system),
                selected = language == AppLanguage.System,
                onClick = {
                    onSelect(AppLanguage.System)
                    open = false
                },
            )
            LanguageMenuItem(
                label = "English",
                selected = language == AppLanguage.English,
                onClick = {
                    onSelect(AppLanguage.English)
                    open = false
                },
            )
            LanguageMenuItem(
                label = "العربية",
                selected = language == AppLanguage.Arabic,
                onClick = {
                    onSelect(AppLanguage.Arabic)
                    open = false
                },
            )
        }
    }
}

@Composable
private fun LanguageMenuItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = onClick,
        trailingIcon = {
            if (selected) {
                Text("✓", color = MaterialTheme.colorScheme.primary)
            }
        },
    )
}

@Composable
private fun ThemeCycleButton(
    theme: AppTheme,
    onSelect: (AppTheme) -> Unit,
) {
    val next = when (theme) {
        AppTheme.System -> AppTheme.Light
        AppTheme.Light -> AppTheme.Dark
        AppTheme.Dark -> AppTheme.System
    }
    val label = when (theme) {
        AppTheme.System -> stringResource(R.string.theme_system)
        AppTheme.Light -> stringResource(R.string.theme_light)
        AppTheme.Dark -> stringResource(R.string.theme_dark)
    }
    IconButton(onClick = { onSelect(next) }) {
        Icon(
            imageVector = when (theme) {
                AppTheme.Light -> Icons.Outlined.LightMode
                AppTheme.Dark -> Icons.Outlined.DarkMode
                AppTheme.System -> Icons.Outlined.SettingsBrightness
            },
            contentDescription = stringResource(R.string.theme) + ": " + label,
        )
    }
}
