package lt.radijas.tv

import androidx.test.core.app.ApplicationProvider
import lt.radijas.tv.data.repository.RadioRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StationCatalogTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun `stations are displayed alphabetically`() {
        assertEquals(
            listOf("ehr", "goldfm", "lietus", "m1", "plus", "powerhit", "powerhitgold", "rc", "relax", "rock", "zip"),
            RadioRepository(context).stations.map { it.id }
        )
    }

    @Test
    fun `ids are unique`() {
        val stations = RadioRepository(context).stations
        assertEquals(stations.size, stations.map { it.id }.toSet().size)
    }

    @Test
    fun `all streams are https`() {
        RadioRepository(context).stations.forEach { assertTrue(it.streamUrl, it.streamUrl.startsWith("https://")) }
    }

    @Test
    fun `no expiring revma tokens`() {
        RadioRepository(context).stations.forEach { assertFalse(it.streamUrl, it.streamUrl.contains("rj-tok")) }
    }

    @Test
    fun `there are eleven stations and lietus is one of them`() {
        val stations = RadioRepository(context).stations
        assertEquals(11, stations.size)
        assertEquals("Lietus", stations.first { it.id == "lietus" }.name)
    }

    @Test
    fun `european hit radio stays in the catalogue`() {
        val station = RadioRepository(context).stations.first { it.id == "ehr" }
        assertEquals("European Hit Radio", station.name)
        assertEquals("https://stream.ehr.lt:8443/ehr", station.streamUrl)
    }
}
