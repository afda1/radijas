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

class PlayerStatusTest {
    @Test
    fun `playing maps to PLAYING`() =
        assertEquals(PLAYING, playerStatus(PlayerState.Playing, true, 0))

    @Test
    fun `buffering while playing maps to CONNECTING`() =
        assertEquals(CONNECTING, playerStatus(PlayerState.Connecting, true, 0))

    @Test
    fun `failure maps to NO_CONNECTION immediately`() =
        assertEquals(NO_CONNECTION, playerStatus(PlayerState.Error("x"), true, 0))

    @Test
    fun `long silence maps to NO_CONNECTION`() =
        assertEquals(NO_CONNECTION, playerStatus(PlayerState.Connecting, true, 31_000))

    @Test
    fun `short silence still retrying`() =
        assertEquals(RECONNECTING, playerStatus(PlayerState.Connecting, true, 5_000))

    @Test
    fun `paused while playWhenReady false is PAUSED`() =
        assertEquals(PAUSED, playerStatus(PlayerState.Paused, false, 0))

    @Test
    fun `idle is CONNECTING`() =
        assertEquals(CONNECTING, playerStatus(PlayerState.Idle, true, 0))
}
