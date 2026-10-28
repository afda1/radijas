package lt.radijas.tv

import android.content.Context
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import lt.radijas.tv.data.model.RadioStation
import lt.radijas.tv.data.repository.RadioRepository
import lt.radijas.tv.ui.HeroPanel
import lt.radijas.tv.ui.PlayerStatus
import lt.radijas.tv.ui.theme.RadijasTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HeroPanelTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val station: RadioStation =
        RadioRepository(ApplicationProvider.getApplicationContext<Context>())
            .stations.first { it.id == "lietus" }

    @Test
    fun `status copy starts at the same x in every state`() {
        var status by mutableStateOf(PlayerStatus.PLAYING)
        composeTestRule.setContent {
            RadijasTheme {
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
}
