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
 * Task 5 will add `status` and `trackTitle` parameters; for now the screen is layout and focus
 * only, so OK on a tile is forwarded to [onSelect] but does not yet change playback.
 */
@Composable
fun NowPlayingScreen(
    station: RadioStation,
    stations: List<RadioStation>,
    onSelect: (RadioStation) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RadijasBackground)
            .padding(vertical = 27.dp)
    ) {
        HeroPanel(
            station = station,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 48.dp)
        )
        Spacer(Modifier.height(20.dp))
        StationShelf(
            stations = stations,
            playingId = station.id,
            onSelect = onSelect,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
