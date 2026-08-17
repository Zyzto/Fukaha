package app.fukaha.web

import app.fukaha.AppLanguage
import kotlinx.browser.window

/**
 * Web copy. Wording is lifted from the Android `values/strings.xml` and `values-ar/strings.xml`
 * so the PWA reads like the app; web-only lines follow the same formal MSA voice in Arabic.
 *
 * Named constructor arguments give compile-time parity between the two locales, which is
 * why this is a class of fields rather than a map.
 */
class Strings(
    val appName: String,
    val settings: String,
    val about: String,
    val back: String,
    val copied: String,
    val copyFailed: String,
    val noLink: String,
    val preparing: String,
    val shareSheetSubtitle: String,
    val unknownPlatform: String,
    val originalPreview: String,
    val cleanedPreview: String,
    val embedPreview: String,
    val actionCopyShort: String,
    val actionShareShort: String,
    val openLink: String,
    val pasteTitle: String,
    val pasteHint: String,
    val pastePlaceholder: String,
    val pasteFromClipboard: String,
    val pasteClear: String,
    val pasteOpen: String,
    val pasteInvalid: String,
    val pasteSample: String,
    val clipboardEmpty: String,
    val clipboardDenied: String,
    val clipboardFailed: String,
    val clipboardInsecure: String,
    val defaultActionHint: String,
    val actionAsk: String,
    val actionAskHint: String,
    val actionClean: String,
    val actionCleanHint: String,
    val actionEmbed: String,
    val actionEmbedHint: String,
    val language: String,
    val languageSystem: String,
    val languageEnglish: String,
    val languageArabic: String,
    val theme: String,
    val themeSystem: String,
    val themeLight: String,
    val themeDark: String,
    val preferredFixers: String,
    val preferredFixerUnknown: String,
    val fixerInUse: String,
    val fixerYourPick: String,
    val fixerCatalogDefault: String,
    val sectionSharing: String,
    val installTitle: String,
    val installBody: String,
    val installAction: String,
    val installedNote: String,
    val iosInstallHint: String,
    val shareSheetUnsupported: String,
    val notInstallableHint: String,
    val openInBrowserTitle: String,
    val openInBrowserBody: String,
    val openInBrowserAction: String,
    val webLimitsTitle: String,
    val shortLinkNote: String,
    val downloadNote: String,
    val androidApp: String,
    val embedHealthTitle: String,
    val embedHealthHint: String,
    val embedHealthRefresh: String,
    val embedHealthNever: String,
    val embedHealthOffline: String,
    val embedHealthBusy: String,
    val embedHealthAlive: String,
    val embedHealthDead: String,
    val embedUnreachable: String,
    val aboutBody: String,
    val githubTitle: String,
    val githubSubtitle: String,
    val donate: String,
    val donateSubtitle: String,
    val creditsTitle: String,
    val credits: String,
    val developerTitle: String,
    val developerSite: String,
    private val fixerByAuthorFormat: String,
    private val fixerPickSubtitleFormat: String,
    private val fixerStandInFormat: String,
    private val embedHealthCountsFormat: String,
    private val embedHealthProgressFormat: String,
    private val embedHealthCooldownFormat: String,
) {
    fun fixerByAuthor(author: String): String = fixerByAuthorFormat.replace("%s", author)

    fun fixerPickSubtitle(platform: String): String = fixerPickSubtitleFormat.replace("%s", platform)

    fun fixerStandIn(picked: String): String = fixerStandInFormat.replace("%name", picked)

    fun embedHealthCounts(alive: Int, dead: Int): String = embedHealthCountsFormat
        .replace("%alive", alive.toString())
        .replace("%dead", dead.toString())

    fun embedHealthProgress(done: Int, total: Int): String = embedHealthProgressFormat
        .replace("%done", done.toString())
        .replace("%total", total.toString())

    fun embedHealthCooldownToast(minutes: Int): String =
        embedHealthCooldownFormat.replace("%min", minutes.toString())

    companion object {
        val EN = Strings(
            appName = "Fukaha",
            settings = "Settings",
            about = "About",
            back = "Back",
            copied = "Copied",
            copyFailed = "Could not copy, select the link instead",
            noLink = "No link found in shared text",
            preparing = "Preparing…",
            shareSheetSubtitle = "Clean it or fix the preview — then share again",
            unknownPlatform = "Unknown",
            originalPreview = "Original link",
            cleanedPreview = "Cleaned link",
            embedPreview = "Embed link",
            actionCopyShort = "Copy",
            actionShareShort = "Share",
            openLink = "Open link",
            pasteTitle = "Paste a link",
            pasteHint = "Clean it or fix the preview, then share it again.",
            pastePlaceholder = "https://x.com/…",
            pasteFromClipboard = "Paste from clipboard",
            pasteClear = "Clear link",
            pasteOpen = "Open share screen",
            pasteInvalid = "That does not look like a link yet",
            pasteSample = "Try a sample link",
            clipboardEmpty = "Your clipboard is empty",
            clipboardDenied = "Your browser blocked clipboard access. Allow it for this site, " +
                "or paste into the field yourself.",
            clipboardFailed = "Could not read the clipboard, paste into the field instead",
            clipboardInsecure = "Browsers block clipboard access on local HTTP addresses. " +
                "Paste into the field yourself.",
            defaultActionHint = "What Fukaha should do when you share a link into it",
            actionAsk = "Ask each time",
            actionAskHint = "Show the options and let you choose",
            actionClean = "Clean link",
            actionCleanHint = "Copy the cleaned URL immediately",
            actionEmbed = "Embed link",
            actionEmbedHint = "Copy the embed-friendly URL immediately",
            language = "Language",
            languageSystem = "System",
            languageEnglish = "English",
            languageArabic = "العربية",
            theme = "Theme",
            themeSystem = "System",
            themeLight = "Light",
            themeDark = "Dark",
            preferredFixers = "Preferred embed fixers",
            preferredFixerUnknown = "No fixer selected",
            fixerInUse = "In use",
            fixerYourPick = "Your pick",
            fixerCatalogDefault = "Default",
            sectionSharing = "Sharing",
            installTitle = "Add Fukaha to your share sheet",
            installBody = "Install the app, then Fukaha shows up whenever you share a link from another app.",
            installAction = "Install",
            installedNote = "Installed. Share a link from any app and pick Fukaha.",
            iosInstallHint = "On iPhone, open the Share menu in Safari and choose Add to Home Screen. " +
                "iOS does not let web apps join the share sheet, so paste links here instead.",
            shareSheetUnsupported = "The share sheet needs Chrome on Android",
            notInstallableHint = "This browser cannot add Fukaha to the share sheet. " +
                "Chrome on Android can, and pasting works everywhere.",
            openInBrowserTitle = "Open Fukaha in your browser",
            openInBrowserBody = "An in-app browser cannot install apps or open the share menu. " +
                "Open Fukaha in Chrome or Safari instead.",
            openInBrowserAction = "Open in browser",
            webLimitsTitle = "Only in the app",
            shortLinkNote = "Short links are not followed here. Browsers block reading other sites' redirects.",
            downloadNote = "Media download needs the Android or iOS app.",
            androidApp = "Android app",
            embedHealthTitle = "Embedder reachability",
            embedHealthHint = "Contacts each fixer host once from your browser to see which still answer. " +
                "Unreachable ones are skipped when rewriting a link.",
            embedHealthRefresh = "Check embedders",
            embedHealthNever = "Not checked yet",
            embedHealthOffline = "Nothing answered, check your connection",
            embedHealthBusy = "A check is already running",
            embedHealthAlive = "Alive",
            embedHealthDead = "Unreachable",
            embedUnreachable = "Embed link · unreachable",
            aboutBody = "Fukaha (فكها) cleans tracking from social links and rewrites them to " +
                "embed-friendly hosts. Install it to clean links straight from the share menu.",
            githubTitle = "GitHub",
            githubSubtitle = "View source and releases",
            donate = "Donate",
            donateSubtitle = "Support the developer on GitHub",
            creditsTitle = "Credits",
            credits = "The embed fixer catalog is assembled from several community collections. " +
                "Thanks to their maintainers and the authors of the listed services.",
            developerTitle = "Developer",
            developerSite = "shenepoy.com",
            fixerByAuthorFormat = "by %s",
            fixerPickSubtitleFormat = "Used when Fukaha rewrites %s links for embeds",
            fixerStandInFormat = "Standing in for %name, which is unreachable",
            embedHealthCountsFormat = "%alive alive · %dead unreachable",
            embedHealthProgressFormat = "Checking %done of %total",
            embedHealthCooldownFormat = "Checked recently, try again in %min min",
        )

        val AR = Strings(
            appName = "فكها",
            settings = "الإعدادات",
            about = "حول",
            back = "رجوع",
            copied = "تم النسخ",
            copyFailed = "تعذّر النسخ، حدّد الرابط ونسخه يدوياً",
            noLink = "عذراً، لم نجد رابطاً في النص المُشارَك",
            preparing = "جاري التجهيز…",
            shareSheetSubtitle = "نظّف الرابط أو جهّز المعاينة — ثم شارك من جديد",
            unknownPlatform = "منصة غير معروفة",
            originalPreview = "الرابط الأصلي",
            cleanedPreview = "الرابط النظيف",
            embedPreview = "رابط المعاينة",
            actionCopyShort = "نسخ",
            actionShareShort = "مشاركة",
            openLink = "افتح الرابط",
            pasteTitle = "الصق رابطاً",
            pasteHint = "نظّفه أو صحّح معاينته، ثم شاركه من جديد.",
            pastePlaceholder = "https://x.com/…",
            pasteFromClipboard = "لصق من الحافظة",
            pasteClear = "إفراغ الحقل",
            pasteOpen = "افتح شاشة المشاركة",
            pasteInvalid = "لا يبدو هذا رابطاً بعد",
            pasteSample = "جرّب رابطاً تجريبياً",
            clipboardEmpty = "الحافظة فارغة",
            clipboardDenied = "منع المتصفح الوصول إلى الحافظة. اسمح به لهذا الموقع، " +
                "أو الصق في الحقل بنفسك.",
            clipboardFailed = "تعذّرت قراءة الحافظة، الصق في الحقل بنفسك",
            clipboardInsecure = "المتصفحات تمنع قراءة الحافظة على عناوين HTTP المحلية. " +
                "الصق في الحقل بنفسك.",
            defaultActionHint = "ما الذي ينفّذه فكها عند مشاركة رابط إليه؟",
            actionAsk = "اسأل في كل مرة",
            actionAskHint = "يعرض الخيارات لتختار بنفسك",
            actionClean = "رابط نظيف",
            actionCleanHint = "ينسخ الرابط النظيف مباشرة",
            actionEmbed = "رابط معاينة",
            actionEmbedHint = "ينسخ رابط المعاينة مباشرة",
            language = "اللغة",
            languageSystem = "حسب النظام",
            languageEnglish = "English",
            languageArabic = "العربية",
            theme = "السمة",
            themeSystem = "حسب النظام",
            themeLight = "فاتح",
            themeDark = "داكن",
            preferredFixers = "خدمات المعاينة المفضّلة",
            preferredFixerUnknown = "لم تُحدَّد خدمة",
            fixerInUse = "قيد الاستخدام",
            fixerYourPick = "اختيارك",
            fixerCatalogDefault = "الافتراضية",
            sectionSharing = "المشاركة",
            installTitle = "أضف فكها إلى قائمة المشاركة",
            installBody = "ثبّت التطبيق ليظهر فكها عند مشاركة أي رابط من تطبيق آخر.",
            installAction = "تثبيت",
            installedNote = "تم التثبيت. شارك رابطاً من أي تطبيق واختر فكها.",
            iosInstallHint = "على iPhone، افتح قائمة المشاركة في Safari واختر «إضافة إلى الشاشة الرئيسية». " +
                "لا يسمح iOS لتطبيقات الويب بالانضمام إلى قائمة المشاركة، فالصق الروابط هنا.",
            shareSheetUnsupported = "تحتاج قائمة المشاركة إلى Chrome على أندرويد",
            notInstallableHint = "هذا المتصفح لا يستطيع إضافة فكها إلى قائمة المشاركة. " +
                "متصفح Chrome على أندرويد يستطيع ذلك، واللصق يعمل في كل مكان.",
            openInBrowserTitle = "افتح فكها في المتصفح",
            openInBrowserBody = "متصفح التطبيقات الداخلي لا يستطيع تثبيت التطبيقات ولا فتح قائمة المشاركة. " +
                "افتح فكها في Chrome أو Safari.",
            openInBrowserAction = "افتح في المتصفح",
            webLimitsTitle = "في التطبيق فقط",
            shortLinkNote = "لا تُتتبَّع الروابط المختصرة هنا، لأن المتصفح يمنع قراءة تحويلات المواقع الأخرى.",
            downloadNote = "يحتاج تحميل الوسائط إلى تطبيق أندرويد أو iOS.",
            androidApp = "تطبيق أندرويد",
            embedHealthTitle = "توفّر خدمات المعاينة",
            embedHealthHint = "يتّصل بكل مضيف مرة واحدة من متصفحك لمعرفة ما يستجيب منها، " +
                "ويتجاوز غير المتاح عند تحويل الرابط.",
            embedHealthRefresh = "فحص خدمات المعاينة",
            embedHealthNever = "لم يُفحص بعد",
            embedHealthOffline = "لم يستجب أي مضيف، تحقّق من اتصالك",
            embedHealthBusy = "هناك فحص جارٍ بالفعل",
            embedHealthAlive = "يعمل",
            embedHealthDead = "غير متاح",
            embedUnreachable = "رابط المعاينة · غير متاح",
            aboutBody = "فكها يزيل التتبع من روابط التواصل الاجتماعي ويحوّلها إلى مضيفات مناسبة للمعاينة. " +
                "ثبّته لتنظيف الروابط من قائمة المشاركة مباشرة.",
            githubTitle = "GitHub",
            githubSubtitle = "عرض المصدر والإصدارات",
            donate = "تبرع",
            donateSubtitle = "ادعم المطوّر عبر GitHub",
            creditsTitle = "شكر وتقدير",
            credits = "قائمة خدمات المعاينة مجمّعة من عدة مجموعات مجتمعية. " +
                "شكراً للقائمين عليها ولمؤلفي الخدمات المدرجة.",
            developerTitle = "المطوّر",
            developerSite = "shenepoy.com",
            fixerByAuthorFormat = "من إعداد %s",
            fixerPickSubtitleFormat = "يستخدمها فكها عند تحويل روابط %s إلى روابط معاينة",
            fixerStandInFormat = "بديلة عن %name لتعذّر الوصول إليه",
            embedHealthCountsFormat = "%alive يعمل · %dead غير متاح",
            embedHealthProgressFormat = "جاري الفحص %done من %total",
            embedHealthCooldownFormat = "تم الفحص قبل قليل، أعد المحاولة بعد %min د",
        )

        fun forLanguage(language: AppLanguage): Strings = when (language) {
            AppLanguage.English -> EN
            AppLanguage.Arabic -> AR
            AppLanguage.System -> if (systemPrefersArabic()) AR else EN
        }

        fun isArabic(language: AppLanguage): Boolean = when (language) {
            AppLanguage.English -> false
            AppLanguage.Arabic -> true
            AppLanguage.System -> systemPrefersArabic()
        }

        private fun systemPrefersArabic(): Boolean =
            window.navigator.language.startsWith("ar", ignoreCase = true) ||
                window.navigator.languages.any { it.startsWith("ar", ignoreCase = true) }
    }
}
