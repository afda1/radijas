package lt.radijas.tv.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Shapes
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme
import lt.radijas.tv.StartupStation

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
internal val RadijasTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 96.sp,
        lineHeight = 105.6.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-1.5).sp
    ),
    headlineSmall = TextStyle(fontSize = 26.sp, lineHeight = 32.sp),
    titleMedium = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    labelLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.1.sp),
)

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
