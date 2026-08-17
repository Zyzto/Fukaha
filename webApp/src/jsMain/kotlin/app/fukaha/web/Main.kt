package app.fukaha.web

import app.fukaha.AppTheme
import app.fukaha.EmbedHealthKeys
import app.fukaha.EmbedHealthPolicy
import app.fukaha.EmbedHealthProgress
import app.fukaha.EmbedHealthSnapshot
import app.fukaha.EmbedHealthStatus
import app.fukaha.EmbedService
import app.fukaha.FukahaBridge
import app.fukaha.FukahaSettings
import app.fukaha.PlatformClock
import app.fukaha.PreparedLink
import app.fukaha.ShareAction
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLElement
import org.w3c.dom.url.URLSearchParams

enum class View { Home, Share, Settings, About }

private const val STATUS_MS = 2600

class App(private val root: HTMLElement) {
    private val scope: CoroutineScope = MainScope()
    private val bridge = FukahaBridge()
    private val store = WebSettingsStore()
    private val healthStore = WebHealthStore()

    var settings: FukahaSettings = FukahaSettings(resolveShortLinks = false)
        private set
    var strings: Strings = Strings.EN
        private set
    var view: View = View.Home
        private set
    var prepared: PreparedLink? = null
        private set
    var status: String? = null
        private set
    var busy: Boolean = false
        private set

    var health: EmbedHealthSnapshot = EmbedHealthSnapshot()
        private set

    /** Non-null while an embedder check is running. */
    var healthProgress: EmbedHealthProgress? = null
        private set

    /** Kept across renders so typing is not lost when the view rebuilds. */
    var draft: String = ""

    /** Platform key whose fixer list is expanded in Settings, if any. */
    var openFixerPlatform: String? = null
        private set

    private var statusTimer: Int = 0

    fun start(sharedText: String?) {
        // Query only: Chromium needs to know we want gesture-gated clipboard-read before
        // the paste click, otherwise localhost/LAN testing often denies without a prompt.
        Clipboard.askReadPermission()
        scope.launch {
            settings = store.get()
            health = healthStore.get()
            applyLocaleAndTheme()
            PwaInstall.onChanged { render() }
            if (sharedText.isNullOrBlank()) {
                render()
            } else {
                consumeSharedText(sharedText)
            }
        }
    }

    // region navigation

    fun show(next: View) {
        view = next
        openFixerPlatform = null
        status = null
        render()
    }

    /** Settings is reached from wherever the user was, so going back returns there. */
    fun leaveSettings() {
        show(if (prepared != null) View.Share else View.Home)
    }

    // endregion

    // region link actions

    fun prepare(text: String) {
        scope.launch { consumeSharedText(text) }
    }

    private suspend fun consumeSharedText(text: String) {
        busy = true
        view = View.Share
        render()

        val result = bridge.prepare(text, settings, health.statuses)
        busy = false
        if (result == null) {
            prepared = null
            view = View.Home
            draft = text.trim()
            notify(strings.noLink)
            return
        }

        prepared = result
        render()
        runDefaultAction(result)
    }

    /**
     * `navigator.share` needs a user gesture and a share-target navigation gives us none, so an
     * immediate action copies rather than opening a share sheet that would be rejected. The
     * result stays on screen with a Share button either way.
     */
    private suspend fun runDefaultAction(link: PreparedLink) {
        val target = when (settings.effectiveDefaultAction()) {
            ShareAction.Clean -> link.detected.cleanedUrl
            ShareAction.Embed -> link.embedUrl ?: link.detected.cleanedUrl
            ShareAction.Ask, ShareAction.Download -> return
        }
        if (Clipboard.write(target)) notify(strings.copied)
    }

    /** Called straight from a click handler so the clipboard write keeps its user activation. */
    fun copy(text: String) {
        val promise = Clipboard.start(text)
        if (promise == null) {
            notify(strings.copyFailed)
            return
        }
        promise.then(
            onFulfilled = { notify(strings.copied) },
            onRejected = { notify(strings.copyFailed) },
        )
    }

    /**
     * Gesture-bound for the same reason as [copy], and less forgiving: a read that starts after
     * the click handler has returned is refused even where the permission was already granted.
     * [onText] receives the clipboard text; every other outcome names itself, because "denied"
     * and "empty" ask the user for different things.
     */
    fun paste(onText: (String) -> Unit) {
        val promise = Clipboard.startRead()
        if (promise == null) {
            notify(if (Clipboard.isSecure) strings.clipboardFailed else strings.clipboardInsecure)
            return
        }
        promise.then(
            onFulfilled = { text ->
                val trimmed = text.trim()
                if (trimmed.isEmpty()) notify(strings.clipboardEmpty) else onText(trimmed)
            },
            onRejected = { error ->
                notify(
                    when {
                        !Clipboard.isSecure -> strings.clipboardInsecure
                        Clipboard.isDenied(error) -> strings.clipboardDenied
                        else -> strings.clipboardFailed
                    },
                )
            },
        )
    }

    /** Also gesture-bound: `navigator.share` rejects if it is called any later. */
    fun share(text: String) {
        val promise = WebShare.start(text)
        if (promise == null) {
            // No Web Share API in this browser, so copying is the useful equivalent.
            copy(text)
            return
        }
        promise.then(
            onFulfilled = { },
            onRejected = { error ->
                // A dismissed sheet was a deliberate choice; only a real failure falls back.
                if (!WebShare.isDismissal(error)) copy(text)
            },
        )
    }

    /** Gesture-bound like [copy]: the install prompt is refused without live user activation. */
    fun install() {
        val promise = PwaInstall.start() ?: return
        promise.then(
            onFulfilled = { render() },
            onRejected = { render() },
        )
    }

    // endregion

    // region settings

    fun toggleFixerPicker(platformKey: String?) {
        openFixerPlatform = if (openFixerPlatform == platformKey) null else platformKey
        render()
    }

    fun updateSettings(transform: (FukahaSettings) -> FukahaSettings) {
        scope.launch {
            store.update(transform)
            settings = store.get()
            applyLocaleAndTheme()
            recomputePrepared()
            render()
        }
    }

    // endregion

    // region embedder health

    /**
     * Manual only. Probing contacts every fixer host from the user's browser, so it never runs
     * unprompted the way the Android app's six-hourly refresh does.
     */
    fun checkEmbedders() {
        if (healthProgress != null) {
            notify(strings.embedHealthBusy)
            return
        }
        val cooldown = EmbedHealthPolicy.cooldownRemainingMs(health.checkedAtEpochMs)
        if (cooldown > 0L) {
            notify(strings.embedHealthCooldownToast(minutesOf(cooldown)))
            return
        }

        scope.launch {
            val hosts = uniqueFixerHosts()
            healthProgress = EmbedHealthProgress(
                currentHost = hosts.firstOrNull().orEmpty(),
                currentIndex = 0,
                total = hosts.size,
            )
            render()

            val results = WebEmbedHealth.refresh(hosts) { progress ->
                healthProgress = progress
                render()
            }
            healthProgress = null

            // Everything failing means the browser is offline, not that the embedders died.
            if (!EmbedHealthPolicy.isUsableResult(results)) {
                notify(strings.embedHealthOffline)
                return@launch
            }

            val now = PlatformClock.epochMillis()
            healthStore.save(results, now)
            health = EmbedHealthSnapshot(results, now)
            recomputePrepared()
            notify(strings.embedHealthCounts(health.aliveCount, health.deadCount))
        }
    }

    fun statusOf(host: String): EmbedHealthStatus = health.statusOf(host)

    private fun uniqueFixerHosts(): List<String> =
        bridge.platformKeys()
            .flatMap { bridge.servicesFor(it) }
            .map { EmbedHealthKeys.normalize(it.normalizedHost()) }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

    fun minutesOf(millis: Long): Int = ((millis + 59_999L) / 60_000L).toInt()

    // endregion

    /** A settings or health change can move the embed link, so recompute what is on screen. */
    private suspend fun recomputePrepared() {
        val current = prepared ?: return
        prepared = bridge.prepare(current.detected.originalUrl, settings, health.statuses)
    }

    /** Shows a transient message in the live region, replacing any message still on screen. */
    fun notify(message: String) {
        window.clearTimeout(statusTimer)
        status = message
        render()
        statusTimer = window.setTimeout({
            status = null
            render()
        }, STATUS_MS)
    }

    private fun applyLocaleAndTheme() {
        strings = Strings.forLanguage(settings.language)
        val arabic = Strings.isArabic(settings.language)
        document.documentElement?.setAttribute("lang", if (arabic) "ar" else "en")
        document.documentElement?.setAttribute("dir", if (arabic) "rtl" else "ltr")
        when (settings.theme) {
            AppTheme.System -> document.documentElement?.removeAttribute("data-theme")
            AppTheme.Light -> document.documentElement?.setAttribute("data-theme", "light")
            AppTheme.Dark -> document.documentElement?.setAttribute("data-theme", "dark")
        }
        document.title = strings.appName
    }

    fun render() {
        root.clear()
        renderApp(this, root)
        document.getElementById("boot-splash")?.remove()
    }

    fun catalogPlatformKeys(): List<String> = bridge.platformKeys()

    fun servicesFor(platformKey: String) = bridge.servicesFor(platformKey)

    /** The user's stored fixer for [platformKey], falling back to the catalog default. */
    fun pickedFixer(platformKey: String): EmbedService? =
        bridge.serviceForHost(platformKey, chosenFixerHost(platformKey))

    /**
     * Fixer a link would really be rewritten to right now. Differs from [pickedFixer] once a
     * check marks the pick dead, so Settings can show what sharing does instead of what is stored.
     */
    fun effectiveFixer(platformKey: String): EmbedService? =
        bridge.effectiveService(platformKey, chosenFixerHost(platformKey), health.statuses)

    private fun chosenFixerHost(platformKey: String): String? =
        settings.preferredFixers[platformKey] ?: bridge.defaultFixer(platformKey)
}

fun main() {
    val root = document.getElementById("app") as? HTMLElement ?: return
    registerServiceWorker()
    App(root).start(readSharedText())
}

/**
 * Web Share Target hands us the link in `url`, `text`, or `title` depending on the sending
 * app — Chrome on Android usually uses `text` for a shared page, not `url`. Everything is
 * concatenated and left to `UrlCleaner.extractFirstUrl`, which already digs a URL out of
 * surrounding prose.
 */
private fun readSharedText(): String? {
    val params = URLSearchParams(window.location.search)
    val combined = listOfNotNull(params.get("url"), params.get("text"), params.get("title"))
        .filter { it.isNotBlank() }
        .joinToString(" ")
    if (combined.isBlank()) return null
    // Drop the shared link from the address bar so a refresh does not reprocess it.
    window.history.replaceState(null, "", window.location.pathname)
    return combined
}

private fun registerServiceWorker() {
    val host = window.location.hostname
    if (host == "localhost" || host == "127.0.0.1") return
    window.addEventListener("load", {
        window.navigator.asDynamic().serviceWorker?.register("/sw.js")
    })
}
