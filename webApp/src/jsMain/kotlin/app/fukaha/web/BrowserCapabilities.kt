package app.fukaha.web

import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlin.js.Promise

/**
 * Bridge to the `window.fukahaPwa` object defined in index.html, which has to live in the
 * page because `beforeinstallprompt` fires before this bundle finishes loading.
 */
object PwaInstall {
    private val bridge: dynamic get() = window.asDynamic().fukahaPwa

    val canPrompt: Boolean get() = bridge?.canPrompt == true

    val isStandalone: Boolean get() = bridge?.isStandalone == true

    /** Notified when the install state changes so the UI can re-render. */
    fun onChanged(listener: () -> Unit) {
        bridge?.onChanged = listener
    }

    /**
     * The saved event's `prompt()` is gated on user activation just like [Clipboard.start], so
     * this has to be called synchronously from the click handler. Returns null when the page
     * bridge is missing, which leaves nothing to prompt with.
     */
    fun start(): Promise<Boolean>? {
        val bridge = bridge ?: return null
        return runCatching { bridge.promptInstall() as Promise<Boolean> }.getOrNull()
    }
}

object Platform {
    private val userAgent: String get() = window.navigator.userAgent

    /**
     * Android remains present in Chromium's reduced user agent. Prefer Client Hints when the
     * browser exposes them, with the user agent as a lightweight fallback for other browsers.
     * Both values are read lazily so platform detection does not run during module evaluation.
     */
    val isAndroid: Boolean
        get() {
            val clientPlatform =
                window.navigator.asDynamic().userAgentData?.platform as? String
            return clientPlatform.equals("Android", ignoreCase = true) ||
                Regex("""\bAndroid\b""", RegexOption.IGNORE_CASE).containsMatchIn(userAgent)
        }

    /**
     * iOS has no Web Share Target at any version, so the share-sheet path is replaced by an
     * Add to Home Screen hint. Also true for iPadOS, which reports itself as a Mac with touch.
     */
    val isIos: Boolean
        get() = Regex("iPhone|iPad|iPod").containsMatchIn(userAgent) ||
            (userAgent.contains("Macintosh") && window.navigator.maxTouchPoints > 1)

    /**
     * Safari and every iOS browser. Chromium also includes "AppleWebKit" in its UA, so Chrome
     * and Android are excluded. Used to skip APIs that WebKit implements but paints incorrectly.
     */
    val isWebKitEngine: Boolean
        get() = userAgent.contains("AppleWebKit") &&
            !Regex("Chrome|Chromium|Android").containsMatchIn(userAgent)

    /**
     * In-app browsers (Instagram, Facebook, X, TikTok and friends) cannot install a PWA and
     * their `navigator.share` is unreliable, so we send the user to a real browser instead.
     * Adapted from Hisab's `web/in_app_browser.js`.
     */
    val isInAppBrowser: Boolean
        get() = Regex(
            "FBAN|FBAV|FB_IAB|Instagram|Line/|MicroMessenger|Snapchat|Pinterest|" +
                "TwitterAndroid|musical_ly|Bytedance|WhatsApp",
        ).containsMatchIn(userAgent)

    val canShare: Boolean get() = window.navigator.asDynamic().share != undefined
}

/**
 * Clipboard access is guarded by transient user activation, which only lives until the click
 * handler returns. Starting the call from a coroutine would dispatch after that window has
 * closed, so [start] and [startRead] must be called synchronously from the handler and their
 * promises observed afterwards.
 *
 * Reading is the stricter of the two: Chromium wants the `clipboard-read` permission on top of
 * the activation, older Firefox does not expose `readText` to page script at all, and Safari
 * answers it with its own paste-confirmation popup. A missing read is reported apart from an
 * empty clipboard, and the paste control stays visible so native paste still works.
 *
 * Chromium also treats HTTP on a LAN IP as an insecure origin, so `navigator.clipboard` is
 * missing there even though `http://localhost` is usually fine. Asking via the Permissions API
 * (with `allowWithoutGesture: false`) is what makes the browser prompt instead of silently
 * denying a read — the failure we hit while testing on localhost and local addresses.
 */
object Clipboard {
    /** False on HTTP LAN IPs and other non-secure origins, where the clipboard object is absent. */
    val isSecure: Boolean
        get() = window.asDynamic().isSecureContext == true

    /** Whether the read call exists at all. It does not off a secure origin, or in old Firefox. */
    val canRead: Boolean
        get() = jsTypeOf(window.navigator.asDynamic().clipboard?.readText) == "function"

    /**
     * Tells Chromium we want gesture-gated `clipboard-read`. Query does not prompt; [startRead]
     * does. Must not be awaited from a click handler or the activation is gone before the read.
     */
    fun askReadPermission() {
        val permissions = window.navigator.asDynamic().permissions ?: return
        val descriptor: dynamic = js("({})")
        descriptor.name = "clipboard-read"
        descriptor.allowWithoutGesture = false
        val queried = runCatching { permissions.query(descriptor) }
        if (queried.isFailure) {
            val fallback: dynamic = js("({})")
            fallback.name = "clipboard-read"
            runCatching { permissions.query(fallback) }
        }
        if (jsTypeOf(permissions.request) == "function") {
            runCatching { permissions.request(descriptor) }
        }
    }

    /** Returns null when there is no clipboard API to call. */
    fun start(text: String): Promise<*>? {
        val clipboard = window.navigator.asDynamic().clipboard ?: return null
        return runCatching { clipboard.writeText(text) as Promise<*> }.getOrNull()
    }

    /** For paths with no user gesture to preserve, such as an incoming share. */
    suspend fun write(text: String): Boolean {
        val promise = start(text) ?: return false
        return runCatching { promise.await() }.isSuccess
    }

    /** Returns null when the read cannot even be attempted. */
    fun startRead(): Promise<String>? {
        if (!canRead) return null
        // Chromium's Permissions API query is useful; on iOS it can consume the user
        // activation that `readText` needs to show Safari's paste confirmation.
        if (!Platform.isIos) askReadPermission()
        val clipboard = window.navigator.asDynamic().clipboard
        return runCatching { clipboard.readText() as Promise<String> }.getOrNull()
    }

    /** True when the browser or the origin refused the read, rather than the call failing. */
    fun isDenied(error: Throwable?): Boolean {
        val name = error?.asDynamic()?.name as? String ?: return false
        return name == "NotAllowedError" || name == "SecurityError"
    }
}

object WebShare {
    /**
     * `navigator.share` rejects outright unless it is called while the user gesture is still
     * live, so like [Clipboard.start] this has to run inside the click handler.
     * Returns null when the browser has no Web Share API.
     */
    fun start(text: String): Promise<*>? {
        if (!Platform.canShare) return null
        val payload: dynamic = js("({})")
        payload.text = text
        return runCatching { window.navigator.asDynamic().share(payload) as Promise<*> }.getOrNull()
    }

    /** True when the sheet was dismissed by the user rather than failing to open. */
    fun isDismissal(error: Throwable?): Boolean =
        error?.asDynamic()?.name == "AbortError"
}
