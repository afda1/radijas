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

    @Test fun `hero omits duplicate station logo and shows clock and date instead`() {
        compose.setContent { HeroPanel(station, PlayerStatus.PLAYING, modifier = Modifier.requiredSize(864.dp, 260.dp)) }
        compose.onNodeWithTag("hero-station-logo").assertDoesNotExist()
        compose.onNodeWithTag("hero-album-art").assertDoesNotExist()
        compose.onNodeWithTag("hero-clock").assertExists()
        compose.onNodeWithTag("hero-date").assertExists()
    }

    @Test fun `song and playback state changes keep the hero clock at the same position and size`() {
        val title = mutableStateOf<String?>(null)
        val status = mutableStateOf(PlayerStatus.CONNECTING)
        compose.setContent {
            HeroPanel(station, status.value, trackTitle = title.value, modifier = Modifier.requiredSize(864.dp, 260.dp))
        }
        var expectedLeft = 0f
        var expectedTop = 0f
        var expectedRight = 0f
        var expectedBottom = 0f
        PlayerStatus.entries.forEachIndexed { index, candidate ->
            compose.runOnIdle { title.value = "Taylor Swift - Shake It Off"; status.value = candidate }
            compose.onNodeWithTag("hero-station-logo").assertDoesNotExist()
            compose.onNodeWithTag("hero-album-art").assertDoesNotExist()
            val bounds = compose.onNodeWithTag("hero-clock-container").getUnclippedBoundsInRoot()
            if (index == 0) {
                expectedLeft = bounds.left.value
                expectedTop = bounds.top.value
                expectedRight = bounds.right.value
                expectedBottom = bounds.bottom.value
            }
            org.junit.Assert.assertEquals(expectedLeft, bounds.left.value, 0.01f)
            org.junit.Assert.assertEquals(expectedTop, bounds.top.value, 0.01f)
            org.junit.Assert.assertEquals(expectedRight, bounds.right.value, 0.01f)
            org.junit.Assert.assertEquals(expectedBottom, bounds.bottom.value, 0.01f)
        }
    }
}
