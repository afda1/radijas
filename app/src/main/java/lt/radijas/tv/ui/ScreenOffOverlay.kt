package lt.radijas.tv.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import lt.radijas.tv.R
import lt.radijas.tv.ui.theme.ScreenOffBlack

private val PixelShiftOrbit: List<Pair<Int, Int>> = listOf(
    0 to 0,
    2 to 1,
    4 to 2,
    6 to 1,
    6 to -1,
    4 to -2,
    2 to -3,
    0 to -4,
    -2 to -3,
    -4 to -2,
    -6 to -1,
    -6 to 1,
    -4 to 2,
    -2 to 3,
    0 to 4,
    2 to 3
)

internal fun screenOffPixelShiftDp(epochMs: Long): Pair<Int, Int> {
    val minuteIndex = Math.floorDiv(epochMs, 60_000L)
    val orbitIndex = Math.floorMod(minuteIndex, PixelShiftOrbit.size)
    return PixelShiftOrbit[orbitIndex]
}

/**
 * The black frame that stands in for "screen off". Any remote key brings the UI back; see
 * `MainActivity.dispatchKeyEvent`. A brief wake hint appears at the bottom for a couple of
 * seconds, while a faint ambient clock, date, and the currently playing track remain centered on black.
 */
@Composable
fun ScreenOffOverlay(
    hintVisible: Boolean,
    trackTitle: String? = null,
    stationName: String? = null,
    nowMs: () -> Long = System::currentTimeMillis
) {
    val clockDate = rememberClockDate(nowMs)
    val onSurface = MaterialTheme.colorScheme.onSurface
    val (shiftX, shiftY) = remember(clockDate, nowMs) { screenOffPixelShiftDp(nowMs()) }
    val subtitle = trackTitle?.takeIf { it.isNotBlank() } ?: stationName?.takeIf { it.isNotBlank() }
    val subtitleAlpha = remember { Animatable(1f) }
    var previousSubtitle by remember { mutableStateOf(subtitle) }
    LaunchedEffect(subtitle) {
        if (subtitle != previousSubtitle) {
            previousSubtitle = subtitle
            if (subtitle != null) {
                subtitleAlpha.snapTo(0.15f)
                subtitleAlpha.animateTo(1f, tween(durationMillis = 200))
            } else {
                subtitleAlpha.snapTo(1f)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenOffBlack)
            .padding(horizontal = 48.dp, vertical = 27.dp)
            .testTag("screen-off-overlay")
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = shiftX.dp, y = shiftY.dp)
                .padding(bottom = 54.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = clockDate.time,
                modifier = Modifier.testTag("screen-off-clock"),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 116.sp,
                    lineHeight = 120.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = (-3.0).sp,
                    fontFeatureSettings = "tnum",
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                color = onSurface.copy(alpha = 0.36f),
                maxLines = 1
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = clockDate.date,
                modifier = Modifier.testTag("screen-off-date"),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 24.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Normal,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                color = onSurface.copy(alpha = 0.28f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(x = shiftX.dp, y = shiftY.dp)
                .padding(bottom = 52.dp)
                .widthIn(max = 720.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AmbientAudioWave(color = MaterialTheme.colorScheme.primary)
            if (subtitle != null) {
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .graphicsLayer { alpha = subtitleAlpha.value }
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.34f),
                                CircleShape
                            )
                            .testTag("screen-off-playing-dot")
                    )
                    Text(
                        text = subtitle,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .graphicsLayer { alpha = subtitleAlpha.value }
                            .testTag("screen-off-track"),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 24.sp,
                            lineHeight = 30.sp,
                            fontWeight = FontWeight.Normal,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        ),
                        color = onSurface.copy(alpha = 0.28f),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (hintVisible) {
            Text(
                text = stringResource(R.string.screen_off_hint),
                style = MaterialTheme.typography.labelLarge,
                color = onSurface.copy(alpha = 0.45f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
            )
        }
    }
}
