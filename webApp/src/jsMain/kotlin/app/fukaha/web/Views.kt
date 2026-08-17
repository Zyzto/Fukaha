package app.fukaha.web

import app.fukaha.EmbedHealthStatus
import app.fukaha.UrlCleaner
import kotlinx.browser.window
import org.w3c.dom.HTMLElement

private const val SAMPLE_LINK = "https://x.com/jack/status/20?s=20&t=tracking_id"
private const val GITHUB_URL = "https://github.com/Zyzto/Fukaha"
private const val RELEASES_URL = "https://github.com/Zyzto/Fukaha/releases/latest"
private const val SPONSOR_URL = "https://github.com/sponsors/Zyzto"
private const val DEVELOPER_URL = "https://shenepoy.com"

fun renderApp(app: App, root: HTMLElement) {
    if (Platform.isInAppBrowser) {
        renderInAppBrowserGate(app, root)
        return
    }

    val shell = root.el("div", if (app.view == View.Share) "shell shell-share" else "shell")
    renderTopAppBar(app, shell)

    val content = shell.el("main", if (app.view == View.Share) "content content-share" else "content")
    when (app.view) {
        View.Home -> renderHome(app, content)
        View.Share -> renderShare(app, content)
        View.Settings -> renderSettings(app, content)
        View.About -> renderAbout(app, content)
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
                el("div", "top-app-bar-title") {
                    el("img", "brand-mark") {
                        setAttribute("src", "/icons/icon.svg")
                        setAttribute("alt", "")
                        setAttribute("width", "32")
                        setAttribute("height", "32")
                    }
                    el("div", "top-app-bar-headline") {
                        el("h1", "title-large", s.appName)
                    }
                }
                iconButton(Icon.SETTINGS, s.settings) { app.show(View.Settings) }
            }
            View.Share -> {
                // The sheet headline carries the name, the way Android's share sheet does, so
                // the bar is only back and settings.
                iconButton(Icon.BACK, s.back, "icon-flip") { app.show(View.Home) }
                el("div", "top-app-bar-title")
                iconButton(Icon.SETTINGS, s.settings) { app.show(View.Settings) }
            }
            View.Settings -> {
                iconButton(Icon.BACK, s.back, "icon-flip") { app.leaveSettings() }
                el("div", "top-app-bar-title") {
                    el("h1", "title-large", s.settings)
                }
            }
            View.About -> {
                iconButton(Icon.BACK, s.back, "icon-flip") { app.show(View.Settings) }
                el("div", "top-app-bar-title") {
                    el("h1", "title-large", s.about)
                }
            }
        }
    }
}

// region home

private fun renderHome(app: App, parent: HTMLElement) {
    renderPasteCard(app, parent)
    renderInstallCard(app, parent)
    renderAppOnlyNotes(app, parent)
}

private fun renderPasteCard(app: App, parent: HTMLElement) {
    val s = app.strings
    parent.el("section", "card card-hero") {
        el("h2", "title-medium", s.pasteTitle)

        // Android carries the hint as the field's support line and every action as an in-field
        // button, so there is neither a paragraph above the field nor a button row under it.
        linkField(
            name = s.pasteTitle,
            placeholder = s.pastePlaceholder,
            value = app.draft,
            hint = s.pasteHint,
            sampleLabel = s.pasteSample,
            // Always offered: a missing clipboard API still focuses the field so native paste
            // works on HTTP LAN IPs, where Chromium hides `navigator.clipboard` entirely.
            pasteLabel = s.pasteFromClipboard,
            clearLabel = s.pasteClear,
            submitLabel = s.pasteOpen,
            errorFor = { if (it.isNotBlank() && shareableLink(it) == null) s.pasteInvalid else null },
            onInput = { app.draft = it },
            // Android opens the share screen on the sample without filling the field in.
            onSample = { app.prepare(SAMPLE_LINK) },
            requestPaste = { apply -> app.paste(apply) },
            onSubmit = { submit(app, it) },
            onClear = { app.draft = "" },
        )
    }
}

private fun submit(app: App, raw: String) {
    val link = shareableLink(raw)
    if (link == null) {
        app.notify(app.strings.pasteInvalid)
        return
    }
    app.draft = raw.trim()
    app.prepare(link)
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

    // A browser that can neither prompt nor add to the home screen leaves the user nothing to
    // do about it, and they would meet the message on every visit, so it stays a quiet line.
    if (!PwaInstall.canPrompt && !Platform.isIos) {
        parent.el("p", "note body-medium", s.shareSheetUnsupported) {
            setAttribute("title", s.notInstallableHint)
        }
        return
    }

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
        el("h1", "headline-medium", s.appName)
        el("p", "body-medium on-surface-variant", s.shareSheetSubtitle)
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

/**
 * One labelled URL row: copy on the left, share on the physical right. The surface is pinned
 * LTR the way Android wraps `LinkActionRow` in `LayoutDirection.Ltr`, so Arabic keeps share
 * on the right even though the section label above still follows the page direction.
 */
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
            el("div", "top-app-bar-title") {
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
    parent.el("section", "card") {
        el("p", "body-medium on-surface-variant", s.aboutBody)
    }
    parent.el("div", "list") {
        linkRow(s.githubTitle, s.githubSubtitle, GITHUB_URL)
        linkRow(s.donate, s.donateSubtitle, SPONSOR_URL)
        linkRow(s.developerTitle, s.developerSite, DEVELOPER_URL)
    }
    parent.el("section", "card") {
        el("h2", "title-medium", s.creditsTitle)
        el("p", "body-medium on-surface-variant", s.credits)
    }
}

private fun HTMLElement.linkRow(title: String, subtitle: String, href: String) {
    anchor(href, "list-item state") {
        el("div", "list-item-text") {
            el("span", "body-large", title)
            el("span", "body-small on-surface-variant", subtitle)
        }
        el("div", "list-item-trailing") {
            icon(Icon.OPEN, "icon icon-sm")
        }
    }
}
