package lt.radijas.tv

import android.content.Context
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
}
