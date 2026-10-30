package lt.radijas.tv

import kotlinx.coroutines.runBlocking
import lt.radijas.tv.player.PowerMetadataClient
import lt.radijas.tv.player.parsePowerTrack
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PowerMetadataTest {
    private val now = 1_793_368_800_000L // 2026-10-30 16:00 Europe/Vilnius

    @Test fun `power track is parsed from structured fields`() {
        assertEquals("KATSEYE - Animal", parsePowerTrack(
            """{"artistName":"KATSEYE","songTitle":"Animal","date":"2026-10-30 15:59:00"}""", now
        ))
    }

    @Test fun `stale future and malformed power metadata are hidden`() {
        for (date in listOf("2020-01-01 10:00:00", "2099-01-01 10:00:00", "no date")) {
            assertNull(parsePowerTrack("""{"artistName":"A","songTitle":"B","date":"$date"}""", now))
        }
        assertNull(parsePowerTrack("not json", now))
        assertNull(parsePowerTrack("""{"song":"Jingle"}""", now))
    }

    @Test fun `metadata client does not query other stations`() = runBlocking {
        assertNull(PowerMetadataClient().powerTitle("lietus"))
        assertNull(PowerMetadataClient().powerTitle("ehr"))
    }
}
