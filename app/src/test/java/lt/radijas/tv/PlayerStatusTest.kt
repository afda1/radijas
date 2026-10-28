package lt.radijas.tv

import lt.radijas.tv.player.PlayerState
import lt.radijas.tv.ui.PlayerStatus.CONNECTING
import lt.radijas.tv.ui.PlayerStatus.NO_CONNECTION
import lt.radijas.tv.ui.PlayerStatus.PAUSED
import lt.radijas.tv.ui.PlayerStatus.PLAYING
import lt.radijas.tv.ui.PlayerStatus.RECONNECTING
import lt.radijas.tv.ui.playerStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Each test pins one precedence rule. The "wins over long silence" cases exist so that moving
 * `Playing`/`Paused` below the silence rules fails here instead of on a TV.
 */
class PlayerStatusTest {
    @Test
    fun `playing maps to PLAYING`() =
        assertEquals(PLAYING, playerStatus(PlayerState.Playing, true, 0))

    @Test
    fun `buffering while playing maps to CONNECTING`() =
        assertEquals(CONNECTING, playerStatus(PlayerState.Connecting, true, 0))

    @Test
    fun `failure with no silence yet still looks like retrying`() =
        assertEquals(RECONNECTING, playerStatus(PlayerState.Error("x"), true, 0))

    @Test
    fun `failure before the threshold still looks like retrying`() =
        assertEquals(RECONNECTING, playerStatus(PlayerState.Error("x"), true, 5_000))

    @Test
    fun `failure after long silence is NO_CONNECTION`() =
        assertEquals(NO_CONNECTION, playerStatus(PlayerState.Error("x"), true, 31_000))

    @Test
    fun `long silence maps to NO_CONNECTION`() =
        assertEquals(NO_CONNECTION, playerStatus(PlayerState.Connecting, true, 31_000))

    @Test
    fun `silence exactly at the threshold is NO_CONNECTION`() =
        assertEquals(NO_CONNECTION, playerStatus(PlayerState.Connecting, true, 30_000))

    @Test
    fun `short silence still retrying`() =
        assertEquals(RECONNECTING, playerStatus(PlayerState.Connecting, true, 5_000))

    @Test
    fun `paused while playWhenReady false is PAUSED`() =
        assertEquals(PAUSED, playerStatus(PlayerState.Paused, false, 0))

    @Test
    fun `idle is CONNECTING`() =
        assertEquals(CONNECTING, playerStatus(PlayerState.Idle, true, 0))

    @Test
    fun `playing wins over long silence`() =
        assertEquals(PLAYING, playerStatus(PlayerState.Playing, true, 31_000))

    @Test
    fun `paused wins over long silence`() =
        assertEquals(PAUSED, playerStatus(PlayerState.Paused, false, 31_000))
}
