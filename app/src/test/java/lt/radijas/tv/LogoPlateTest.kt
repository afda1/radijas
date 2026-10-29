package lt.radijas.tv

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import lt.radijas.tv.data.repository.RadioRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Contract test for the logo plate flag.
 *
 * The flag is derived from the mean luminance of each logo asset's own opaque pixels, not from the
 * brand colour and not from a visual preference. If the artwork changes, re-derive the flag from
 * the new asset rather than editing these lists to match the code.
 *
 * Catalogue ids are not always the asset suffix: "rock" carries ic_station_rockfm and "plus"
 * carries ic_station_m1plus.
 */
@RunWith(RobolectricTestRunner::class)
class LogoPlateTest {
    private val stations =
        RadioRepository(ApplicationProvider.getApplicationContext<Context>()).stations

    @Test
    fun `the four dark logos get a light plate`() {
        val plated = stations.filter { it.logoOnLightPlate }.map { it.id }.toSet()
        assertEquals(
            "plated ids must be the artwork at or below 44/255 mean luminance",
            setOf("lietus", "rock", "plus", "rc"),
            plated
        )
    }

    @Test
    fun `the six readable logos keep the dark scrim`() {
        val unplated = stations.filterNot { it.logoOnLightPlate }.map { it.id }.toSet()
        assertEquals(
            "unplated ids must be the artwork at or above 53/255 mean luminance",
            setOf("zip", "powerhit", "powerhitgold", "relax", "m1", "goldfm"),
            unplated
        )
    }
}
