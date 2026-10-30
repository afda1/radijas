package lt.radijas.tv.player

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/** RMS of signed 16-bit little-endian PCM; playback's position/order are never changed. */
internal class PcmLevelMeter {
    fun measure(pcm: ByteBuffer): Float {
        val samples = pcm.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        val count = samples.remaining() / 2
        if (count == 0) return 0f
        var squares = 0.0
        repeat(count) {
            val value = samples.short / 32768.0
            squares += value * value
        }
        return sqrt(squares / count).toFloat().coerceIn(0f, 1f)
    }
}
