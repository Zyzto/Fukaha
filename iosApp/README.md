# iOS setup (Xcode on macOS)

Fukaha ships Swift sources for the main app and Share Extension. Wire them to the KMP `Shared` framework:

## 1. Create Xcode project

1. Open Xcode → New iOS App → Product Name `Fukaha`, Bundle ID `app.fukaha`, SwiftUI.
2. Add a Share Extension target `FukahaShare` with Bundle ID `app.fukaha.share`.
3. Replace generated Swift with:
   - `iosApp/Fukaha/FukahaApp.swift`
   - `iosApp/Fukaha/en.lproj/InfoPlist.strings` and `ar.lproj/InfoPlist.strings` (localized display name)
   - `iosShareExtension/ShareViewController.swift` + `Info.plist`
   - `iosShareExtension/en.lproj/InfoPlist.strings` and `ar.lproj/InfoPlist.strings`

## 2. Link Shared framework

In both targets’ Build Phases, add a Run Script **before** Compile Sources:

```bash
cd "$SRCROOT/.."
./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

Set Framework Search Paths to:

```
$(SRCROOT)/../shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)
```

Link `Shared.framework` and enable App Group `group.app.fukaha` on both targets.

## 3. Run

Build & run the app, then share a URL from Safari into **Fukaha**.

Note: large video downloads may fail inside the Share Extension memory limit; Fukaha falls back to the embed link.
