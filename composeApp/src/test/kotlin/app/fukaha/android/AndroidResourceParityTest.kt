package app.fukaha.android

import app.fukaha.BuildConfig
import app.fukaha.android.settings.shareableLink
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.w3c.dom.Element

class AndroidResourceParityTest {
    private val resourceRoot = File("src/main/res")
    private val localizedFiles = linkedMapOf(
        "en" to File(resourceRoot, "values/strings.xml"),
        "ar" to File(resourceRoot, "values-ar/strings.xml"),
        "ja" to File(resourceRoot, "values-ja/strings.xml"),
        "zh-CN" to File(resourceRoot, "values-zh-rCN/strings.xml"),
        "es" to File(resourceRoot, "values-es/strings.xml"),
    )

    @Test
    fun everySupportedLocaleHasExactlyTheBaseResourcesAndPlaceholders() {
        val resources = localizedFiles.mapValues { (_, file) -> readStrings(file) }
        val base = resources.getValue("en")

        resources.forEach { (locale, strings) ->
            assertEquals(base.keys, strings.keys, "$locale string names differ from base")
            base.forEach { (name, value) ->
                assertEquals(
                    placeholders(value),
                    placeholders(strings.getValue(name)),
                    "$locale placeholders differ for $name",
                )
                assertTrue(strings.getValue(name).isNotBlank(), "$locale has a blank $name")
            }
        }
    }

    @Test
    fun localeConfigExactlyMatchesLocalizedResourcesAndParsesAsXml() {
        val config = parse(File(resourceRoot, "xml/locales_config.xml"))
        val localeNodes = config.getElementsByTagName("locale")
        val configured = (0 until localeNodes.length).map { index ->
            (localeNodes.item(index) as Element).getAttributeNS(ANDROID_NS, "name")
        }

        assertEquals(localizedFiles.keys.toList(), configured)
        localizedFiles.values.forEach { parse(it) }
    }

    @Test
    fun manifestEnablesRtlAndReferencesLocaleConfig() {
        val manifest = parse(File("src/main/AndroidManifest.xml"))
        val application = manifest.getElementsByTagName("application").item(0) as? Element
        assertNotNull(application)
        assertEquals("true", application.getAttributeNS(ANDROID_NS, "supportsRtl"))
        assertEquals("@xml/locales_config", application.getAttributeNS(ANDROID_NS, "localeConfig"))
    }

    @Test
    fun mainTopBarUsesTheTransparentWebsiteBrandArtwork() {
        val mainActivity = File(
            "src/main/kotlin/app/fukaha/android/MainActivity.kt",
        ).readText()
        val brandTitle = mainActivity
            .substringAfter("private fun FukahaBrandTitle()")
            .substringBefore("/**")

        assertTrue(
            "painterResource(R.drawable.ic_fukaha_brand)" in brandTitle,
            "The main top bar must use the dedicated transparent brand drawable",
        )
        assertTrue("R.drawable.ic_launcher" !in brandTitle)
        assertTrue("tint = Color.Unspecified" in brandTitle)
        assertTrue("contentDescription = null" in brandTitle)

        val brandFile = File(resourceRoot, "drawable/ic_fukaha_brand.xml")
        val webIconFile = File("../webApp/src/jsMain/resources/icons/icon.svg")
        val webTopBar = File(
            "../webApp/src/jsMain/kotlin/app/fukaha/web/Views.kt",
        ).readText()
        val brand = parse(brandFile)
        val webIcon = parse(webIconFile)
        assertEquals("vector", brand.documentElement.tagName)
        assertEquals(0, brand.getElementsByTagName("background").length)
        assertEquals(0, brand.getElementsByTagName("adaptive-icon").length)
        assertTrue("""setAttribute("src", "/icons/icon.svg")""" in webTopBar)
        assertEquals("0 0 128 128", webIcon.documentElement.getAttribute("viewBox"))
        assertEquals("128", brand.documentElement.getAttributeNS(ANDROID_NS, "viewportWidth"))
        assertEquals("128", brand.documentElement.getAttributeNS(ANDROID_NS, "viewportHeight"))
        assertEquals(
            pathTokens(webIcon, "d").sortedBy { it.joinToString() },
            pathTokens(brand, "pathData", ANDROID_NS).sortedBy { it.joinToString() },
            "Android brand geometry must match the website icon SVG",
        )
        assertEquals(
            hexColors(webIconFile.readText()),
            hexColors(brandFile.readText()),
            "Android brand colors must match the website icon SVG",
        )
        assertTrue(
            brand.getElementsByTagName("gradient").length > 1,
            "Brand drawable must preserve its original multicolor gradients",
        )
    }

    @Test
    fun androidSettingsKeepsTheInAppPasteFieldAtTheTop() {
        val settings = File(
            "src/main/kotlin/app/fukaha/android/settings/SettingsScreens.kt",
        ).readText()
        val firstItem = settings
            .substringAfter("LazyColumn(")
            .substringAfter("item {")
            .substringBefore("item {")

        assertTrue(
            "QuickLinkSection(" in firstItem,
            "Paste-a-link must remain the first Android Settings section",
        )
        assertTrue("fun QuickLinkSection(" in settings)
        assertTrue("Icons.Outlined.Science" in settings)
        assertTrue("Icons.Outlined.ContentPaste" in settings)
        assertTrue("R.string.quick_use_clear" in settings)
        assertTrue("R.string.quick_use_open" in settings)
        assertTrue("ShareActivity.EXTRA_FORCE_ASK" in settings)
        assertTrue("Intent(this, ShareActivity::class.java)" in settings)

        val strings = readStrings(localizedFiles.getValue("en"))
        listOf(
            "section_quick_use",
            "quick_use_hint",
            "quick_use_field_placeholder",
            "quick_use_paste",
            "quick_use_clear",
            "quick_use_open",
            "quick_use_invalid",
            "quick_use_clipboard_empty",
            "quick_use_sample",
        ).forEach { name ->
            assertTrue(strings.containsKey(name), "Missing Android paste-field string $name")
        }
    }

    @Test
    fun shareableLinkAcceptsUrlsAndBareHosts() {
        assertEquals(
            "https://x.com/user/status/1",
            shareableLink("https://x.com/user/status/1"),
        )
        assertEquals("https://x.com/user/status/1", shareableLink("  x.com/user/status/1  "))
        assertEquals(null, shareableLink(""))
        assertEquals(null, shareableLink("not a link"))
    }

    @Test
    fun buildVersionAndLocalizedVersionLabelsStayConsistent() {
        assertEquals("0.5.1", BuildConfig.VERSION_NAME)
        assertTrue(BuildConfig.VERSION_CODE >= 12, "versionCode must not decrease")

        val expectedLabels = mapOf(
            "en" to "Version",
            "ar" to "الإصدار",
            "ja" to "バージョン",
            "zh-CN" to "版本",
            "es" to "Versión",
        )
        assertEquals(
            expectedLabels,
            localizedFiles.mapValues { (_, file) -> readStrings(file).getValue("version") },
        )
    }

    private fun readStrings(file: File): Map<String, String> {
        val document = parse(file)
        val nodes = document.getElementsByTagName("string")
        return buildMap {
            repeat(nodes.length) { index ->
                val element = nodes.item(index) as Element
                val name = element.getAttribute("name")
                check(put(name, element.textContent) == null) {
                    "Duplicate string $name in ${file.path}"
                }
            }
        }
    }

    private fun parse(file: File) =
        DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().parse(file).also { it.documentElement.normalize() }

    private fun placeholders(value: String): List<String> =
        PLACEHOLDER.findAll(value).map { it.value }.sorted().toList()

    private fun pathTokens(
        document: org.w3c.dom.Document,
        attribute: String,
        namespace: String? = null,
    ): List<List<String>> {
        val paths = document.getElementsByTagName("path")
        return (0 until paths.length).map { index ->
            val path = paths.item(index) as Element
            val value = if (namespace == null) {
                path.getAttribute(attribute)
            } else {
                path.getAttributeNS(namespace, attribute)
            }
            PATH_TOKEN.findAll(value).map { match ->
                match.value.toDoubleOrNull()?.toString() ?: match.value
            }.toList()
        }
    }

    private fun hexColors(value: String): Set<String> =
        HEX_COLOR.findAll(value).map { it.value.uppercase() }.toSet()

    private companion object {
        const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
        val PLACEHOLDER = Regex("""%(?:\d+\$)?[a-zA-Z]""")
        val PATH_TOKEN = Regex("""[a-zA-Z]|[-+]?(?:\d*\.\d+|\d+\.?)(?:[eE][-+]?\d+)?""")
        val HEX_COLOR = Regex("""#[0-9a-fA-F]{6}""")
    }
}
