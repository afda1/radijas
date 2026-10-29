#!/usr/bin/env bash
#
# Regenerates every launcher raster and the TV banner from the two master
# SVGs in this directory. Run from anywhere:
#
#   ./assets/brand/render.sh
#
# Requires rsvg-convert (librsvg). The masters (radijas-mark.svg,
# radijas-banner.svg) are the source of truth; this script only scales and
# re-colours them, it never redraws anything.
set -euo pipefail

here=$(cd "$(dirname "$0")" && pwd)
res=$(cd "$here/../../app/src/main/res" && pwd)
tmp=$(mktemp -d "${TMPDIR:-/tmp}/radijas-brand.XXXXXX")
trap 'rm -rf "$tmp"' EXIT

# The mark is a 100x100 viewBox whose artwork spans x 8..92, so the visible
# radio is 84 units wide. `box` turns a desired fraction of the canvas width
# into the side of the nested 100x100 viewport.
box() { awk -v f="$1" 'BEGIN { printf "%.4f", f * 10000 / 84 }'; }
off() { awk -v b="$1" 'BEGIN { printf "%.4f", (100 - b) / 2 }'; }

mark=$(sed -e '1d' -e '$d' "$here/radijas-mark.svg")

# One wrapper per layer. TILE = 72 % mark on the brand surface; FG/MONO = 62 %
# mark on a transparent canvas (the 66/108 adaptive-icon safe zone).
emit_wrapper() { # <file> <mark-body> <geometric-fraction> <background?>
  local inner; inner=$(box "$3"); local pad; pad=$(off "$inner")
  {
    printf '<svg xmlns="http://www.w3.org/2000/svg" width="100%%" height="100%%" viewBox="0 0 100 100">\n'
    [ "${4:-}" = bg ] && printf '  <rect width="100" height="100" fill="#141218"/>\n'
    printf '  <svg x="%s" y="%s" width="%s" height="%s" viewBox="0 0 100 100">\n' "$pad" "$pad" "$inner" "$inner"
    printf '%s\n' "$2"
    printf '  </svg>\n</svg>\n'
  } > "$1"
}

# Monochrome: the same geometry used as a luminance mask (white ink, black
# detail) and painted flat white, so every detail is punched out of the alpha
# channel and Android can tint the layer.
mono_mask=$(printf '%s\n' "$mark" \
  | sed -e 's/fill="#141218"/fill="#000000"/g' \
        -e 's/fill="#F2EDF2"/fill="#FFFFFF"/g')
mono_mark=$(printf '<defs>\n  <mask id="mono">\n    <rect width="100" height="100" fill="#000000"/>\n%s\n  </mask>\n</defs>\n<rect width="100" height="100" fill="#FFFFFF" mask="url(#mono)"/>' "$mono_mask")

emit_wrapper "$tmp/tile.svg" "$mark" 0.72 bg
emit_wrapper "$tmp/foreground.svg" "$mark" 0.62
emit_wrapper "$tmp/monochrome.svg" "$mono_mark" 0.62

# 1. TV banner — the manifest asks for exactly 320x180.
rsvg-convert -w 320 -h 180 "$here/radijas-banner.svg" \
  -o "$res/drawable-nodpi/banner.png"

# 2. Legacy launcher icons (full-bleed square, launcher applies its own mask).
for entry in mdpi:48 hdpi:72 xhdpi:96 xxhdpi:144 xxxhdpi:192; do
  dpi=${entry%%:*}; px=${entry##*:}
  rsvg-convert -w "$px" -h "$px" "$tmp/tile.svg" \
    -o "$res/mipmap-$dpi/ic_launcher.png"
  rsvg-convert -w "$px" -h "$px" "$tmp/tile.svg" \
    -o "$res/mipmap-$dpi/ic_launcher_round.png"
done

# 3./4. Adaptive foreground + monochrome, 108 dp at every density.
for entry in mdpi:108 hdpi:162 xhdpi:216 xxhdpi:324 xxxhdpi:432; do
  dpi=${entry%%:*}; px=${entry##*:}
  rsvg-convert -w "$px" -h "$px" "$tmp/foreground.svg" \
    -o "$res/mipmap-$dpi/ic_launcher_foreground.png"
  rsvg-convert -w "$px" -h "$px" "$tmp/monochrome.svg" \
    -o "$res/mipmap-$dpi/ic_launcher_monochrome.png"
done

echo "rendered banner + launcher icons into $res"
