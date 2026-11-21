package lt.radijas.tv

import android.content.Context
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import java.util.Calendar
import java.util.TimeZone
import lt.radijas.tv.data.model.RadioStation
import lt.radijas.tv.data.repository.RadioRepository
import lt.radijas.tv.ui.ClockDateText
import lt.radijas.tv.ui.HeroPanel
import lt.radijas.tv.ui.PlayerStatus
import lt.radijas.tv.ui.formatClockDate
import lt.radijas.tv.ui.theme.OutfitFamily
import lt.radijas.tv.ui.theme.RadijasTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
class HeroClockTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val stations: List<RadioStation> =
        RadioRepository(ApplicationProvider.getApplicationContext<Context>()).stations

    private fun epochMs(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long = Calendar.getInstance(timeZone).apply {
        set(year, month, day, hour, minute, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @Test
    fun `formats Lithuanian weekday, genitive month, day and 24h time`() {
        val vilnius = TimeZone.getTimeZone("Europe/Vilnius")

        val nov14 = formatClockDate(epochMs(2026, Calendar.NOVEMBER, 14, 18, 25, vilnius), vilnius)
        assertEquals(ClockDateText(time = "18:25", date = "Šeštadienis, lapkričio 14 d."), nov14)
        assertEquals("Šeštadienis", nov14.weekday)
        assertEquals("lapkričio 14 d.", nov14.monthDay)
        assertEquals(
            ClockDateText(time = "07:03", date = "Pirmadienis, sausio 5 d."),
            formatClockDate(epochMs(2026, Calendar.JANUARY, 5, 7, 3, vilnius), vilnius)
        )

        val expectedMonths = listOf(
            Calendar.JANUARY to "sausio",
            Calendar.FEBRUARY to "vasario",
            Calendar.MARCH to "kovo",
            Calendar.APRIL to "balandžio",
            Calendar.MAY to "gegužės",
            Calendar.JUNE to "birželio",
            Calendar.JULY to "liepos",
            Calendar.AUGUST to "rugpjūčio",
            Calendar.SEPTEMBER to "rugsėjo",
            Calendar.OCTOBER to "spalio",
            Calendar.NOVEMBER to "lapkričio",
            Calendar.DECEMBER to "gruodžio"
        )
        expectedMonths.forEach { (month, name) ->
            val formatted = formatClockDate(epochMs(2026, month, 10, 12, 0, vilnius), vilnius)
            assertTrue("month $month must map to $name: ${formatted.date}", formatted.date.contains(" $name 10 d."))
        }

        // 2026-11-09 through 2026-11-15 covers Monday through Sunday.
        val expectedWeekdays = listOf(
            9 to "Pirmadienis",
            10 to "Antradienis",
            11 to "Trečiadienis",
            12 to "Ketvirtadienis",
            13 to "Penktadienis",
            14 to "Šeštadienis",
            15 to "Sekmadienis"
        )
        expectedWeekdays.forEach { (day, weekday) ->
            val formatted = formatClockDate(epochMs(2026, Calendar.NOVEMBER, day, 9, 15, vilnius), vilnius)
            assertTrue("day $day must start with $weekday: ${formatted.date}", formatted.date.startsWith("$weekday, "))
        }
    }

    @Test
    fun `clock and date sit top-right in the 1080p hero without overlapping the screen-off button`() {
        val fixedNowMs = epochMs(2026, Calendar.NOVEMBER, 14, 18, 25)
        var currentStation by mutableStateOf(stations.first())

        composeTestRule.setContent {
            RadijasTheme(stationId = currentStation.id) {
                HeroPanel(
                    station = currentStation,
                    status = PlayerStatus.PLAYING,
                    modifier = Modifier.requiredSize(864.dp, 254.dp),
                    nowMs = { fixedNowMs }
                )
            }
        }

        stations.forEach { station ->
            composeTestRule.runOnUiThread { currentStation = station }
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("hero-clock").assertTextEquals("18:25")
            composeTestRule.onNodeWithTag("hero-date").assertTextEquals("Šeštadienis\nlapkričio 14 d.")

            val clock = composeTestRule.onNodeWithTag("hero-clock-container").getUnclippedBoundsInRoot()
            val button = composeTestRule.onNodeWithTag("screen-off-button").getUnclippedBoundsInRoot()

            assertTrue("${station.id}: clock top ${clock.top} must be inside hero", clock.top.value >= 0f)
            assertTrue("${station.id}: clock bottom ${clock.bottom} exceeds 254.dp", clock.bottom.value <= 254f)
            assertTrue("${station.id}: clock right ${clock.right} exceeds 864.dp", clock.right.value <= 864f)
            assertTrue(
                "${station.id}: clock top ${clock.top} must sit above screen-off button top ${button.top}",
                clock.top.value < button.top.value
            )
            assertTrue(
                "${station.id}: clock left ${clock.left} too close to button right ${button.right}",
                clock.left.value - button.right.value >= 48f
            )
        }
    }

    @Test
    fun `clock text uses tabular numerals and Outfit font`() {
        val fixedNowMs = epochMs(2026, Calendar.NOVEMBER, 14, 18, 25)
        val station = stations.first()

        composeTestRule.setContent {
            RadijasTheme(stationId = station.id) {
                HeroPanel(
                    station = station,
                    status = PlayerStatus.PLAYING,
                    modifier = Modifier.requiredSize(864.dp, 254.dp),
                    nowMs = { fixedNowMs }
                )
            }
        }

        val layouts = mutableListOf<TextLayoutResult>()
        composeTestRule.onNodeWithTag("hero-clock")
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue("clock text must be laid out", layouts.isNotEmpty())
        val style = layouts.single().layoutInput.style
        assertEquals("tnum", style.fontFeatureSettings)
        assertEquals(OutfitFamily, style.fontFamily)
        assertEquals(60.sp, style.fontSize)
        assertEquals(FontWeight.Normal, style.fontWeight)

        val dateLayouts = mutableListOf<TextLayoutResult>()
        composeTestRule.onNodeWithTag("hero-date")
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(dateLayouts) }
        val dateStyle = dateLayouts.single().layoutInput.style
        assertEquals(OutfitFamily, dateStyle.fontFamily)
        assertEquals(FontWeight.Normal, dateStyle.fontWeight)
    }

    @Test
    fun `screen-off pill aligns with equalizer column and date stacks right-aligned under clock`() {
        val fixedNowMs = epochMs(2026, Calendar.NOVEMBER, 14, 18, 25)
        val station = stations.first()

        composeTestRule.setContent {
            RadijasTheme(stationId = station.id) {
                HeroPanel(
                    station = station,
                    status = PlayerStatus.PLAYING,
                    modifier = Modifier.requiredSize(864.dp, 254.dp),
                    nowMs = { fixedNowMs }
                )
            }
        }

        val buttonBounds = composeTestRule
            .onNodeWithTag("screen-off-button")
            .getUnclippedBoundsInRoot()
        val equalizerBounds = composeTestRule
            .onNodeWithTag("hero-equalizer")
            .getUnclippedBoundsInRoot()
        assertEquals(
            "screen-off pill must align with the left hero column",
            equalizerBounds.left.value,
            buttonBounds.left.value,
            1.0f
        )

        val dateBounds = composeTestRule.onNodeWithTag("hero-date").getUnclippedBoundsInRoot()
        val clockBounds = composeTestRule.onNodeWithTag("hero-clock").getUnclippedBoundsInRoot()
        assertTrue(
            "date (${dateBounds.top}) must sit below clock (${clockBounds.bottom})",
            dateBounds.top.value > clockBounds.bottom.value
        )
        assertEquals(
            "date and clock must right-align inside the hero column",
            clockBounds.right.value,
            dateBounds.right.value,
            2.0f
        )
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `hero clock and screen-off control fit without overlapping at fontScale 1_3`() {
        // Long Lithuanian weekday + month name ("Ketvirtadienis\nrugpjūčio 27 d.").
        val longDateMs = epochMs(2026, Calendar.AUGUST, 27, 18, 25)
        val station = stations.first()

        composeTestRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = baseDensity.density, fontScale = 1.3f)
            ) {
                RadijasTheme(stationId = station.id) {
                    HeroPanel(
                        station = station,
                        status = PlayerStatus.PLAYING,
                        modifier = Modifier.requiredSize(864.dp, 254.dp),
                        nowMs = { longDateMs }
                    )
                }
            }
        }

        val button = composeTestRule.onNodeWithTag("screen-off-button").getUnclippedBoundsInRoot()
        val equalizer = composeTestRule.onNodeWithTag("hero-equalizer").getUnclippedBoundsInRoot()
        val clock = composeTestRule.onNodeWithTag("hero-clock-container").getUnclippedBoundsInRoot()

        assertTrue(
            "button right ${button.right} overlaps clock left ${clock.left} at fontScale 1.3",
            clock.left.value - button.right.value >= 24f
        )
        assertTrue(
            "button top ${button.top} overlaps equalizer bottom ${equalizer.bottom} at fontScale 1.3",
            button.top.value > equalizer.bottom.value
        )
        assertTrue("clock right ${clock.right} exceeds 864.dp at fontScale 1.3", clock.right.value <= 864f)
        assertTrue("clock bottom ${clock.bottom} exceeds 254.dp at fontScale 1.3", clock.bottom.value <= 254f)
        assertTrue("button bottom ${button.bottom} exceeds 254.dp at fontScale 1.3", button.bottom.value <= 254f)
    }
}
