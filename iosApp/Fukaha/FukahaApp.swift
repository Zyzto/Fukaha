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
            AboutView(isArabic: isArabic)
                .tabItem { Label(isArabic ? "حول" : "About", systemImage: "info.circle") }
                .tag(1)
        }
        .environment(\.layoutDirection, isArabic ? .rightToLeft : .leftToRight)
    }
}

struct SettingsSnapshot {
    var defaultAction: String = "Ask"
    var cobaltBaseUrl: String = "https://api.cobalt.tools"
    var cobaltApiKey: String = ""
    var resolveShortLinks: Bool = true
    var deleteCacheAfterShare: Bool = true
    var language: String = "System"
    var theme: String = "System"
    var preferredFixers: [String: String] = [:]

    static let suite = UserDefaults(suiteName: IosSettingsKeys.shared.APP_GROUP) ?? .standard

    static func load() -> SettingsSnapshot {
        let d = suite
        var s = SettingsSnapshot()
        s.defaultAction = d.string(forKey: IosSettingsKeys.shared.DEFAULT_ACTION) ?? "Ask"
        s.cobaltBaseUrl = d.string(forKey: IosSettingsKeys.shared.COBALT) ?? s.cobaltBaseUrl
        s.cobaltApiKey = d.string(forKey: IosSettingsKeys.shared.COBALT_API_KEY) ?? ""
        if d.object(forKey: IosSettingsKeys.shared.RESOLVE_SHORT) != nil {
            s.resolveShortLinks = d.bool(forKey: IosSettingsKeys.shared.RESOLVE_SHORT)
        }
        if d.object(forKey: IosSettingsKeys.shared.DELETE_CACHE) != nil {
            s.deleteCacheAfterShare = d.bool(forKey: IosSettingsKeys.shared.DELETE_CACHE)
        }
        s.language = d.string(forKey: IosSettingsKeys.shared.LANGUAGE) ?? "System"
        s.theme = d.string(forKey: IosSettingsKeys.shared.THEME) ?? "System"
        if let raw = d.string(forKey: IosSettingsKeys.shared.PREFERRED_FIXERS) {
            s.preferredFixers = Dictionary(
                uniqueKeysWithValues: raw.split(separator: "\n").compactMap { line -> (String, String)? in
                    let parts = line.split(separator: "\t", maxSplits: 1).map(String.init)
                    guard parts.count == 2 else { return nil }
                    return (parts[0], parts[1])
                }
            )
        }
        return s
    }

    func save() {
        let d = Self.suite
        d.set(defaultAction, forKey: IosSettingsKeys.shared.DEFAULT_ACTION)
        d.set(cobaltBaseUrl, forKey: IosSettingsKeys.shared.COBALT)
        d.set(cobaltApiKey, forKey: IosSettingsKeys.shared.COBALT_API_KEY)
        d.set(resolveShortLinks, forKey: IosSettingsKeys.shared.RESOLVE_SHORT)
        d.set(deleteCacheAfterShare, forKey: IosSettingsKeys.shared.DELETE_CACHE)
        d.set(language, forKey: IosSettingsKeys.shared.LANGUAGE)
        d.set(theme, forKey: IosSettingsKeys.shared.THEME)
        let raw = preferredFixers.map { "\($0.key)\t\($0.value)" }.joined(separator: "\n")
        d.set(raw, forKey: IosSettingsKeys.shared.PREFERRED_FIXERS)
    }
}

struct SettingsView: View {
    @Binding var settings: SettingsSnapshot
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
        [
            ("Ask", isArabic ? "اسأل في كل مرة" : "Ask each time"),
            ("Clean", isArabic ? "رابط نظيف" : "Clean link"),
            ("Embed", isArabic ? "رابط معاينة" : "Embed link"),
            ("Download", isArabic ? "تحميل الوسائط" : "Download media"),
        ]
    }

    var body: some View {
        NavigationStack {
            Form {
                Section(isArabic ? "الإجراء الافتراضي" : "Default action") {
                    Picker(isArabic ? "الإجراء" : "Action", selection: $settings.defaultAction) {
                        ForEach(actionOptions, id: \.id) { option in
                            Text(option.label).tag(option.id)
                        }
                    }
                }
                Section(isArabic ? "الشبكة" : "Network") {
                    Toggle(isArabic ? "تتبّع الروابط المختصرة" : "Resolve short links", isOn: $settings.resolveShortLinks)
                    TextField(isArabic ? "عنوان خادم Cobalt" : "Cobalt API base URL", text: $settings.cobaltBaseUrl)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                    TextField(isArabic ? "مفتاح Cobalt (اختياري)" : "Cobalt API key (optional)", text: $settings.cobaltApiKey)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                    Text(isArabic
                         ? "استخدم خادم Cobalt الخاص بك؛ فالخدمة العامة محمية ضد الروبوتات."
                         : "Use your own Cobalt instance; the public API is bot-protected.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                    Toggle(isArabic ? "حذف الملفات المؤقتة بعد المشاركة" : "Delete cache after share", isOn: $settings.deleteCacheAfterShare)
                }
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
                Section(isArabic ? "خدمات المعاينة المفضّلة" : "Preferred embed fixers") {
                    ForEach(Array(facade.platformKeys()), id: \.self) { key in
                        let services = facade.serviceNames(platformKey: key)
                        if !services.isEmpty {
                            Picker(key.capitalized, selection: fixerBinding(key, fallback: services)) {
                                ForEach(services, id: \.self) { row in
                                    let parts = row.split(separator: "\t", maxSplits: 1).map(String.init)
                                    let name = parts.first ?? row
                                    let host = parts.count > 1 ? parts[1] : row
                                    Text("\(name) (\(host))").tag(host)
                                }
                            }
                        }
                    }
                }
            }
            .navigationTitle(isArabic ? "الإعدادات" : "Settings")
            .onChange(of: settings.defaultAction) { _, _ in settings.save() }
            .onChange(of: settings.cobaltBaseUrl) { _, _ in settings.save() }
            .onChange(of: settings.cobaltApiKey) { _, _ in settings.save() }
            .onChange(of: settings.resolveShortLinks) { _, _ in settings.save() }
            .onChange(of: settings.deleteCacheAfterShare) { _, _ in settings.save() }
            .onChange(of: settings.language) { _, _ in settings.save() }
            .onChange(of: settings.theme) { _, _ in settings.save() }
        }
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

    private var appName: String { isArabic ? "فكها" : "Fukaha" }
    private let siteUrl = URL(string: "https://shenepoy.com")!

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text(appName)
                        .font(.largeTitle.bold())
                    Text(isArabic
                         ? "فكها تزيل التتبع من روابط التواصل الاجتماعي، وتحوّلها إلى مضيفات مناسبة للمعاينة، أو تحمّل الوسائط لمشاركتها كملف. افتحها من قائمة المشاركة في النظام."
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
                         ? "قائمة خدمات المعاينة مأخوذة من قائمة Lexedia العامة. شكراً لمؤلفي VixBluesky وInstaFix وfxreddit وfxTikTok وBetterTwitFix والمشاريع ذات الصلة."
                         : "Embed fixer list based on Lexedia’s public gist. Thanks to the authors of VixBluesky, InstaFix, fxreddit, fxTikTok, BetterTwitFix, and related projects.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                    Text("v0.1.1").font(.caption)
                }
                .padding()
            }
            .navigationTitle(isArabic ? "حول" : "About")
        }
    }
}
