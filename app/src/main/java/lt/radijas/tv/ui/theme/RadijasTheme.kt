package lt.radijas.tv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** Page background from the design spec. */
val RadijasBackground = Color(0xFF141218)

/** Primary text colour from the design spec. */
val RadijasContent = Color(0xFFF2EDF2)

/** Minimum WCAG 2.1 contrast ratio for text drawn on a tonal surface. */
private const val MinTextContrast = 4.5f

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
