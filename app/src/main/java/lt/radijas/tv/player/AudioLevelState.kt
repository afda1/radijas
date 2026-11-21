package lt.radijas.tv.player

import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import java.nio.ByteBuffer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class AudioLevels(val values: List<Float> = listOf(0f, 0f, 0f), val updatedAt: Long = 0) {
    val bass: Float get() = values.getOrElse(0) { 0f }
    val mid: Float get() = values.getOrElse(1) { 0f }
    val treble: Float get() = values.getOrElse(2) { 0f }
}

/** No PCM is stored or transmitted. Only three frequency-band energies cross into the UI. */
internal object AudioLevelState {
    private val mutable = MutableStateFlow(AudioLevels())
    val levels = mutable.asStateFlow()
    fun clear() { mutable.value = AudioLevels() }
    fun publish(level: Float, now: Long) {
        mutable.value = AudioLevels(mutable.value.values.drop(1) + level, now)
    }
    fun publishBands(bass: Float, mid: Float, treble: Float, now: Long) {
        mutable.value = AudioLevels(listOf(bass, mid, treble), now)
    }
}

@OptIn(UnstableApi::class)
internal class LevelAudioBufferSink : TeeAudioProcessor.AudioBufferSink {
    private val analyzer = PcmSpectrumAnalyzer()
    private var supported = false
    private var sampleRateHz = 44_100
    private var channelCount = 2
    private var lastPublished = 0L

    override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
        supported = encoding == C.ENCODING_PCM_16BIT
        this.sampleRateHz = sampleRateHz
        this.channelCount = channelCount
        lastPublished = 0L
        AudioLevelState.clear()
    }

    override fun handleBuffer(buffer: ByteBuffer) {
        val now = SystemClock.elapsedRealtime()
        if (!supported || now - lastPublished < 40) return
        lastPublished = now
        val bands = analyzer.measureBands(buffer, sampleRateHz, channelCount)
        AudioLevelState.publishBands(bands.bass, bands.mid, bands.treble, now)
    }
}
