package lt.radijas.tv

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import lt.radijas.tv.data.model.RadioStation
import lt.radijas.tv.data.repository.RadioRepository
import lt.radijas.tv.player.PlayerState
import lt.radijas.tv.player.RadioPlayerManager
import lt.radijas.tv.ui.NowPlayingScreen
import lt.radijas.tv.ui.NowPlayingUiState
import lt.radijas.tv.ui.ScreenOffOverlay
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

    /** The black frame that replaces "screen off"; any remote key brings it back. */
    private var screenOff by mutableStateOf(false)
    private var hintVisible by mutableStateOf(false)
    private val leaveGuard = LeavePlaybackGuard()
    private val leaveHandler = Handler(Looper.getMainLooper())
    private val dreamReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_DREAMING_STARTED) {
                Log.d("RadioLeave", "Dream started; keep playback")
                leaveGuard.onDreamStarted()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ContextCompat.registerReceiver(this, dreamReceiver, IntentFilter(Intent.ACTION_DREAMING_STARTED),
            ContextCompat.RECEIVER_EXPORTED)
        screenOff = savedInstanceState?.getBoolean(KEY_SCREEN_OFF) ?: false

        repository = RadioRepository(this)
        playerManager = RadioPlayerManager(this)

        val station = StartupStation.resolve(repository)
        currentStation = station
        repository.setLastStationId(station.id)
        playerManager.playStation(station)

        setContent {
            RadijasTheme(stationId = currentStation?.id ?: StartupStation.DEFAULT_ID) {
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
                val currentId = currentStation?.id
                val onSelect = remember(playerManager, currentId) {
                    { selected: RadioStation ->
                        when (stationAction(selected.id, currentId, latestStatus.value)) {
                            StationAction.SELECT -> selectStation(selected)
                            // Only the failing tile retries; OK on the playing tile is a no-op.
                            StationAction.RETRY -> {
                                restartSilenceClock()
                                playerManager.retry()
                            }
                            StationAction.IGNORE -> Unit
                        }
                    }
                }

                DisposableEffect(screenOff) {
                    if (screenOff) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
                }
                LaunchedEffect(screenOff) {
                    if (screenOff) {
                        hintVisible = true
                        delay(SCREEN_OFF_HINT_MS)
                        hintVisible = false
                    }
                }
                BackHandler(enabled = screenOff) { screenOff = false }
                Box(Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .focusProperties { canFocus = !screenOff }
                            .then(if (screenOff) Modifier.clearAndSetSemantics { } else Modifier)
                    ) {
                        currentStation?.let { selectedStation ->
                            NowPlayingScreen(
                                uiState = NowPlayingUiState(
                                    station = selectedStation,
                                    status = status,
                                    trackTitle = trackTitle
                                ),
                                stations = repository.stations,
                                onSelect = onSelect,
                                onScreenOff = { screenOff = true }
                            )
                        }
                    }
                    if (screenOff) {
                        ScreenOffOverlay(
                            hintVisible = hintVisible,
                            trackTitle = trackTitle,
                            stationName = currentStation?.name
                        )
                    }
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
        // Unless it is still playing: a relaunch must not rebuffer a stream that never stopped.
        val alreadyRunning = !RelaunchPolicy.shouldRestart(
            requestedStationId = station.id,
            currentStationId = currentStation?.id,
            playWhenReady = playerManager.playWhenReady.value
        )
        if (alreadyRunning) return
        restartSilenceClock()
        currentStation = station
        repository.setLastStationId(station.id)
        playerManager.playStation(station)
    }

    override fun onResume() {
        super.onResume()
        leaveGuard.onResumed()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        Log.d("RadioLeave", "User leave callback")
        if (StopPolicy.shouldStop(userLeftHint = true)) {
            val token = leaveGuard.onUserLeave()
            // Some TVs deliver onUserLeaveHint for DreamActivity. The protected dream broadcast
            // can arrive before or after that callback; defer Home's stop and cancel it for dreams.
            leaveHandler.postDelayed({
                if (leaveGuard.shouldStop(token)) {
                    screenOff = false
                    playerManager.stop()
                    currentStation?.let { repository.setLastStationId(it.id) }
                }
            }, LEAVE_GRACE_MS)
        }
    }

    /**
     * While the black frame is up every remote key is swallowed, and the release wakes the screen:
     * waking on DOWN would let the matching UP reach a station tile.
     *
     * dispatchKeyEvent (not onKeyDown) is required: arrow keys are consumed by Compose focus
     * before the activity sees them, so onKeyDown would never wake the screen.
     */
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        when (screenOffAction(event.action, screenOff, event.keyCode)) {
            ScreenOffAction.PASS -> super.dispatchKeyEvent(event)
            ScreenOffAction.CONSUME -> true
            ScreenOffAction.CONSUME_AND_WAKE -> {
                screenOff = false
                true
            }
        }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_SCREEN_OFF, screenOff)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        leaveHandler.removeCallbacksAndMessages(null)
        unregisterReceiver(dreamReceiver)
        if (isFinishing) {
            playerManager.stop()
        }
        // Released even when the system destroys the activity without finishing (the screensaver
        // and background flows where playback is meant to continue): the controller, its listener
        // and this activity's Context would otherwise outlive the activity for as long as the
        // service plays.
        playerManager.release()
        super.onDestroy()
    }

    companion object {
        private const val SILENCE_TICK_MS = 1_000L
        private const val LEAVE_GRACE_MS = 2_000L
        private const val SCREEN_OFF_HINT_MS = 2_500L
        private const val KEY_SCREEN_OFF = "lt.radijas.tv.extra.SCREEN_OFF"

        /**
         * Set only on the playback notification's content intent. Without it, a live activity
         * cannot tell "the user tapped the notification" from "the launcher (re)started us" - and
         * the latter is the only case where replaying the last station is wanted.
         */
        const val EXTRA_FROM_NOTIFICATION = "lt.radijas.tv.extra.FROM_NOTIFICATION"
    }
}
