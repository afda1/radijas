package lt.radijas.tv.player

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** Only Power's official current-song feed. No artwork search, downloads or cache. */
internal class PowerMetadataClient {
    suspend fun powerTitle(stationId: String): String? {
        val suffix = when (stationId) {
            "powerhit" -> "?action=getsong"
            "powerhitgold" -> "?gold=1&action=getsong"
            else -> return null
        }
        return try {
            val json = fetch("https://www.powerhitradio.lt/Pwr/lastSong/$suffix")
            parsePowerTrack(json.toString(Charsets.UTF_8), System.currentTimeMillis())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) { null }
    }

    private suspend fun fetch(rawUrl: String): ByteArray = withContext(Dispatchers.IO) {
        val url = URL(rawUrl)
        require(url.protocol == "https" && url.userInfo == null && url.port == -1)
        require(url.host == "www.powerhitradio.lt")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            connection.setRequestProperty("User-Agent", "Radijas/0.1 (Android TV)")
            require(connection.responseCode == 200)
            require(connection.contentLengthLong <= MAX_BYTES)
            connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= MAX_BYTES)
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val MAX_BYTES = 64 * 1024
    }
}
