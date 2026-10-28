package lt.radijas.tv

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme
import lt.radijas.tv.data.model.RadioStation
import lt.radijas.tv.data.repository.RadioRepository
import lt.radijas.tv.player.RadioPlayerManager

class MainActivity : ComponentActivity() {
    private lateinit var repository: RadioRepository
    private lateinit var playerManager: RadioPlayerManager
    private var currentStation: RadioStation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repository = RadioRepository(this)
        playerManager = RadioPlayerManager(this)

        val station = StartupStation.resolve(repository)
        currentStation = station
        repository.setLastStationId(station.id)
        playerManager.playStation(station)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF141218))) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Radijas", color = Color(0xFFF2EDF2))
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
