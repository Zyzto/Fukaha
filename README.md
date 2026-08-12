<!-- markdownlint-disable MD033 MD060 -->

<p align="center">
  <img src="assets/fukaha-logo.svg" alt="Fukaha" width="160" />
</p>

<h1 align="center">Fukaha - فكها</h1>

<p align="center">
  <strong>Untangle social links. Share them clean.</strong><br/>
  From the system share sheet — strip tracking, rewrite to embed-friendly hosts,<br/>
  or download media and share the file. Kotlin Multiplatform · Android (+ iOS WIP).
</p>

<p align="center">
  <a href="https://github.com/Zyzto/Fukaha/releases/latest"><img alt="release" src="https://img.shields.io/github/v/release/Zyzto/Fukaha?style=flat-square&color=00687A" /></a>
  <a href="https://github.com/Zyzto/Fukaha"><img alt="repo" src="https://img.shields.io/badge/github-Zyzto%2FFukaha-C0C0C0?style=flat-square" /></a>
  <a href="https://apps.obtainium.imranr.dev/redirect.html?r=obtainium://add/https://github.com/Zyzto/Fukaha/releases"><img alt="Obtainium" src="https://img.shields.io/badge/Obtainium-add-00687A?style=flat-square&logo=android&logoColor=white" /></a>
  <img alt="kotlin" src="https://img.shields.io/badge/Kotlin-Multiplatform-C0C0C0?style=flat-square&logo=kotlin&logoColor=white" />
  <img alt="android" src="https://img.shields.io/badge/Android-26%2B-00687A?style=flat-square&logo=android&logoColor=white" />
  <img alt="license" src="https://img.shields.io/badge/license-CC%20BY--NC--SA%204.0-00687A?style=flat-square" />
</p>

<p align="center">
  <a href="https://github.com/Zyzto/Fukaha/releases/latest"><strong>Latest release</strong></a>
  ·
  <a href="#what-you-get">What you get</a>
  ·
  <a href="#install">Install</a>
  ·
  <a href="#develop">Develop</a>
  ·
  <a href="README.ar.md">العربية</a>
</p>

<p align="center">
  The name <strong>Fukaha</strong> comes from Arabic
  <span dir="rtl"><strong>فكها</strong></span>
  (<em>fakkahā</em>): untangle / unwrap it —
  pulling tracking and clutter off a shared link.
</p>

---

## What you get

| | |
|---|---|
| **Share sheet** | Appears in Android’s system share menu for text/links — overlay sheet, not a full browser app. |
| **Clean link** | Strips `utm_*`, `fbclid`, `igshid`, and similar noise; normalizes hosts. |
| **Embed link** | Rewrites to fixer hosts (vxTwitter, ddinstagram, fxTikTok, …) from [Lexedia’s list](https://gist.github.com/Lexedia/bbbde4dbbf628b0bfe8476a96a977a8f). |
| **Media file** | Optional download via a Cobalt-compatible API, then re-share as a file. |
| **Settings** | Default action, per-network fixer, Cobalt URL/key, short-link resolve, EN/AR, theme. |
| **Lite** | No background sync, no accounts — network only when you share. |

**Default actions**

| Action | Behaviour |
|--------|-----------|
| **Ask each time** | Show the sheet (clean / embed / download / copy). |
| **Clean / Embed / Download** | Run immediately, then open the system share chooser. |

---

## Install

### Android

| Option | |
|--------|--|
| **Obtainium** (recommended) | [![Obtainium](https://img.shields.io/badge/Obtainium-add-00687A?style=flat-square&logo=android&logoColor=white)](https://apps.obtainium.imranr.dev/redirect.html?r=obtainium://add/https://github.com/Zyzto/Fukaha/releases) — tracks [GitHub Releases](https://github.com/Zyzto/Fukaha/releases) |
| **APK** | Download from [latest release](https://github.com/Zyzto/Fukaha/releases/latest) |

### iOS

Share Extension + Settings sources live under `iosApp/` and `iosShareExtension/`. Wire the KMP `Shared` framework in Xcode — see [iosApp/README.md](iosApp/README.md). Not shipped as a store build yet.

---

## Develop

**Requirements:** JDK 17 · Android SDK 35 · Android device/emulator (minSdk 26)

```bash
export ANDROID_HOME=~/Android/Sdk   # or your SDK path
./gradlew :shared:testDebugUnitTest
./gradlew :composeApp:assembleDebug
adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

Open **Fukaha** for Settings / About, or share any social URL into it from another app.

**Modules**

| Module | Role |
|--------|------|
| `shared` | URL clean, embed catalog, Cobalt download, settings models (Android + iOS) |
| `composeApp` | Android UI — share sheet + Settings/About |
| `iosApp` / `iosShareExtension` | SwiftUI Settings + Share Extension (manual Xcode setup) |

**Media download:** set your own Cobalt instance URL (and API key if required) in Settings. The public `api.cobalt.tools` endpoint is bot-protected and not intended for third-party apps.

---

## Architecture (short)

- **Shared core (KMP)** — parse URL → clean → embed rewrite → optional Cobalt download  
- **Android** — translucent `ShareActivity` + Material 3 Compose Settings  
- **iOS** — Share Extension calling `FukahaIosFacade` + App Group settings  

```text
Share menu → Fukaha → clean | embed | file → system share again
```

---

## CI / CD

| Workflow | Trigger | What it does |
|----------|---------|----------------|
| [CI](.github/workflows/ci.yml) | push / PR to `main` | Shared unit tests + debug APK assemble |
| [Release](.github/workflows/release.yml) | tag `v*` / manual | Release APK artifact + GitHub Release |

Tag a release:

```bash
git tag v0.1.1
git push origin v0.1.1
```

---

## Contributing & secrets

This repository is **public**. Never commit keystores, API keys, or `local.properties`. Use Settings in-app for Cobalt credentials.

---

## License

[CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/) — share and adapt with attribution, **non-commercial** only, same license for derivatives.  
Full text: [LICENSE](LICENSE).

Embed fixer catalog credit: [Lexedia’s gist](https://gist.github.com/Lexedia/bbbde4dbbf628b0bfe8476a96a977a8f) and the authors of the listed services.

---

<p align="center">
  Made by <a href="https://shenepoy.com"><strong>shenepoy</strong></a>
  ·
  <a href="https://github.com/Zyzto">GitHub</a>
</p>
