package lt.radijas.tv.ui

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import lt.radijas.tv.player.PlayerState

/**
 * How long the current playback attempt has been silent, and whether that silence means a *lost*
 * connection or a first connection still in flight.
 *
 * [playerStatus] needs both answers because the same silence is two different things on screen: a
 * cold start that is still buffering is „Jungiamasi…", while a stream that played and then went
 * quiet is „Bandau vėl…". Only this clock knows which one it is.
 *
 * Time is read through [nowMs] so tests can drive it without sleeping.
 */
class SilenceClock(private val nowMs: () -> Long = SystemClock::elapsedRealtime) {
    /** Time since the current attempt started, or since playback was last audible. */
    var silentForMs by mutableLongStateOf(0L)
        private set

    /** True once the current attempt has been audible, i.e. silence now means a reconnection. */
    var reconnecting by mutableStateOf(false)
        private set

    private var silentSinceMs = nowMs()

    // Transient: the state we saw last was Playing. Used only to stamp the moment silence began;
    // it must not be re-applied on later Connecting/Error events, or the 30 s failure copy would
    // never arrive (the retry loop emits those states continuously).
    private var wasPlaying = false

    /**
     * A new attempt starts here: launch, station selection, and OK on a dead station.
     *
     * Resetting is what stops the previous station's silence - and the previous station's
     * `reconnecting` - from leaking into the new one. Without it a fresh station's very first
     * buffer is reported as „Bandau vėl…" after one tick.
     */
    fun restart() {
        silentSinceMs = nowMs()
        silentForMs = 0L
        wasPlaying = false
        reconnecting = false
    }

    /**
     * Applies a player state and returns whether the caller should keep ticking: Playing and Idle
     * are not silence, so they need no one-second loop.
     */
    fun onState(state: PlayerState): Boolean = when (state) {
        is PlayerState.Playing -> {
            // The attempt has been audible at least once; any later silence is a reconnect.
            wasPlaying = true
            reconnecting = true
            silentSinceMs = nowMs()
            silentForMs = 0L
            false
        }
        is PlayerState.Idle -> {
            // Nothing is being attempted, so there is no silence to count either.
            wasPlaying = false
            reconnecting = false
            silentSinceMs = nowMs()
            silentForMs = 0L
            false
        }
        else -> {
            // Stamp the moment playback stopped exactly once. Later Connecting/Error states are
            // the service retrying, not a new outage, so they must leave the clock alone.
            if (wasPlaying) silentSinceMs = nowMs()
            wasPlaying = false
            true
        }
    }

    /** Advances [silentForMs]; called once per [SILENCE_TICK_MS] while the player is silent. */
    fun tick() {
        silentForMs = nowMs() - silentSinceMs
    }
}
