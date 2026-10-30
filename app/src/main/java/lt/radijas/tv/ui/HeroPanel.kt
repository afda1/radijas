package lt.radijas.tv.ui

import android.os.SystemClock
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import lt.radijas.tv.R
import lt.radijas.tv.data.model.RadioStation
import lt.radijas.tv.player.AudioLevelState
import lt.radijas.tv.ui.theme.HeroCoreX
import lt.radijas.tv.ui.theme.HeroCoreY
import lt.radijas.tv.ui.theme.HeroGlowRadiusX
import lt.radijas.tv.ui.theme.HeroGlowRadiusY
import lt.radijas.tv.ui.theme.RadijasContent
import lt.radijas.tv.ui.theme.heroGlowStops
import lt.radijas.tv.ui.theme.heroSurface

/**
 * Display-only "now playing" hero.
 *
 * Deliberately has no `clickable`, `focusable` or `onKeyEvent` modifier: the shelf below is the
 * screen's only focus zone, so the D-pad can never strand focus on the hero.
 *
 * The status line is the honest part: it says what the player is actually doing, including the
 * states where nothing is audible. The track-title line above it is the station's own metadata,
 * shown in a fixed-height block so its arrival never reflows the hero.
 */
@Composable
fun HeroPanel(
    station: RadioStation,
    status: PlayerStatus,
    trackTitle: String? = null,
    modifier: Modifier = Modifier
) {
    val brandColor = station.brandColor
    // Remembered, not rebuilt: the hero recomposes on every status and track tick, and a new
    // Brush each time would throw away the shader. drawWithCache then builds the brush once per
    // size, and only when the station changes.
    val heroBackground = remember(brandColor) { heroGlowBackground(brandColor) }

    Box(
        modifier = modifier
            .testTag("hero-panel")
            .clip(RoundedCornerShape(28.dp))
            // The hero is a tinted version of the page, never a flat brand slab: a radial glow
            // whose core sits under the station icon, fading into the page colour before it
            // reaches the text column (see heroSurface).
            .then(heroBackground)
            // 20 dp, not 28: the 96 sp name plus the fixed 26 sp track block and the status
            // line only just fit the hero at 1080p (a 540 dp-tall canvas), and the later
            // children of a Column are the ones a short hero would squeeze to nothing.
            .padding(horizontal = 44.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.labelMedium,
                    color = RadijasContent.copy(alpha = 0.55f)
                )
                Spacer(Modifier.height(6.dp))
                StationName(station.name)
                Spacer(Modifier.height(2.dp))
                TrackTitleLine(trackTitle)
                Spacer(Modifier.height(2.dp))
                StatusLine(status)
            }
            Image(
                painter = painterResource(station.iconResId),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                // The logo is already a white silhouette on transparency and the brand lives in
                // the glow behind it, so the mark needs no plate: the glow is its backdrop and
                // the tile's brand colour is the only accent.
                colorFilter = ColorFilter.tint(Color.White),
                modifier = Modifier.size(168.dp).testTag("hero-station-logo")
            )
        }
    }
}

/**
 * Paints [heroSurface] behind the hero.
 *
 * The brush is a pixel-space circle scaled vertically, which turns it into the ellipse
 * [HeroGlowRadiusX]/[HeroGlowRadiusY] describe; that keeps the drawn pixels identical to the
 * fraction-space model the contrast test samples. The brush is built inside `drawWithCache`,
 * i.e. once per size and brand, not per recomposition.
 */
private fun heroGlowBackground(brand: Long): Modifier = Modifier.drawWithCache {
    val width = size.width
    val height = size.height
    val center = Offset(HeroCoreX * width, HeroCoreY * height)
    val radiusX = HeroGlowRadiusX * width
    val radiusY = HeroGlowRadiusY * height
    val glow = Brush.radialGradient(
        *heroGlowStops(brand),
        center = center,
        radius = radiusX
    )
    onDrawBehind {
        // The glow's rectangle must fade into the same tonal surface, not the page colour.
        drawRect(heroSurface(brand, 0f, 0f))
        // Pixel-space shader avoids precision artefacts from a one-pixel radial brush on TV GPUs.
        scale(scaleX = 1f, scaleY = radiusY / radiusX, pivot = center) {
            drawCircle(brush = glow, radius = radiusX, center = center)
        }
    }
}

/** Largest hero-name size, from the design spec. */
private val HeroNameMaxSize = 96.sp

/** Smallest the name may shrink to before an ellipsis is the lesser evil. */
private val HeroNameMinSize = 48.sp

/** Line height of the name; the band below keeps the block at one height for every station. */
private val HeroNameLineHeight = 104.sp

/**
 * The station name, at 96 sp whenever it fits the hero's text column.
 *
 * At 1080p that column is ~608 dp, and "Power Hit Radio Gold" is wider than that at 96 sp, so
 * a fixed size would ellipsise two of the eleven stations to "Power Hit R…". Instead the name
 * steps down until it fits on one line: a smaller name is still the station's name, a cut one
 * is not. The floor is 48 sp, below which the ellipsis returns.
 */
@Composable
private fun StationName(name: String) {
    val style = MaterialTheme.typography.displaySmall.copy(
        fontSize = HeroNameMaxSize,
        lineHeight = HeroNameLineHeight,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-1.5).sp,
        // The hero is height-starved at 1080p; the font's built-in top and bottom padding
        // would cost ~8 dp and push the status line out.
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )
    BoxWithConstraints(
        // A fixed band, like the track block: a station with a fitted 64 sp name must not
        // shift the label above it or the status line below it when the station changes. 113 dp
        // is the 96 sp name's natural line box (its font metrics are 0.5 dp taller than the
        // 104 sp line height); heightIn lets a larger system font scale grow the band instead
        // of painting the name over the track line.
        modifier = Modifier.heightIn(min = 113.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val measurer = rememberTextMeasurer()
        val fontScale = LocalDensity.current.fontScale
        val availableWidth = constraints.maxWidth
        val fontSize = remember(name, availableWidth, fontScale, style) {
            var size = HeroNameMaxSize.value
            while (size > HeroNameMinSize.value) {
                val measured = measurer.measure(
                    text = AnnotatedString(name),
                    style = style.copy(fontSize = size.sp),
                    maxLines = 1,
                    softWrap = false
                )
                if (measured.size.width <= availableWidth) break
                size -= 4f
            }
            size.sp
        }
        Text(
            text = name,
            style = style.copy(fontSize = fontSize),
            color = RadijasContent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TrackTitleLine(trackTitle: String?) {
    // A fixed-height block that is composed in both cases: the empty state reserves exactly the
    // same 44 dp, so the station name and status line never move when a title arrives.
    Box(
        modifier = Modifier.height(44.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = trackTitle.orEmpty(),
            modifier = Modifier.testTag("hero-track-title"),
            fontSize = 26.sp,
            lineHeight = 32.sp,
            color = RadijasContent.copy(alpha = 0.66f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
        // the hero's ~600dp text column, so wrapping is impossible and the line height - and
        // with it the station name above - cannot move between states.
        Text(
            text = stringResource(status.textRes()),
            modifier = Modifier.testTag("hero-status"),
            style = MaterialTheme.typography.titleMedium,
            color = RadijasContent.copy(alpha = 0.82f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Three recent PCM levels; the canvas has fixed bounds so the status copy never moves. */
@Composable
private fun EqualizerBars(active: Boolean) {
    val sample by AudioLevelState.levels.collectAsStateWithLifecycle()
    var fresh by remember { mutableStateOf(false) }
    LaunchedEffect(sample, active) {
        fresh = active && SystemClock.elapsedRealtime() - sample.updatedAt < 300
        delay(300)
        fresh = false
    }
    val targets = sample.values.map { if (fresh && active) kotlin.math.sqrt(it.coerceIn(0f, 1f)) else 0f }
    val first by animateFloatAsState(targets[0], tween(100), label = "level-1")
    val second by animateFloatAsState(targets[1], tween(100), label = "level-2")
    val third by animateFloatAsState(targets[2], tween(100), label = "level-3")
    Canvas(Modifier.size(18.dp, 18.dp).alpha(if (active) 1f else 0f).testTag("hero-equalizer")) {
        listOf(first, second, third).forEachIndexed { index, level ->
            val barHeight = 3.dp.toPx() + (size.height - 3.dp.toPx()) * level
            drawRoundRect(
                color = RadijasContent.copy(alpha = 0.85f),
                topLeft = Offset(index * 7.dp.toPx(), size.height - barHeight),
                size = Size(4.dp.toPx(), barHeight), cornerRadius = CornerRadius(2.dp.toPx())
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
