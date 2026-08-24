package app.fukaha.android.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import app.fukaha.AppLanguage
import app.fukaha.AppTheme
import app.fukaha.R
import app.fukaha.THEME_SYSTEM_HOLD_MS
import app.fukaha.android.LocaleHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
            offset = DpOffset(x = (-22).dp, y = 0.dp),
            modifier = Modifier.width(92.dp),
            shape = RoundedCornerShape(12.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 2.dp,
            shadowElevation = 3.dp,
        ) {
            Column(
                modifier = Modifier.padding(4.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp),
            ) {
                LanguageMenuItem(
                    label = "العربية",
                    flag = "🇸🇦",
                    code = "AR",
                    selected = selected == AppLanguage.Arabic,
                    onClick = {
                        open = false
                        if (selected != AppLanguage.Arabic) {
                            onSelect(AppLanguage.Arabic)
                        }
                    },
                )
                LanguageMenuItem(
                    label = "English",
                    flag = "🇬🇧",
                    code = "EN",
                    selected = selected == AppLanguage.English,
                    onClick = {
                        open = false
                        if (selected != AppLanguage.English) {
                            onSelect(AppLanguage.English)
                        }
                    },
                )
                LanguageMenuItem(
                    label = "日本語",
                    flag = "🇯🇵",
                    code = "JA",
                    selected = selected == AppLanguage.Japanese,
                    onClick = {
                        open = false
                        if (selected != AppLanguage.Japanese) {
                            onSelect(AppLanguage.Japanese)
                        }
                    },
                )
                LanguageMenuItem(
                    label = "简体中文",
                    flag = "🇨🇳",
                    code = "ZH",
                    selected = selected == AppLanguage.SimplifiedChinese,
                    onClick = {
                        open = false
                        if (selected != AppLanguage.SimplifiedChinese) {
                            onSelect(AppLanguage.SimplifiedChinese)
                        }
                    },
                )
                LanguageMenuItem(
                    label = "Español",
                    flag = "🇪🇸",
                    code = "ES",
                    selected = selected == AppLanguage.Spanish,
                    onClick = {
                        open = false
                        if (selected != AppLanguage.Spanish) {
                            onSelect(AppLanguage.Spanish)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun LanguageMenuItem(
    label: String,
    flag: String,
    code: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    // Match the web link-input hero surface: 28% primary container over surface-container-low.
    val heroTonalSurface = lerp(
        MaterialTheme.colorScheme.surfaceContainerLow,
        MaterialTheme.colorScheme.primaryContainer,
        0.28f,
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            heroTonalSurface
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        label = "languageSelectionContainer",
    )
    val codeColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        label = "languageSelectionCode",
    )
    Surface(
        onClick = onClick,
        shape = shape,
        color = containerColor,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = label
                role = Role.RadioButton
                this.selected = selected
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = flag, style = MaterialTheme.typography.titleMedium)
            Text(
                text = code,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                ),
                color = codeColor,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
            )
        }
    }
}

@Composable
fun ThemeCycleButton(
    theme: AppTheme,
    onSelect: (AppTheme) -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val label = when (theme) {
        AppTheme.System -> stringResource(R.string.theme_system)
        AppTheme.Light -> stringResource(R.string.theme_light)
        AppTheme.Dark -> stringResource(R.string.theme_dark)
    }
    val holdHint = stringResource(R.string.theme_hold_system)
    val contentDescription = stringResource(R.string.theme) + ": " + label + ". " + holdHint
    val scope = rememberCoroutineScope()
    val triggerScale = remember { Animatable(1f) }
    val shakeX = remember { Animatable(0f) }
    val shakeRot = remember { Animatable(0f) }
    var triggerAnimating by remember { mutableStateOf(false) }
    var holdConsumed by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    LaunchedEffect(pressed) {
        if (!pressed) {
            if (shakeX.value != 0f) shakeX.animateTo(0f, tween(140))
            if (shakeRot.value != 0f) shakeRot.animateTo(0f, tween(140))
            return@LaunchedEffect
        }
        if (theme == AppTheme.System) return@LaunchedEffect

        holdConsumed = false
        delay(70)
        var sign = 1f
        var lastTick = 0
        var elapsed = 70L
        try {
            while (elapsed < THEME_SYSTEM_HOLD_MS) {
                val progress = (elapsed / THEME_SYSTEM_HOLD_MS.toFloat()).coerceIn(0f, 1f)
                val eased = progress * progress
                shakeX.snapTo((2f + 16f * eased) * sign)
                shakeRot.snapTo((5f + 24f * eased) * sign)
                sign = -sign
                val band = (progress * 6).toInt()
                if (band > lastTick) {
                    lastTick = band
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
                val period = (50f - 28f * eased).toLong().coerceAtLeast(16L)
                delay(period)
                elapsed += period
            }
            holdConsumed = true
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onSelect(AppTheme.System)
        } finally {
            shakeX.animateTo(0f, tween(150))
            shakeRot.animateTo(0f, tween(150))
        }
    }

    IconButton(
        onClick = {
            if (holdConsumed) {
                holdConsumed = false
                return@IconButton
            }
            if (!triggerAnimating) {
                triggerAnimating = true
                scope.launch {
                    try {
                        triggerScale.animateTo(
                            targetValue = 0.82f,
                            animationSpec = tween(durationMillis = 120),
                        )
                        onSelect(theme.toggled(systemDark))
                        triggerScale.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = 320,
                                easing = FastOutSlowInEasing,
                            ),
                        )
                    } finally {
                        triggerScale.snapTo(1f)
                        triggerAnimating = false
                    }
                }
            }
        },
        interactionSource = interactionSource,
        modifier = Modifier.semantics {
            this.contentDescription = contentDescription
            customActions = listOf(
                CustomAccessibilityAction(holdHint) {
                    if (theme != AppTheme.System) onSelect(AppTheme.System)
                    true
                },
            )
        },
    ) {
        Crossfade(
            targetState = theme,
            animationSpec = tween(durationMillis = 440, easing = FastOutSlowInEasing),
            label = "themeIcon",
            modifier = Modifier.graphicsLayer {
                scaleX = triggerScale.value
                scaleY = triggerScale.value
                translationX = shakeX.value
                rotationZ = shakeRot.value
            },
        ) { currentTheme ->
            when (currentTheme) {
                AppTheme.Light -> Icon(
                    imageVector = Icons.Outlined.LightMode,
                    contentDescription = null,
                )
                AppTheme.Dark -> Icon(
                    imageVector = Icons.Outlined.DarkMode,
                    contentDescription = null,
                )
                AppTheme.System -> Icon(
                    painter = painterResource(R.drawable.ic_routine),
                    contentDescription = null,
                )
            }
        }
    }
}
