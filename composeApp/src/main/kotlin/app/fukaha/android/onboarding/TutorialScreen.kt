package app.fukaha.android.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.fukaha.R
import kotlinx.coroutines.launch

private const val TUTORIAL_PAGES = 3

/**
 * Classic carousel onboarding: hero, short centered copy, dots, full-width CTA.
 * Used for first run and as a replayable tour.
 */
@Composable
fun TutorialScreen(
    firstRun: Boolean,
    onFinish: () -> Unit,
) {
    val pager = rememberPagerState(pageCount = { TUTORIAL_PAGES })
    val scope = rememberCoroutineScope()
    val onLastPage = pager.currentPage == TUTORIAL_PAGES - 1

    BackHandler(enabled = pager.currentPage > 0 || !firstRun) {
        if (pager.currentPage > 0) {
            scope.launch { pager.animateScrollToPage(pager.currentPage - 1) }
        } else {
            onFinish()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                if (firstRun) {
                    TextButton(
                        onClick = onFinish,
                        modifier = Modifier.align(Alignment.CenterEnd),
                    ) {
                        Text(stringResource(R.string.onboarding_skip))
                    }
                } else {
                    IconButton(
                        onClick = onFinish,
                        modifier = Modifier.align(Alignment.CenterEnd),
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.tutorial_close),
                        )
                    }
                }
            }

            HorizontalPager(
                state = pager,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { page ->
                OnboardingPage(page = page)
            }

            PageDots(
                current = pager.currentPage,
                count = TUTORIAL_PAGES,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp, bottom = 20.dp),
            )

            Button(
                onClick = {
                    if (onLastPage) {
                        onFinish()
                    } else {
                        scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
            ) {
                Text(
                    text = stringResource(
                        when {
                            !onLastPage -> R.string.onboarding_next
                            firstRun -> R.string.onboarding_done
                            else -> R.string.tutorial_close
                        },
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun OnboardingPage(page: Int) {
    val title: String
    val body: String
    when (page) {
        0 -> {
            title = stringResource(R.string.tutorial_welcome_title)
            body = stringResource(R.string.tutorial_welcome_body)
        }
        1 -> {
            title = stringResource(R.string.tutorial_share_title)
            body = stringResource(R.string.tutorial_share_body)
        }
        else -> {
            title = stringResource(R.string.tutorial_paste_title)
            body = stringResource(R.string.tutorial_paste_body)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            when (page) {
                0 -> WelcomeHero()
                1 -> ShareHero()
                else -> PasteHero()
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun WelcomeHero() {
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val blob = minOf(maxWidth, maxHeight) * 0.78f
        Box(
            modifier = Modifier
                .size(blob)
                .clip(CircleShape)
                .background(colors.primaryContainer.copy(alpha = 0.45f)),
        )
        Box(
            modifier = Modifier
                .size(blob * 0.68f)
                .clip(CircleShape)
                .background(colors.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Link,
                contentDescription = null,
                tint = colors.onPrimaryContainer,
                modifier = Modifier.size(blob * 0.28f),
            )
        }
        FloatingChip(
            icon = Icons.Outlined.CleaningServices,
            label = stringResource(R.string.action_clean),
            modifier = Modifier.offset(x = -(blob * 0.42f), y = -(blob * 0.18f)),
        )
        FloatingChip(
            icon = Icons.Outlined.Visibility,
            label = stringResource(R.string.action_embed),
            modifier = Modifier.offset(x = blob * 0.38f, y = -(blob * 0.08f)),
        )
        FloatingChip(
            icon = Icons.Outlined.Download,
            label = stringResource(R.string.action_download),
            modifier = Modifier.offset(x = blob * 0.02f, y = blob * 0.38f),
        )
    }
}

@Composable
private fun ShareHero() {
    val colors = MaterialTheme.colorScheme
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(260.dp)
                .clip(CircleShape)
                .background(colors.secondaryContainer.copy(alpha = 0.55f)),
        )
        Surface(
            modifier = Modifier.width(220.dp),
            shape = RoundedCornerShape(28.dp),
            color = colors.surfaceContainerLowest,
            tonalElevation = 2.dp,
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(colors.outlineVariant),
                )
                Text(
                    text = stringResource(R.string.action_share_short),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                )
                ShareAppRow(name = "Fukaha", highlight = true)
                ShareAppRow(name = "X")
                ShareAppRow(name = "Instagram")
            }
        }
    }
}

@Composable
private fun ShareAppRow(name: String, highlight: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = if (highlight) colors.primary else colors.surfaceContainerHigh,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (highlight) Icons.Outlined.Share else Icons.Outlined.Link,
                    contentDescription = null,
                    tint = if (highlight) colors.onPrimary else colors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            color = if (highlight) colors.primary else colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PasteHero() {
    val colors = MaterialTheme.colorScheme
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(260.dp)
                .clip(CircleShape)
                .background(colors.tertiaryContainer.copy(alpha = 0.55f)),
        )
        Surface(
            modifier = Modifier.width(260.dp),
            shape = RoundedCornerShape(24.dp),
            color = colors.surfaceContainerLowest,
            tonalElevation = 2.dp,
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surfaceContainerLow,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Link,
                            contentDescription = null,
                            tint = colors.primary,
                        )
                        Text(
                            text = stringResource(R.string.quick_use_field_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Icon(
                            Icons.Outlined.ContentPaste,
                            contentDescription = null,
                            tint = colors.primary,
                        )
                    }
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = colors.primary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.quick_use_open),
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.onPrimary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingChip(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = colors.surfaceContainerLowest,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurface,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PageDots(
    current: Int,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val active = index == current
            val width by animateDpAsState(if (active) 22.dp else 8.dp, label = "dot")
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(
                        if (active) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    ),
            )
        }
    }
}
