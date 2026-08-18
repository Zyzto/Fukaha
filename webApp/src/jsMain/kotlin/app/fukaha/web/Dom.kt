package app.fukaha.web

import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement

/** Material Symbols ligature names. The set is subsetted in index.html, so keep them in sync. */
object Icon {
    const val BACK = "arrow_back"
    const val CHECK = "check"
    const val CHEVRON = "chevron_right"
    const val CLEAN = "cleaning_services"
    const val CODE = "code"
    const val COPY = "content_copy"
    const val CLOSE = "close"
    const val EXPAND = "expand_more"
    const val FAVORITE = "favorite"
    const val GO = "arrow_forward"
    const val INFO = "info"
    const val INSTALL = "install_mobile"
    const val LANGUAGE = "language"
    const val LINK = "link"
    const val LIGHT = "light_mode"
    const val DARK = "dark_mode"
    const val PUBLIC = "public"
    const val SHARE = "share"
    const val OPEN_IN_BROWSER = "open_in_browser"
    const val OPEN = "open_in_new"
    const val PASTE = "content_paste"
    const val PERSON = "person"
    const val REFRESH = "refresh"
    const val SAMPLE = "science"
    const val SETTINGS = "settings"
    const val PREVIEW = "visibility"
    const val SYSTEM = "routine"
}

enum class ButtonStyle(val classes: String) {
    Filled("btn btn-filled state"),
    Tonal("btn btn-tonal state"),
    Outlined("btn btn-outlined state"),
    Text("btn btn-text state"),
}

fun Element.clear() {
    while (firstChild != null) removeChild(firstChild!!)
}

fun Element.el(
    tag: String,
    classes: String? = null,
    text: String? = null,
    build: HTMLElement.() -> Unit = {},
): HTMLElement {
    val child = document.createElement(tag) as HTMLElement
    classes?.let { child.className = it }
    text?.let { child.textContent = it }
    child.build()
    appendChild(child)
    return child
}

fun Element.icon(name: String, classes: String = "icon"): HTMLElement =
    el("span", classes, name) {
        setAttribute("aria-hidden", "true")
    }

fun Element.button(
    label: String,
    style: ButtonStyle = ButtonStyle.Filled,
    leadingIcon: String? = null,
    extraClasses: String = "",
    onClick: () -> Unit,
): HTMLButtonElement {
    val child = document.createElement("button") as HTMLButtonElement
    child.className = listOf(style.classes, extraClasses).filter { it.isNotBlank() }.joinToString(" ")
    child.type = "button"
    leadingIcon?.let { child.icon(it) }
    child.el("span", "btn-label", label)
    child.onclick = { onClick() }
    appendChild(child)
    return child
}

/**
 * M3 icon button. [label] is not drawn, so it carries the accessible name the glyph cannot.
 */
fun Element.iconButton(
    iconName: String,
    label: String,
    extraClasses: String = "",
    onClick: () -> Unit,
): HTMLButtonElement {
    val child = document.createElement("button") as HTMLButtonElement
    child.className = listOf("icon-btn state", extraClasses).filter { it.isNotBlank() }.joinToString(" ")
    child.type = "button"
    child.setAttribute("aria-label", label)
    child.setAttribute("title", label)
    child.icon(iconName)
    child.onclick = { onClick() }
    appendChild(child)
    return child
}

/** External anchor with the safe target/rel pair applied. Content is left to [build]. */
fun Element.anchor(
    href: String,
    classes: String,
    build: HTMLAnchorElement.() -> Unit,
): HTMLAnchorElement {
    val child = document.createElement("a") as HTMLAnchorElement
    child.className = classes
    child.href = href
    child.target = "_blank"
    child.rel = "noopener noreferrer"
    child.build()
    appendChild(child)
    return child
}

fun Element.link(
    label: String,
    href: String,
    classes: String = "btn btn-text state",
    trailingIcon: String? = null,
): HTMLAnchorElement = anchor(href, classes) {
    el("span", "btn-label", label)
    trailingIcon?.let { icon(it, "icon icon-sm") }
}

/** Anchor drawn as an M3 icon button, so [label] only carries the accessible name. */
fun Element.iconLink(
    iconName: String,
    label: String,
    href: String,
    extraClasses: String = "",
): HTMLAnchorElement = anchor(
    href,
    listOf("icon-btn state", extraClasses).filter { it.isNotBlank() }.joinToString(" "),
) {
    setAttribute("aria-label", label)
    setAttribute("title", label)
    icon(iconName)
}

private var fieldSeq = 0

/**
 * M3 outlined field for a URL, matching Android's quick-link field: sample and link icons
 * lead, while the available paste, clear, and filled go actions trail inside the outline.
 *
 * [name] is the accessible name the missing label would otherwise carry, and [errorFor] returns
 * the message for text the share flow cannot use, or null while the text is still fine.
 * [pasteLabel] is null only when the caller wants no paste control. The click focuses the
 * field first on non-iOS browsers so a blocked programmatic read (localhost / LAN HTTP) still
 * leaves native paste available. On iPhone the focus is deferred until after `readText`,
 * otherwise Safari consumes the user gesture and never shows its paste confirmation.
 * [requestPaste] is handed the callback that applies the pasted text, and is
 * invoked straight from the click so the read keeps its user activation.
 *
 * The buttons are siblings of the `<label>`, not children: a click inside a label is forwarded
 * to the labelled control, which would refocus the input instead of running the action. They
 * and the support line are updated in place rather than by re-rendering, so typing is never
 * interrupted mid-word.
 */
fun Element.linkField(
    name: String,
    placeholder: String,
    value: String,
    hint: String,
    sampleLabel: String,
    pasteLabel: String?,
    clearLabel: String,
    submitLabel: String,
    submittingLabel: String,
    submitting: Boolean,
    errorFor: (String) -> String?,
    onInput: (String) -> Unit,
    onSample: () -> Unit,
    requestPaste: (apply: (String) -> Unit, onFallback: () -> Unit) -> Unit,
    onSubmit: (String) -> Unit,
    onClear: () -> Unit,
): HTMLInputElement {
    val supportId = "field-support-${++fieldSeq}"
    lateinit var input: HTMLInputElement
    lateinit var sample: HTMLButtonElement
    lateinit var clear: HTMLButtonElement
    lateinit var submit: HTMLButtonElement
    var paste: HTMLButtonElement? = null
    lateinit var support: HTMLElement
    var refresh: () -> Unit = {}

    val field = el("div", "field") {
        el("div", "field-box") {
            el("div", "field-leading") {
                sample = iconButton(Icon.SAMPLE, sampleLabel, "field-btn") { onSample() }
            }
            el("label", "field-main") {
                icon(Icon.LINK)
                input = el("input", "field-input") {
                    setAttribute("type", "url")
                    setAttribute("inputmode", "url")
                    setAttribute("placeholder", placeholder)
                    setAttribute("aria-label", name)
                    setAttribute("aria-describedby", supportId)
                    setAttribute("spellcheck", "false")
                    setAttribute("autocapitalize", "off")
                    setAttribute("autocomplete", "off")
                    setAttribute("enterkeyhint", "go")
                    dir = "ltr"
                } as HTMLInputElement
            }
            el("div", "field-trailing") {
                clear = iconButton(Icon.CLOSE, clearLabel, "field-btn") {
                    input.value = ""
                    onClear()
                    refresh()
                    input.focus()
                }
                pasteLabel?.let { label ->
                    paste = iconButton(Icon.PASTE, label, "field-btn") {
                        // iOS loses the click's user activation if the input is focused first,
                        // so Safari never shows its paste confirmation. Focus only as fallback.
                        if (!Platform.isIos) input.focus()
                        requestPaste(
                            { pasted ->
                                input.value = pasted
                                onInput(pasted)
                                refresh()
                                input.focus()
                            },
                            { input.focus() },
                        )
                    }
                }
                submit = iconButton(Icon.GO, submitLabel, "field-btn icon-btn-filled") {
                    onSubmit(input.value)
                }
                if (submitting) {
                    submit.clear()
                    submit.el("span", "spinner field-spinner") {
                        setAttribute("aria-hidden", "true")
                    }
                    submit.setAttribute("aria-label", submittingLabel)
                    submit.setAttribute("title", submittingLabel)
                }
            }
        }
        support = el("p", "field-support body-small") {
            id = supportId
            setAttribute("role", "status")
            setAttribute("aria-live", "polite")
        }
    }

    refresh = {
        val empty = input.value.isBlank()
        val error = errorFor(input.value)
        field.setAttribute("aria-busy", submitting.toString())
        input.disabled = submitting
        support.textContent = error ?: hint
        field.classList.toggle("field-error", error != null)
        input.setAttribute("aria-invalid", (error != null).toString())
        if (error != null) input.setAttribute("aria-errormessage", supportId)
        else input.removeAttribute("aria-errormessage")
        sample.classList.toggle("field-btn-off", !empty)
        sample.disabled = !empty || submitting
        paste?.classList?.toggle("field-btn-off", !empty)
        paste?.disabled = !empty || submitting
        clear.classList.toggle("field-btn-off", empty)
        clear.disabled = empty || submitting
        submit.classList.toggle("field-btn-off", !submitting && (empty || error != null))
        submit.disabled = submitting || empty || error != null
    }

    input.value = value
    input.oninput = {
        refresh()
        onInput(input.value)
        Unit
    }
    input.onkeydown = { event ->
        // Android binds Go to the same condition as the arrow, so an unusable link does nothing
        // beyond the error already sitting in the support line.
        val usable = input.value.isNotBlank() && errorFor(input.value) == null
        if (event.asDynamic().key == "Enter" && usable && !submitting) onSubmit(input.value)
        Unit
    }
    refresh()
    return input
}

/**
 * URLs are rendered inside RTL pages too, where bidi reordering would move trailing
 * punctuation and make the link unreadable. Matches `asLtrUrl` on Android.
 */
fun Element.urlText(url: String, classes: String = "url") {
    el("span", classes, url) {
        dir = "ltr"
        setAttribute("lang", "en")
    }
}
