package lt.radijas.tv.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import lt.radijas.tv.R
import lt.radijas.tv.data.model.RadioStation

/**
 * Display-only "now playing" hero.
 *
 * Deliberately has no `clickable`, `focusable` or `onKeyEvent` modifier: the shelf below is the
 * screen's only focus zone, so the D-pad can never strand focus on the hero.
 *
 * The status line is the honest part: it says what the player is actually doing, including the
 * states where nothing is audible.
 */
@Composable
fun HeroPanel(
    station: RadioStation,
    status: PlayerStatus,
    modifier: Modifier = Modifier
) {
    val brandColor = Color(station.brandColor)

    Box(
        modifier = modifier
            .testTag("hero-panel")
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.horizontalGradient(
                    0f to brandColor.copy(alpha = 0.55f),
                    0.55f to brandColor.copy(alpha = 0.16f),
                    1f to Color.Transparent
                )
            )
            .padding(horizontal = 44.dp, vertical = 28.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.55f)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.W600,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(12.dp))
                StatusLine(status)
            }
            Image(
                painter = painterResource(station.iconResId),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(168.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.18f))
            )
        }
    }
}

@Composable
private fun StatusLine(status: PlayerStatus) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        // Composed in every state and only faded out: a conditional bar block would start the
        // copy 28 dp further right in PLAYING and make the text jump between states.
        EqualizerBars(active = status == PlayerStatus.PLAYING)
        Spacer(Modifier.width(10.dp))
        // One line, always: the longest copy (~38 characters at 16sp) is a small fraction of
        // the hero's ~1500dp text column, so wrapping is impossible and the line height - and
        // with it the station name above - cannot move between states.
        Text(
            text = stringResource(status.textRes()),
            modifier = Modifier.testTag("hero-status"),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.82f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Three static bars: the design's "live" mark. Static so the layout never moves. */
@Composable
private fun EqualizerBars(active: Boolean) {
    Row(
        modifier = Modifier
            .alpha(if (active) 1f else 0f)
            .testTag("hero-equalizer"),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        listOf(10.dp, 18.dp, 7.dp).forEach { barHeight ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.85f))
            )
        }
    }
}

@StringRes
private fun PlayerStatus.textRes(): Int = when (this) {
    PlayerStatus.CONNECTING -> R.string.status_connecting
    PlayerStatus.PLAYING -> R.string.status_playing
    PlayerStatus.RECONNECTING -> R.string.status_reconnecting
    PlayerStatus.NO_CONNECTION -> R.string.status_no_connection
    PlayerStatus.PAUSED -> R.string.status_paused
}
