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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import lt.radijas.tv.ui.theme.RadijasBackground
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
    playingId: String?,
    onSelect: (RadioStation) -> Unit,
    modifier: Modifier = Modifier,
    // Hoisted so a test can observe the row's scroll position; production call sites keep the
    // default and never share the state.
    listState: LazyListState = rememberLazyListState()
) {
    val focusRequesters = remember(stations) {
        stations.associate { station -> station.id to FocusRequester() }
    }

    // One-shot: the shelf reveals the playing station once, when that station first becomes
    // known, and never moves again on its own. Selection, status ticks and recomposition all
    // leave the row where the user put it.
    var hasRevealed by remember(stations) { mutableStateOf(false) }

    // Keyed on `playingId` as well as the station list *because* the id can arrive late: launch
    // resolves the station after the first composition, so the first run of this effect has no
    // id to reveal. It has to run again when the id appears - and the flag stops every later run.
    // (`playingId` intentionally does not drive a scroll on selection: the user's focus is
    // already on the tile they pressed, and scrolling would fight Compose's own
    // bring-into-view animation, which the user saw as the carousel jumping and distorting.)
    LaunchedEffect(stations, playingId) {
        if (hasRevealed) return@LaunchedEffect
        val revealId = playingId ?: return@LaunchedEffect
        val revealIndex = stations.indexOfFirst { it.id == revealId }
        if (revealIndex < 0) return@LaunchedEffect
        // LazyRow only composes on-screen items, so a bare requestFocus on an off-screen
        // station (m1 at launch) would silently fail. Scroll first, then wait for the
        // scrolled-to tile to be composed and attached to its FocusRequester.
        listState.scrollToItem(revealIndex)
        withFrameNanos { }
        focusRequesters[revealId]?.requestFocus()
        hasRevealed = true
    }

    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth().drawWithContent {
            drawContent()
            // Fade only at physical edges, never over the safe-area focused card.
            val edge = 28.dp.toPx()
            if (listState.canScrollBackward) drawRect(
                brush = Brush.horizontalGradient(listOf(RadijasBackground, Color.Transparent), endX = edge),
                size = Size(edge, size.height)
            )
            if (listState.canScrollForward) drawRect(
                brush = Brush.horizontalGradient(listOf(Color.Transparent, RadijasBackground), startX = size.width - edge, endX = size.width),
                topLeft = Offset(size.width - edge, 0f), size = Size(edge, size.height)
            )
        },
        // Content inset, not a clipped viewport: cards now scroll through the safe margins.
        // 8 dp vertical head-room clears the full 6 % focused scale (~6.24 dp).
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 8.dp),
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
