#!/usr/bin/env bash
# Regenerates the PWA icon set from assets/fukaha-logo.svg.
#
# The icons are committed, so this only needs running when the logo changes.
# Requires rsvg-convert (librsvg) and python3.
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
logo="$repo_root/assets/fukaha-logo.svg"
out_dir="$repo_root/webApp/src/jsMain/resources/icons"
mkdir -p "$out_dir"

# Same mark the README uses: full canvas, no badge plate.
cp "$logo" "$out_dir/icon.svg"

# Any-purpose icons keep the transparent broom at the logo's own scale.
render_any() {
  local size=$1 name=$2
  rsvg-convert "$logo" \
    --width "$size" --height "$size" \
    --format png \
    --output "$out_dir/$name"
  echo "wrote $name (${size}px, transparent)"
}

# Maskable icons get cropped to a circle covering the middle 80%, so the logo
# sits smaller on an opaque plate. White matches the Android launcher background.
render_maskable() {
  local size=$1 fraction=$2 name=$3
  local inner offset
  inner=$(python3 -c "print(round($size * $fraction))")
  offset=$(python3 -c "print(round(($size - $inner) / 2))")
  rsvg-convert "$logo" \
    --width "$inner" --height "$inner" \
    --page-width "$size" --page-height "$size" \
    --top "$offset" --left "$offset" \
    --background-color white \
    --format png \
    --output "$out_dir/$name"
  echo "wrote $name (${size}px, logo ${inner}px, white plate)"
}

render_any 192 icon-192.png
render_any 512 icon-512.png

render_maskable 192 0.58 icon-maskable-192.png
render_maskable 512 0.58 icon-maskable-512.png
