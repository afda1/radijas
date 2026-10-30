package lt.radijas.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import lt.radijas.tv.data.model.RadioStation
import androidx.tv.material3.MaterialTheme

/**
 * The TV home screen: hero on top, focusable station shelf below.
 *
 * The screen is a pure function of [uiState]; playback decisions belong to the caller via
 * [onSelect], so this composable stays previewable and testable.
 */
@Composable
fun NowPlayingScreen(
    uiState: NowPlayingUiState,
    stations: List<RadioStation>,
    onSelect: (RadioStation) -> Unit,
    onScreenOff: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // Vertical overscan safety. The shelf itself extends to the physical screen edges.
            .padding(vertical = 27.dp)
    ) {
        HeroPanel(
            station = uiState.station,
            status = uiState.status,
            trackTitle = uiState.trackTitle,
            onScreenOff = onScreenOff,
            modifier = Modifier
                .padding(horizontal = 48.dp)
                .weight(1f)
                .fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
        StationShelf(
            stations = stations,
            playingId = uiState.station.id,
            onSelect = onSelect,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
