package lt.radijas.tv

import androidx.test.core.app.ApplicationProvider
import lt.radijas.tv.data.repository.RadioRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StartupStationTest {

    @Test
    fun `first launch plays lietus`() {
        val repo = RadioRepository(ApplicationProvider.getApplicationContext())
        assertEquals("lietus", StartupStation.resolve(repo).id)
    }

    @Test
    fun `second launch plays last station`() {
        val repo = RadioRepository(ApplicationProvider.getApplicationContext())
        repo.setLastStationId("zip")
        assertEquals("zip", StartupStation.resolve(repo).id)
    }

    @Test
    fun `unknown saved id falls back to lietus`() {
        val repo = RadioRepository(ApplicationProvider.getApplicationContext())
        repo.setLastStationId("deleted-station")
        assertEquals("lietus", StartupStation.resolve(repo).id)
    }
}
