package lt.radijas.tv.ui

import lt.radijas.tv.player.PlayerState

/** What the hero tells the viewer about playback. Mirrors the five states in the design spec. */
enum class PlayerStatus {
    CONNECTING,
    PLAYING,
    RECONNECTING,
    NO_CONNECTION,
    PAUSED
}

/** Silence longer than this is no longer "trying again" - the hero admits it cannot connect. */
const val NO_CONNECTION_AFTER_MS = 30_000L

/**
 * Maps the player's state to what the screen shows.
 *
 * Pure on purpose: no Android imports, no clock, no coroutines, so the precedence below is
 * unit-tested instead of discovered by staring at a TV.
 *
 * Order matters:
 * 1. A player error is a failure right now, regardless of how long it has been silent.
 * 2. Playing is playing, even if the silence counter has not been reset yet.
 * 3. Paused without intent to play means something else took the audio focus.
 * 4. Silence past the threshold is a failure the retry loop has not fixed yet.
 * 5. Any shorter silence means the retry loop is still working.
 */
fun playerStatus(
    playerState: PlayerState,
    playWhenReady: Boolean,
    silentForMs: Long
): PlayerStatus = when {
    playerState is PlayerState.Error -> PlayerStatus.NO_CONNECTION
    playerState is PlayerState.Playing -> PlayerStatus.PLAYING
    playerState is PlayerState.Paused && !playWhenReady -> PlayerStatus.PAUSED
    silentForMs >= NO_CONNECTION_AFTER_MS -> PlayerStatus.NO_CONNECTION
    silentForMs > 0 -> PlayerStatus.RECONNECTING
    else -> PlayerStatus.CONNECTING
}
