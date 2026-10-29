package lt.radijas.tv

import lt.radijas.tv.player.PlayerState
import lt.radijas.tv.ui.PlayerStatus.CONNECTING
import lt.radijas.tv.ui.PlayerStatus.NO_CONNECTION
import lt.radijas.tv.ui.PlayerStatus.PAUSED
import lt.radijas.tv.ui.PlayerStatus.PLAYING
import lt.radijas.tv.ui.PlayerStatus.RECONNECTING
import lt.radijas.tv.ui.SilenceClock
import lt.radijas.tv.ui.playerStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Each test pins one precedence rule. The "wins over long silence" cases exist so that moving
 * `Playing`/`Paused` below the silence rules fails here instead of on a TV.
 *
 * `reconnecting` is the difference between a stream that went quiet after playing ("Bandau
 * vėl…") and one that has not connected yet ("Jungiamasi…"). Both can be silent for the same
 * amount of time, so the mapping cannot infer it from `silentForMs` alone.
 */
class PlayerStatusTest {
    @Test
    fun `playing maps to PLAYING`() =
        assertEquals(PLAYING, playerStatus(PlayerState.Playing, true, 0, reconnecting = true))

    @Test
    fun `buffering while playing maps to CONNECTING`() =
        assertEquals(CONNECTING, playerStatus(PlayerState.Connecting, true, 0, reconnecting = false))

    @Test
    fun `failure with no silence yet still looks like retrying`() =
        assertEquals(RECONNECTING, playerStatus(PlayerState.Error("x"), true, 0, reconnecting = true))

    @Test
    fun `failure before the threshold still looks like retrying`() =
        assertEquals(RECONNECTING, playerStatus(PlayerState.Error("x"), true, 5_000, reconnecting = true))

    @Test
    fun `failure after long silence is NO_CONNECTION`() =
        assertEquals(NO_CONNECTION, playerStatus(PlayerState.Error("x"), true, 31_000, reconnecting = true))

    @Test
    fun `long silence maps to NO_CONNECTION`() =
        assertEquals(NO_CONNECTION, playerStatus(PlayerState.Connecting, true, 31_000, reconnecting = true))

    @Test
    fun `silence exactly at the threshold is NO_CONNECTION`() =
        assertEquals(NO_CONNECTION, playerStatus(PlayerState.Connecting, true, 30_000, reconnecting = true))

    @Test
    fun `short silence still retrying`() =
        assertEquals(RECONNECTING, playerStatus(PlayerState.Connecting, true, 5_000, reconnecting = true))

    @Test
    fun `paused while playWhenReady false is PAUSED`() =
        assertEquals(PAUSED, playerStatus(PlayerState.Paused, false, 0, reconnecting = true))

    @Test
    fun `idle is CONNECTING`() =
        assertEquals(CONNECTING, playerStatus(PlayerState.Idle, true, 0, reconnecting = false))

    @Test
    fun `playing wins over long silence`() =
        assertEquals(PLAYING, playerStatus(PlayerState.Playing, true, 31_000, reconnecting = true))

    @Test
    fun `paused wins over long silence`() =
        assertEquals(PAUSED, playerStatus(PlayerState.Paused, false, 31_000, reconnecting = true))

    // A first connection may take a while (cold start, slow station). Until it has played once,
    // its silence is a buffer, not a lost connection, so the hero must keep saying "Jungiamasi…".
    @Test
    fun `a fresh station buffering is CONNECTING`() =
        assertEquals(CONNECTING, playerStatus(PlayerState.Connecting, true, 5_000, reconnecting = false))

    @Test
    fun `a fresh station that errors is still CONNECTING`() =
        assertEquals(CONNECTING, playerStatus(PlayerState.Error("x"), true, 5_000, reconnecting = false))

    // ...but a station that never connects must still admit failure after the 30 s threshold.
    @Test
    fun `a fresh station that never connects is NO_CONNECTION at the threshold`() {
        assertEquals(NO_CONNECTION, playerStatus(PlayerState.Connecting, true, 30_000, reconnecting = false))
        assertEquals(NO_CONNECTION, playerStatus(PlayerState.Error("x"), true, 31_000, reconnecting = false))
    }

    /**
     * The regression itself: the silence clock used to keep counting from launch (or from the
     * previous station) because it was only reset while `Playing`. Selecting a station must reset
     * it, so the new station's first buffer maps to CONNECTING even after a second.
     */
    @Test
    fun `selecting a station resets the clock so the first buffer is CONNECTING`() {
        var now = 0L
        val clock = SilenceClock(nowMs = { now })

        // The previous station had been playing for a minute when the user switched.
        clock.onState(PlayerState.Playing)
        now = 60_000
        clock.restart()
        now = 61_000
        assertTrue("a new attempt must keep ticking", clock.onState(PlayerState.Connecting))
        clock.tick()

        assertEquals(1_000, clock.silentForMs)
        assertFalse("a new station has not played yet, so it cannot be reconnecting", clock.reconnecting)
        assertEquals(
            CONNECTING,
            playerStatus(PlayerState.Connecting, true, clock.silentForMs, clock.reconnecting)
        )
    }

    /** A stream that played and then went quiet is a real reconnection, and still fails once at 30 s. */
    @Test
    fun `a lost connection is RECONNECTING and then NO_CONNECTION that stays`() {
        var now = 0L
        val clock = SilenceClock(nowMs = { now })
        clock.restart()
        clock.onState(PlayerState.Playing)

        now = 10_000
        assertTrue(clock.onState(PlayerState.Connecting))
        now = 12_000
        clock.tick()
        assertTrue(clock.reconnecting)
        assertEquals(2_000, clock.silentForMs)
        assertEquals(
            RECONNECTING,
            playerStatus(PlayerState.Connecting, true, clock.silentForMs, clock.reconnecting)
        )

        // The service's retries must not push the clock forward, or the failure copy would
        // oscillate back to "Bandau vėl…" instead of appearing once and staying.
        now = 20_000
        clock.onState(PlayerState.Error("x"))
        clock.tick()
        assertEquals(10_000, clock.silentForMs)

        now = 40_000
        clock.onState(PlayerState.Error("x"))
        clock.tick()
        assertEquals(
            NO_CONNECTION,
            playerStatus(PlayerState.Error("x"), true, clock.silentForMs, clock.reconnecting)
        )

        now = 45_000
        clock.onState(PlayerState.Connecting)
        clock.tick()
        assertEquals(
            NO_CONNECTION,
            playerStatus(PlayerState.Connecting, true, clock.silentForMs, clock.reconnecting)
        )
    }

    /** A first attempt that never succeeds still fails at 30 s instead of buffering forever. */
    @Test
    fun `a first attempt that never connects fails at the threshold`() {
        var now = 0L
        val clock = SilenceClock(nowMs = { now })
        clock.restart()
        clock.onState(PlayerState.Connecting)

        now = 29_999
        clock.tick()
        assertEquals(
            CONNECTING,
            playerStatus(PlayerState.Connecting, true, clock.silentForMs, clock.reconnecting)
        )

        now = 30_000
        clock.tick()
        assertEquals(
            NO_CONNECTION,
            playerStatus(PlayerState.Connecting, true, clock.silentForMs, clock.reconnecting)
        )
    }
}
