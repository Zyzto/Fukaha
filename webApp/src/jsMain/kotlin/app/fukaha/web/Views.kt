package app.fukaha.web

import app.fukaha.AppLanguage
import app.fukaha.AppTheme
import app.fukaha.EmbedHealthStatus
import app.fukaha.UrlCleaner
import kotlinx.browser.window
import org.w3c.dom.HTMLElement

private const val GITHUB_URL = "https://github.com/Zyzto/Fukaha"
private const val RELEASES_URL = "https://github.com/Zyzto/Fukaha/releases/latest"
private const val SPONSOR_URL = "https://github.com/sponsors/Zyzto"
private const val DEVELOPER_URL = "https://shenepoy.com"

fun renderApp(app: App, root: HTMLElement) {
    if (Platform.isInAppBrowser) {
        renderInAppBrowserGate(app, root)
        return
    }

    val shellParent = if (app.view == View.Share) {
        root.el("div", "sheet-scrim") {
            onclick = { event ->
                if (event.target == event.currentTarget) app.show(View.Home)
                Unit
            }
        }
    } else {
        root
    }
    val shellClasses = when (app.view) {
        View.Share -> "shell shell-share"
        View.Home, View.Settings -> "shell"
    }
    val shell = shellParent.el("div", shellClasses)
    if (app.view == View.Share) {
        shell.setAttribute("role", "dialog")
        shell.setAttribute("aria-modal", "true")
        shell.setAttribute("aria-labelledby", "page-title")
        shell.onkeydown = { event ->
            if (event.asDynamic().key == "Escape") app.show(View.Home)
            Unit
        }
        shell.el("div", "sheet-handle") { setAttribute("aria-hidden", "true") }
    } else {
        renderTopAppBar(app, shell)
    }

    val content = shell.el("main", if (app.view == View.Share) "content content-share" else "content")
    when (app.view) {
        View.Home -> renderHome(app, content)
        View.Share -> renderShare(app, content)
        View.Settings -> renderSettings(app, content)
    }

    shell.el(
        "div",
        if (app.status != null) "snackbar snackbar-visible" else "snackbar",
        app.status ?: "",
    ) {
        setAttribute("role", "status")
        setAttribute("aria-live", "polite")
    }
}

private fun renderTopAppBar(app: App, parent: HTMLElement) {
    val s = app.strings
    parent.el("header", "top-app-bar") {
        when (app.view) {
            View.Home -> {
                el("div", "top-app-bar-title top-app-bar-title-home") {
                    el("img", "brand-mark") {
                        setAttribute("src", "/icons/icon.svg")
                        setAttribute("alt", "")
                        setAttribute("width", "48")
                        setAttribute("height", "48")
                    }
                    el("div", "top-app-bar-headline") {
                        el("h1", "title-large", s.appName) {
                            id = "page-title"
                            setAttribute("tabindex", "-1")
                        }
                    }
                }
                iconButton(
                    Icon.SETTINGS,
                    s.settings,
                    "top-app-bar-slot top-app-bar-route-action",
                ) { app.show(View.Settings) }
                renderAppearanceActions(app, this)
            }
            View.Share -> {
                // The sheet headline carries the name, the way Android's share sheet does, so
                // the bar is only back and settings.
                iconButton(
                    Icon.BACK,
                    s.back,
                    "icon-flip top-app-bar-slot top-app-bar-navigation",
                ) { app.show(View.Home) }
                el("div", "top-app-bar-title")
                iconButton(
                    Icon.SETTINGS,
                    s.settings,
                    "top-app-bar-slot top-app-bar-route-action",
                ) { app.show(View.Settings) }
            }
            View.Settings -> {
                iconButton(
                    Icon.BACK,
                    s.back,
                    "icon-flip top-app-bar-slot top-app-bar-navigation",
                ) { app.leaveSettings() }
                el("div", "top-app-bar-title") {
                    el("h1", "title-large", s.settings) {
                        id = "page-title"
                        setAttribute("tabindex", "-1")
                    }
                }
                el("div", "top-app-bar-slot top-app-bar-route-action top-app-bar-placeholder") {
                    setAttribute("aria-hidden", "true")
                }
                renderAppearanceActions(app, this)
            }
        }
    }
}

private fun renderAppearanceActions(app: App, parent: HTMLElement) {
    val s = app.strings
    val selectedLanguage = Strings.resolveLanguage(app.settings.language)
    parent.el("div", "top-menu-anchor top-app-bar-slot top-app-bar-language") {
        id = LANGUAGE_MENU_ANCHOR_ID
        iconButton(Icon.LANGUAGE, s.language) { app.toggleLanguageMenu() }.apply {
            id = LANGUAGE_MENU_TRIGGER_ID
            setAttribute("aria-haspopup", "menu")
            setAttribute("aria-expanded", app.languageMenuOpen.toString())
            setAttribute("aria-controls", LANGUAGE_MENU_ID)
        }
        if (app.languageMenuOpen) {
            el("div", "language-menu") {
                id = LANGUAGE_MENU_ID
                setAttribute("role", "menu")
                LANGUAGE_OPTIONS.forEach { option ->
                    languageMenuOption(
                        label = s.languageLabel(option.language),
                        flag = option.flag,
                        code = option.code,
                        selected = selectedLanguage == option.language,
                    ) { app.selectLanguage(option.language) }
                }
            }
        }
    }
    val themeIcon = when (app.settings.theme) {
        AppTheme.System -> Icon.SYSTEM
        AppTheme.Light -> Icon.LIGHT
        AppTheme.Dark -> Icon.DARK
    }
    parent.iconButton(
        themeIcon,
        s.theme,
        "theme-toggle top-app-bar-slot top-app-bar-theme",
    ) {}.apply {
        onclick = { event ->
            app.cycleTheme(event.currentTarget as HTMLElement)
            Unit
        }
    }
}

private fun Strings.languageLabel(language: AppLanguage): String = when (language) {
    AppLanguage.Arabic -> languageArabic
    AppLanguage.English -> languageEnglish
    AppLanguage.Japanese -> languageJapanese
    AppLanguage.SimplifiedChinese -> languageSimplifiedChinese
    AppLanguage.Spanish -> languageSpanish
    AppLanguage.System -> languageSystem
}

private fun HTMLElement.languageMenuOption(
    label: String,
    flag: String,
    code: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    el("button", if (selected) "language-option language-option-selected state" else "language-option state") {
        setAttribute("type", "button")
        setAttribute("role", "menuitemradio")
        setAttribute("aria-checked", selected.toString())
        el("span", "language-flag", flag) { setAttribute("aria-hidden", "true") }
        el("span", "body-large language-code", code)
        el("span", "sr-only", label)
        onclick = { onClick(); Unit }
    }
}

// region home

private fun renderHome(app: App, parent: HTMLElement) {
    renderPasteCard(app, parent)
    renderInstallCard(app, parent)
    renderAppOnlyNotes(app, parent)
}

fun renderPasteCard(app: App, parent: HTMLElement) {
    val s = app.strings
    parent.el("section", "section paste-section") {
        el("div", "card card-hero paste-card") {
            linkField(
                name = s.pasteTitle,
                placeholder = s.pastePlaceholder,
                value = app.draft,
                hint = s.pasteHint,
                sampleLabel = s.pasteSample,
                pasteLabel = s.pasteFromClipboard,
                clearLabel = s.pasteClear,
                submitLabel = s.pasteOpen,
                submittingLabel = s.preparing,
                submitting = app.homeSubmitting,
                errorFor = { if (it.isNotBlank() && shareableLink(it) == null) s.pasteInvalid else null },
                onInput = { app.draft = it },
                onSample = { app.prepare(SAMPLE_LINK) },
                requestPaste = { apply -> app.paste(apply) },
                onSubmit = { submit(app, it) },
                onClear = { app.draft = "" },
            )
        }
    }
}

private fun submit(app: App, raw: String) {
    val link = shareableLink(raw)
    if (link == null) {
        app.notify(app.strings.pasteInvalid)
        return
    }
    val submittedDraft = raw.trim()
    app.draft = submittedDraft
    app.submitHome(link, submittedDraft)
}

/** Matches a scheme-less host with an optional path, e.g. `x.com/user/status/1`. */
private val bareHostRegex = Regex("""[\w-]+(\.[\w-]+)+(/\S*)?""")

/**
 * Text the share flow can resolve to a link, or null when it is not usable yet. Mirrors
 * `shareableLink` on Android so the field's error line and the submit button agree, including
 * the `https://` a typed bare host is missing.
 */
private fun shareableLink(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    if (UrlCleaner.extractFirstUrl(trimmed) != null) return trimmed
    val firstToken = trimmed.substringBefore(' ')
    return if (bareHostRegex.matches(firstToken)) "https://$firstToken" else null
}

private fun renderInstallCard(app: App, parent: HTMLElement) {
    val s = app.strings
    if (PwaInstall.isStandalone) {
        parent.el("p", "note note-ok body-medium", s.installedNote)
        return
    }

    // Only Android browsers need the Chrome-specific share-target guidance. Other platforms
    // should not see an Android requirement when this browser cannot raise an install prompt.
    if (shouldShowAndroidInstallNotice(PwaInstall.canPrompt, Platform.isAndroid, Platform.isIos)) {
        parent.el("p", "note body-medium", s.shareSheetUnsupported) {
            setAttribute("title", s.notInstallableHint)
        }
        return
    }
    if (!PwaInstall.canPrompt && !Platform.isIos) return

    parent.el("section", "card card-primary") {
        el("h2", "title-medium", s.installTitle)
        if (PwaInstall.canPrompt) {
            el("p", "body-medium on-surface-variant", s.installBody)
            el("div", "actions") {
                button(s.installAction, ButtonStyle.Filled, Icon.INSTALL) { app.install() }
            }
        } else {
            el("p", "body-medium on-surface-variant", s.iosInstallHint)
        }
    }
}

private fun renderAppOnlyNotes(app: App, parent: HTMLElement) {
    val s = app.strings
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
            el("div", "actions") {
                link(s.androidApp, RELEASES_URL, trailingIcon = Icon.OPEN)
            }
        }
    }
}

// endregion

// region share result

/**
 * Mirrors `ShareSheet` on Android, including the no-media subtitle: the PWA cannot download
 * a file, so the download row is omitted the same way the app hides it without a Cobalt URL.
 */
private fun renderShare(app: App, parent: HTMLElement) {
    val s = app.strings
    val prepared = app.prepared

    parent.el("div", "share-head") {
        iconButton(Icon.BACK, s.back, "icon-flip") { app.show(View.Home) }
        el("div", "share-head-copy") {
            el("h1", "headline-medium", s.appName) {
                id = "page-title"
                setAttribute("tabindex", "-1")
            }
            el("p", "body-medium on-surface-variant", s.shareSheetSubtitle)
        }
    }

    if (app.busy || prepared == null) {
        parent.el("div", "share-loading") {
            el("span", "spinner") { setAttribute("aria-hidden", "true") }
            el("p", "title-medium", s.preparing)
        }
        return
    }

    linkActionRow(
        app = app,
        parent = parent,
        title = s.originalPreview,
        url = prepared.detected.originalUrl,
        sectionIcon = Icon.PUBLIC,
        titleTrailing = prepared.detected.platformName ?: s.unknownPlatform,
        shareable = false,
    )

    linkActionRow(
        app = app,
        parent = parent,
        title = s.cleanedPreview,
        url = prepared.detected.cleanedUrl,
        sectionIcon = Icon.CLEAN,
        shareable = true,
    )

    prepared.embedUrl?.let { embed ->
        val dead = prepared.embedHealth == EmbedHealthStatus.Dead
        linkActionRow(
            app = app,
            parent = parent,
            title = if (dead) s.embedUnreachable else s.embedPreview,
            url = embed,
            sectionIcon = Icon.PREVIEW,
            titleTrailing = if (dead) s.embedHealthDead else null,
            shareable = true,
        )
    }
}

/** Matches Android's compact link row: the URL copies on click and Share stays on the right. */
private fun linkActionRow(
    app: App,
    parent: HTMLElement,
    title: String,
    url: String,
    sectionIcon: String,
    shareable: Boolean,
    titleTrailing: String? = null,
) {
    val s = app.strings
    parent.el("div", "share-section") {
        el("div", "share-label") {
            icon(sectionIcon, "icon icon-sm")
            el("span", "label-large", title)
            titleTrailing?.let { el("span", "label-large share-label-trailing", it) }
        }
        el("div", if (shareable) "share-row share-row-has-share" else "share-row") {
            icon(sectionIcon, "icon share-watermark")
            el("div", "share-row-inner") {
                el("button", "share-copy state") {
                    setAttribute("type", "button")
                    setAttribute("aria-label", s.actionCopyShort)
                    setAttribute("title", s.actionCopyShort)
                    onclick = { app.copy(url); Unit }
                    icon(Icon.COPY)
                    urlText(url, "share-url")
                }
                if (shareable) {
                    el("button", "share-send state") {
                        setAttribute("type", "button")
                        setAttribute("aria-label", s.actionShareShort)
                        setAttribute("title", s.actionShareShort)
                        onclick = { app.share(url); Unit }
                        icon(Icon.SHARE)
                    }
                }
            }
        }
    }
}

// endregion

private fun renderInAppBrowserGate(app: App, root: HTMLElement) {
    val s = app.strings
    root.el("div", "shell") {
        el("div", "top-app-bar") {
            el("div", "top-app-bar-title top-app-bar-title-home") {
                el("h1", "title-large", s.appName)
            }
        }
        el("section", "card card-primary") {
            el("h2", "title-medium", s.openInBrowserTitle)
            el("p", "body-medium on-surface-variant", s.openInBrowserBody)
            el("div", "actions") {
                button(s.openInBrowserAction, ButtonStyle.Filled, Icon.OPEN_IN_BROWSER) {
                    window.open(window.location.href, "_blank")
                }
            }
            urlText(window.location.href)
        }
    }
}

fun renderAbout(app: App, parent: HTMLElement) {
    val s = app.strings
    parent.el("section", "section") {
        el("h2", "title-small section-header", s.appName)
        el("div", "card settings-surface") {
            el("div", "list-item static-list-item") {
                icon(Icon.INFO)
                el("div", "list-item-text") {
                    el("span", "body-large", s.appName)
                    el("span", "body-medium on-surface-variant", s.aboutBody)
                }
            }
            el("div", "list-item static-list-item version-row") {
                setAttribute("aria-label", "${s.version}: $WEB_APP_VERSION")
                icon(Icon.INFO)
                el("div", "list-item-text") {
                    el("span", "body-large", s.version)
                    el("span", "body-small on-surface-variant", WEB_APP_VERSION) {
                        dir = "ltr"
                        setAttribute("data-app-version", WEB_APP_VERSION)
                    }
                }
            }
        }
    }
    parent.el("section", "section") {
        el("h2", "title-small section-header", s.developerTitle)
        el("div", "list") {
            linkRow(s.developerTitle, s.developerSite, DEVELOPER_URL, Icon.PERSON)
            linkRow(s.githubTitle, s.githubSubtitle, GITHUB_URL, Icon.CODE)
            linkRow(s.donate, s.donateSubtitle, SPONSOR_URL, Icon.FAVORITE)
        }
    }
    parent.el("section", "section") {
        el("h2", "title-small section-header", s.creditsTitle)
        el("div", "card settings-surface") {
            el("div", "list-item static-list-item") {
                icon(Icon.LINK)
                el("div", "list-item-text") {
                    el("span", "body-large", s.creditsTitle)
                    el("span", "body-medium on-surface-variant", s.credits)
                }
            }
        }
    }
}

private fun HTMLElement.linkRow(title: String, subtitle: String, href: String, leadingIcon: String) {
    anchor(href, "list-item state") {
        icon(leadingIcon)
        el("div", "list-item-text") {
            el("span", "body-large", title)
            el("span", "body-small on-surface-variant", subtitle)
        }
        el("div", "list-item-trailing") {
            icon(Icon.OPEN, "icon icon-sm")
        }
    }
}
