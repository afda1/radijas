package lt.radijas.tv.data.repository

import android.content.Context
import androidx.core.content.edit
import lt.radijas.tv.R
import lt.radijas.tv.StartupStation
import lt.radijas.tv.data.model.RadioStation

class RadioRepository(context: Context) {
    private val prefs = context.getSharedPreferences("fogwave_prefs", Context.MODE_PRIVATE)

    val stations: List<RadioStation> = listOf(
        RadioStation(
            id = "zip",
            name = "ZIP FM",
            streamUrl = "https://stream4.radijas.lt/zipfm",
            iconResId = R.drawable.ic_station_zipfm,
            brandColor = 0xFFFF5A1FL
        ),
        RadioStation(
            id = "powerhit",
            name = "Power Hit Radio",
            // Stable revma entry point: it redirects to a fresh node URL with a short-lived token.
            streamUrl = "https://stream.rcs.revma.com/f31w7e0fveuvv",
            iconResId = R.drawable.ic_station_powerhit,
            brandColor = 0xFFE5007DL
        ),
        RadioStation(
            id = "powerhitgold",
            name = "Power Hit Radio Gold",
            streamUrl = "https://stream.rcs.revma.com/asdpz3qfbhuvv",
            iconResId = R.drawable.ic_station_powerhitgold,
            brandColor = 0xFFC9A227L
        ),
        RadioStation(
            id = "rock",
            name = "Rock FM",
            streamUrl = "https://stream4.radijas.lt/rockfm",
            iconResId = R.drawable.ic_station_rockfm,
            brandColor = 0xFFD7263DL
        ),
        RadioStation(
            id = "relax",
            name = "Relax FM",
            streamUrl = "https://stream4.radijas.lt/relaxfm",
            iconResId = R.drawable.ic_station_relaxfm,
            brandColor = 0xFF35B08AL
        ),
        RadioStation(
            id = "lietus",
            name = "Lietus",
            streamUrl = "https://stream.m-1.fm/lietus/aacp64",
            iconResId = R.drawable.ic_station_lietus,
            brandColor = 0xFF0E8FD1L
        ),
        RadioStation(
            id = "m1",
            name = "M-1",
            streamUrl = "https://stream.m-1.fm/m1/aacp64",
            iconResId = R.drawable.ic_station_m1,
            brandColor = 0xFFFFC400L
        ),
        RadioStation(
            id = "plus",
            name = "M-1 Plius",
            streamUrl = "https://radio.m-1.fm/m1plius/aacp64",
            iconResId = R.drawable.ic_station_m1plus,
            brandColor = 0xFF1E6FE8L
        ),
        RadioStation(
            id = "goldfm",
            name = "Gold FM",
            streamUrl = "https://stream.goldfm.lt/goldfm.aac",
            iconResId = R.drawable.ic_station_goldfm,
            brandColor = 0xFFB98B3EL
        ),
        RadioStation(
            id = "rc",
            name = "RC",
            streamUrl = "https://stream1.rc.lt/rc128.mp3",
            iconResId = R.drawable.ic_station_rc,
            brandColor = 0xFFE4322BL
        )
    )

    fun lastStationId(): String =
        prefs.getString("last_station_id", null) ?: StartupStation.DEFAULT_ID

    fun setLastStationId(stationId: String) {
        prefs.edit { putString("last_station_id", stationId) }
    }
}
