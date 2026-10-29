# Radijas brand assets

The two SVGs here are the masters. Everything else is generated from them.

| file | what it is |
| --- | --- |
| `radijas-mark.svg` | the mark — a radio receiver, 100×100 viewBox, artwork spans x 8…92 |
| `radijas-banner.svg` | the Android TV banner, mark + „Radijas", 320×180 |
| `render.sh` | regenerates every raster below from the two masters |

Do not redraw the masters for a density; scale them.

## Regenerating

```sh
./assets/brand/render.sh
```

Only `rsvg-convert` (librsvg) is required. The banner's wordmark is live
`<text>` in Liberation Sans Bold, so that font must be installed — with a
different font the metrics change and the word can run past the canvas. The
script writes:

| output | size |
| --- | --- |
| `app/src/main/res/drawable-nodpi/banner.png` | 320×180 |
| `mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher.png`, `ic_launcher_round.png` | 48, 72, 96, 144, 192 |
| `mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher_foreground.png`, `ic_launcher_monochrome.png` | 108, 162, 216, 324, 432 |

## What the commands do

The banner is a straight render, because its master is already 320×180:

```sh
rsvg-convert -w 320 -h 180 assets/brand/radijas-banner.svg \
  -o app/src/main/res/drawable-nodpi/banner.png
```

The launcher layers are rendered from temporary wrappers that nest the mark's
body in a 100×100 canvas. `render.sh` builds them with `box()`/`off()`; the
geometry is:

- **tile** (legacy icons): `#141218` square, mark at 72 % of the canvas width,
  so the nested 100-unit box is `7200/84 = 85.714` wide at `7.143` from each
  edge. Launchers apply their own mask, so the square is full-bleed.
- **foreground / monochrome**: transparent canvas, mark at 62 % of the canvas
  width (100-unit box `73.810` at `13.095`), which keeps it inside the
  66 dp / 108 dp adaptive-icon safe zone.
- **monochrome** uses the same geometry as an SVG `<mask>` (white ink, black
  detail) painted flat white, so the details are transparent and Android can
  tint the layer.

Rendering one size is just the wrapper at that size:

```sh
rsvg-convert -w 192 -h 192 /tmp/tile.svg \
  -o app/src/main/res/mipmap-xxxhdpi/ic_launcher.png
rsvg-convert -w 432 -h 432 /tmp/foreground.svg \
  -o app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.png
```

The adaptive icon XML (`mipmap-anydpi-v26/ic_launcher.xml` and
`ic_launcher_round.xml`) points at `@color/ic_launcher_background`
(`#FF141218`), `@mipmap/ic_launcher_foreground` and
`@mipmap/ic_launcher_monochrome`; there are no per-density `ic_launcher`
webp files any more.
