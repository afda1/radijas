package lt.radijas.tv

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import java.util.TimeZone
import lt.radijas.tv.ui.ScreenOffOverlay
import lt.radijas.tv.ui.screenOffPixelShiftDp
import lt.radijas.tv.ui.theme.OutfitFamily
import lt.radijas.tv.ui.theme.RadijasTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
class ScreenOffOverlayTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun epochMs(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance(TimeZone.getDefault()).apply {
            set(year, month, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    @Test
    fun `the black frame covers the screen and explains how to wake it`() {
        composeTestRule.setContent {
            RadijasTheme { ScreenOffOverlay(hintVisible = true) }
        }

        composeTestRule.onNodeWithTag("screen-off-overlay").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ekranas patamsintas. Paspausk bet kurį mygtuką.")
            .assertIsDisplayed()
    }

    @Test
    fun `after the hint fades the frame is still there`() {
        composeTestRule.setContent {
            RadijasTheme { ScreenOffOverlay(hintVisible = false) }
        }

        composeTestRule.onNodeWithTag("screen-off-overlay").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ekranas patamsintas. Paspausk bet kurį mygtuką.")
            .assertDoesNotExist()
    }

    @Test
    fun `faint clock and track title remain visible after the wake hint fades`() {
        val fixedNowMs = epochMs(2026, Calendar.NOVEMBER, 14, 18, 25)
        composeTestRule.setContent {
            RadijasTheme {
                ScreenOffOverlay(
                    hintVisible = false,
                    trackTitle = "DONNY MONTELL - LEISK MYLET",
                    nowMs = { fixedNowMs }
                )
            }
        }

        composeTestRule.onNodeWithTag("screen-off-clock")
            .assertIsDisplayed()
            .assertTextEquals("18:25")
        composeTestRule.onNodeWithTag("screen-off-track")
            .assertIsDisplayed()
            .assertTextEquals("DONNY MONTELL - LEISK MYLET")

        val clockLayouts = mutableListOf<TextLayoutResult>()
        composeTestRule.onNodeWithTag("screen-off-clock")
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(clockLayouts) }
        val clockStyle = clockLayouts.single().layoutInput.style
        assertEquals("tnum", clockStyle.fontFeatureSettings)
        assertEquals(116.sp, clockStyle.fontSize)
        assertEquals(FontWeight.Normal, clockStyle.fontWeight)
        assertTrue(
            "clock color must stay faint (${clockStyle.color})",
            clockStyle.color != Color.Unspecified && clockStyle.color.alpha <= 0.40f
        )
        composeTestRule.onNodeWithTag("screen-off-playing-dot").assertIsDisplayed()

        val trackLayouts = mutableListOf<TextLayoutResult>()
        composeTestRule.onNodeWithTag("screen-off-track")
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(trackLayouts) }
        val trackStyle = trackLayouts.single().layoutInput.style
        assertTrue(
            "track color must stay faint (${trackStyle.color})",
            trackStyle.color != Color.Unspecified && trackStyle.color.alpha <= 0.40f
        )
    }

    @Test
    fun `clock stays at the same vertical position when track title arrives or disappears and when hint fades`() {
        val fixedNowMs = epochMs(2026, Calendar.NOVEMBER, 14, 18, 25)
        var hintVisible by mutableStateOf(true)
        var trackTitle by mutableStateOf<String?>(null)

        composeTestRule.setContent {
            RadijasTheme {
                Box(Modifier.requiredSize(960.dp, 540.dp)) {
                    ScreenOffOverlay(
                        hintVisible = hintVisible,
                        trackTitle = trackTitle,
                        nowMs = { fixedNowMs }
                    )
                }
            }
        }

        val initialTop = composeTestRule.onNodeWithTag("screen-off-clock")
            .getUnclippedBoundsInRoot().top.value

        composeTestRule.runOnUiThread {
            hintVisible = false
            trackTitle = "DONNY MONTELL - LEISK MYLET"
        }
        composeTestRule.waitForIdle()

        val afterTrackTop = composeTestRule.onNodeWithTag("screen-off-clock")
            .getUnclippedBoundsInRoot().top.value
        assertEquals(initialTop, afterTrackTop, 0.01f)

        composeTestRule.runOnUiThread {
            trackTitle = null
        }
        composeTestRule.waitForIdle()

        val afterClearTop = composeTestRule.onNodeWithTag("screen-off-clock")
            .getUnclippedBoundsInRoot().top.value
        assertEquals(initialTop, afterClearTop, 0.01f)
    }

    @Test
    fun `falls back to station name when track title is missing or blank`() {
        val fixedNowMs = epochMs(2026, Calendar.NOVEMBER, 14, 18, 25)
        var trackTitle by mutableStateOf<String?>(null)

        composeTestRule.setContent {
            RadijasTheme {
                ScreenOffOverlay(
                    hintVisible = false,
                    trackTitle = trackTitle,
                    stationName = "LRT Radijas",
                    nowMs = { fixedNowMs }
                )
            }
        }

        composeTestRule.onNodeWithTag("screen-off-track")
            .assertIsDisplayed()
            .assertTextEquals("LRT Radijas")

        composeTestRule.runOnUiThread { trackTitle = "   " }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("screen-off-track")
            .assertIsDisplayed()
            .assertTextEquals("LRT Radijas")

        composeTestRule.runOnUiThread { trackTitle = "JAZZ FM - NIGHTFALL" }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("screen-off-track")
            .assertIsDisplayed()
            .assertTextEquals("JAZZ FM - NIGHTFALL")
    }

    @Test
    fun `pixel shift moves ambient display subtly across minutes while staying within safe bounds`() {
        val baseMinuteMs = epochMs(2026, Calendar.NOVEMBER, 14, 18, 0)
        val shifts = (0 until 16).map { minute ->
            screenOffPixelShiftDp(baseMinuteMs + minute * 60_000L)
        }

        shifts.forEach { (x, y) ->
            assertTrue("horizontal shift $x out of ±6 dp bounds", x in -6..6)
            assertTrue("vertical shift $y out of ±4 dp bounds", y in -4..4)
        }
        assertNotEquals(
            "adjacent minutes must shift position to prevent static OLED burn-in",
            shifts[0],
            shifts[1]
        )
        assertEquals(
            "orbit repeats cleanly across cycles",
            shifts[0],
            screenOffPixelShiftDp(baseMinuteMs + 16 * 60_000L)
        )
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `long track title wraps up to two lines without shifting the clock`() {
        val fixedNowMs = epochMs(2026, Calendar.NOVEMBER, 14, 18, 25)
        var trackTitle by mutableStateOf<String?>(null)

        composeTestRule.setContent {
            RadijasTheme {
                Box(Modifier.requiredSize(960.dp, 540.dp)) {
                    ScreenOffOverlay(
                        hintVisible = false,
                        trackTitle = trackTitle,
                        nowMs = { fixedNowMs }
                    )
                }
            }
        }

        val initialTop = composeTestRule.onNodeWithTag("screen-off-clock")
            .getUnclippedBoundsInRoot().top.value

        composeTestRule.runOnUiThread {
            trackTitle = "Atlikėjas ir dar vienas atlikėjas - labai ilgas kūrinio pavadinimas, kuris tikrai netelpa į vieną eilutę ir tęsiasi į antrą eilutę"
        }
        composeTestRule.waitForIdle()

        val layouts = mutableListOf<TextLayoutResult>()
        composeTestRule.onNodeWithTag("screen-off-track")
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals("long title should use two lines", 2, layouts.single().lineCount)
        assertEquals(
            "clock must stay at fixed vertical position when subtitle wraps to two lines",
            initialTop,
            composeTestRule.onNodeWithTag("screen-off-clock").getUnclippedBoundsInRoot().top.value,
            0.01f
        )
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `faint Lithuanian date is displayed below the clock and above the track title`() {
        val fixedNowMs = epochMs(2026, Calendar.NOVEMBER, 21, 18, 25)
        composeTestRule.setContent {
            RadijasTheme {
                Box(Modifier.requiredSize(960.dp, 540.dp)) {
                    ScreenOffOverlay(
                        hintVisible = true,
                        trackTitle = "DONNY MONTELL - LEISK MYLET",
                        nowMs = { fixedNowMs }
                    )
                }
            }
        }

        val dateNode = composeTestRule.onNodeWithTag("screen-off-date")
            .assertIsDisplayed()
            .assertTextEquals("Šeštadienis, lapkričio 21 d.")

        val dateLayouts = mutableListOf<TextLayoutResult>()
        dateNode.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(dateLayouts) }
        val dateStyle = dateLayouts.single().layoutInput.style
        assertEquals(OutfitFamily, dateStyle.fontFamily)
        assertTrue(
            "date color must stay faint (${dateStyle.color})",
            dateStyle.color != Color.Unspecified && dateStyle.color.alpha <= 0.40f
        )

        val clockBounds = composeTestRule.onNodeWithTag("screen-off-clock").getUnclippedBoundsInRoot()
        val dateBounds = dateNode.getUnclippedBoundsInRoot()
        val trackBounds = composeTestRule.onNodeWithTag("screen-off-track").getUnclippedBoundsInRoot()
        val hintBounds = composeTestRule.onNodeWithText("Ekranas patamsintas. Paspausk bet kurį mygtuką.")
            .getUnclippedBoundsInRoot()

        assertTrue(
            "date (${dateBounds.top}) must sit below clock (${clockBounds.bottom})",
            clockBounds.bottom.value < dateBounds.top.value
        )
        assertTrue(
            "track (${trackBounds.top}) must sit well below Nest Hub date (${dateBounds.bottom})",
            trackBounds.top.value - dateBounds.bottom.value >= 60f
        )
        assertTrue("date left (${dateBounds.left}) out of bounds", dateBounds.left.value >= 0f)
        assertTrue("date right (${dateBounds.right}) exceeds 960.dp", dateBounds.right.value <= 960f)
        assertTrue(
            "track (${trackBounds.bottom}) must stay above wake hint (${hintBounds.top})",
            trackBounds.bottom.value < hintBounds.top.value
        )
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `long date and two-line track stay vertically stable and clear the wake hint at fontScale 1_3`() {
        val longDateMs = epochMs(2026, Calendar.AUGUST, 27, 18, 25)
        var trackTitle by mutableStateOf<String?>(null)

        composeTestRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = baseDensity.density, fontScale = 1.3f)
            ) {
                RadijasTheme {
                    Box(Modifier.requiredSize(960.dp, 540.dp)) {
                        ScreenOffOverlay(
                            hintVisible = true,
                            trackTitle = trackTitle,
                            nowMs = { longDateMs }
                        )
                    }
                }
            }
        }

        val dateNode = composeTestRule.onNodeWithTag("screen-off-date")
            .assertIsDisplayed()
            .assertTextEquals("Ketvirtadienis, rugpjūčio 27 d.")

        val dateLayouts = mutableListOf<TextLayoutResult>()
        dateNode.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(dateLayouts) }
        assertEquals("long date must fit on a single line at fontScale 1.3", 1, dateLayouts.single().lineCount)

        val initialClockTop = composeTestRule.onNodeWithTag("screen-off-clock")
            .getUnclippedBoundsInRoot().top.value
        val initialDateTop = dateNode.getUnclippedBoundsInRoot().top.value

        composeTestRule.runOnUiThread {
            trackTitle = "Atlikėjas ir dar vienas atlikėjas - labai ilgas kūrinio pavadinimas, kuris tikrai netelpa į vieną eilutę ir tęsiasi į antrą eilutę"
        }
        composeTestRule.waitForIdle()

        assertEquals(
            initialClockTop,
            composeTestRule.onNodeWithTag("screen-off-clock").getUnclippedBoundsInRoot().top.value,
            0.01f
        )
        assertEquals(
            initialDateTop,
            composeTestRule.onNodeWithTag("screen-off-date").getUnclippedBoundsInRoot().top.value,
            0.01f
        )

        val trackBounds = composeTestRule.onNodeWithTag("screen-off-track").getUnclippedBoundsInRoot()
        val hintBounds = composeTestRule.onNodeWithText("Ekranas patamsintas. Paspausk bet kurį mygtuką.")
            .getUnclippedBoundsInRoot()
        assertTrue(
            "two-line track (${trackBounds.bottom}) must stay above wake hint (${hintBounds.top}) at fontScale 1.3",
            trackBounds.bottom.value < hintBounds.top.value
        )

        composeTestRule.runOnUiThread { trackTitle = null }
        composeTestRule.waitForIdle()

        assertEquals(
            initialClockTop,
            composeTestRule.onNodeWithTag("screen-off-clock").getUnclippedBoundsInRoot().top.value,
            0.01f
        )
        assertEquals(
            initialDateTop,
            composeTestRule.onNodeWithTag("screen-off-date").getUnclippedBoundsInRoot().top.value,
            0.01f
        )
    }

    @Test
    fun `ambient playing dot hides when both track title and station name are missing`() {
        val fixedNowMs = epochMs(2026, Calendar.NOVEMBER, 21, 18, 25)
        var stationName by mutableStateOf<String?>(null)

        composeTestRule.setContent {
            RadijasTheme {
                ScreenOffOverlay(
                    hintVisible = false,
                    trackTitle = null,
                    stationName = stationName,
                    nowMs = { fixedNowMs }
                )
            }
        }

        composeTestRule.onNodeWithTag("screen-off-playing-dot").assertDoesNotExist()

        composeTestRule.runOnUiThread { stationName = "Lietus" }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("screen-off-playing-dot").assertIsDisplayed()
    }
}
