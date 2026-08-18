package app.fukaha.web

import app.fukaha.AppLanguage
import app.fukaha.ShareAction
import org.w3c.dom.url.URLSearchParams

internal const val SAMPLE_LINK =
    "https://x.com/makkahregion/status/1902619525532512361" +
        "?utm_source=share&utm_medium=android_app&fbclid=IwAR0fukaha_test"

internal data class LanguageOption(
    val language: AppLanguage,
    val flag: String,
    val code: String,
)

internal val LANGUAGE_OPTIONS = listOf(
    LanguageOption(AppLanguage.Arabic, "🇸🇦", "AR"),
    LanguageOption(AppLanguage.English, "🇬🇧", "EN"),
    LanguageOption(AppLanguage.Japanese, "🇯🇵", "JA"),
    LanguageOption(AppLanguage.SimplifiedChinese, "🇨🇳", "ZH"),
    LanguageOption(AppLanguage.Spanish, "🇪🇸", "ES"),
)

internal fun resolveLanguageTags(tags: List<String>): AppLanguage =
    tags.firstNotNullOfOrNull { tag ->
        when {
            tag.matchesLanguagePrefix("ar") -> AppLanguage.Arabic
            tag.matchesLanguagePrefix("ja") -> AppLanguage.Japanese
            tag.matchesLanguagePrefix("zh-CN") ||
                tag.matchesLanguagePrefix("zh-SG") ||
                tag.matchesLanguagePrefix("zh-Hans") ->
                AppLanguage.SimplifiedChinese
            tag.matchesLanguagePrefix("es") -> AppLanguage.Spanish
            tag.matchesLanguagePrefix("en") -> AppLanguage.English
            else -> null
        }
    } ?: AppLanguage.English

private fun String.matchesLanguagePrefix(prefix: String): Boolean =
    equals(prefix, ignoreCase = true) || startsWith("$prefix-", ignoreCase = true)

internal fun immediateHomeTarget(
    action: ShareAction,
    cleanedUrl: String,
    embedUrl: String?,
): String? = when (action) {
    ShareAction.Clean -> cleanedUrl
    ShareAction.Embed -> embedUrl ?: cleanedUrl
    ShareAction.Ask, ShareAction.Download -> null
}

internal fun draftAfterImmediateCopy(
    copied: Boolean,
    currentDraft: String,
    submittedDraft: String,
): String = if (copied && currentDraft == submittedDraft) "" else currentDraft

internal fun shouldShowAndroidInstallNotice(
    canPrompt: Boolean,
    isAndroid: Boolean,
    isIos: Boolean,
): Boolean = !canPrompt && !isIos && isAndroid

internal data class StartupNavigation(
    val sharedText: String?,
    val canonicalUrl: String,
    val shouldReplace: Boolean,
)

/**
 * Captures startup data before replacing an arbitrary document pathname with the PWA root.
 * Share-target fields are removed once consumed so refresh cannot process them twice; unrelated
 * query state and the fragment stay visible on the canonical root URL.
 */
internal fun resolveStartupNavigation(
    pathname: String,
    search: String,
    hash: String,
): StartupNavigation {
    val params = URLSearchParams(search)
    val sharedText = listOfNotNull(params.get("url"), params.get("text"), params.get("title"))
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { null }

    val canonicalSearch = if (sharedText == null) {
        search
    } else {
        params.delete("url")
        params.delete("text")
        params.delete("title")
        params.toString().takeIf { it.isNotBlank() }?.let { "?$it" }.orEmpty()
    }
    val canonicalHash = when {
        hash.isBlank() -> ""
        hash.startsWith("#") -> hash
        else -> "#$hash"
    }

    return StartupNavigation(
        sharedText = sharedText,
        canonicalUrl = "/$canonicalSearch$canonicalHash",
        shouldReplace = pathname != "/" || sharedText != null,
    )
}

/** Serializes theme and language transitions so their shared root snapshot cannot overlap. */
internal class AppearanceTransitionGate {
    var isActive: Boolean = false
        private set

    fun acquire(): Boolean {
        if (isActive) return false
        isActive = true
        return true
    }

    fun release() {
        isActive = false
    }
}
