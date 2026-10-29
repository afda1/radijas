package lt.radijas.tv

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import lt.radijas.tv.ui.SilenceClock
import lt.radijas.tv.ui.playerStatus
import lt.radijas.tv.ui.theme.RadijasTheme

class MainActivity : ComponentActivity() {
    private lateinit var repository: RadioRepository
    private lateinit var playerManager: RadioPlayerManager
    private var currentStation by mutableStateOf<RadioStation?>(null)

    // Elapsed-realtime based: wall clock can jump, and the user cares about "how long have I been
    // staring at a silent TV". Owns the silence tick and the "first connection vs lost
    // connection" distinction that both feed the hero's status line.
    private val silence = SilenceClock()

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

                val status = playerStatus(
                    playerState = playerState,
                    playWhenReady = playWhenReady,
                    silentForMs = silence.silentForMs,
                    // A silence is only a reconnection once this attempt has been audible.
                    reconnecting = silence.reconnecting
                )
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
     * A new attempt gets its own 30 s of "trying", and its first silence is a *connection*, not a
     * reconnection.
     *
     * Without this, OK on a dead station (or a switch to another one) would leave the failure
     * copy on screen, because the previous outage's silence is still counted, and the press
     * would look like it did nothing. It also stops the previous station's `reconnecting` from
     * making the new station's first buffer read „Bandau vėl…".
     */
    private fun restartSilenceClock() {
        silence.restart()
    }

    /**
     * Keeps the silence clock honest: zero while playing, then the time since playback stopped.
     *
     * The tick drives the `RECONNECTING` -> `NO_CONNECTION` threshold, so it has to keep running
     * while the player is silent even though nothing else on screen changes.
     */
    private suspend fun trackSilence(state: PlayerState) {
        if (!silence.onState(state)) return
        while (true) {
            silence.tick()
            delay(SILENCE_TICK_MS)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Tapping the playback notification only brings the app forward. The stream is already
        // playing (the notification exists because of it), so replaying the last station would
        // audibly restart and rebuffer it - and blank the now-playing line.
        if (intent.getBooleanExtra(EXTRA_FROM_NOTIFICATION, false)) return
        // Launching from the TV launcher while the activity is still alive arrives here
        // (launchMode="singleTop"), so play the last station again after Home stopped it.
        val station = StartupStation.resolve(repository)
        restartSilenceClock()
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

    companion object {
        private const val SILENCE_TICK_MS = 1_000L

        /**
         * Set only on the playback notification's content intent. Without it, a live activity
         * cannot tell "the user tapped the notification" from "the launcher (re)started us" - and
         * the latter is the only case where replaying the last station is wanted.
         */
        const val EXTRA_FROM_NOTIFICATION = "lt.radijas.tv.extra.FROM_NOTIFICATION"
    }
}
