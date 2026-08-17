package app.fukaha.web

import app.fukaha.AppLanguage
import app.fukaha.AppTheme
import app.fukaha.EmbedHealthKeys
import app.fukaha.EmbedHealthPolicy
import app.fukaha.EmbedHealthStatus
import app.fukaha.EmbedService
import app.fukaha.ShareAction
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement

fun renderSettings(app: App, parent: HTMLElement) {
    val s = app.strings

    renderDefaultAction(app, parent)
    renderAppearance(app, parent)
    renderEmbedders(app, parent)

    parent.el("details", "disclosure") {
        el("summary", null) {
            icon(Icon.INFO, "icon icon-sm")
            el("span", null, s.webLimitsTitle)
            icon(Icon.EXPAND)
        }
        el("div", "disclosure-body") {
            el("ul", "bullets") {
                el("li", null, s.shortLinkNote)
                el("li", null, s.downloadNote)
            }
        }
    }

    parent.el("div", "actions") {
        button(s.about, ButtonStyle.Text, Icon.INFO) { app.show(View.About) }
    }
}

private fun renderDefaultAction(app: App, parent: HTMLElement) {
    val s = app.strings
    parent.el("section", "section") {
        el("h2", "title-small section-header", s.sectionSharing)
        el("p", "body-medium on-surface-variant section-hint", s.defaultActionHint)
        el("div", "list") {
            radioItem(s.actionAsk, s.actionAskHint, "default-action", app.settings.defaultAction == ShareAction.Ask) {
                app.updateSettings { it.copy(defaultAction = ShareAction.Ask) }
            }
            radioItem(s.actionClean, s.actionCleanHint, "default-action", app.settings.defaultAction == ShareAction.Clean) {
                app.updateSettings { it.copy(defaultAction = ShareAction.Clean) }
            }
            radioItem(s.actionEmbed, s.actionEmbedHint, "default-action", app.settings.defaultAction == ShareAction.Embed) {
                app.updateSettings { it.copy(defaultAction = ShareAction.Embed) }
            }
        }
    }
}

private fun renderAppearance(app: App, parent: HTMLElement) {
    val s = app.strings
    parent.el("section", "section") {
        el("h2", "title-small section-header", s.language)
        segmented(
            listOf(
                Segment(s.languageSystem, app.settings.language == AppLanguage.System) {
                    app.updateSettings { it.copy(language = AppLanguage.System) }
                },
                Segment(s.languageEnglish, app.settings.language == AppLanguage.English) {
                    app.updateSettings { it.copy(language = AppLanguage.English) }
                },
                Segment(s.languageArabic, app.settings.language == AppLanguage.Arabic) {
                    app.updateSettings { it.copy(language = AppLanguage.Arabic) }
                },
            ),
        )

        el("h2", "title-small section-header", s.theme)
        segmented(
            listOf(
                Segment(s.themeSystem, app.settings.theme == AppTheme.System) {
                    app.updateSettings { it.copy(theme = AppTheme.System) }
                },
                Segment(s.themeLight, app.settings.theme == AppTheme.Light) {
                    app.updateSettings { it.copy(theme = AppTheme.Light) }
                },
                Segment(s.themeDark, app.settings.theme == AppTheme.Dark) {
                    app.updateSettings { it.copy(theme = AppTheme.Dark) }
                },
            ),
        )
    }
}

private fun renderEmbedders(app: App, parent: HTMLElement) {
    val s = app.strings
    parent.el("section", "section") {
        el("h2", "title-small section-header", s.preferredFixers)

        el("div", "card") {
            el("div", "link-tile-head") {
                el("div", "list-item-text") {
                    el("span", "body-large", s.embedHealthTitle)
                    el("span", "body-small on-surface-variant", healthSummary(app))
                }
                el("div", "link-tile-actions") {
                    button(s.embedHealthRefresh, ButtonStyle.Tonal, Icon.REFRESH) {
                        app.checkEmbedders()
                    }.disabled = app.healthProgress != null
                }
            }
            el("p", "body-small on-surface-variant", s.embedHealthHint)

            app.healthProgress?.let { progress ->
                el("div", "progress") {
                    el("div", "progress-bar") {
                        style.width = "${(progress.fraction * 100).toInt()}%"
                    }
                }
            }
        }

        el("div", "list") {
            app.catalogPlatformKeys().forEach { key ->
                val services = app.servicesFor(key)
                if (services.isEmpty()) return@forEach
                renderFixerRow(app, this, key, services)
            }
        }
    }
}

private fun healthSummary(app: App): String {
    val s = app.strings
    app.healthProgress?.let { progress ->
        return s.embedHealthProgress(progress.completedCount, progress.total)
    }
    val checkedAt = app.health.checkedAtEpochMs ?: return s.embedHealthNever
    val counts = s.embedHealthCounts(app.health.aliveCount, app.health.deadCount)
    val cooldown = EmbedHealthPolicy.cooldownRemainingMs(checkedAt)
    return if (cooldown > 0L) "$counts · ${s.embedHealthCooldownToast(app.minutesOf(cooldown))}" else counts
}

private fun renderFixerRow(
    app: App,
    parent: HTMLElement,
    platformKey: String,
    services: List<EmbedService>,
) {
    val s = app.strings
    // The pick is what the user stored; the effective fixer is what a share would really use.
    // They part ways once a check marks the pick dead, and the row has to admit that.
    val picked = app.pickedFixer(platformKey)
    val inUse = app.effectiveFixer(platformKey)
    val standingInFor = picked?.takeIf { inUse != null && inUse != it }
    val expanded = app.openFixerPlatform == platformKey
    val pickedLabel = if (app.settings.preferredFixers.containsKey(platformKey)) {
        s.fixerYourPick
    } else {
        s.fixerCatalogDefault
    }

    parent.el("button", "list-item state") {
        setAttribute("type", "button")
        setAttribute("aria-expanded", expanded.toString())
        el("div", "list-item-text") {
            el("span", "body-large", platformLabel(platformKey))
            el("span", "body-small on-surface-variant", inUse?.name ?: s.preferredFixerUnknown)
            standingInFor?.let {
                // Muted on purpose: the status dot already carries the alarm, and several
                // platforms standing in at once turned the list into a wall of red.
                el("span", "body-small on-surface-variant", s.fixerStandIn(it.name))
            }
        }
        el("div", "list-item-trailing") {
            inUse?.let { statusDot(app, it) }
            icon(Icon.EXPAND)
        }
        onclick = { app.toggleFixerPicker(platformKey); Unit }
    }

    if (!expanded) return

    parent.el("div", "fixer-options") {
        el("p", "body-small on-surface-variant", s.fixerPickSubtitle(platformLabel(platformKey)))
        services.forEach { service ->
            renderFixerOption(
                app = app,
                parent = this,
                platformKey = platformKey,
                service = service,
                isInUse = service == inUse,
                isPicked = service == picked,
                pickedLabel = pickedLabel,
            )
        }
    }
}

private fun renderFixerOption(
    app: App,
    parent: HTMLElement,
    platformKey: String,
    service: EmbedService,
    isInUse: Boolean,
    isPicked: Boolean,
    pickedLabel: String,
) {
    val s = app.strings
    parent.el("button", if (isInUse) "fixer-option fixer-option-selected state" else "fixer-option state") {
        setAttribute("type", "button")
        if (isInUse) setAttribute("aria-current", "true")
        statusDot(app, service)
        el("div", "list-item-text") {
            el("span", "title-small", service.name)
            el("span", "fixer-host") {
                dir = "ltr"
                textContent = EmbedHealthKeys.displayHost(service.normalizedHost())
            }
            service.author?.let { el("span", "body-small on-surface-variant", s.fixerByAuthor(it)) }
        }
        el("div", "list-item-trailing") {
            // Only worth labelling when the two disagree; otherwise the check mark says it all.
            if (isPicked && !isInUse) el("span", "chip chip-small chip-outline", pickedLabel)
            if (isInUse && !isPicked) el("span", "chip chip-small", s.fixerInUse)
            el("span", healthLabelClasses(app, service), healthLabel(app, service))
            if (isInUse) icon(Icon.CHECK, "icon icon-sm")
        }
        onclick = {
            app.updateSettings { current ->
                current.copy(
                    preferredFixers = current.preferredFixers + (platformKey to service.normalizedHost()),
                )
            }
            Unit
        }
    }
}

private fun HTMLElement.statusDot(app: App, service: EmbedService) {
    val classes = when (app.statusOf(service.normalizedHost())) {
        EmbedHealthStatus.Alive -> "status-dot status-alive"
        EmbedHealthStatus.Dead -> "status-dot status-dead"
        EmbedHealthStatus.Unknown -> "status-dot"
    }
    el("span", classes) {
        setAttribute("aria-hidden", "true")
    }
}

private fun healthLabel(app: App, service: EmbedService): String {
    val s = app.strings
    return when (app.statusOf(service.normalizedHost())) {
        EmbedHealthStatus.Alive -> s.embedHealthAlive
        EmbedHealthStatus.Dead -> s.embedHealthDead
        EmbedHealthStatus.Unknown -> ""
    }
}

private fun healthLabelClasses(app: App, service: EmbedService): String =
    if (app.statusOf(service.normalizedHost()) == EmbedHealthStatus.Dead) {
        "body-small status-label-dead"
    } else {
        "body-small"
    }

private fun platformLabel(key: String): String = key.replaceFirstChar { it.uppercase() }

private class Segment(
    val label: String,
    val selected: Boolean,
    val onSelect: () -> Unit,
)

/** M3 single-select segmented button. The check mark marks the active choice, as in the spec. */
private fun HTMLElement.segmented(segments: List<Segment>) {
    el("div", "segmented") {
        setAttribute("role", "group")
        segments.forEach { segment ->
            el("button", if (segment.selected) "segment segment-selected state" else "segment state") {
                setAttribute("type", "button")
                setAttribute("aria-pressed", segment.selected.toString())
                if (segment.selected) icon(Icon.CHECK, "icon icon-sm")
                el("span", null, segment.label)
                onclick = { segment.onSelect(); Unit }
            }
        }
    }
}

private fun HTMLElement.radioItem(
    label: String,
    hint: String,
    group: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    el("label", if (selected) "list-item list-item-selected state" else "list-item state") {
        val input = el("input", "radio") {
            setAttribute("type", "radio")
            setAttribute("name", group)
        } as HTMLInputElement
        input.checked = selected
        input.onchange = { if (input.checked) onSelect(); Unit }
        el("div", "list-item-text") {
            el("span", "body-large", label)
            el("span", "body-small on-surface-variant", hint)
        }
    }
}
