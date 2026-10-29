package lt.radijas.tv

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import lt.radijas.tv.data.repository.RadioRepository
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
    private val stationBrands: List<Long> =
        RadioRepository(ApplicationProvider.getApplicationContext<Context>())
            .stations.map { it.brandColor }

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
