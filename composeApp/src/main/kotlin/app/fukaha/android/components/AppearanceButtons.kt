package app.fukaha.android.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.fukaha.AppLanguage
import app.fukaha.AppTheme
import app.fukaha.R
import app.fukaha.android.LocaleHelper

@Composable
fun LanguageMenuButton(
    language: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val selected = LocaleHelper.resolve(language)

    Box {
        IconButton(onClick = { open = true }) {
            Icon(
                Icons.Outlined.Language,
                contentDescription = stringResource(R.string.language),
            )
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier
                .widthIn(min = 200.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp)) {
                LanguageMenuItem(
                    label = "English",
                    subtitle = "EN",
                    selected = selected == AppLanguage.English,
                    onClick = {
                        onSelect(AppLanguage.English)
                        open = false
                    },
                )
                LanguageMenuItem(
                    label = "العربية",
                    subtitle = "AR",
                    selected = selected == AppLanguage.Arabic,
                    onClick = {
                        onSelect(AppLanguage.Arabic)
                        open = false
                    },
                )
            }
        }
    }
}

@Composable
private fun LanguageMenuItem(
    label: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
fun ThemeCycleButton(
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
    val contentDescription = stringResource(R.string.theme) + ": " + label
    IconButton(onClick = { onSelect(next) }) {
        when (theme) {
            AppTheme.Light -> Icon(
                imageVector = Icons.Outlined.LightMode,
                contentDescription = contentDescription,
            )
            AppTheme.Dark -> Icon(
                imageVector = Icons.Outlined.DarkMode,
                contentDescription = contentDescription,
            )
            AppTheme.System -> Icon(
                painter = painterResource(R.drawable.ic_routine),
                contentDescription = contentDescription,
            )
        }
    }
}
