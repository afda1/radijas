package lt.radijas.tv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

/** Page background from the design spec. */
val RadijasBackground = Color(0xFF141218)

/** Primary text colour from the design spec. */
val RadijasContent = Color(0xFFF2EDF2)

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
