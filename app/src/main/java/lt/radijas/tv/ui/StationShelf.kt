package lt.radijas.tv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import lt.radijas.tv.data.model.RadioStation

/**
 * The screen's single focus zone: one horizontal row of stations.
 *
 * Focus never wraps and never leaves the row: [LazyRow]'s default one-dimensional search stops at
 * the first and last tile, and there is nothing above or below the row to move into.
 *
 * Keeping the focused tile visible is left to Compose: focusing a tile inside a lazy list already
 * animates the list until the tile sits in view. Scrolling again from `onFocusChanged` would fight
 * that animation, so this composable only performs the one scroll Compose cannot infer - the
 * initial jump to an off-screen playing station.
 */
@Composable
fun StationShelf(
    stations: List<RadioStation>,
    playingId: String,
    onSelect: (RadioStation) -> Unit,
    modifier: Modifier = Modifier,
    // Hoisted so a test can observe the row's scroll position; production call sites keep the
    // default and never share the state.
    listState: LazyListState = rememberLazyListState()
) {
    val focusRequesters = remember(stations) {
        stations.associate { station -> station.id to FocusRequester() }
    }

    // Captured, not read live: the effect below must reveal the station that was playing when the
    // shelf first appeared, not chase every later selection.
    val revealId = remember(stations) { playingId }

    // Keyed on the station list only. `playingId` changes on every selection, and keying on it
    // made the shelf scroll again - snapping the pressed tile to the row's left edge and fighting
    // Compose's own focus bring-into-view animation, which the user saw as the carousel jumping
    // and distorting. The user's focus is already on the tile they pressed, so there is nothing
    // left to scroll to.
    LaunchedEffect(stations) {
        val revealIndex = stations.indexOfFirst { it.id == revealId }
        if (revealIndex >= 0) {
            // LazyRow only composes on-screen items, so a bare requestFocus on an off-screen
            // station (m1 at launch) would silently fail. Scroll first, then wait for the
            // scrolled-to tile to be composed and attached to its FocusRequester.
            listState.scrollToItem(revealIndex)
            withFrameNanos { }
            focusRequesters[revealId]?.requestFocus()
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        // Not the safe area (that is applied once by the screen): this 8 dp inset is only
        // head-room for the focused tile's 5 % zoom, which otherwise gets its ring clipped
        // by the lazy row's own bounds at the first and last station.
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(stations, key = { station -> station.id }) { station ->
            // Kept stable per tile: an inline lambda would be a new object on every
            // recomposition (and there is now one per status tick), which would make every
            // tile recompose and lose its focus state.
            val select = remember(station, onSelect) { { onSelect(station) } }
            StationTile(
                station = station,
                focusRequester = focusRequesters.getValue(station.id),
                onSelect = select
            )
        }
    }
}
