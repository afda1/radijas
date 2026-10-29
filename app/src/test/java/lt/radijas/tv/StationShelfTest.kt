package lt.radijas.tv

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.core.app.ApplicationProvider
import lt.radijas.tv.data.model.RadioStation
import lt.radijas.tv.data.repository.RadioRepository
import lt.radijas.tv.ui.StationShelf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StationShelfTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val stations: List<RadioStation> =
        RadioRepository(ApplicationProvider.getApplicationContext<Context>()).stations

    @Test
    fun `focus starts on playing station`() {
        composeTestRule.setContent { StationShelf(stations, playingId = "m1", onSelect = {}) }
        composeTestRule.onNodeWithTag("station-tile-m1").assertIsFocused()
    }

    @Test
    fun `right moves focus to next station`() {
        composeTestRule.setContent { StationShelf(stations, playingId = "zip", onSelect = {}) }
        composeTestRule.onNodeWithTag("station-tile-zip").performKeyInput { pressKey(Key.DirectionRight) }
        composeTestRule.onNodeWithTag("station-tile-powerhit").assertIsFocused()
    }

    @Test
    fun `left at first station keeps focus`() {
        composeTestRule.setContent { StationShelf(stations, playingId = "zip", onSelect = {}) }
        composeTestRule.onNodeWithTag("station-tile-zip").performKeyInput { pressKey(Key.DirectionLeft) }
        composeTestRule.onNodeWithTag("station-tile-zip").assertIsFocused()
    }

    @Test
    fun `up and down do nothing`() {
        composeTestRule.setContent { StationShelf(stations, playingId = "zip", onSelect = {}) }
        composeTestRule.onNodeWithTag("station-tile-zip").performKeyInput { pressKey(Key.DirectionDown) }
        composeTestRule.onNodeWithTag("station-tile-zip").assertIsFocused()
    }

    @Test
    fun `ok on focused station calls onSelect`() {
        var selected: RadioStation? = null
        composeTestRule.setContent { StationShelf(stations, playingId = "m1", onSelect = { selected = it }) }
        composeTestRule.onNodeWithTag("station-tile-m1").performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals("m1", selected?.id)
    }

    /**
     * The shelf reveals the playing station exactly once, on first composition. Picking a station
     * changes [StationShelf]'s `playingId`, but the user's D-pad focus is already on the tile they
     * pressed, so a second scroll would snap that tile to the row's left edge and fight Compose's
     * own bring-into-view animation - the jump-and-distort the user reported.
     */
    @Test
    fun `selection does not move the shelf or the focus`() {
        lateinit var listState: LazyListState
        val playingId = mutableStateOf("zip")
        composeTestRule.setContent {
            listState = rememberLazyListState()
            StationShelf(
                stations = stations,
                playingId = playingId.value,
                onSelect = {},
                listState = listState
            )
        }
        composeTestRule.onNodeWithTag("station-tile-zip").assertIsFocused()
        assertEquals("the initial reveal leaves the playing tile first", 0, listState.firstVisibleItemIndex)

        // Selecting m1 (index 6) would once have scrolled the row to it and re-requested focus.
        playingId.value = "m1"
        composeTestRule.waitForIdle()

        assertEquals("selection must not scroll the shelf", 0, listState.firstVisibleItemIndex)
        composeTestRule.onNodeWithTag("station-tile-zip").assertIsFocused()
    }

    /**
     * At launch the shelf is composed before the station is known, so the first run of the reveal
     * effect has no id to reveal. The reveal must run again when that initial station arrives -
     * otherwise the row stays at ZIP FM and the ring lands on the first tile even though the last
     * played station is, say, Radiocentras (index 9).
     */
    @Test
    fun `reveals the station when it is resolved after first composition`() {
        lateinit var listState: LazyListState
        val playingId = mutableStateOf<String?>(null)
        composeTestRule.setContent {
            listState = rememberLazyListState()
            StationShelf(
                stations = stations,
                playingId = playingId.value,
                onSelect = {},
                listState = listState
            )
        }
        composeTestRule.waitForIdle()
        assertEquals("nothing to reveal before the station is known", 0, listState.firstVisibleItemIndex)

        // Radiocentras is the last station, well outside the initial viewport.
        playingId.value = "rc"
        composeTestRule.waitForIdle()

        assertTrue("the shelf must scroll to the resolved station", listState.firstVisibleItemIndex > 0)
        composeTestRule.onNodeWithTag("station-tile-rc").assertIsFocused()
    }

    /** Once the initial station has been revealed, later selections must never move the row again. */
    @Test
    fun `a later playing id change does not move the shelf or the focus`() {
        lateinit var listState: LazyListState
        val playingId = mutableStateOf<String?>(null)
        composeTestRule.setContent {
            listState = rememberLazyListState()
            StationShelf(
                stations = stations,
                playingId = playingId.value,
                onSelect = {},
                listState = listState
            )
        }
        playingId.value = "rc"
        composeTestRule.waitForIdle()
        val revealedIndex = listState.firstVisibleItemIndex
        assertTrue("the late reveal must have scrolled the row", revealedIndex > 0)

        playingId.value = "zip"
        composeTestRule.waitForIdle()

        assertEquals("only the initial reveal may scroll", revealedIndex, listState.firstVisibleItemIndex)
        composeTestRule.onNodeWithTag("station-tile-rc").assertIsFocused()
    }
}
