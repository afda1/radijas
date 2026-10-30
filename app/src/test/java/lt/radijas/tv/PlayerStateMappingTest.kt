package lt.radijas.tv

import androidx.media3.common.Player
import lt.radijas.tv.player.PlayerState
import lt.radijas.tv.player.derivePlayerState
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins the one mapping every screen decision depends on: player facts -> what the hero is told.
 *
 * The bug these cases exist for: ExoPlayer keeps `playWhenReady` true while it suppresses audio
 * for a transient focus loss (Assistant, an alarm, TTS). "Gyvai" must follow what is audible
 * (`isPlaying`), never the intent to play - otherwise the screen says live over a silent TV.
 */
class PlayerStateMappingTest {

    @Test
    fun `ready and audible is Playing`() = assertEquals(
        PlayerState.Playing,
        derivePlayerState(Player.STATE_READY, playWhenReady = true, isPlaying = true, mediaItemCount = 1)
    )

    /**
     * The regression: playWhenReady stays true during transient audio-focus suppression, so the
     * old mapping reported Playing and the hero lied.
     */
    @Test
    fun `ready but suppressed is Paused even though playWhenReady is true`() = assertEquals(
        PlayerState.Paused,
        derivePlayerState(Player.STATE_READY, playWhenReady = true, isPlaying = false, mediaItemCount = 1)
    )

    @Test
    fun `ready and stopped is Paused`() = assertEquals(
        PlayerState.Paused,
        derivePlayerState(Player.STATE_READY, playWhenReady = false, isPlaying = false, mediaItemCount = 1)
    )

    @Test
    fun `buffering while trying is Connecting`() = assertEquals(
        PlayerState.Connecting,
        derivePlayerState(Player.STATE_BUFFERING, playWhenReady = true, isPlaying = false, mediaItemCount = 1)
    )

    @Test
    fun `buffering while not trying is Paused`() = assertEquals(
        PlayerState.Paused,
        derivePlayerState(Player.STATE_BUFFERING, playWhenReady = false, isPlaying = false, mediaItemCount = 1)
    )

    /** A live radio never ends: STATE_ENDED means the server dropped the connection. */
    @Test
    fun `ended live stream while trying is Connecting`() = assertEquals(
        PlayerState.Connecting,
        derivePlayerState(Player.STATE_ENDED, playWhenReady = true, isPlaying = false, mediaItemCount = 1)
    )

    @Test
    fun `idle with nothing queued is Idle`() = assertEquals(
        PlayerState.Idle,
        derivePlayerState(Player.STATE_IDLE, playWhenReady = false, isPlaying = false, mediaItemCount = 0)
    )

    @Test
    fun `idle with an item and intent to play is Connecting`() = assertEquals(
        PlayerState.Connecting,
        derivePlayerState(Player.STATE_IDLE, playWhenReady = true, isPlaying = false, mediaItemCount = 1)
    )
}
