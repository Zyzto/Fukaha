import UIKit
import UniformTypeIdentifiers
import Shared

class ShareViewController: UIViewController {
    private let facade = FukahaIosFacade()
    private var settings = SettingsSnapshot.load()

    private let stack = UIStackView()
    private let titleLabel = UILabel()
    private let platformLabel = UILabel()
    private let urlLabel = UILabel()
    private let statusLabel = UILabel()
    private let spinner = UIActivityIndicatorView(style: .medium)

    private var preparedClean: String?
    private var preparedEmbed: String?

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .systemBackground
        setupUI()
        loadSharedUrl()
    }

    deinit {
        facade.close()
    }

    private func setupUI() {
        stack.axis = .vertical
        stack.spacing = 12
        stack.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(stack)

        titleLabel.text = "Fukaha · فكها"
        titleLabel.font = .boldSystemFont(ofSize: 22)
        platformLabel.font = .preferredFont(forTextStyle: .headline)
        urlLabel.font = .preferredFont(forTextStyle: .body)
        urlLabel.numberOfLines = 3
        urlLabel.textColor = .secondaryLabel
        statusLabel.font = .preferredFont(forTextStyle: .footnote)
        statusLabel.textColor = .secondaryLabel
        statusLabel.numberOfLines = 2

        let cleanBtn = makeButton("Share cleaned link", #selector(shareClean))
        let embedBtn = makeButton("Share embed link", #selector(shareEmbed))
        let mediaBtn = makeButton("Share media file", #selector(shareMedia))
        let cancelBtn = makeButton("Cancel", #selector(cancel))

        [titleLabel, platformLabel, urlLabel, spinner, statusLabel, cleanBtn, embedBtn, mediaBtn, cancelBtn]
            .forEach { stack.addArrangedSubview($0) }

        NSLayoutConstraint.activate([
            stack.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 20),
            stack.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -20),
            stack.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 20),
        ])
    }

    private func makeButton(_ title: String, _ sel: Selector) -> UIButton {
        let btn = UIButton(type: .system)
        btn.setTitle(title, for: .normal)
        btn.addTarget(self, action: sel, for: .touchUpInside)
        return btn
    }

    private func loadSharedUrl() {
        spinner.startAnimating()
        statusLabel.text = "Preparing…"

        guard let item = extensionContext?.inputItems.first as? NSExtensionItem,
              let provider = item.attachments?.first else {
            statusLabel.text = "No link found"
            spinner.stopAnimating()
            return
        }

        let urlType = UTType.url.identifier
        let plain = UTType.plainText.identifier

        if provider.hasItemConformingToTypeIdentifier(urlType) {
            provider.loadItem(forTypeIdentifier: urlType, options: nil) { item, _ in
                let url = (item as? URL)?.absoluteString
                DispatchQueue.main.async { self.prepare(from: url) }
            }
        } else if provider.hasItemConformingToTypeIdentifier(plain) {
            provider.loadItem(forTypeIdentifier: plain, options: nil) { item, _ in
                DispatchQueue.main.async { self.prepare(from: item as? String) }
            }
        } else {
            statusLabel.text = "No link found"
            spinner.stopAnimating()
        }
    }

    private func prepare(from text: String?) {
        guard let text else {
            statusLabel.text = "No link found"
            spinner.stopAnimating()
            return
        }

        let preferred: String? = {
            if let url = facade.extractUrl(text: text),
               let platform = facade.detectPlatform(url: url) {
                return settings.preferredFixers[platform]
            }
            return nil
        }()

        facade.prepare(
            text: text,
            cobaltBaseUrl: settings.cobaltBaseUrl,
            resolveShortLinks: settings.resolveShortLinks,
            preferredFixerHost: preferred
        ) { clean, embed, platform, error in
            DispatchQueue.main.async {
                self.spinner.stopAnimating()
                if let error {
                    self.statusLabel.text = error
                    return
                }
                self.preparedClean = clean
                self.preparedEmbed = embed
                self.platformLabel.text = "Platform: \(platform ?? "Unknown")"
                self.urlLabel.text = clean
                self.statusLabel.text = embed
                self.runDefaultActionIfNeeded()
            }
        }
    }

    private func runDefaultActionIfNeeded() {
        switch settings.defaultAction {
        case "Clean":
            shareClean()
        case "Embed":
            shareEmbed()
        case "Download":
            shareMedia()
        default:
            break
        }
    }

    @objc private func shareClean() {
        guard let text = preparedClean else { return }
        presentShare([text])
    }

    @objc private func shareEmbed() {
        presentShare([preparedEmbed ?? preparedClean ?? ""])
    }

    @objc private func shareMedia() {
        guard let url = preparedClean else { return }
        spinner.startAnimating()
        statusLabel.text = "Downloading media…"

        let cache = FileManager.default.temporaryDirectory.appendingPathComponent("fukaha", isDirectory: true)
        try? FileManager.default.createDirectory(at: cache, withIntermediateDirectories: true)

        facade.download(
            url: url,
            cobaltBaseUrl: settings.cobaltBaseUrl,
            cobaltApiKey: settings.cobaltApiKey,
            cacheDirPath: cache.path
        ) { path, mime, error in
            DispatchQueue.main.async {
                self.spinner.stopAnimating()
                if let path {
                    let fileUrl = URL(fileURLWithPath: path)
                    self.presentShare([fileUrl]) {
                        if self.settings.deleteCacheAfterShare {
                            try? FileManager.default.removeItem(at: fileUrl)
                        }
                    }
                } else {
                    self.statusLabel.text = error ?? "Download failed — large videos may fail in Share Extension"
                    if let embed = self.preparedEmbed {
                        self.presentShare([embed])
                    }
                }
            }
        }
    }

    private func presentShare(_ items: [Any], onDone: (() -> Void)? = nil) {
        let vc = UIActivityViewController(activityItems: items, applicationActivities: nil)
        vc.completionWithItemsHandler = { _, _, _, _ in
            onDone?()
            self.extensionContext?.completeRequest(returningItems: nil, completionHandler: nil)
        }
        present(vc, animated: true)
    }

    @objc private func cancel() {
        extensionContext?.completeRequest(returningItems: nil, completionHandler: nil)
    }
}

struct SettingsSnapshot {
    var defaultAction: String = "Ask"
    var cobaltBaseUrl: String = "https://api.cobalt.tools"
    var cobaltApiKey: String = ""
    var resolveShortLinks: Bool = true
    var deleteCacheAfterShare: Bool = true
    var preferredFixers: [String: String] = [:]

    static let suite = UserDefaults(suiteName: "group.app.fukaha") ?? .standard

    static func load() -> SettingsSnapshot {
        let d = suite
        var s = SettingsSnapshot()
        s.defaultAction = d.string(forKey: "default_action") ?? "Ask"
        s.cobaltBaseUrl = d.string(forKey: "cobalt_base_url") ?? s.cobaltBaseUrl
        s.cobaltApiKey = d.string(forKey: "cobalt_api_key") ?? ""
        if d.object(forKey: "resolve_short_links") != nil {
            s.resolveShortLinks = d.bool(forKey: "resolve_short_links")
        }
        if d.object(forKey: "delete_cache_after_share") != nil {
            s.deleteCacheAfterShare = d.bool(forKey: "delete_cache_after_share")
        }
        if let raw = d.string(forKey: "preferred_fixers") {
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
}
