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

    var body: some View {
        TabView(selection: $tab) {
            SettingsView(settings: $settings)
                .tabItem { Label(settings.language == "Arabic" ? "الإعدادات" : "Settings", systemImage: "gearshape") }
                .tag(0)
            AboutView(language: settings.language)
                .tabItem { Label(settings.language == "Arabic" ? "حول" : "About", systemImage: "info.circle") }
                .tag(1)
        }
        .environment(\.layoutDirection, settings.language == "Arabic" ? .rightToLeft : .leftToRight)
    }
}

struct SettingsSnapshot {
    var defaultAction: String = "Ask"
    var cobaltBaseUrl: String = "https://api.cobalt.tools"
    var cobaltApiKey: String = ""
    var resolveShortLinks: Bool = true
    var deleteCacheAfterShare: Bool = true
    var language: String = "English"
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
        s.language = d.string(forKey: IosSettingsKeys.shared.LANGUAGE) ?? "English"
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
    private let actions = ["Ask", "Clean", "Embed", "Download"]
    private let facade = FukahaIosFacade()

    var body: some View {
        NavigationStack {
            Form {
                Section(settings.language == "Arabic" ? "الإجراء الافتراضي" : "Default action") {
                    Picker("Action", selection: $settings.defaultAction) {
                        ForEach(actions, id: \.self) { Text($0).tag($0) }
                    }
                }
                Section(settings.language == "Arabic" ? "الشبكة" : "Network") {
                    Toggle(settings.language == "Arabic" ? "حل الروابط المختصرة" : "Resolve short links", isOn: $settings.resolveShortLinks)
                    TextField("Cobalt API base URL", text: $settings.cobaltBaseUrl)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                    TextField("Cobalt API key (optional)", text: $settings.cobaltApiKey)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                    Text("Use your own Cobalt instance; the public API is bot-protected.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                    Toggle(settings.language == "Arabic" ? "حذف الذاكرة بعد المشاركة" : "Delete cache after share", isOn: $settings.deleteCacheAfterShare)
                }
                Section(settings.language == "Arabic" ? "المظهر" : "Appearance") {
                    Picker(settings.language == "Arabic" ? "اللغة" : "Language", selection: $settings.language) {
                        Text("English").tag("English")
                        Text("العربية").tag("Arabic")
                    }
                    Picker(settings.language == "Arabic" ? "المظهر" : "Theme", selection: $settings.theme) {
                        Text("System").tag("System")
                        Text("Light").tag("Light")
                        Text("Dark").tag("Dark")
                    }
                }
                Section(settings.language == "Arabic" ? "مفضّلات التضمين" : "Preferred embed fixers") {
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
            .navigationTitle(settings.language == "Arabic" ? "الإعدادات" : "Settings")
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
    var language: String

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text("Fukaha · فكها")
                        .font(.largeTitle.bold())
                    Text(language == "Arabic"
                         ? "فكها ينظّف روابط التواصل من التتبع، ويعيد كتابتها لمضيفات تضمين أفضل، أو ينزّل الوسائط لمشاركتها كملف. افتحه من قائمة المشاركة."
                         : "Fukaha cleans tracking from social links, rewrites them to embed-friendly hosts, or downloads media to re-share as a file. Open it from the system share sheet.")
                    Divider()
                    Text(language == "Arabic"
                         ? "قائمة مصلحي التضمين مبنية على gist عام لـ Lexedia. شكرًا لمؤلفي المشاريع ذات الصلة."
                         : "Embed fixer list based on Lexedia’s public gist. Thanks to the authors of VixBluesky, InstaFix, fxreddit, fxTikTok, BetterTwitFix, and related projects.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                    Text("v0.1.0").font(.caption)
                }
                .padding()
            }
            .navigationTitle(language == "Arabic" ? "حول التطبيق" : "About")
        }
    }
}
