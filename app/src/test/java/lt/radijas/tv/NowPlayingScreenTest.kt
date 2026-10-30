package lt.radijas.tv

import android.content.Context
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import lt.radijas.tv.data.repository.RadioRepository
import lt.radijas.tv.ui.HeroPanel
import lt.radijas.tv.ui.PlayerStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NowPlayingScreenTest {
    @get:Rule val compose = createComposeRule()
    private val station = RadioRepository(ApplicationProvider.getApplicationContext<Context>()).stations.first()

    @Test fun `hero always shows station logo`() {
        compose.setContent { HeroPanel(station, PlayerStatus.PLAYING, modifier = Modifier.requiredSize(864.dp, 260.dp)) }
        compose.onNodeWithTag("hero-station-logo").assertExists()
        compose.onNodeWithTag("hero-album-art").assertDoesNotExist()
    }

    @Test fun `song and playback state changes keep the station logo at the same size`() {
        val title = mutableStateOf<String?>(null)
        val status = mutableStateOf(PlayerStatus.CONNECTING)
        compose.setContent {
            HeroPanel(station, status.value, trackTitle = title.value, modifier = Modifier.requiredSize(864.dp, 260.dp))
        }
        var expectedWidth = 0f
        var expectedHeight = 0f
        PlayerStatus.entries.forEachIndexed { index, candidate ->
            compose.runOnIdle { title.value = "Taylor Swift - Shake It Off"; status.value = candidate }
            compose.onNodeWithTag("hero-station-logo").assertExists()
            compose.onNodeWithTag("hero-album-art").assertDoesNotExist()
            val bounds = compose.onNodeWithTag("hero-station-logo").getUnclippedBoundsInRoot()
            val width = (bounds.right - bounds.left).value
            val height = (bounds.bottom - bounds.top).value
            if (index == 0) {
                // The logic stays out of the markup; only the geometry must not move between states.
                expectedWidth = width
                expectedHeight = height
            }
            org.junit.Assert.assertEquals(expectedWidth, width, 0.01f)
            org.junit.Assert.assertEquals(expectedHeight, height, 0.01f)
        }
    }
}
