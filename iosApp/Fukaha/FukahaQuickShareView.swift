import Foundation
import SwiftUI
import UIKit
import Shared

/**
 * Runs the settings quick-link field through the same processed share UI as an
 * external share. Quick use deliberately forces Ask, matching Android, so the
 * pasted link is cleaned before the user chooses what to share.
 */
struct FukahaQuickShareView: View {
    let text: String
    let settings: SettingsSnapshot
    let facade: FukahaIosFacade
    let onDismiss: () -> Void

    @StateObject private var model = FukahaShareModel()
    @State private var didStartPreparing = false
    @State private var pendingShare: FukahaPendingShare?

    private var isArabic: Bool {
        switch settings.language {
        case "Arabic": return true
        case "English", "Japanese", "SimplifiedChinese", "Spanish": return false
        default:
            return Locale.current.language.languageCode?.identifier == "ar"
        }
    }

    var body: some View {
        FukahaShareScreen(
            model: model,
            isArabic: isArabic,
            mediaDownloadEnabled: settings.hasValidCobaltBaseUrl,
            onDismiss: onDismiss,
            onShareCleaned: { queueTextShare(model.cleanedUrl) },
            onShareEmbed: { queueTextShare(model.embedUrl ?? model.cleanedUrl) },
            onShareMedia: shareMedia,
            onCopyOriginal: { copyText(model.originalUrl) },
            onCopyCleaned: { copyText(model.cleanedUrl) },
            onCopyEmbed: { copyText(model.embedUrl) },
        )
        .task { prepare() }
        .sheet(item: $pendingShare) { pending in
            FukahaActivityView(items: pending.items) { completed in
                pending.cleanup?()
                pendingShare = nil
                if completed {
                    onDismiss()
                }
            }
        }
    }

    private func prepare() {
        guard !didStartPreparing else { return }
        didStartPreparing = true

        let originalUrl = facade.extractUrl(text: text) ?? text
        let preferred: String? = {
            guard let url = facade.extractUrl(text: text),
                  let platform = facade.detectPlatform(url: url) else {
                return nil
            }
            return settings.preferredFixers[platform]
        }()

        facade.prepare(
            text: text,
            cobaltBaseUrl: settings.cobaltBaseUrl,
            resolveShortLinks: settings.resolveShortLinks,
            preferredFixerHost: preferred,
        ) { clean, embed, platform, error in
            DispatchQueue.main.async {
                model.loading = false
                if let error {
                    model.error = localizedError(error)
                    return
                }
                guard let clean else {
                    model.error = textFor(en: "Could not prepare the link", ar: "تعذّر تجهيز الرابط")
                    return
                }
                model.originalUrl = originalUrl
                model.cleanedUrl = clean
                model.embedUrl = embed
                model.platform = platformDisplayName(platform)
            }
        }
    }

    private func queueTextShare(_ value: String?) {
        guard let value, !value.isEmpty else { return }
        pendingShare = FukahaPendingShare(items: [value])
    }

    private func shareMedia() {
        guard let url = model.cleanedUrl else { return }
        model.downloading = true
        model.error = nil

        let cache = FileManager.default.temporaryDirectory.appendingPathComponent("fukaha", isDirectory: true)
        try? FileManager.default.createDirectory(at: cache, withIntermediateDirectories: true)

        facade.download(
            url: url,
            cobaltBaseUrl: settings.cobaltBaseUrl,
            cobaltApiKey: settings.cobaltApiKey,
            cacheDirPath: cache.path,
        ) { path, _, error in
            DispatchQueue.main.async {
                model.downloading = false
                guard let path else {
                    model.error = localizedError(error ?? textFor(
                        en: "Download failed — large videos may fail in Share Extension",
                        ar: "عذراً، تعذّر تحميل الملف. حاول مجدداً",
                    ))
                    return
                }

                let fileUrl = URL(fileURLWithPath: path)
                pendingShare = FukahaPendingShare(
                    items: [fileUrl],
                    cleanup: settings.deleteCacheAfterShare
                        ? { try? FileManager.default.removeItem(at: fileUrl) }
                        : nil,
                )
            }
        }
    }

    private func copyText(_ value: String?) {
        guard let value else { return }
        UIPasteboard.general.string = value
        model.copiedMessage = textFor(en: "Copied", ar: "تم النسخ")
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.4) {
            guard model.copiedMessage != nil else { return }
            model.copiedMessage = nil
        }
    }

    private func localizedError(_ raw: String) -> String {
        switch raw {
        case "No link found":
            return textFor(en: "No link found", ar: "عذراً، لم نجد رابطاً في النص المُشارَك")
        case "Download failed", "cobalt.base_url.missing":
            return textFor(
                en: "Download failed — large videos may fail in Share Extension",
                ar: "عذراً، تعذّر تحميل الملف. حاول مجدداً",
            )
        default:
            return raw
        }
    }

    private func textFor(en: String, ar: String) -> String {
        isArabic ? ar : en
    }

    private func platformDisplayName(_ platform: String?) -> String? {
        guard let platform, !platform.isEmpty else { return nil }
        switch platform.lowercased() {
        case "x": return "X (Twitter)"
        case "youtube": return "YouTube"
        case "tiktok": return "TikTok"
        case "instagram": return "Instagram"
        case "facebook": return "Facebook"
        case "reddit": return "Reddit"
        case "deviantart": return "DeviantArt"
        case "bilibili": return "Bilibili"
        default: return platform.capitalized
        }
    }
}

private struct FukahaPendingShare: Identifiable {
    let id = UUID()
    let items: [Any]
    let cleanup: (() -> Void)?

    init(items: [Any], cleanup: (() -> Void)? = nil) {
        self.items = items
        self.cleanup = cleanup
    }
}

private struct FukahaActivityView: UIViewControllerRepresentable {
    let items: [Any]
    let onComplete: (Bool) -> Void

    func makeUIViewController(context: Context) -> UIActivityViewController {
        let controller = UIActivityViewController(
            activityItems: items,
            applicationActivities: nil,
        )
        controller.completionWithItemsHandler = { _, completed, _, _ in
            DispatchQueue.main.async {
                onComplete(completed)
            }
        }
        return controller
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
