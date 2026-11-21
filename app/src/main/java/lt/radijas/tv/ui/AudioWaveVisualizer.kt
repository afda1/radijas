package lt.radijas.tv.ui

import android.os.SystemClock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import kotlinx.coroutines.delay
import lt.radijas.tv.player.AudioLevelState

private const val WAVE_STEPS = 48
private const val FRESH_TIMEOUT_MS = 350L

private fun buildWavePath(
    linePath: Path,
    ribbonPath: Path?,
    width: Float,
    height: Float,
    cycles: Float,
    phase: Float,
    amplitude: Float,
    envelopePower: Double
) {
    linePath.reset()
    ribbonPath?.reset()
    if (width <= 0f || height <= 0f) return

    val midY = height * 0.5f
    var prevX = 0f
    var prevY = midY
    linePath.moveTo(0f, midY)
    ribbonPath?.moveTo(0f, midY)

    for (step in 1..WAVE_STEPS) {
        val u = step.toFloat() / WAVE_STEPS
        val x = width * u
        val envelope = sin(PI * u).pow(envelopePower).toFloat()
        val y = midY - sin(u * cycles * 2f * PI.toFloat() + phase) * amplitude * envelope
        val cx = (prevX + x) * 0.5f
        val cy = (prevY + y) * 0.5f
        linePath.quadraticTo(prevX, prevY, cx, cy)
        ribbonPath?.quadraticTo(prevX, prevY, cx, cy)
        prevX = x
        prevY = y
    }
    linePath.lineTo(width, midY)

    if (ribbonPath != null) {
        ribbonPath.lineTo(width, midY)
        prevX = width
        prevY = midY
        for (step in WAVE_STEPS - 1 downTo 0) {
            val u = step.toFloat() / WAVE_STEPS
            val x = width * u
            val envelope = sin(PI * u).pow(envelopePower).toFloat()
            val y = midY + sin(u * cycles * 2f * PI.toFloat() + phase) * amplitude * envelope
            val cx = (prevX + x) * 0.5f
            val cy = (prevY + y) * 0.5f
            ribbonPath.quadraticTo(prevX, prevY, cx, cy)
            prevX = x
            prevY = y
        }
        ribbonPath.close()
    }
}

/**
 * Google Nest-inspired three-layer fluid audio ribbon driven by real 256-point PCM FFT bands
 * (bass, mid, treble) while staying calm when silent or paused.
 */
@Composable
internal fun HeroAudioWave(
    active: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val sample by AudioLevelState.levels.collectAsStateWithLifecycle()
    var fresh by remember { mutableStateOf(false) }
    LaunchedEffect(sample, active) {
        val age = SystemClock.elapsedRealtime() - sample.updatedAt
        val isRecent = active && sample.updatedAt > 0L && age < FRESH_TIMEOUT_MS
        fresh = isRecent
        if (isRecent) {
            delay(FRESH_TIMEOUT_MS)
            fresh = false
        }
    }

    val smoothBass by animateFloatAsState(if (fresh && active) sample.bass else 0f, tween(110), label = "wave-bass")
    val smoothMid by animateFloatAsState(if (fresh && active) sample.mid else 0f, tween(110), label = "wave-mid")
    val smoothTreble by animateFloatAsState(if (fresh && active) sample.treble else 0f, tween(110), label = "wave-treble")

    var phase by remember { mutableFloatStateOf(0.7f) }
    LaunchedEffect(active, fresh) {
        while (active && fresh) {
            delay(33L)
            phase = (phase + 0.14f) % (2f * PI.toFloat() * 12f)
        }
    }

    val strokePath = remember { Path() }
    val fillPath = remember { Path() }

    Canvas(
        modifier = modifier
            .size(width = 200.dp, height = 28.dp)
            .alpha(if (active) 1f else 0f)
            .testTag("hero-equalizer")
    ) {
        val maxAmp = size.height * 0.44f

        buildWavePath(
            linePath = strokePath,
            ribbonPath = fillPath,
            width = size.width,
            height = size.height,
            cycles = 1.5f,
            phase = phase * 0.85f,
            amplitude = maxAmp * (0.12f + 0.88f * smoothBass),
            envelopePower = 1.6
        )
        drawPath(
            path = fillPath,
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, color.copy(alpha = 0.24f), Color.Transparent)
            )
        )
        drawPath(
            path = strokePath,
            color = color.copy(alpha = 0.38f),
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        buildWavePath(
            linePath = strokePath,
            ribbonPath = fillPath,
            width = size.width,
            height = size.height,
            cycles = 2.4f,
            phase = -phase * 1.15f + 1.9f,
            amplitude = maxAmp * (0.09f + 0.78f * smoothMid),
            envelopePower = 1.6
        )
        drawPath(
            path = fillPath,
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, color.copy(alpha = 0.14f), Color.Transparent)
            )
        )
        drawPath(
            path = strokePath,
            color = color.copy(alpha = 0.70f),
            style = Stroke(width = 1.75.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        buildWavePath(
            linePath = strokePath,
            ribbonPath = null,
            width = size.width,
            height = size.height,
            cycles = 3.5f,
            phase = phase * 1.55f + 3.6f,
            amplitude = maxAmp * (0.06f + 0.62f * smoothTreble),
            envelopePower = 1.6
        )
        drawPath(
            path = strokePath,
            color = color.copy(alpha = 0.94f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * Ultra-quiet night whisper thread for [ScreenOffOverlay], gently breathing with overall audio
 * energy without drawing peripheral attention in a dark room.
 */
@Composable
internal fun AmbientAudioWave(
    color: Color,
    modifier: Modifier = Modifier
) {
    val sample by AudioLevelState.levels.collectAsStateWithLifecycle()
    var fresh by remember { mutableStateOf(false) }
    LaunchedEffect(sample) {
        val age = SystemClock.elapsedRealtime() - sample.updatedAt
        val isRecent = sample.updatedAt > 0L && age < FRESH_TIMEOUT_MS
        fresh = isRecent
        if (isRecent) {
            delay(FRESH_TIMEOUT_MS)
            fresh = false
        }
    }

    val targetEnergy = if (fresh) {
        (sample.bass * 0.55f + sample.mid * 0.35f + sample.treble * 0.10f).coerceIn(0f, 1f)
    } else {
        0f
    }
    val smoothEnergy by animateFloatAsState(targetEnergy, tween(260), label = "ambient-energy")

    var phase by remember { mutableFloatStateOf(0.5f) }
    LaunchedEffect(fresh) {
        while (fresh) {
            delay(40L)
            phase = (phase + 0.08f) % (2f * PI.toFloat() * 12f)
        }
    }

    val wavePath = remember { Path() }
    Canvas(
        modifier = modifier
            .size(width = 160.dp, height = 14.dp)
            .testTag("screen-off-wave")
    ) {
        buildWavePath(
            linePath = wavePath,
            ribbonPath = null,
            width = size.width,
            height = size.height,
            cycles = 1.8f,
            phase = phase,
            amplitude = (size.height * 0.44f) * (0.10f + 0.90f * smoothEnergy),
            envelopePower = 1.8
        )
        drawPath(
            path = wavePath,
            brush = Brush.horizontalGradient(
                listOf(
                    Color.Transparent,
                    color.copy(alpha = if (fresh) 0.24f else 0.14f),
                    Color.Transparent
                )
            ),
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
