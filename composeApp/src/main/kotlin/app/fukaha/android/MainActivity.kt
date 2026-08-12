package app.fukaha.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import app.fukaha.FukahaSettings
import app.fukaha.R
import app.fukaha.android.settings.AboutScreen
import app.fukaha.android.settings.SettingsScreen
import app.fukaha.android.theme.FukahaTheme
import app.fukaha.fukaha
import java.io.File
import java.util.Locale
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

            LaunchedEffect(Unit) {
                settings = app.settingsStore.get()
                applyLocale(settings.language, recreateActivity = false)
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
                            onChange = { next ->
                                val languageChanged = next.language != settings.language
                                settings = next
                                scope.launch {
                                    app.settingsStore.update { next }
                                    applyLocale(next.language, recreateActivity = languageChanged)
                                }
                            },
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

    private fun applyLocale(language: AppLanguage, recreateActivity: Boolean) {
        val locale = when (language) {
            AppLanguage.English -> Locale.ENGLISH
            AppLanguage.Arabic -> Locale.forLanguageTag("ar")
        }
        val config = resources.configuration
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
        if (recreateActivity) recreate()
    }
}
