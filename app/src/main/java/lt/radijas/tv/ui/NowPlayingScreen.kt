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
import lt.radijas.tv.ui.theme.RadijasBackground

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
    onSelect: (RadioStation) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RadijasBackground)
            // The 5 % safe area, applied once for the whole screen: no child repeats it, so
            // there is a single pair of numbers to change if a TV overscans more than this.
            .padding(horizontal = 48.dp, vertical = 27.dp)
    ) {
        HeroPanel(
            station = uiState.station,
            status = uiState.status,
            trackTitle = uiState.trackTitle,
            modifier = Modifier
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
