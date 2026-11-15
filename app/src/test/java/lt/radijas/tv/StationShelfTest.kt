package lt.radijas.tv

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.test.core.app.ApplicationProvider
import lt.radijas.tv.data.model.RadioStation
import lt.radijas.tv.data.repository.RadioRepository
import lt.radijas.tv.ui.StationShelf
import lt.radijas.tv.ui.StationTile
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
        composeTestRule.setContent { StationShelf(stations, playingId = "ehr", onSelect = {}) }
        composeTestRule.onNodeWithTag("station-tile-ehr").performKeyInput { pressKey(Key.DirectionRight) }
        composeTestRule.onNodeWithTag("station-tile-goldfm").assertIsFocused()
    }

    @Test
    fun `left at first station keeps focus`() {
        composeTestRule.setContent { StationShelf(stations, playingId = "ehr", onSelect = {}) }
        composeTestRule.onNodeWithTag("station-tile-ehr").performKeyInput { pressKey(Key.DirectionLeft) }
        composeTestRule.onNodeWithTag("station-tile-ehr").assertIsFocused()
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
        val playingId = mutableStateOf("ehr")
        composeTestRule.setContent {
            listState = rememberLazyListState()
            StationShelf(
                stations = stations,
                playingId = playingId.value,
                onSelect = {},
                listState = listState
            )
        }
        composeTestRule.onNodeWithTag("station-tile-ehr").assertIsFocused()
        assertEquals("the initial reveal leaves the playing tile first", 0, listState.firstVisibleItemIndex)

        // Selecting another station must not scroll or re-request focus.
        playingId.value = "m1"
        composeTestRule.waitForIdle()

        assertEquals("selection must not scroll the shelf", 0, listState.firstVisibleItemIndex)
        composeTestRule.onNodeWithTag("station-tile-ehr").assertIsFocused()
    }

    /**
     * At launch the shelf is composed before the station is known, so the first run of the reveal
     * effect has no id to reveal. The reveal must run again when that initial station arrives -
     * otherwise the ring lands on the first tile even though a later station is playing.
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

        // Radiocentras is near the end of the row, well outside the initial viewport.
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

    /**
     * A tile is a button. Without a click action an accessibility service can focus it but never
     * press it, which would make the whole app unusable for a low-vision user: the screen has no
     * other control.
     */
    @Test
    fun `tile is a button that assistive tech can activate`() {
        var selected: String? = null
        composeTestRule.setContent {
            StationShelf(stations, playingId = "m1", onSelect = { selected = it.id })
        }
        val tile = composeTestRule.onNodeWithTag("station-tile-m1")

        tile.assertHasClickAction()
        tile.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))

        tile.performSemanticsAction(SemanticsActions.OnClick)
        assertEquals("m1", selected)
    }

    /** The tile that is playing says so; the other ten do not. */
    @Test
    fun `only the playing tile announces that it is playing`() {
        val playingLabel =
            ApplicationProvider.getApplicationContext<Context>().getString(R.string.station_playing)
        // The first station, so the row does not scroll and both compared tiles are composed.
        composeTestRule.setContent {
            StationShelf(stations, playingId = "ehr", onSelect = {})
        }

        composeTestRule.onNodeWithTag("station-tile-ehr")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, playingLabel))
        composeTestRule.onNodeWithTag("station-tile-goldfm")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
    }

    @Test
    fun `unfocused playing tile keeps medium label weight while focused other tile uses semibold`() {
        composeTestRule.setContent {
            StationShelf(stations, playingId = "ehr", onSelect = {})
        }
        composeTestRule.onNodeWithTag("station-tile-ehr").performKeyInput { pressKey(Key.DirectionRight) }
        composeTestRule.onNodeWithTag("station-tile-goldfm").assertIsFocused()

        val ehrLayouts = mutableListOf<TextLayoutResult>()
        composeTestRule.onNodeWithTag("station-tile-ehr")
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(ehrLayouts) }
        assertEquals(FontWeight.Medium, ehrLayouts.single().layoutInput.style.fontWeight)

        val goldLayouts = mutableListOf<TextLayoutResult>()
        composeTestRule.onNodeWithTag("station-tile-goldfm")
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(goldLayouts) }
        assertEquals(FontWeight.SemiBold, goldLayouts.single().layoutInput.style.fontWeight)
    }

    @Test
    fun `playing dot and full station name fit without ellipsis on every station`() {
        var currentStation by mutableStateOf(stations.first())
        composeTestRule.setContent {
            StationTile(
                station = currentStation,
                isPlaying = true
            )
        }

        stations.forEach { station ->
            composeTestRule.runOnUiThread { currentStation = station }
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("station-tile-playing-dot-${station.id}", useUnmergedTree = true)
                .assertIsDisplayed()
            val layouts = mutableListOf<TextLayoutResult>()
            composeTestRule.onNodeWithTag("station-tile-${station.id}")
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertTrue(
                "${station.id} station name must not ellipsize next to playing dot",
                !layouts.single().isLineEllipsized(0)
            )
        }
    }
}
