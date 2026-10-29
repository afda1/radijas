package lt.radijas.tv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/** Page background from the design spec. */
val RadijasBackground = Color(0xFF141218)

/** Primary text colour from the design spec. */
val RadijasContent = Color(0xFFF2EDF2)

/**
 * Plate behind logo artwork that is too dark for the brand-tinted surface. Shared by the shelf
 * tile and the hero so both read as the same framed station mark.
 */
val LogoPlate = Color(0xFFF2EFF4)

/** Minimum WCAG 2.1 contrast ratio for text drawn on a tonal surface. */
private const val MinTextContrast = 4.5f

/**
 * Where the hero glow's core sits, as fractions of the hero box.
 *
 * x ≈ 0.90 is the station icon (the hero's right-hand 168 dp block, starting at x ≈ 0.755), so
 * the bright part of the gradient can never sit under a letter: the text column ends at
 * x ≈ 0.755. Both the drawing and [heroSurface] use these constants.
 */
internal const val HeroCoreX = 0.90f
internal const val HeroCoreY = 0.44f

/**
 * Glow radii as fractions of the hero: x of its width, y of its height. Both describe one
 * ellipse: [heroSurface] measures distance in these units, and the drawing maps it to the same
 * ellipse, so the function is independent of the box's pixel size and aspect ratio (and of
 * Android), which is what makes the contrast invariant in `BrandPaletteTest` testable.
 *
 * On the 1080p hero (864 × 249 dp) the glow is ~190 × 154 dp: a halo around the icon, wide
 * enough to tint the right of the hero, far enough from the text column not to wash out a title.
 */
internal const val HeroGlowRadiusX = 0.22f
internal const val HeroGlowRadiusY = 0.62f

/** Brand amount at the core, on the mid ring and at the edge of the glow. */
private const val HeroCoreTint = 0.55f
private const val HeroMidTint = 0.24f
private const val HeroEdgeTint = 0f

/** Distance (in glow radii) where the core ends, and where the mid ring starts to fade. */
private const val HeroCoreStop = 0.18f
private const val HeroMidStop = 0.45f

/**
 * The hero background at one point, as a function of the station brand.
 *
 * A radial glow: [HeroCoreTint] at the core under the icon, a [HeroMidTint] ring around it,
 * then a fade to the page colour. The distance is measured in [HeroGlowRadiusX] of the hero
 * width and [HeroGlowRadiusY] of its height, so the function is independent of the box's pixel
 * size and aspect ratio (and of Android), which is what makes the contrast invariant in
 * `BrandPaletteTest` testable.
 *
 * The numbers are chosen so that the 26 sp track line, drawn at 66 % alpha, still clears
 * 4.5:1 against every point of the text column; the core is deliberately outside that column.
 */
fun heroSurface(brand: Long, xFraction: Float, yFraction: Float): Color {
    val dx = (xFraction - HeroCoreX) / HeroGlowRadiusX
    val dy = (yFraction - HeroCoreY) / HeroGlowRadiusY
    val distance = sqrt(dx * dx + dy * dy)

    val amount = when {
        distance <= HeroCoreStop -> HeroCoreTint
        distance <= HeroMidStop ->
            HeroCoreTint + (HeroMidTint - HeroCoreTint) *
                (distance - HeroCoreStop) / (HeroMidStop - HeroCoreStop)
        distance <= 1f ->
            HeroMidTint + (HeroEdgeTint - HeroMidTint) *
                (distance - HeroMidStop) / (1f - HeroMidStop)
        else -> HeroEdgeTint
    }
    return tonalSurface(brand, amount)
}

/**
 * The same glow as a list of colour stops for a radial brush, so the drawn hero and
 * [heroSurface] cannot drift apart. `tonalSurface` is linear in `amount`, which makes the
 * gradient's interpolation between these stops identical to [heroSurface]'s piecewise one.
 */
internal fun heroGlowStops(brand: Long): Array<Pair<Float, Color>> = arrayOf(
    HeroCoreStop to tonalSurface(brand, HeroCoreTint),
    HeroMidStop to tonalSurface(brand, HeroMidTint),
    1f to tonalSurface(brand, HeroEdgeTint)
)

/**
 * The station's brand colour mixed into the page background.
 *
 * Linear interpolation in sRGB float channels between [RadijasBackground] and [brand]: the
 * `amount` is how much of the brand survives, clamped to 0..1. The endpoints are returned as
 * exact values rather than computed ones, so `1f` is bit-for-bit `Color(brand)`.
 */
fun tonalSurface(brand: Long, amount: Float): Color {
    val fraction = amount.coerceIn(0f, 1f)
    if (fraction == 0f) return RadijasBackground

    val brandColor = Color(brand)
    if (fraction == 1f) return brandColor

    fun mix(base: Float, target: Float) = base + (target - base) * fraction

    return Color(
        red = mix(RadijasBackground.red, brandColor.red),
        green = mix(RadijasBackground.green, brandColor.green),
        blue = mix(RadijasBackground.blue, brandColor.blue),
        alpha = mix(RadijasBackground.alpha, brandColor.alpha)
    )
}

/**
 * Text colour that stays readable on [tonalSurface] of the same brand.
 *
 * The brand itself is tried first, then progressively lightened towards white until it clears
 * WCAG AA (4.5:1) against the 34 % tonal surface. No per-station values: any brand works. White
 * is the last resort. It always suffices here: the 34 % surface never gets lighter than a
 * relative luminance of ~0.13, where white still contrasts at ~5.9:1.
 */
fun onBrand(brand: Long): Color {
    val surface = tonalSurface(brand, 0.34f)
    val brandColor = Color(brand)
    if (contrastRatio(brandColor, surface) >= MinTextContrast) return brandColor

    for (step in 1..32) {
        val fraction = step / 32f
        val lightened = Color(
            red = brandColor.red + (1f - brandColor.red) * fraction,
            green = brandColor.green + (1f - brandColor.green) * fraction,
            blue = brandColor.blue + (1f - brandColor.blue) * fraction,
            alpha = brandColor.alpha
        )
        if (contrastRatio(lightened, surface) >= MinTextContrast) return lightened
    }

    return Color.White
}

/** WCAG 2.1 relative luminance of one sRGB channel. */
private fun linearize(channel: Float): Float =
    if (channel <= 0.03928f) channel / 12.92f else ((channel + 0.055f) / 1.055f).pow(2.4f)

private fun relativeLuminance(color: Color): Float =
    0.2126f * linearize(color.red) +
        0.7152f * linearize(color.green) +
        0.0722f * linearize(color.blue)

private fun contrastRatio(first: Color, second: Color): Float {
    val a = relativeLuminance(first)
    val b = relativeLuminance(second)
    return (max(a, b) + 0.05f) / (min(a, b) + 0.05f)
}

@Composable
fun RadijasTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFFF5A1F),
            onPrimary = Color(0xFF141218),
            background = RadijasBackground,
            onBackground = RadijasContent,
            surface = Color(0xFF1F1A21),
            onSurface = RadijasContent
        ),
        content = content
    )
}
