package app.fukaha.web

import app.fukaha.EmbedHealthProgress
import app.fukaha.EmbedHealthStatus
import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.events.Event
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class WebDomTest {
    private lateinit var root: HTMLElement

    @BeforeTest
    fun setUp() {
        localStorage.removeItem("fukaha_settings")
        root = document.createElement("div") as HTMLElement
        root.id = "test-app"
        document.body!!.appendChild(root)
    }

    @AfterTest
    fun tearDown() {
        root.remove()
        localStorage.removeItem("fukaha_settings")
        document.documentElement?.setAttribute("lang", "en")
        document.documentElement?.setAttribute("dir", "ltr")
        document.documentElement?.removeAttribute("data-theme")
    }

    @Test
    fun homeAndSettingsKeepStableTopBarSlotsAndDirectionalBackArrow() {
        val app = App(root)
        app.render()

        assertNotNull(root.querySelector(".top-app-bar-title-home"))
        assertNotNull(root.querySelector(".top-app-bar-route-action"))
        assertNotNull(root.querySelector(".top-app-bar-language"))
        assertNotNull(root.querySelector(".top-app-bar-theme"))
        assertNull(root.querySelector(".top-app-bar-navigation"))
        assertNotNull(root.querySelector("main.content .paste-section"))

        app.show(View.Settings)

        val back = root.querySelector(".top-app-bar-navigation") as HTMLElement
        assertTrue(back.classList.contains("icon-flip"))
        assertEquals(Icon.BACK, back.querySelector(".icon")?.textContent)
        assertNotNull(root.querySelector(".top-app-bar-placeholder[aria-hidden=true]"))
        assertNotNull(root.querySelector(".top-app-bar-language"))
        assertNotNull(root.querySelector(".top-app-bar-theme"))
        assertNotNull(root.querySelector("main.content .settings-first-section"))
        assertNull(root.querySelector(".paste-section"))
    }

    @Test
    fun healthProgressUpdatesOnlyItsSettingsBoundary() {
        val app = App(root)
        app.show(View.Settings)
        val unrelatedAction = root.querySelector(".settings-first-section .action-option")
        val summary = root.querySelector("#$EMBED_HEALTH_SUMMARY_ID")
        val progress = root.querySelector("#$EMBED_HEALTH_PROGRESS_ID") as HTMLElement
        assertNotNull(unrelatedAction)
        assertNotNull(summary)
        assertTrue(progress.hasAttribute("hidden"))

        app.updateHealthProgress(
            progress = EmbedHealthProgress(
                currentHost = "fixupx.com",
                currentIndex = 1,
                total = 3,
                aliveCount = 1,
            ),
            completedStatus = EmbedHealthStatus.Alive,
        )

        assertSame(unrelatedAction, root.querySelector(".settings-first-section .action-option"))
        assertSame(summary, root.querySelector("#$EMBED_HEALTH_SUMMARY_ID"))
        assertEquals(app.strings.embedHealthProgress(1, 3), summary.textContent)
        assertFalse(progress.hasAttribute("hidden"))
        assertEquals("1", progress.getAttribute("aria-valuenow"))
        assertTrue(
            (progress.querySelector(".progress-bar") as HTMLElement).style.transform.contains("0.333"),
        )
    }

    @Test
    fun aboutDisplaysCanonicalVersionAsAccessibleNonInteractiveText() {
        val app = App(root)
        app.show(View.Settings)

        val row = root.querySelector(".version-row") as HTMLElement
        val value = row.querySelector("[data-app-version]") as HTMLElement

        assertEquals("${app.strings.version}: $WEB_APP_VERSION", row.getAttribute("aria-label"))
        assertEquals(WEB_APP_VERSION, value.textContent)
        assertEquals(WEB_APP_VERSION, value.getAttribute("data-app-version"))
        assertEquals("ltr", value.dir)
        assertNull(row.getAttribute("href"))
        assertNull(row.getAttribute("role"))
    }

    @Test
    fun shareRouteRendersResponsiveModalStateAndAccessibleDialog() {
        val app = App(root)
        app.show(View.Share)

        val scrim = root.querySelector(".sheet-scrim")
        val dialog = root.querySelector(".shell.shell-share") as HTMLElement
        assertNotNull(scrim)
        assertEquals("dialog", dialog.getAttribute("role"))
        assertEquals("true", dialog.getAttribute("aria-modal"))
        assertEquals("page-title", dialog.getAttribute("aria-labelledby"))
        assertNotNull(dialog.querySelector(".sheet-handle[aria-hidden=true]"))
        assertNotNull(dialog.querySelector("main.content-share"))
        assertNull(dialog.querySelector(".top-app-bar"))
    }

    @Test
    fun languageMenuExposesSelectionStateAndDismissesOutside() {
        val app = App(root)
        app.render()
        val trigger = root.querySelector("#$LANGUAGE_MENU_TRIGGER_ID") as HTMLButtonElement

        trigger.click()
        val openTrigger = root.querySelector("#$LANGUAGE_MENU_TRIGGER_ID") as HTMLButtonElement

        assertEquals("true", openTrigger.getAttribute("aria-expanded"))
        assertEquals("menu", root.querySelector("#$LANGUAGE_MENU_ID")?.getAttribute("role"))
        val options = root.querySelectorAll("[role=menuitemradio]")
        assertEquals(LANGUAGE_OPTIONS.size, options.length)
        assertEquals(
            1,
            (0 until options.length).count {
                (options.item(it) as? Element)?.getAttribute("aria-checked") == "true"
            },
        )

        document.body!!.dispatchEvent(js("new Event('pointerdown', { bubbles: true })") as Event)

        assertEquals("false", openTrigger.getAttribute("aria-expanded"))
        assertTrue((root.querySelector("#$LANGUAGE_MENU_ID") as HTMLElement).classList.contains("motion-exit"))
    }

    @Test
    fun escapeDismissesLanguageMenuAndReturnsFocus() {
        val app = App(root)
        app.render()
        val trigger = root.querySelector("#$LANGUAGE_MENU_TRIGGER_ID") as HTMLButtonElement
        trigger.click()
        val openTrigger = root.querySelector("#$LANGUAGE_MENU_TRIGGER_ID") as HTMLButtonElement

        document.dispatchEvent(
            js("new KeyboardEvent('keydown', { key: 'Escape', bubbles: true })") as Event,
        )

        assertEquals("false", openTrigger.getAttribute("aria-expanded"))
        assertEquals(LANGUAGE_MENU_TRIGGER_ID, document.activeElement?.id)
    }

    @Test
    fun selectingLanguagePersistsAndAppliesArabicRtlWithoutMotion(): dynamic {
        val browserWindow = kotlinx.browser.window.asDynamic()
        val originalMatchMedia = browserWindow.matchMedia
        browserWindow.matchMedia =
            js("function(query) { return {matches: query === '(prefers-reduced-motion: reduce)'}; }")
        return MainScope().promise {
            try {
                val app = App(root)
                app.render()
                (root.querySelector("#$LANGUAGE_MENU_TRIGGER_ID") as HTMLButtonElement).click()
                (root.querySelector("[role=menuitemradio]") as HTMLButtonElement).click()
                delay(50)

                assertEquals("ar", document.documentElement?.getAttribute("lang"))
                assertEquals("rtl", document.documentElement?.getAttribute("dir"))
                val stored = localStorage.getItem("fukaha_settings")
                assertNotNull(stored)
                assertTrue(stored.contains("\"language\":\"Arabic\""))
                assertNull(root.querySelector("#$LANGUAGE_MENU_ID"))
                assertFalse(document.documentElement?.classList?.contains("language-view-transition") == true)
            } finally {
                browserWindow.matchMedia = originalMatchMedia
            }
        }
    }
}
