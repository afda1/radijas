package lt.radijas.tv.player

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

internal data class AudioBands(
    val bass: Float,
    val mid: Float,
    val treble: Float
)

/**
 * Lightweight 256-point Radix-2 FFT extracting bass, mid and treble energy from ExoPlayer's
 * 16-bit little-endian PCM stream without touching playback position or allocating per buffer.
 */
internal class PcmSpectrumAnalyzer {
    private val real = DoubleArray(FFT_SIZE)
    private val imag = DoubleArray(FFT_SIZE)
    private val hann = DoubleArray(FFT_SIZE) { i ->
        0.5 * (1.0 - cos(2.0 * PI * i / (FFT_SIZE - 1)))
    }
    private val bitRev = IntArray(FFT_SIZE) { i ->
        Integer.reverse(i) ushr (32 - FFT_BITS)
    }

    fun measureBands(
        pcm: ByteBuffer,
        sampleRateHz: Int,
        channelCount: Int
    ): AudioBands {
        val channels = channelCount.coerceAtLeast(1)
        if (sampleRateHz <= 0) return AudioBands(0f, 0f, 0f)

        val samples = pcm.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        val totalFrames = samples.remaining() / (2 * channels)
        if (totalFrames <= 0) return AudioBands(0f, 0f, 0f)

        real.fill(0.0)
        imag.fill(0.0)
        val frameCount = minOf(FFT_SIZE, totalFrames)
        for (i in 0 until frameCount) {
            var sum = 0.0
            for (ch in 0 until channels) {
                sum += samples.short / 32768.0
            }
            val mono = sum / channels
            real[bitRev[i]] = mono * hann[i]
        }

        // Iterative in-place Cooley-Tukey Radix-2 FFT.
        var halfStep = 1
        while (halfStep < FFT_SIZE) {
            val step = halfStep shl 1
            val angleStep = -2.0 * PI / step
            val wStepReal = cos(angleStep)
            val wStepImag = sin(angleStep)
            for (group in 0 until FFT_SIZE step step) {
                var wReal = 1.0
                var wImag = 0.0
                for (pair in 0 until halfStep) {
                    val even = group + pair
                    val odd = even + halfStep
                    val tReal = wReal * real[odd] - wImag * imag[odd]
                    val tImag = wReal * imag[odd] + wImag * real[odd]
                    real[odd] = real[even] - tReal
                    imag[odd] = imag[even] - tImag
                    real[even] += tReal
                    imag[even] += tImag
                    val nextWReal = wReal * wStepReal - wImag * wStepImag
                    wImag = wReal * wStepImag + wImag * wStepReal
                    wReal = nextWReal
                }
            }
            halfStep = step
        }

        val binHz = sampleRateHz.toDouble() / FFT_SIZE
        val maxBin = FFT_SIZE / 2 - 1
        val bassEnd = (400.0 / binHz).toInt().coerceIn(1, maxBin - 2)
        val midStart = bassEnd + 1
        val midEnd = (2600.0 / binHz).toInt().coerceIn(midStart, maxBin - 1)
        val trebleStart = midEnd + 1
        val trebleEnd = (10000.0 / binHz).toInt().coerceIn(trebleStart, maxBin)
        val norm = FFT_SIZE * 0.25

        return AudioBands(
            bass = bandEnergy(1, bassEnd, norm, boost = 1.6),
            mid = bandEnergy(midStart, midEnd, norm, boost = 2.8),
            treble = bandEnergy(trebleStart, trebleEnd, norm, boost = 4.5)
        )
    }

    private fun bandEnergy(fromBin: Int, toBin: Int, norm: Double, boost: Double): Float {
        var sqSum = 0.0
        var peak = 0.0
        var count = 0
        for (bin in fromBin..toBin) {
            val mag = hypot(real[bin], imag[bin]) / norm
            sqSum += mag * mag
            if (mag > peak) peak = mag
            count++
        }
        if (count == 0) return 0f
        val rms = sqrt(sqSum / count)
        val level = 0.65 * rms + 0.35 * peak
        return sqrt((level * boost).coerceIn(0.0, 1.0)).toFloat()
    }

    private companion object {
        private const val FFT_SIZE = 256
        private const val FFT_BITS = 8
    }
}
