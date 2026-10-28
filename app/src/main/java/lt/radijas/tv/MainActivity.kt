package lt.radijas.tv

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import lt.radijas.tv.data.model.RadioStation
import lt.radijas.tv.data.repository.RadioRepository
import lt.radijas.tv.player.PlayerState
import lt.radijas.tv.player.RadioPlayerManager
import lt.radijas.tv.ui.NowPlayingScreen
import lt.radijas.tv.ui.NowPlayingUiState
import lt.radijas.tv.ui.PlayerStatus
import lt.radijas.tv.ui.playerStatus
import lt.radijas.tv.ui.theme.RadijasTheme

class MainActivity : ComponentActivity() {
    private lateinit var repository: RadioRepository
    private lateinit var playerManager: RadioPlayerManager
    private var currentStation by mutableStateOf<RadioStation?>(null)

    // Elapsed-realtime mark of when playback last stopped being `Playing`. Wall clock is useless
    // here: it can jump, and the user cares about "how long have I been staring at a silent TV".
    private var silentSinceMs by mutableLongStateOf(SystemClock.elapsedRealtime())
    private var silentForMs by mutableLongStateOf(0L)
    private var wasPlaying by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repository = RadioRepository(this)
        playerManager = RadioPlayerManager(this)

        val station = StartupStation.resolve(repository)
        currentStation = station
        repository.setLastStationId(station.id)
        playerManager.playStation(station)

        setContent {
            RadijasTheme {
                val playerState by playerManager.playerState.collectAsStateWithLifecycle()
                val playWhenReady by playerManager.playWhenReady.collectAsStateWithLifecycle()
                val trackTitle by playerManager.trackTitle.collectAsStateWithLifecycle()

                LaunchedEffect(playerState) { trackSilence(playerState) }

                val status = playerStatus(playerState, playWhenReady, silentForMs)
                // Read through a State so the callback below can stay remembered (stable) while
                // still seeing the newest status: a new lambda every tick would recompose every
                // station tile and undo the tile-level `remember`.
                val latestStatus = rememberUpdatedState(status)
                val onSelect = remember(playerManager) {
                    { selected: RadioStation ->
                        val failing = latestStatus.value == PlayerStatus.NO_CONNECTION ||
                            latestStatus.value == PlayerStatus.PAUSED
                        // Only the tile that is failing retries (spec 6.2: OK "ant tos pačios
                        // plytelės"); any other tile switches, even in the middle of an outage.
                        if (failing && selected.id == currentStation?.id) {
                            restartSilenceClock()
                            playerManager.retry()
                        } else {
                            selectStation(selected)
                        }
                    }
                }

                currentStation?.let { selectedStation ->
                    NowPlayingScreen(
                        uiState = NowPlayingUiState(
                            station = selectedStation,
                            status = status,
                            trackTitle = trackTitle
                        ),
                        stations = repository.stations,
                        onSelect = onSelect
                    )
                }
            }
        }
    }

    private fun selectStation(station: RadioStation) {
        restartSilenceClock()
        currentStation = station
        repository.setLastStationId(station.id)
        playerManager.playStation(station)
    }

    /**
     * A new attempt gets its own 30 s of "trying".
     *
     * Without this, OK on a dead station (or a switch to another one) would leave the failure
     * copy on screen, because the previous outage's silence is still counted, and the press
     * would look like it did nothing.
     */
    private fun restartSilenceClock() {
        silentSinceMs = SystemClock.elapsedRealtime()
        silentForMs = 0L
    }

    /**
     * Keeps [silentForMs] honest: zero while playing, then the time since playback stopped.
     *
     * The tick drives the `RECONNECTING` -> `NO_CONNECTION` threshold, so it has to keep running
     * while the player is silent even though nothing else on screen changes.
     */
    private suspend fun trackSilence(state: PlayerState) {
        when (state) {
            is PlayerState.Playing -> {
                wasPlaying = true
                silentSinceMs = SystemClock.elapsedRealtime()
                silentForMs = 0L
                return
            }
            // Nothing is being attempted (stopped): not silence, and no reason to keep a
            // one-second tick alive while the activity sits in the background.
            is PlayerState.Idle -> {
                wasPlaying = false
                silentSinceMs = SystemClock.elapsedRealtime()
                silentForMs = 0L
                return
            }
            else -> Unit
        }
        if (wasPlaying) {
            silentSinceMs = SystemClock.elapsedRealtime()
        }
        wasPlaying = false
        while (true) {
            silentForMs = SystemClock.elapsedRealtime() - silentSinceMs
            delay(SILENCE_TICK_MS)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Launching from the TV launcher while the activity is still alive arrives here
        // (launchMode="singleTop"), so play the last station again after Home stopped it.
        val station = StartupStation.resolve(repository)
        currentStation = station
        repository.setLastStationId(station.id)
        playerManager.playStation(station)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (StopPolicy.shouldStop(userLeftHint = true)) {
            playerManager.stop()
            currentStation?.let { repository.setLastStationId(it.id) }
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            playerManager.stop()
            playerManager.release()
        }
        super.onDestroy()
    }

    private companion object {
        const val SILENCE_TICK_MS = 1_000L
    }
}
