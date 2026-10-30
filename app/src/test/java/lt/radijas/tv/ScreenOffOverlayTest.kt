package lt.radijas.tv

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import lt.radijas.tv.ui.ScreenOffOverlay
import lt.radijas.tv.ui.theme.RadijasTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScreenOffOverlayTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `the black frame covers the screen and explains how to wake it`() {
        composeTestRule.setContent {
            RadijasTheme { ScreenOffOverlay(hintVisible = true) }
        }

        composeTestRule.onNodeWithTag("screen-off-overlay").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ekranas išjungtas. Paspausk bet kurį mygtuką.")
            .assertIsDisplayed()
    }

    @Test
    fun `after the hint fades the frame is still there`() {
        composeTestRule.setContent {
            RadijasTheme { ScreenOffOverlay(hintVisible = false) }
        }

        composeTestRule.onNodeWithTag("screen-off-overlay").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ekranas išjungtas. Paspausk bet kurį mygtuką.")
            .assertDoesNotExist()
    }
}
