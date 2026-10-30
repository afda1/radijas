package lt.radijas.tv.player

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import org.json.JSONObject

/** Station timestamps are Lithuanian local time. Fail closed on stale or invalid responses. */
internal fun parsePowerTrack(json: String, nowMs: Long): String? = try {
    val data = JSONObject(json)
    val artist = data.optString("artistName").trim()
    val title = data.optString("songTitle").trim()
    val date = data.optString("date")
    val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).apply {
        timeZone = TimeZone.getTimeZone("Europe/Vilnius")
        isLenient = false
    }
    val position = ParsePosition(0)
    val timestamp = format.parse(date, position)?.time
    if (artist.isBlank() || title.isBlank() || artist.length > 150 || title.length > 150 ||
        timestamp == null || position.index != date.length || nowMs - timestamp !in -30_000L..600_000L
    ) null else "$artist - $title"
} catch (_: Exception) { null }
