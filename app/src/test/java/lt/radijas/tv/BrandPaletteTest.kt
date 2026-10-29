package lt.radijas.tv

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import lt.radijas.tv.data.model.RadioStation
import lt.radijas.tv.data.repository.RadioRepository
import lt.radijas.tv.ui.theme.HeroCoreX
import lt.radijas.tv.ui.theme.RadijasContent
import lt.radijas.tv.ui.theme.heroSurface
import lt.radijas.tv.ui.theme.onBrand
import lt.radijas.tv.ui.theme.tonalSurface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

@RunWith(RobolectricTestRunner::class)
class BrandPaletteTest {

    // Taken from the real catalogue so a new station cannot silently escape the contrast check.
    private val catalogue: List<RadioStation> =
        RadioRepository(ApplicationProvider.getApplicationContext<Context>()).stations

    private val stationBrands: List<Long> = catalogue.map { it.brandColor }

    @Test
    fun `zero amount returns base surface`() =
        assertEquals(Color(0xFF141218), tonalSurface(0xFF0E8FD1, 0f))

    @Test
    fun `full amount returns brand color`() =
        assertEquals(Color(0xFF0E8FD1), tonalSurface(0xFF0E8FD1, 1f))

    @Test
    fun `mix is between the two endpoints in every channel`() {
        val base = Color(0xFF141218)
        val brand = Color(0xFF0E8FD1)
        val mixed = tonalSurface(0xFF0E8FD1, 0.5f)
        // min/max, not base/brand: the brand's red (0x0E) is darker than the base's (0x14),
        // so a plain "mixed.red > base.red" assertion could never hold for this brand.
        assertTrue(mixed.red > min(base.red, brand.red) && mixed.red < max(base.red, brand.red))
        assertTrue(mixed.green > min(base.green, brand.green) && mixed.green < max(base.green, brand.green))
        assertTrue(mixed.blue > min(base.blue, brand.blue) && mixed.blue < max(base.blue, brand.blue))
    }

    @Test
    fun `every station brand has readable text`() {
        assertEquals("the catalogue must still have ten stations", 10, stationBrands.size)
        stationBrands.forEach { brand ->
            val contrast = contrastRatio(onBrand(brand), tonalSurface(brand, 0.34f))
            assertTrue("contrast $contrast for $brand", contrast >= 4.5)
        }
    }

    /**
     * The hero is a picture, so its contrast was previously judged by eye and shipped wrong: the
     * radial core used to sit at the hero's centre, inside the text column, and the 26 sp track
     * line fell to 2.8:1 on M-1. This pins the whole text zone instead: every point the hero's
     * copy can cover, for every station, at the two opacities the copy is drawn with.
     *
     * x stops at 0.78 because the text column ends there (48 dp safe area + 44 dp hero padding
     * + 608 dp column = 700 dp of the 864 dp hero = 0.755, plus rounding). The glow's core sits
     * at [HeroCoreX] = 0.90, under the icon, i.e. outside this range by construction.
     */
    @Test
    fun `hero copy keeps 4_5 to 1 over the whole text column`() {
        catalogue.forEach { station ->
            for (xStep in 0..39) {
                val x = xStep * 0.02f
                for (yStep in 1..10) {
                    val y = yStep * 0.10f
                    val surface = heroSurface(station.brandColor, x, y)

                    val status = contrastRatio(RadijasContent, surface)
                    assertTrue(
                        "${station.name} (${hex(station.brandColor)}): status copy at " +
                            "x=${"%.2f".format(x)} y=${"%.2f".format(y)} is only $status:1",
                        status >= 4.5
                    )

                    val track = contrastRatio(whiteAt(0.66f, surface), surface)
                    assertTrue(
                        "${station.name} (${hex(station.brandColor)}): 26 sp track line at " +
                            "x=${"%.2f".format(x)} y=${"%.2f".format(y)} is only $track:1",
                        track >= 4.5
                    )
                }
            }
        }
    }

    @Test
    fun `hero glow core sits under the station icon, outside the text column`() {
        assertTrue(
            "core x=$HeroCoreX must be right of the text column's 0.755",
            HeroCoreX >= 0.755f
        )
    }

    private fun hex(brand: Long): String = "#%06X".format(brand and 0xFFFFFF)

    /** White at [alpha], composited over [background] the way Compose draws a translucid line. */
    private fun whiteAt(alpha: Float, background: Color): Color = Color(
        red = alpha + (1f - alpha) * background.red,
        green = alpha + (1f - alpha) * background.green,
        blue = alpha + (1f - alpha) * background.blue
    )

    /** WCAG 2.1 relative luminance of one sRGB channel. */
    private fun linearize(channel: Float): Double {
        val value = channel.toDouble()
        return if (value <= 0.03928) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
    }

    private fun contrastRatio(foreground: Color, background: Color): Double {
        fun luminance(color: Color): Double =
            0.2126 * linearize(color.red) +
                0.7152 * linearize(color.green) +
                0.0722 * linearize(color.blue)

        val first = luminance(foreground)
        val second = luminance(background)
        return (max(first, second) + 0.05) / (min(first, second) + 0.05)
    }
}
