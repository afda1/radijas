package lt.radijas.tv

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import lt.radijas.tv.data.model.RadioStation
import lt.radijas.tv.data.repository.RadioRepository
import lt.radijas.tv.player.RadioPlayerManager
import lt.radijas.tv.ui.NowPlayingScreen
import lt.radijas.tv.ui.theme.RadijasTheme

class MainActivity : ComponentActivity() {
    private lateinit var repository: RadioRepository
    private lateinit var playerManager: RadioPlayerManager
    private var currentStation by mutableStateOf<RadioStation?>(null)

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
                currentStation?.let { station ->
                    NowPlayingScreen(
                        station = station,
                        stations = repository.stations,
                        // Task 5 wires OK to playback.
                        onSelect = {}
                    )
                }
            }
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
}
