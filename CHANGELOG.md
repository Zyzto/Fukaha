# Changelog

All notable changes to Fukaha are documented here.

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
