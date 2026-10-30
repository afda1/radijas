package lt.radijas.tv.player

import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import java.nio.ByteBuffer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class AudioLevels(val values: List<Float> = listOf(0f, 0f, 0f), val updatedAt: Long = 0)

/** No PCM is stored or transmitted. Only three recent RMS values cross into the UI. */
internal object AudioLevelState {
    private val mutable = MutableStateFlow(AudioLevels())
    val levels = mutable.asStateFlow()
    fun clear() { mutable.value = AudioLevels() }
    fun publish(level: Float, now: Long) {
        mutable.value = AudioLevels(mutable.value.values.drop(1) + level, now)
    }
}

@OptIn(UnstableApi::class)
internal class LevelAudioBufferSink : TeeAudioProcessor.AudioBufferSink {
    private val meter = PcmLevelMeter()
    private var supported = false
    private var lastPublished = 0L

    override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
        supported = encoding == C.ENCODING_PCM_16BIT
        lastPublished = 0
        AudioLevelState.clear()
    }

    override fun handleBuffer(buffer: ByteBuffer) {
        val now = SystemClock.elapsedRealtime()
        if (!supported || now - lastPublished < 50) return
        lastPublished = now
        AudioLevelState.publish(meter.measure(buffer), now)
    }
}
