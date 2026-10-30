package lt.radijas.tv.player

import androidx.media3.common.Player

/**
 * Maps the facts a player reports to the single state the screen is told.
 *
 * Pure on purpose (no `Player`, no Android): every status the hero shows is decided by this
 * function, so the precedence is unit-tested instead of discovered on a television.
 *
 * `isPlaying` - not `playWhenReady` - decides whether playback is "playing". ExoPlayer keeps
 * `playWhenReady` true while it suppresses audio for a transient audio-focus loss (Google
 * Assistant, TTS, an alarm, a call), and during that suppression nothing is audible. Reading the
 * intent to play instead of the fact produced a screen that said „Gyvai" over a silent TV, which
 * is the one state this app promises never to show (README, design spec §6.3).
 *
 * A player error is not handled here: its user-facing message is a string resource, so the caller
 * resolves it first and asks this function only for the non-error mapping.
 */
internal fun derivePlayerState(
    playbackState: Int,
    playWhenReady: Boolean,
    isPlaying: Boolean,
    mediaItemCount: Int
): PlayerState = when (playbackState) {
    Player.STATE_READY -> if (isPlaying) PlayerState.Playing else PlayerState.Paused
    // A live radio never ends: STATE_ENDED means the server dropped the connection, and the
    // service reconnects. Without the intent to play there is nothing to reconnect, hence Paused.
    Player.STATE_BUFFERING, Player.STATE_ENDED ->
        if (playWhenReady) PlayerState.Connecting else PlayerState.Paused
    else -> when {
        mediaItemCount == 0 -> PlayerState.Idle
        playWhenReady -> PlayerState.Connecting
        else -> PlayerState.Paused
    }
}
