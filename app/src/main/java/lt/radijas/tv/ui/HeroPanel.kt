package lt.radijas.tv.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
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
import lt.radijas.tv.R
import lt.radijas.tv.data.model.RadioStation
import lt.radijas.tv.ui.theme.HeroCoreX
import lt.radijas.tv.ui.theme.HeroCoreY
import lt.radijas.tv.ui.theme.HeroGlowRadiusX
import lt.radijas.tv.ui.theme.HeroGlowRadiusY
import lt.radijas.tv.ui.theme.RadijasContent
import lt.radijas.tv.ui.theme.heroGlowStops

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
                // No scrim plate: the glow behind the icon is its backdrop now, and a dark
                // rounded plate over it read as a rendering artefact. It also made the black
                // station logos harder to see, not easier.
                modifier = Modifier
                    .size(168.dp)
                    .clip(RoundedCornerShape(24.dp))
            )
        }
    }
}

/**
 * Paints [heroSurface] behind the hero.
 *
 * The brush is a unit circle drawn through a translate + scale, which turns it into the ellipse
 * [HeroGlowRadiusX]/[HeroGlowRadiusY] describe; that keeps the drawn pixels identical to the
 * fraction-space model the contrast test samples. The brush is built inside `drawWithCache`,
 * i.e. once per size and brand, not per recomposition.
 */
private fun heroGlowBackground(brand: Long): Modifier = Modifier.drawWithCache {
    val width = size.width
    val height = size.height
    val glow = Brush.radialGradient(
        *heroGlowStops(brand),
        center = Offset.Zero,
        radius = 1f
    )
    onDrawBehind {
        translate(left = HeroCoreX * width, top = HeroCoreY * height) {
            scale(
                scaleX = HeroGlowRadiusX * width,
                scaleY = HeroGlowRadiusY * height,
                pivot = Offset.Zero
            ) {
                drawRect(brush = glow, topLeft = Offset(-1f, -1f), size = Size(2f, 2f))
            }
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
 * a fixed size would ellipsise two of the ten stations to "Power Hit R…". Instead the name
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
                    .background(RadijasContent.copy(alpha = 0.85f))
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
