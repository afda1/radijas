package lt.radijas.tv.ui.theme

import androidx.compose.ui.graphics.painter.Painter

/**
 * How much of the logo box a station mark takes, by width.
 *
 * The brand marks differ in proportion (1.15 to 3.25) and in ink density, so fitting every mark to
 * the same box made M-1 Plius about five times the visual weight of Lietus. Wide wordmarks stay at
 * full width (they are already limited by it); dense compact marks shrink until their ink area is
 * about [LogoInkTarget] square dp. `StationLogoScaleTest` recomputes this from the PNGs, so the
 * table cannot silently drift.
 */
internal const val LogoInkTarget = 4200f

internal val stationLogoWidthFraction: Map<String, Float> = mapOf(
    "ehr" to 1.00f,
    "goldfm" to 1.00f,
    "lietus" to 1.00f,
    "m1" to 0.80f,
    "plus" to 0.71f,
    "powerhit" to 0.93f,
    "powerhitgold" to 0.83f,
    "rc" to 0.84f,
    "relax" to 1.00f,
    "rock" to 1.00f,
    "zip" to 1.00f,
)

/** Intrinsic width / height of a drawable painter, so a mark is sized without distortion. */
internal fun logoAspect(painter: Painter): Float =
    painter.intrinsicSize.takeIf { it.height > 0f && it.width > 0f }
        ?.let { it.width / it.height }
        ?: 1f
