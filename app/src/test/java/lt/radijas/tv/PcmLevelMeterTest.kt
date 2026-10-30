package lt.radijas.tv

import java.nio.ByteBuffer
import java.nio.ByteOrder
import lt.radijas.tv.player.PcmLevelMeter
import org.junit.Assert.*
import org.junit.Test

class PcmLevelMeterTest {
    @Test fun `silence produces zero and full scale produces one`() {
        val meter = PcmLevelMeter()
        assertEquals(0f, meter.measure(ByteBuffer.allocate(16)), 0.001f)
        val pcm = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
        repeat(8) { pcm.putShort(Short.MAX_VALUE) }
        pcm.flip()
        assertEquals(1f, meter.measure(pcm), 0.001f)
    }

    @Test fun `measurement does not consume or alter playback buffer`() {
        val pcm = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN)
        pcm.position(2)
        pcm.limit(14)
        PcmLevelMeter().measure(pcm)
        assertEquals(2, pcm.position())
        assertEquals(14, pcm.limit())
        assertEquals(ByteOrder.BIG_ENDIAN, pcm.order())
    }

    @Test fun `empty and odd buffers are safe`() {
        assertEquals(0f, PcmLevelMeter().measure(ByteBuffer.allocate(0)), 0f)
        assertEquals(0f, PcmLevelMeter().measure(ByteBuffer.allocate(1)), 0f)
    }
}
