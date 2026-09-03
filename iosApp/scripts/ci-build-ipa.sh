#!/usr/bin/env bash
# Build an unsigned arm64 iOS device IPA for jailbreak-side installation.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
IOS="$ROOT/iosApp"
BUILD_DIR="$IOS/build"
ARCHIVE="$BUILD_DIR/Fukaha.xcarchive"
DERIVED_DATA="$BUILD_DIR/DerivedData-iphoneos"
DIST="$ROOT/dist"

eval "$(bash "$ROOT/scripts/ci/app_version.sh")"
IPA="$DIST/fukaha-${name}-unsigned.ipa"

if ! command -v xcodegen >/dev/null 2>&1; then
  echo "xcodegen is required. Install with: brew install xcodegen" >&2
  exit 1
fi

if ! command -v xcodebuild >/dev/null 2>&1; then
  echo "xcodebuild is required. Run this on macOS with Xcode installed." >&2
  exit 1
fi

cd "$IOS"
xcodegen generate --spec "$IOS/project.yml"

rm -rf "$ARCHIVE"
xcodebuild \
  -project "$IOS/Fukaha.xcodeproj" \
  -scheme Fukaha \
  -configuration Release \
  -destination "generic/platform=iOS" \
  -sdk iphoneos \
  -arch arm64 \
  -derivedDataPath "$DERIVED_DATA" \
  -archivePath "$ARCHIVE" \
  CODE_SIGNING_ALLOWED=NO \
  CODE_SIGNING_REQUIRED=NO \
  CODE_SIGN_IDENTITY=- \
  PROVISIONING_PROFILE_SPECIFIER= \
  archive

APP="$ARCHIVE/Products/Applications/Fukaha.app"
test -d "$APP"

rm -rf "$DIST"
mkdir -p "$DIST/Payload"
cp -R "$APP" "$DIST/Payload/"
(cd "$DIST" && zip -qry "$IPA" Payload)
rm -rf "$DIST/Payload"

test -f "$IPA"
echo "Created $IPA"
unzip -l "$IPA"
