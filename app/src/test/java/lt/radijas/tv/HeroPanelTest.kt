package lt.radijas.tv

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import lt.radijas.tv.data.model.RadioStation
import lt.radijas.tv.data.repository.RadioRepository
import lt.radijas.tv.ui.HeroPanel
import lt.radijas.tv.ui.NowPlayingScreen
import lt.radijas.tv.ui.NowPlayingUiState
import lt.radijas.tv.ui.PlayerStatus
import lt.radijas.tv.ui.StationShelf
import lt.radijas.tv.ui.ShelfFocusState
import lt.radijas.tv.ui.theme.RadijasTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
class HeroPanelTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val stations: List<RadioStation> =
        RadioRepository(ApplicationProvider.getApplicationContext<Context>()).stations
    private val station: RadioStation = stations.first { it.id == "lietus" }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `only overflowing track titles get faded edges`() {
        var title by mutableStateOf("Trumpa daina")
        composeTestRule.setContent {
            RadijasTheme(stationId = station.id) {
                HeroPanel(station, PlayerStatus.PLAYING, title, modifier = Modifier.requiredSize(864.dp, 254.dp))
            }
        }
        composeTestRule.onNodeWithTag("hero-track-fade").assertDoesNotExist()
        composeTestRule.runOnUiThread {
            title = "Atlikėjas ir dar vienas atlikėjas - labai ilgas kūrinio pavadinimas, kuris netelpa į vieną eilutę televizoriaus ekrane"
        }
        composeTestRule.onNodeWithTag("hero-track-fade").assertExists()
    }

    @Test
    fun `long track title stays complete without an ellipsis`() {
        val title = "Atlikėjas - labai ilgas kūrinio pavadinimas, kuris netelpa į vieną eilutę televizoriaus ekrane"
        composeTestRule.setContent {
            RadijasTheme(stationId = station.id) {
                HeroPanel(station, PlayerStatus.PLAYING, title, modifier = Modifier.requiredSize(864.dp, 254.dp))
            }
        }
        val layouts = mutableListOf<TextLayoutResult>()
        composeTestRule.onNodeWithTag("hero-track-title")
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue("long title must be laid out", layouts.isNotEmpty())
        assertTrue("the full title must not be replaced by an ellipsis", !layouts.single().isLineEllipsized(0))
    }

    @Test
    fun `screen control sits between the equalizer and station shelf`() {
        composeTestRule.setContent {
            RadijasTheme(stationId = station.id) {
                Box(Modifier.requiredSize(960.dp, 540.dp)) {
                    NowPlayingScreen(
                        uiState = NowPlayingUiState(station = station, status = PlayerStatus.PLAYING),
                        stations = stations,
                        onSelect = {}
                    )
                }
            }
        }
        composeTestRule.onNodeWithTag("station-tile-lietus").performKeyInput { pressKey(Key.DirectionUp) }
        val equalizer = composeTestRule.onNodeWithTag("hero-equalizer").getUnclippedBoundsInRoot()
        val button = composeTestRule.onNodeWithTag("screen-off-button").getUnclippedBoundsInRoot()
        val shelf = composeTestRule.onNodeWithTag("station-tile-lietus").getUnclippedBoundsInRoot()
        val above = (button.top - equalizer.bottom).value
        val below = (shelf.top - button.bottom).value
        assertTrue("button needs generous space above it: $above", above >= 16f)
        assertTrue("button needs generous breathing room above the shelf: $below", below >= 24f)
    }

    @Test
    fun `returning from screen control keeps the carousel position`() {
        lateinit var listState: LazyListState
        val focusState = ShelfFocusState()
        composeTestRule.setContent {
            listState = rememberLazyListState()
            RadijasTheme(stationId = "ehr") {
                Column(Modifier.requiredSize(960.dp, 540.dp)) {
                    HeroPanel(
                        station = stations.first(),
                        status = PlayerStatus.PLAYING,
                        onNavigateDown = focusState::focusLastFocusedStation,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                    StationShelf(stations, "ehr", {}, listState = listState, focusState = focusState)
                }
            }
        }
        composeTestRule.onNodeWithTag("station-tile-ehr").performKeyInput { pressKey(Key.DirectionRight) }
        composeTestRule.onNodeWithTag("station-tile-goldfm").assertIsFocused()
        val before = listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        composeTestRule.onNodeWithTag("station-tile-goldfm").performKeyInput { pressKey(Key.DirectionUp) }
        composeTestRule.onNodeWithTag("screen-off-button").performKeyInput { pressKey(Key.DirectionDown) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("station-tile-goldfm").assertIsFocused()
        assertEquals(before, listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset)
    }

    @Test
    fun `status copy starts at the same x in every state`() {
        var status by mutableStateOf(PlayerStatus.PLAYING)
        composeTestRule.setContent {
            RadijasTheme(stationId = station.id) {
                HeroPanel(
                    station = station,
                    status = status,
                    // requiredSize, not size: the test window is smaller than a TV, and the
                    // point of the test is the hero's own layout, not the window's.
                    modifier = Modifier.requiredSize(1200.dp, 400.dp)
                )
            }
        }

        val leftEdges = PlayerStatus.entries.filter { it != PlayerStatus.PLAYING }.associateWith { candidate ->
            composeTestRule.runOnUiThread { status = candidate }
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("hero-status").getUnclippedBoundsInRoot().left
        }

        assertEquals(
            "status copy must not move between states: $leftEdges",
            1,
            leftEdges.values.toSet().size
        )
    }

    @Test
    fun `clock, equalizer and screen-off button fit the real 1080p hero`() {
        composeTestRule.setContent {
            RadijasTheme(stationId = station.id) {
                // The real hero at 1080p: 864 dp wide, 254 dp tall (540 - insets - shelf).
                HeroPanel(
                    station = station,
                    status = PlayerStatus.PLAYING,
                    modifier = Modifier.requiredSize(864.dp, 254.dp)
                )
            }
        }

        composeTestRule.onNodeWithTag("hero-station-logo").assertDoesNotExist()
        val clock = composeTestRule.onNodeWithTag("hero-clock-container").getUnclippedBoundsInRoot()
        val button = composeTestRule.onNodeWithTag("screen-off-button").getUnclippedBoundsInRoot()

        assertTrue("clock must stay inside hero width", clock.right.value <= 864f)
        assertTrue("clock must stay inside hero height", clock.bottom.value <= 254f)
        assertTrue("the control stays inside the hero", button.bottom.value <= 254f)
        assertTrue("the control has room for a visible label", button.right.value - button.left.value > 48f)
        composeTestRule.onNodeWithText("Patamsinti ekraną").assertExists()
        composeTestRule.onNodeWithTag("hero-status").assertDoesNotExist()
        val equalizer = composeTestRule.onNodeWithTag("hero-equalizer").getUnclippedBoundsInRoot()
        assertTrue(
            "the fluid wave equalizer must span the wide hero slot: ${(equalizer.right - equalizer.left).value}",
            (equalizer.right - equalizer.left).value >= 160f
        )
        assertTrue("the focused button must not touch the equalizer", (button.top - equalizer.bottom).value >= 8f)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `overflowing track title stays flush-left aligned with the equalizer`() {
        val longTitle = "Atlikėjas ir dar vienas atlikėjas - labai ilgas kūrinio pavadinimas, kuris netelpa į vieną eilutę televizoriaus ekrane"
        composeTestRule.setContent {
            RadijasTheme(stationId = station.id) {
                HeroPanel(
                    station = station,
                    status = PlayerStatus.PLAYING,
                    trackTitle = longTitle,
                    modifier = Modifier.requiredSize(864.dp, 254.dp)
                )
            }
        }

        composeTestRule.onNodeWithTag("hero-track-fade").assertExists()
        val titleLeft = composeTestRule.onNodeWithTag("hero-track-title").getUnclippedBoundsInRoot().left.value
        val equalizerLeft = composeTestRule.onNodeWithTag("hero-equalizer").getUnclippedBoundsInRoot().left.value
        assertEquals(
            "overflowing track title must stay flush-left with the equalizer",
            equalizerLeft,
            titleLeft,
            1.0f
        )
    }

    @Test
    fun `up from the shelf moves focus to the screen-off button`() {
        composeTestRule.setContent {
            RadijasTheme(stationId = station.id) {
                Column(Modifier.requiredSize(960.dp, 540.dp)) {
                    HeroPanel(
                        station = station,
                        status = PlayerStatus.PLAYING,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                    StationShelf(stations = stations, playingId = station.id, onSelect = {})
                }
            }
        }

        composeTestRule.onNodeWithTag("station-tile-lietus").assertIsFocused()
        composeTestRule.onNodeWithTag("station-tile-lietus").performKeyInput { pressKey(Key.DirectionUp) }

        composeTestRule.onNodeWithTag("screen-off-button").assertIsFocused()
    }
    @Test
    fun `down from the screen-off control returns to the station that was left`() {
        // The real screen, because the down key is wired there through ShelfFocusState.
        composeTestRule.setContent {
            RadijasTheme(stationId = station.id) {
                Box(Modifier.requiredSize(960.dp, 540.dp)) {
                    NowPlayingScreen(
                        uiState = NowPlayingUiState(station = station, status = PlayerStatus.PLAYING),
                        stations = stations,
                        onSelect = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("station-tile-lietus").assertIsFocused()
        composeTestRule.onNodeWithTag("station-tile-lietus").performKeyInput { pressKey(Key.DirectionUp) }
        composeTestRule.onNodeWithTag("screen-off-button").assertIsFocused()

        composeTestRule.onNodeWithTag("screen-off-button").performKeyInput { pressKey(Key.DirectionDown) }
        // The shelf scrolls the tile back into view in a coroutine before focusing it; give the
        // test clock a few frames to run that scroll and the focus request after it.
        repeat(4) { composeTestRule.mainClock.advanceTimeByFrame() }
        composeTestRule.waitForIdle()

        // The bug this pins: geometric focus search landed on the tile under the control's x
        // position instead of the station the user actually left.
        composeTestRule.onNodeWithTag("station-tile-lietus").assertIsFocused()
    }
}
