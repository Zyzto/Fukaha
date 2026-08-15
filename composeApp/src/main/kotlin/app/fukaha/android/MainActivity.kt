package app.fukaha.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import app.fukaha.AppRelease
import app.fukaha.AppUpdateChecker
import app.fukaha.AppUpdatePolicy
import app.fukaha.BuildConfig
import app.fukaha.EmbedHealthSnapshot
import app.fukaha.FukahaSettings
import app.fukaha.PlatformClock
import app.fukaha.R
import app.fukaha.UpdateCheckResult
import app.fukaha.android.components.LanguageMenuButton
import app.fukaha.android.components.ThemeCycleButton
import app.fukaha.android.onboarding.TutorialScreen
import app.fukaha.android.settings.AboutScreen
import app.fukaha.android.settings.SettingsScreen
import app.fukaha.android.settings.UpdateAvailableDialog
import app.fukaha.android.theme.FukahaTheme
import app.fukaha.fukaha
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application.fukaha()

        setContent {
            var settings by remember { mutableStateOf(FukahaSettings()) }
            var settingsLoaded by remember { mutableStateOf(false) }
            var tutorialOpen by remember { mutableStateOf(false) }
            var tab by remember { mutableIntStateOf(0) }
            var pendingRelease by remember { mutableStateOf<AppRelease?>(null) }
            var updateChecking by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()
            val snackbar = remember { SnackbarHostState() }
            val uriHandler = LocalUriHandler.current
            val updateChecker = remember { AppUpdateChecker() }
            val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

            fun persist(next: FukahaSettings) {
                val languageChanged = next.language != settings.language
                settings = next
                scope.launch {
                    app.settingsStore.update { next }
                    if (languageChanged) {
                        withContext(Dispatchers.Main.immediate) {
                            LocaleHelper.apply(next.language)
                        }
                    }
                }
            }

            LaunchedEffect(Unit) {
                settings = app.settingsStore.get()
                LocaleHelper.apply(settings.language)
                settingsLoaded = true
                // Opening the app is the only moment the 6h check may kick in, so a
                // share never waits on probes.
                app.healthController.startAutoIfDue()
            }

            suspend fun runUpdateCheck(manual: Boolean) {
                if (updateChecking) return
                updateChecking = true
                val result = withContext(Dispatchers.IO) {
                    updateChecker.check(BuildConfig.VERSION_NAME)
                }
                val checkedAt = PlatformClock.epochMillis()
                app.settingsStore.update { it.copy(lastUpdateCheckEpochMs = checkedAt) }
                settings = settings.copy(lastUpdateCheckEpochMs = checkedAt)
                updateChecking = false
                when (result) {
                    is UpdateCheckResult.Available -> {
                        if (manual || AppUpdatePolicy.shouldPrompt(result.release, settings.skippedUpdateVersion)) {
                            pendingRelease = result.release
                        }
                    }
                    is UpdateCheckResult.UpToDate -> if (manual) {
                        snackbar.showSnackbar(getString(R.string.update_up_to_date))
                    }
                    is UpdateCheckResult.Failed -> if (manual) {
                        snackbar.showSnackbar(getString(R.string.update_check_failed))
                    }
                }
            }

            LaunchedEffect(
                settingsLoaded,
                settings.onboardingCompleted,
                settings.checkUpdatesOnLaunch,
                tutorialOpen,
            ) {
                if (!settingsLoaded || !settings.onboardingCompleted || !settings.checkUpdatesOnLaunch) {
                    return@LaunchedEffect
                }
                if (tutorialOpen) return@LaunchedEffect
                if (!AppUpdatePolicy.isLaunchCheckDue(settings.lastUpdateCheckEpochMs)) {
                    return@LaunchedEffect
                }
                runUpdateCheck(manual = false)
            }

            FukahaTheme(theme = settings.theme) {
                // Wait for the stored flag so upgrading users never flash the tour.
                val firstRun = settingsLoaded && !settings.onboardingCompleted
                if (firstRun || tutorialOpen) {
                    TutorialScreen(
                        firstRun = firstRun,
                        language = settings.language,
                        theme = settings.theme,
                        onLanguageSelect = { persist(settings.copy(language = it)) },
                        onThemeSelect = { persist(settings.copy(theme = it)) },
                        onFinish = {
                            tutorialOpen = false
                            if (!settings.onboardingCompleted) {
                                persist(settings.copy(onboardingCompleted = true))
                            }
                        },
                    )
                } else {
                    val barColors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    )
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
                                    IconButton(onClick = { tutorialOpen = true }) {
                                        Icon(
                                            Icons.AutoMirrored.Outlined.HelpOutline,
                                            contentDescription = stringResource(R.string.help),
                                        )
                                    }
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
                                colors = barColors,
                            )
                        },
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            ) {
                                NavigationBarItem(
                                    selected = tab == 0,
                                    onClick = { tab = 0 },
                                    icon = {
                                        Icon(Icons.Outlined.Settings, contentDescription = null)
                                    },
                                    label = { Text(stringResource(R.string.settings)) },
                                )
                                NavigationBarItem(
                                    selected = tab == 1,
                                    onClick = { tab = 1 },
                                    icon = {
                                        Icon(Icons.Outlined.Info, contentDescription = null)
                                    },
                                    label = { Text(stringResource(R.string.about)) },
                                )
                            }
                        },
                        snackbarHost = { SnackbarHost(snackbar) },
                    ) { padding ->
                        when (tab) {
                            0 -> SettingsTab(
                                controller = app.healthController,
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
                            else -> AboutScreen(
                                padding = padding,
                                onOpenTutorial = { tutorialOpen = true },
                                onCheckUpdates = {
                                    scope.launch { runUpdateCheck(manual = true) }
                                },
                                updateChecking = updateChecking,
                            )
                        }
                    }
                    pendingRelease?.let { release ->
                        UpdateAvailableDialog(
                            release = release,
                            onViewRelease = {
                                uriHandler.openUri(release.htmlUrl)
                                pendingRelease = null
                            },
                            onLater = { pendingRelease = null },
                            onSkip = {
                                persist(settings.copy(skippedUpdateVersion = release.version))
                                pendingRelease = null
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Owns the embedder-health state so a probe run, which reports progress every few
 * hundred milliseconds, only recomposes the settings list and not the app shell.
 */
@Composable
private fun SettingsTab(
    controller: EmbedHealthController,
    padding: PaddingValues,
    settings: FukahaSettings,
    onChange: (FukahaSettings) -> Unit,
    onClearCache: () -> Unit,
) {
    val health by controller.observeSnapshot().collectAsState(initial = EmbedHealthSnapshot())
    val checking by controller.inProgress.collectAsState()
    val progress by controller.progress.collectAsState()
    val unreachable by controller.lastRunUnreachable.collectAsState()

    SettingsScreen(
        padding = padding,
        settings = settings,
        onChange = onChange,
        onClearCache = onClearCache,
        health = health,
        healthChecking = checking,
        healthProgress = progress,
        healthUnreachable = unreachable,
        onRefreshHealth = controller::refresh,
    )
}