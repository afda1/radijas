package lt.radijas.tv.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Shapes
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme
import lt.radijas.tv.StartupStation
import lt.radijas.tv.R

/** Page colours from the design spec. They are the same for every station. */
internal val PageBackground = Color(0xFF141218)
internal val PageContent = Color(0xFFF2EDF2)

/** M3 shape scale; the tile and the hero use [Shapes.extraLarge] (28 dp). */
internal val RadijasShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * The TV type scale with this app's sizes. The sizes are not M3's: a 3 m viewing distance needs
 * them, and they are pinned by the design spec. M3 contributes the line heights and tracking.
 */
internal val OutfitFamily = FontFamily(
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_medium, FontWeight.Medium),
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
    Font(R.font.outfit_bold, FontWeight.Bold)
)

internal val RadijasTypography = Typography(
    // The station name is as big as it needs to be to read the logo's own words from a sofa,
    // not the biggest thing that fits: the logo already carries the name, and the song and the
    // instruction are what the eye needs. 64 sp keeps the hierarchy on the user's job.
    displayLarge = TextStyle(
        fontFamily = OutfitFamily,
        fontSize = 64.sp,
        lineHeight = 70.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-1.0).sp
    ),
    headlineSmall = TextStyle(fontFamily = OutfitFamily, fontSize = 32.sp, lineHeight = 38.sp),
    titleMedium = TextStyle(fontFamily = OutfitFamily, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold),
    labelLarge = TextStyle(fontFamily = OutfitFamily, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.1.sp),
)

/**
 * The blackout frame. Deliberately absolute black: the point of the control is the least light
 * the panel can emit, so this is one surface that is not tinted towards the station hue.
 */
internal val ScreenOffBlack = Color(0xFF000000)

/**
 * The M3 colour scheme for one station. Unknown ids fall back to the first-run station, so a
 * station removed from the generated table cannot crash the screen.
 */
internal fun stationScheme(stationId: String): ColorScheme {
    val c = stationColors[stationId] ?: stationColors.getValue(StartupStation.DEFAULT_ID)
    return darkColorScheme(
        primary = Color(c.primary),
        onPrimary = Color(c.onPrimary),
        primaryContainer = Color(c.primaryContainer),
        onPrimaryContainer = Color(c.onPrimaryContainer),
        background = PageBackground,
        onBackground = PageContent,
        surface = Color(c.surface),
        onSurface = Color(c.onSurface),
        surfaceVariant = Color(c.surfaceVariant),
        onSurfaceVariant = Color(c.onSurfaceVariant),
        // M3's surfaceTint: what surfaces are lifted towards. The tiles use it for the resting
        // elevation tint, which is why they are not flat grey.
        surfaceTint = Color(c.primary),
        border = Color(c.border),
        borderVariant = Color(0xFF49454F),
        scrim = Color.Black
    )
}

@Composable
fun RadijasTheme(
    stationId: String = StartupStation.DEFAULT_ID,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = stationScheme(stationId),
        shapes = RadijasShapes,
        typography = RadijasTypography,
        content = content
    )
}
