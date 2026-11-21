package lt.radijas.tv

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import lt.radijas.tv.player.AudioBands
import lt.radijas.tv.player.PcmSpectrumAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PcmSpectrumAnalyzerTest {
    private val analyzer = PcmSpectrumAnalyzer()

    private fun sinePcm(
        freqHz: Double,
        sampleRateHz: Int = 44_100,
        channels: Int = 2,
        frames: Int = 256,
        amplitude: Double = 0.5
    ): ByteBuffer {
        val buf = ByteBuffer.allocate(frames * channels * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until frames) {
            val sample = (sin(2.0 * PI * freqHz * i / sampleRateHz) * amplitude * Short.MAX_VALUE)
                .roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
            repeat(channels) { buf.putShort(sample) }
        }
        buf.flip()
        return buf
    }

    @Test
    fun `silence and empty or odd buffers produce zero bands`() {
        assertEquals(AudioBands(0f, 0f, 0f), analyzer.measureBands(ByteBuffer.allocate(0), 44_100, 2))
        assertEquals(AudioBands(0f, 0f, 0f), analyzer.measureBands(ByteBuffer.allocate(1), 44_100, 2))
        assertEquals(AudioBands(0f, 0f, 0f), analyzer.measureBands(ByteBuffer.allocate(1024), 44_100, 2))
        assertEquals(AudioBands(0f, 0f, 0f), analyzer.measureBands(sinePcm(200.0), 0, 2))
    }

    @Test
    fun `low, mid and high frequency sine waves isolate into bass, mid and treble bands`() {
        listOf(1, 2).forEach { channels ->
            val low = analyzer.measureBands(sinePcm(200.0, channels = channels), 44_100, channels)
            assertTrue("ch=$channels 200 Hz bass=${low.bass} mid=${low.mid} treble=${low.treble}", low.bass > 0.5f)
            assertTrue("ch=$channels 200 Hz must isolate bass (${low})", low.bass > low.mid * 2f && low.bass > low.treble * 2f)

            val mid = analyzer.measureBands(sinePcm(1200.0, channels = channels), 44_100, channels)
            assertTrue("ch=$channels 1200 Hz mid=${mid.mid}", mid.mid > 0.5f)
            assertTrue("ch=$channels 1200 Hz must isolate mid (${mid})", mid.mid > mid.bass * 2f && mid.mid > mid.treble * 2f)

            val high = analyzer.measureBands(sinePcm(5500.0, channels = channels), 44_100, channels)
            assertTrue("ch=$channels 5500 Hz treble=${high.treble}", high.treble > 0.5f)
            assertTrue("ch=$channels 5500 Hz must isolate treble (${high})", high.treble > high.bass * 2f && high.treble > high.mid * 2f)
        }
    }

    @Test
    fun `analysis does not consume or alter playback buffer`() {
        val pcm = ByteBuffer.allocate(1024).order(ByteOrder.BIG_ENDIAN)
        pcm.position(4)
        pcm.limit(520)
        analyzer.measureBands(pcm, 44_100, 2)
        assertEquals(4, pcm.position())
        assertEquals(520, pcm.limit())
        assertEquals(ByteOrder.BIG_ENDIAN, pcm.order())
    }
}
