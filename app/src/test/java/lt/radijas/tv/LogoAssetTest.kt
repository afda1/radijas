package lt.radijas.tv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import lt.radijas.tv.data.repository.RadioRepository
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Contract test for the station logo assets.
 *
 * The brand colour is carried by the tile and hero surfaces; the artwork itself is a plain white
 * silhouette (see the conversion note in the task history). This decodes every catalogue asset at
 * its real pixel size and fails on the first station whose visible pixels are not pure white, so a
 * coloured or off-white asset cannot quietly return.
 *
 * Threshold: alpha > 32. Pixels at or below that are the antialiased rim of the artwork, which is
 * blended at draw time anyway; the test pins the ink, not the fringe.
 */
@RunWith(RobolectricTestRunner::class)
class LogoAssetTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val stations = RadioRepository(context).stations

    @Test
    fun `every visible logo pixel is pure white`() {
        stations.forEach { station ->
            val bitmap = BitmapFactory.decodeResource(context.resources, station.iconResId)
            requireNotNull(bitmap) { "${station.id}: ${station.name} logo failed to decode" }

            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

            var visible = 0
            var nonWhite = 0
            var firstOffender = ""
            pixels.forEachIndexed { index, pixel ->
                val alpha = (pixel ushr 24) and 0xFF
                if (alpha <= 32) return@forEachIndexed
                visible++
                if ((pixel and 0x00FFFFFF) != 0x00FFFFFF) {
                    nonWhite++
                    if (firstOffender.isEmpty()) {
                        val x = index % bitmap.width
                        val y = index / bitmap.width
                        firstOffender = "first at (x=$x, y=$y): #%08X".format(pixel)
                    }
                }
            }

            val where = "${station.id} (${station.name}, ${bitmap.width}x${bitmap.height})"
            assertTrue("$where has no visible pixels", visible > 0)
            assertTrue(
                "$where has $nonWhite of $visible visible pixels that are not pure white; " +
                    firstOffender,
                nonWhite == 0
            )
            bitmap.recycle()
        }
    }
}
