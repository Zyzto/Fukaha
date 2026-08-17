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
    const val CLEAN = "cleaning_services"
    const val COPY = "content_copy"
    const val CLOSE = "close"
    const val EXPAND = "expand_more"
    const val GO = "arrow_forward"
    const val INFO = "info"
    const val INSTALL = "install_mobile"
    const val LINK = "link"
    const val PUBLIC = "public"
    const val SHARE = "share"
    const val OPEN_IN_BROWSER = "open_in_browser"
    const val OPEN = "open_in_new"
    const val PASTE = "content_paste"
    const val REFRESH = "refresh"
    const val SAMPLE = "science"
    const val SETTINGS = "settings"
    const val PREVIEW = "visibility"
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
 * M3 outlined field for a URL, matching the Android quick-link field one for one: a sample
 * button and the link icon leading, paste-or-clear plus a filled go button trailing, and a
 * support line that swaps to the error copy. Android shows a plain placeholder rather than a
 * floating label, so the outline needs no notch cut into it.
 *
 * [name] is the accessible name the missing label would otherwise carry, and [errorFor] returns
 * the message for text the share flow cannot use, or null while the text is still fine.
 * [pasteLabel] is null only when the caller wants no paste control. The click focuses the
 * field first so a blocked programmatic read (localhost / LAN HTTP) still leaves native paste
 * available. [requestPaste] is handed the callback that applies the pasted text, and is
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
    errorFor: (String) -> String?,
    onInput: (String) -> Unit,
    onSample: () -> Unit,
    requestPaste: ((String) -> Unit) -> Unit,
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
                // Paste and clear are never up at the same time, so they share one slot.
                el("div", "field-swap") {
                    pasteLabel?.let { label ->
                        paste = iconButton(Icon.PASTE, label, "field-btn") {
                            // Focus first so a blocked programmatic read still leaves the field
                            // ready for a native paste (the localhost / LAN-IP case).
                            input.focus()
                            requestPaste { pasted ->
                                input.value = pasted
                                onInput(pasted)
                                refresh()
                            }
                        }
                    }
                    clear = iconButton(Icon.CLOSE, clearLabel, "field-btn") {
                        input.value = ""
                        onClear()
                        refresh()
                        input.focus()
                    }
                }
                submit = iconButton(Icon.GO, submitLabel, "field-btn icon-btn-filled") {
                    onSubmit(input.value)
                }
            }
        }
        support = el("p", "field-support body-small") { id = supportId }
    }

    refresh = {
        val empty = input.value.isBlank()
        val error = errorFor(input.value)
        support.textContent = error ?: hint
        field.classList.toggle("field-error", error != null)
        input.setAttribute("aria-invalid", (error != null).toString())
        // Every button keeps its slot and only hides, so the text under the caret does not jump
        // sideways as buttons come and go on a keystroke.
        sample.classList.toggle("field-btn-off", !empty)
        paste?.classList?.toggle("field-btn-off", !empty)
        clear.classList.toggle("field-btn-off", empty)
        submit.classList.toggle("field-btn-off", empty || error != null)
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
        if (event.asDynamic().key == "Enter" && usable) onSubmit(input.value)
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
