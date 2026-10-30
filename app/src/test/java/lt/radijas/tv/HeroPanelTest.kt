package lt.radijas.tv

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import lt.radijas.tv.data.model.RadioStation
import lt.radijas.tv.data.repository.RadioRepository
import lt.radijas.tv.ui.HeroPanel
import lt.radijas.tv.ui.PlayerStatus
import lt.radijas.tv.ui.StationShelf
import lt.radijas.tv.ui.theme.RadijasTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HeroPanelTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val stations: List<RadioStation> =
        RadioRepository(ApplicationProvider.getApplicationContext<Context>()).stations
    private val station: RadioStation = stations.first { it.id == "lietus" }

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

        val leftEdges = PlayerStatus.entries.associateWith { candidate ->
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
    fun `logo and screen-off button fit the real 1080p hero`() {
        composeTestRule.setContent {
            RadijasTheme(stationId = station.id) {
                // The real hero at 1080p: 864 dp wide, 242 dp tall (540 - insets - shelf).
                HeroPanel(
                    station = station,
                    status = PlayerStatus.PLAYING,
                    modifier = Modifier.requiredSize(864.dp, 242.dp)
                )
            }
        }

        val logo = composeTestRule.onNodeWithTag("hero-station-logo").getUnclippedBoundsInRoot()
        val button = composeTestRule.onNodeWithTag("screen-off-button").getUnclippedBoundsInRoot()

        assertEquals("the logo keeps the full slot width", 168f, (logo.right - logo.left).value, 0.5f)
        assertTrue("the wide mark is shorter than the slot", (logo.bottom - logo.top).value < 168f)
        assertTrue("the control stays inside the hero", button.bottom.value <= 242f)
        assertTrue("the control is a small inline target", button.right.value - button.left.value <= 48f)
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
}
