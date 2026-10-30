package lt.radijas.tv

import lt.radijas.tv.ui.PlayerStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins what OK on a station tile does, because it is the one press this audience makes by
 * accident and the one press that must be safe.
 *
 * The regression: pressing OK on the station that was already playing fell through to "select"
 * and reopened the stream - a 1-3 s silence and a blanked title, while the design spec (§6.3)
 * says OK on a playing station does nothing at all.
 */
class StationSelectionTest {

    @Test
    fun `ok on the playing station does nothing`() = assertEquals(
        StationAction.IGNORE,
        stationAction(selectedId = "m1", currentId = "m1", status = PlayerStatus.PLAYING)
    )

    @Test
    fun `ok on another station switches`() = assertEquals(
        StationAction.SELECT,
        stationAction(selectedId = "zip", currentId = "m1", status = PlayerStatus.PLAYING)
    )

    /** Spec 6.2: OK on the failing tile retries it immediately, without waiting for the backoff. */
    @Test
    fun `ok on the failed station retries it`() {
        assertEquals(
            StationAction.RETRY,
            stationAction(selectedId = "m1", currentId = "m1", status = PlayerStatus.NO_CONNECTION)
        )
        assertEquals(
            StationAction.RETRY,
            stationAction(selectedId = "m1", currentId = "m1", status = PlayerStatus.RECONNECTING)
        )
    }

    /** „Pristabdyta. Paspausk OK." is a promise: the press must start playback again. */
    @Test
    fun `ok on a paused station retries it`() = assertEquals(
        StationAction.RETRY,
        stationAction(selectedId = "m1", currentId = "m1", status = PlayerStatus.PAUSED)
    )

    /** Any other tile still switches during an outage: the user is not locked into the dead one. */
    @Test
    fun `ok on another station during an outage switches`() = assertEquals(
        StationAction.SELECT,
        stationAction(selectedId = "zip", currentId = "m1", status = PlayerStatus.NO_CONNECTION)
    )

    @Test
    fun `ok before a station is chosen switches`() = assertEquals(
        StationAction.SELECT,
        stationAction(selectedId = "m1", currentId = null, status = PlayerStatus.CONNECTING)
    )
}
