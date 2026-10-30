package lt.radijas.tv

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import lt.radijas.tv.data.repository.RadioRepository
import lt.radijas.tv.ui.theme.LogoInkTarget
import lt.radijas.tv.ui.theme.stationLogoWidthFraction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The carousel must not make one station's mark five times heavier than another's. This pins the
 * optical normalisation: either the mark is shrunk so its ink area matches [LogoInkTarget], or it
 * is a wide wordmark already capped by the box width.
 */
@RunWith(RobolectricTestRunner::class)
class StationLogoScaleTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val stations = RadioRepository(context).stations
    private val boxWidth = 156f

    @Test
    fun `every station has a logo scale`() {
        assertEquals(stations.map { it.id }.toSet(), stationLogoWidthFraction.keys)
        stationLogoWidthFraction.values.forEach { fraction ->
            assertTrue("fraction $fraction must be within the box", fraction in 0.1f..1f)
        }
    }

    @Test
    fun `logo ink is equalised, or the mark is a width-capped wordmark`() {
        stations.forEach { station ->
            val bitmap = BitmapFactory.decodeResource(context.resources, station.iconResId)
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val ink = pixels.count { (it ushr 24) > 8 }

            val fraction = stationLogoWidthFraction.getValue(station.id)
            val renderedWidth = boxWidth * fraction
            val scale = renderedWidth / bitmap.width
            val renderedInk = ink * scale * scale

            if (fraction < 1f) {
                assertTrue(
                    "${station.id}: rendered ink $renderedInk, target $LogoInkTarget",
                    renderedInk in 0.85f * LogoInkTarget..1.15f * LogoInkTarget
                )
            } else {
                assertTrue(
                    "${station.id} keeps full width only because it is a wide wordmark",
                    bitmap.width >= bitmap.height
                )
            }
        }
    }
}
