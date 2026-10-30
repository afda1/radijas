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
 * 1. Playing is playing, even if the silence counter has not been reset yet.
 * 2. A paused player is not audible, so it is reported as paused - whatever `playWhenReady` says.
 *    ExoPlayer keeps the intent to play while it suppresses audio for a transient audio-focus
 *    loss, and the hero must not keep saying „Gyvai" over a silent set.
 * 3. An idle player that is not trying to play was stopped on purpose; calling that silence a
 *    connection attempt would leave „Jungiamasi…" on screen forever with nothing connecting.
 * 4. Silence past the threshold is a failure the retry loop has not fixed yet.
 * 5. Below the threshold, only a stream that has already played counts as a reconnection. A first
 *    connection may buffer for a while, but it is still connecting, not "trying again".
 * 6. A player error below the threshold is not a final failure: the service keeps retrying, so
 *    the hero must not blink to the failure copy on every attempt (spec 6.2: the failure copy
 *    appears after ~30 s of failure).
 */
fun playerStatus(
    playerState: PlayerState,
    playWhenReady: Boolean,
    silentForMs: Long,
    reconnecting: Boolean
): PlayerStatus = when {
    playerState is PlayerState.Playing -> PlayerStatus.PLAYING
    playerState is PlayerState.Paused -> PlayerStatus.PAUSED
    playerState is PlayerState.Idle && !playWhenReady -> PlayerStatus.PAUSED
    silentForMs >= NO_CONNECTION_AFTER_MS -> PlayerStatus.NO_CONNECTION
    playerState is PlayerState.Error && reconnecting -> PlayerStatus.RECONNECTING
    silentForMs > 0 && reconnecting -> PlayerStatus.RECONNECTING
    else -> PlayerStatus.CONNECTING
}
