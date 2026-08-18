# Changelog

All notable changes to Fukaha are documented here.

## [Unreleased]

## [0.5.1] — 2026-08-18

### Added
- Locale-switch snapshot cover so changing language does not flash a black window
- Night-mode window surface colors that match the Compose theme
- Settings, embedders, About, and share screenshots for English, Arabic, Japanese, Chinese, and Spanish in light and dark

### Changed
- Restore the paste-a-link field as the first Settings section
- Tests lock the paste-field strings and `shareableLink` parsing
- Android versionName 0.5.1 with versionCode 12

## [0.5.0] — 2026-08-18

### Added
- Japanese, Simplified Chinese, and Spanish on Android and the PWA
- Language menu and theme-cycle controls with motion on both platforms
- About credits, version, tutorial, and update check inline on Settings
- Android brand vector that matches the website icon
- Content-hashed production JS, hashed service-worker cache, and Firebase headers for those bundles
- Tests for locale helpers, Android resource parity, web UI/DOM/settings, and PWA static assets
- Screenshots for the language picker, embedder health, RTL help, and JA/ZH/ES Settings

### Changed
- Web Settings, share sheet, theme, and language UI aligned with Android
- Embed sharing falls back to the cleaned URL when no fixer exists
- Language persistence accepts locale tags and migrates legacy values
- R8 no longer applies blanket serialization and OkHttp keep rules
- Android versionName 0.5.0 with versionCode 11

## [0.4.4] — 2026-08-15

### Changed
- Launcher icon is a broom sweeping a broken chain: wood handle, Hisab/Adati green chain, gold sparkles
- Paste-a-link field stays left-to-right in Arabic so sample, URL, and paste keep English order

## [0.4.3] — 2026-08-15

### Added
- Update dialog can download the GitHub APK and open the system installer instead of only linking to the release page

## [0.4.2] — 2026-08-15

### Fixed
- Release APKs now use a stable signing key so Obtainium can update without a conflict
- Users on 0.4.0 or 0.4.1 must uninstall once, then install 0.4.2; later updates keep the same signature

## [0.4.1] — 2026-08-15

### Fixed
- URL fields and share-sheet previews keep `https://` in order under Arabic RTL
- Opening the Settings test link no longer cancels an in-progress embedder check

## [0.4.0] — 2026-08-15

### Added
- Check GitHub Releases on launch (about once a day) and from About; shows the changelog with Later / Skip this version / View release
- Settings toggle to turn off the launch check (Obtainium / F-Droid users can leave it off)
- Shared tests for link prepare, settings clamps, catalog identity, and cached health-host lists

### Changed
- Parse the bundled embed catalog once and reuse derived platform lookups
- Settings migrations run once per process; unused settings setters and share-sheet strings are gone
- Embedder health state stays on the Settings tab so a probe run does not redraw the whole app
- Drop unused Compose Navigation / ViewModel dependencies
- Split the iOS Settings form so Swift can type-check it on CI

## [0.3.1] — 2026-08-14

### Added
- Paste a link at the top of Settings to run it through Fukaha without sharing in from another app
- First-run onboarding tour, replayable any time from the Help button or About
- Default action can be picked from the last tour page

### Changed
- The Test section is now "Use a link now"; the sample link moved there as a secondary action
- The share sheet hides "Share media file" entirely when no Cobalt URL is set, instead of showing a disabled button
- "Open share screen" only appears once the pasted text contains a usable link
- Share buttons in the share sheet stretch to the full height of their link row

### Fixed
- Sharing a second link while the share sheet was still open showed the previous link

## [0.3.0] — 2026-08-12

### Added
- Embed fixer health checks with caching so broken hosts can be avoided
- About screen credits (GitHub, donate, Lexedia catalog, shenepoy)
- Collapsible Cobalt / media download settings (no public default URL)
- Routine theme option and polished Settings / share sheet UI
- README screenshots (EN/AR)

### Changed
- Media download actions stay visible but disabled until a valid Cobalt `http(s)` URL is set
- Fixer picker sheet shows richer service details
- Relicensed from CC BY-NC-SA 4.0 to AGPL-3.0

## [0.2.0] — 2026-08-12

### Fixed
- Arabic copy treats Fukaha as masculine (`تطبيق`): verb/pronoun agreement (ينفّذ، يزيل، افتحه، إليه)

## [0.1.1] — 2026-08-12

### Added
- Top app bar language menu (System / English / العربية) and one-tap theme cycle
- Embed fixer picker bottom sheet with service details and info link
- Official CC BY-NC-SA 4.0 license text
- Launcher icon matching README logo (adaptive + mipmaps)
- Developer credit (shenepoy / shenepoy.com) in About and READMEs
- App display name follows OS language by default (`AppLanguage.System`)

### Changed
- Arabic UI/docs rewritten to Spacetoon MSA (no dialect)
- Preferred fixer rows show service name, host, and expand affordance

## [0.1.0] — 2026-08-12

### Added
- Android share overlay: clean link, embed rewrite, media download (Cobalt), copy actions
- Settings & About (EN / AR) with Material 3 UI
- Bundled embed-fixer catalog (Lexedia gist)
- Kotlin Multiplatform `shared` module + iOS Share Extension / Settings sources
- CI (tests + debug APK) and Release workflow (tag `v*`)
