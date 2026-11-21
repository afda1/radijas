package lt.radijas.tv.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import lt.radijas.tv.data.model.RadioStation
import androidx.tv.material3.MaterialTheme

/**
 * The TV home screen: atmospheric station wash behind a smoked hero, focusable station shelf below.
 *
 * The screen is a pure function of [uiState]; playback decisions belong to the caller via
 * [onSelect], so this composable stays previewable and testable.
 */
@Composable
fun NowPlayingScreen(
    uiState: NowPlayingUiState,
    stations: List<RadioStation>,
    onSelect: (RadioStation) -> Unit,
    onScreenOff: () -> Unit = {},
    nowMs: () -> Long = System::currentTimeMillis
) {
    val shelfFocus = remember { ShelfFocusState() }
    val pageBg = MaterialTheme.colorScheme.background
    val backdropTint by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.primaryContainer,
        animationSpec = tween(durationMillis = 450),
        label = "backdrop-tint"
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                val center = Offset(0.76f * size.width, 0.12f * size.height)
                val radiusX = 0.72f * size.width
                val radiusY = 0.56f * size.height
                val wash = Brush.radialGradient(
                    colors = listOf(
                        backdropTint.copy(alpha = 0.26f),
                        backdropTint.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radiusX
                )
                onDrawBehind {
                    drawRect(pageBg)
                    scale(scaleX = 1f, scaleY = radiusY / radiusX, pivot = center) {
                        drawCircle(brush = wash, radius = radiusX, center = center)
                    }
                }
            }
            // Vertical overscan safety. The shelf itself extends to the physical screen edges.
            .padding(vertical = 27.dp)
    ) {
        HeroPanel(
            station = uiState.station,
            status = uiState.status,
            trackTitle = uiState.trackTitle,
            onScreenOff = onScreenOff,
            onNavigateDown = shelfFocus::focusLastFocusedStation,
            modifier = Modifier
                .padding(horizontal = 48.dp)
                .weight(1f)
                .fillMaxWidth(),
            nowMs = nowMs
        )
        Spacer(Modifier.height(18.dp))
        StationShelf(
            stations = stations,
            playingId = uiState.station.id,
            onSelect = onSelect,
            focusState = shelfFocus,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
