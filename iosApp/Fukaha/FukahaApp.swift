//
// Fukaha iOS container app — Settings + About
//

import SwiftUI
import Shared

@main
struct FukahaApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

struct ContentView: View {
    @State private var tab = 0
    @State private var settings = SettingsSnapshot.load()
    @State private var pendingUpdate: PendingAppUpdate?
    @State private var updateChecking = false
    @State private var updateMessage: String?
    private let facade = FukahaIosFacade()

    private var isArabic: Bool {
        switch settings.language {
        case "Arabic": return true
        case "English": return false
        default:
            return Locale.current.language.languageCode?.identifier == "ar"
        }
    }

    var body: some View {
        TabView(selection: $tab) {
            SettingsView(settings: $settings)
                .tabItem { Label(isArabic ? "الإعدادات" : "Settings", systemImage: "gearshape") }
                .tag(0)
            AboutView(
                isArabic: isArabic,
                updateChecking: updateChecking,
                onCheckUpdates: { runUpdateCheck(manual: true) },
            )
                .tabItem { Label(isArabic ? "حول" : "About", systemImage: "info.circle") }
                .tag(1)
        }
        .environment(\.layoutDirection, isArabic ? .rightToLeft : .leftToRight)
        .task { runUpdateCheck(manual: false) }
        .sheet(item: $pendingUpdate) { update in
            UpdateAvailableView(
                update: update,
                isArabic: isArabic,
                onSkip: {
                    settings.skippedUpdateVersion = update.version
                    settings.save()
                    pendingUpdate = nil
                },
                onDismiss: { pendingUpdate = nil },
            )
        }
        .alert(updateMessage ?? "", isPresented: Binding(
            get: { updateMessage != nil },
            set: { if !$0 { updateMessage = nil } },
        )) {
            Button(isArabic ? "حسناً" : "OK", role: .cancel) { updateMessage = nil }
        }
    }

    private func currentVersion() -> String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "0.0.0"
    }

    private func runUpdateCheck(manual: Bool) {
        if updateChecking { return }
        if !manual {
            guard settings.checkUpdatesOnLaunch else { return }
            let now = Int64(Date().timeIntervalSince1970 * 1000)
            let interval: Int64 = 24 * 60 * 60 * 1000
            if settings.lastUpdateCheckEpochMs > 0 && now - settings.lastUpdateCheckEpochMs < interval {
                return
            }
        }
        updateChecking = true
        facade.checkForUpdate(currentVersion: currentVersion()) { status, version, changelog, htmlUrl, _ in
            DispatchQueue.main.async {
                settings.lastUpdateCheckEpochMs = Int64(Date().timeIntervalSince1970 * 1000)
                settings.save()
                updateChecking = false
                switch status {
                case "available":
                    if manual || version != settings.skippedUpdateVersion {
                        pendingUpdate = PendingAppUpdate(
                            version: version,
                            changelog: changelog,
                            htmlUrl: htmlUrl,
                        )
                    }
                case "up_to_date":
                    if manual {
                        updateMessage = isArabic ? "لديك أحدث إصدار" : "You are on the latest version"
                    }
                default:
                    if manual {
                        updateMessage = isArabic ? "تعذّر البحث عن تحديثات" : "Could not check for updates"
                    }
                }
            }
        }
    }
}

struct PendingAppUpdate: Identifiable {
    let version: String
    let changelog: String
    let htmlUrl: String
    var id: String { version }
}

struct UpdateAvailableView: View {
    let update: PendingAppUpdate
    let isArabic: Bool
    let onSkip: () -> Void
    let onDismiss: () -> Void

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    Text("v\(update.version)")
                        .font(.headline)
                    Text(update.changelog.isEmpty
                         ? (isArabic ? "لا توجد ملاحظات لهذا الإصدار." : "No release notes for this version.")
                         : update.changelog)
                        .font(.body)
                        .foregroundStyle(.secondary)
                    if let url = URL(string: update.htmlUrl) {
                        Link(isArabic ? "عرض الإصدار" : "View release", destination: url)
                    }
                    Button(isArabic ? "تخطَّ هذا الإصدار" : "Skip this version", action: onSkip)
                        .padding(.top, 8)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding()
            }
            .navigationTitle(isArabic ? "يتوفر تحديث" : "Update available")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(isArabic ? "لاحقاً" : "Later", action: onDismiss)
                }
            }
        }
    }
}

struct SettingsSnapshot {
    var defaultAction: String = "Ask"
    var cobaltBaseUrl: String = ""
    var cobaltApiKey: String = ""
    var resolveShortLinks: Bool = true
    var deleteCacheAfterShare: Bool = true
    var language: String = "System"
    var theme: String = "System"
    var preferredFixers: [String: String] = [:]
    var checkUpdatesOnLaunch: Bool = true
    var skippedUpdateVersion: String = ""
    var lastUpdateCheckEpochMs: Int64 = 0

    static let suite = UserDefaults(suiteName: IosSettingsKeys.shared.APP_GROUP) ?? .standard
    private static let legacyPublicCobalt = "https://api.cobalt.tools"
    private static let cobaltPublicClearedKey = "cobalt_public_default_cleared"

    var hasValidCobaltBaseUrl: Bool {
        let trimmed = cobaltBaseUrl.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return false }
        let lower = trimmed.lowercased()
        return lower.hasPrefix("http://") || lower.hasPrefix("https://")
    }

    static func load() -> SettingsSnapshot {
        let d = suite
        migrateCobaltPublicDefaultIfNeeded(d)
        var s = SettingsSnapshot()
        s.defaultAction = d.string(forKey: IosSettingsKeys.shared.DEFAULT_ACTION) ?? "Ask"
        s.cobaltBaseUrl = d.string(forKey: IosSettingsKeys.shared.COBALT) ?? ""
        s.cobaltApiKey = d.string(forKey: IosSettingsKeys.shared.COBALT_API_KEY) ?? ""
        if d.object(forKey: IosSettingsKeys.shared.RESOLVE_SHORT) != nil {
            s.resolveShortLinks = d.bool(forKey: IosSettingsKeys.shared.RESOLVE_SHORT)
        }
        if d.object(forKey: IosSettingsKeys.shared.DELETE_CACHE) != nil {
            s.deleteCacheAfterShare = d.bool(forKey: IosSettingsKeys.shared.DELETE_CACHE)
        }
        s.language = d.string(forKey: IosSettingsKeys.shared.LANGUAGE) ?? "System"
        s.theme = d.string(forKey: IosSettingsKeys.shared.THEME) ?? "System"
        if d.object(forKey: IosSettingsKeys.shared.CHECK_UPDATES) != nil {
            s.checkUpdatesOnLaunch = d.bool(forKey: IosSettingsKeys.shared.CHECK_UPDATES)
        }
        s.skippedUpdateVersion = d.string(forKey: IosSettingsKeys.shared.SKIPPED_UPDATE) ?? ""
        if let stored = d.object(forKey: IosSettingsKeys.shared.LAST_UPDATE_CHECK) as? NSNumber {
            s.lastUpdateCheckEpochMs = stored.int64Value
        }
        if let raw = d.string(forKey: IosSettingsKeys.shared.PREFERRED_FIXERS) {
            s.preferredFixers = Dictionary(
                uniqueKeysWithValues: raw.split(separator: "\n").compactMap { line -> (String, String)? in
                    let parts = line.split(separator: "\t", maxSplits: 1).map(String.init)
                    guard parts.count == 2 else { return nil }
                    return (parts[0], parts[1])
                }
            )
        }
        if s.defaultAction == "Download" && !s.hasValidCobaltBaseUrl {
            s.defaultAction = "Ask"
        }
        return s
    }

    private static func migrateCobaltPublicDefaultIfNeeded(_ d: UserDefaults) {
        if d.bool(forKey: cobaltPublicClearedKey) { return }
        let stored = d.string(forKey: IosSettingsKeys.shared.COBALT)
        let normalized = stored?.trimmingCharacters(in: .whitespacesAndNewlines)
            .trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        if stored == nil || normalized?.caseInsensitiveCompare(legacyPublicCobalt) == .orderedSame {
            d.set("", forKey: IosSettingsKeys.shared.COBALT)
        }
        let url = d.string(forKey: IosSettingsKeys.shared.COBALT) ?? ""
        let valid = {
            let trimmed = url.trimmingCharacters(in: .whitespacesAndNewlines)
            let lower = trimmed.lowercased()
            return !trimmed.isEmpty && (lower.hasPrefix("http://") || lower.hasPrefix("https://"))
        }()
        if d.string(forKey: IosSettingsKeys.shared.DEFAULT_ACTION) == "Download" && !valid {
            d.set("Ask", forKey: IosSettingsKeys.shared.DEFAULT_ACTION)
        }
        d.set(true, forKey: cobaltPublicClearedKey)
    }

    func save() {
        let d = Self.suite
        var action = defaultAction
        if action == "Download" && !hasValidCobaltBaseUrl {
            action = "Ask"
        }
        d.set(action, forKey: IosSettingsKeys.shared.DEFAULT_ACTION)
        d.set(cobaltBaseUrl, forKey: IosSettingsKeys.shared.COBALT)
        d.set(cobaltApiKey, forKey: IosSettingsKeys.shared.COBALT_API_KEY)
        d.set(resolveShortLinks, forKey: IosSettingsKeys.shared.RESOLVE_SHORT)
        d.set(deleteCacheAfterShare, forKey: IosSettingsKeys.shared.DELETE_CACHE)
        d.set(language, forKey: IosSettingsKeys.shared.LANGUAGE)
        d.set(theme, forKey: IosSettingsKeys.shared.THEME)
        d.set(checkUpdatesOnLaunch, forKey: IosSettingsKeys.shared.CHECK_UPDATES)
        d.set(skippedUpdateVersion, forKey: IosSettingsKeys.shared.SKIPPED_UPDATE)
        d.set(NSNumber(value: lastUpdateCheckEpochMs), forKey: IosSettingsKeys.shared.LAST_UPDATE_CHECK)
        let raw = preferredFixers.map { "\($0.key)\t\($0.value)" }.joined(separator: "\n")
        d.set(raw, forKey: IosSettingsKeys.shared.PREFERRED_FIXERS)
    }
}

struct SettingsView: View {
    @Binding var settings: SettingsSnapshot
    @State private var cobaltExpanded = false
    private let facade = FukahaIosFacade()
    private var isArabic: Bool {
        switch settings.language {
        case "Arabic": return true
        case "English": return false
        default:
            return Locale.current.language.languageCode?.identifier == "ar"
        }
    }

    private var actionOptions: [(id: String, label: String)] {
        var options = [
            ("Ask", isArabic ? "اسأل في كل مرة" : "Ask each time"),
            ("Clean", isArabic ? "رابط نظيف" : "Clean link"),
            ("Embed", isArabic ? "رابط معاينة" : "Embed link"),
        ]
        if settings.hasValidCobaltBaseUrl {
            options.append(("Download", isArabic ? "تحميل الوسائط" : "Download media"))
        }
        return options
    }

    var body: some View {
        NavigationStack {
            Form {
                defaultActionSection
                preferredFixersSection
                networkSection
                updatesSection
                cobaltSection
                appearanceSection
            }
            .navigationTitle(isArabic ? "الإعدادات" : "Settings")
            .onChange(of: settings.defaultAction) { _ in settings.save() }
            .onChange(of: settings.cobaltBaseUrl) { _ in persistCobaltUrl() }
            .onChange(of: settings.cobaltApiKey) { _ in settings.save() }
            .onChange(of: settings.resolveShortLinks) { _ in settings.save() }
            .onChange(of: settings.deleteCacheAfterShare) { _ in settings.save() }
            .onChange(of: settings.language) { _ in settings.save() }
            .onChange(of: settings.theme) { _ in settings.save() }
            .onChange(of: settings.checkUpdatesOnLaunch) { _ in settings.save() }
        }
    }

    private var defaultActionSection: some View {
        Section(isArabic ? "الإجراء الافتراضي" : "Default action") {
            Picker(isArabic ? "الإجراء" : "Action", selection: $settings.defaultAction) {
                ForEach(actionOptions, id: \.id) { option in
                    Text(option.label).tag(option.id)
                }
            }
            if !settings.hasValidCobaltBaseUrl {
                Text(isArabic ? "تحميل الوسائط" : "Download media")
                    .foregroundStyle(.tertiary)
                Text(isArabic
                     ? "عيّن عنوان Cobalt في تحميل الوسائط أدناه."
                     : "Set your Cobalt URL in Media download below.")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
        }
    }

    private var preferredFixersSection: some View {
        Section(isArabic ? "خدمات المعاينة المفضّلة" : "Preferred embed fixers") {
            ForEach(Array(facade.platformKeys()), id: \.self) { key in
                fixerPicker(for: key)
            }
        }
    }

    @ViewBuilder
    private func fixerPicker(for key: String) -> some View {
        let services = facade.serviceNames(platformKey: key)
        if !services.isEmpty {
            Picker(key.capitalized, selection: fixerBinding(key, fallback: services)) {
                ForEach(services, id: \.self) { row in
                    fixerLabel(row)
                }
            }
        }
    }

    private func fixerLabel(_ row: String) -> some View {
        let parts = row.split(separator: "\t", maxSplits: 1).map(String.init)
        let name = parts.first ?? row
        let host = parts.count > 1 ? parts[1] : row
        return Text("\(name) (\(host))").tag(host)
    }

    private var networkSection: some View {
        Section(isArabic ? "الشبكة" : "Network") {
            Toggle(isArabic ? "تتبّع الروابط المختصرة" : "Resolve short links", isOn: $settings.resolveShortLinks)
            Toggle(isArabic ? "حذف الملفات المؤقتة بعد المشاركة" : "Delete cache after share", isOn: $settings.deleteCacheAfterShare)
        }
    }

    private var updatesSection: some View {
        Section(isArabic ? "التحديثات" : "Updates") {
            Toggle(isArabic ? "البحث عن تحديثات عند الفتح" : "Check for updates on launch", isOn: $settings.checkUpdatesOnLaunch)
            Text(isArabic
                 ? "يفحص إصدارات GitHub نحو مرة في اليوم. يمكنك الفحص أيضاً من صفحة حول."
                 : "Looks at GitHub Releases about once a day. You can also check from About.")
                .font(.footnote)
                .foregroundStyle(.secondary)
        }
    }

    private var cobaltSection: some View {
        Section {
            DisclosureGroup(isExpanded: $cobaltExpanded) {
                Text(isArabic
                     ? "يحتاج تحميل الوسائط إلى عنوان خادم Cobalt تستضيفه بنفسك (ومفتاح API إن طلبه خادمك). واجهة cobalt.tools العامة لا تعمل مع هذا التطبيق."
                     : "Media download needs your own self-hosted Cobalt instance URL (and API key if your instance requires one). The public cobalt.tools API will not work with this app.")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                TextField(isArabic ? "عنوان خادم Cobalt" : "Cobalt instance URL", text: $settings.cobaltBaseUrl)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                TextField(isArabic ? "مفتاح Cobalt (اختياري)" : "Cobalt API key (optional)", text: $settings.cobaltApiKey)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
            } label: {
                Text(isArabic ? "تحميل الوسائط" : "Media download")
            }
        }
    }

    private var appearanceSection: some View {
        Section(isArabic ? "المظهر" : "Appearance") {
            Picker(isArabic ? "اللغة" : "Language", selection: $settings.language) {
                Text(isArabic ? "حسب النظام" : "System").tag("System")
                Text("English").tag("English")
                Text("العربية").tag("Arabic")
            }
            Picker(isArabic ? "السمة" : "Theme", selection: $settings.theme) {
                Text(isArabic ? "حسب النظام" : "System").tag("System")
                Text(isArabic ? "فاتح" : "Light").tag("Light")
                Text(isArabic ? "داكن" : "Dark").tag("Dark")
            }
        }
    }

    private func persistCobaltUrl() {
        if settings.defaultAction == "Download" && !settings.hasValidCobaltBaseUrl {
            settings.defaultAction = "Ask"
        }
        settings.save()
    }

    private func fixerBinding(_ key: String, fallback: [String]) -> Binding<String> {
        Binding(
            get: {
                if let existing = settings.preferredFixers[key] { return existing }
                if let def = facade.defaultFixer(platformKey: key) { return def }
                let first = fallback.first?.split(separator: "\t").last.map(String.init)
                return first ?? ""
            },
            set: { newValue in
                settings.preferredFixers[key] = newValue
                settings.save()
            }
        )
    }
}

struct AboutView: View {
    var isArabic: Bool
    var updateChecking: Bool = false
    var onCheckUpdates: () -> Void = {}

    private var appName: String { isArabic ? "فكها" : "Fukaha" }
    private let siteUrl = URL(string: "https://shenepoy.com")!
    private static let creditSources: [(englishTitle: String, arabicTitle: String, url: String)] = [
        ("Lexedia’s embed fixer list", "قائمة Lexedia لخدمات المعاينة", "https://gist.github.com/Lexedia/bbbde4dbbf628b0bfe8476a96a977a8f"),
        ("FixTweetBot fixer list", "قائمة FixTweetBot", "https://github.com/Kyrela/FixTweetBot#awesome-fixers"),
        ("mohsreg’s Discord embed list", "قائمة mohsreg لمعاينات ديسكورد", "https://gist.github.com/mohsreg/927bf8b2092515ee1a8ee88c3e4d2c14"),
        ("meqativ’s embed fixer list", "قائمة meqativ لخدمات المعاينة", "https://gist.github.com/meqativ/ea15d319f7889a02c893605c62f148c2"),
        ("Postrediori’s embed list", "قائمة Postrediori", "https://gist.github.com/Postrediori/cc52b0ca054179a91aab2e63582265b6"),
        ("EmbedFixer plugin", "إضافة EmbedFixer", "https://github.com/k33bs/EmbedFixer"),
    ]

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text(appName)
                        .font(.largeTitle.bold())
                    Text(isArabic
                         ? "فكها يزيل التتبع من روابط التواصل الاجتماعي، ويحوّلها إلى مضيفات مناسبة للمعاينة، أو يحمّل الوسائط لمشاركتها كملف. افتحه من قائمة المشاركة في النظام."
                         : "Fukaha cleans tracking from social links, rewrites them to embed-friendly hosts, or downloads media to re-share as a file. Open it from the system share sheet.")
                    Divider()
                    Link(destination: siteUrl) {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(isArabic ? "المطوّر" : "Developer")
                                .font(.headline)
                            Text("shenepoy")
                            Text("shenepoy.com")
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                        }
                    }
                    Divider()
                    Text(isArabic
                         ? "القائمة مجمّعة من عدة مجموعات مجتمعية. شكراً للقائمين عليها ولمؤلفي الخدمات المدرجة."
                         : "The catalog is assembled from several community collections. Thanks to their maintainers and the authors of the listed services.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                    ForEach(Self.creditSources, id: \.url) { source in
                        Link(destination: URL(string: source.url)!) {
                            Text(isArabic ? source.arabicTitle : source.englishTitle)
                                .font(.footnote)
                        }
                    }
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(isArabic ? "الإصدار" : "Version")
                                .font(.headline)
                            Text(updateChecking
                                 ? (isArabic ? "جاري فحص إصدارات GitHub…" : "Checking GitHub Releases…")
                                 : "v\(Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "0.4.0")")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
                        Button(action: onCheckUpdates) {
                            if updateChecking {
                                ProgressView()
                            } else {
                                Image(systemName: "arrow.clockwise")
                            }
                        }
                        .disabled(updateChecking)
                        .accessibilityLabel(isArabic ? "البحث عن تحديثات" : "Check for updates")
                    }
                }
                .padding()
            }
            .navigationTitle(isArabic ? "حول" : "About")
        }
    }
}
