package lt.radijas.tv.ui

import android.os.SystemClock
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
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
import lt.radijas.tv.ui.theme.ControlTint
import lt.radijas.tv.ui.theme.logoAspect
import lt.radijas.tv.ui.theme.stationLogoWidthFraction
import lt.radijas.tv.data.model.RadioStation
import lt.radijas.tv.player.AudioLevelState

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
    onScreenOff: () -> Unit = {},
    onNavigateDown: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    // Remembered, not rebuilt: the hero recomposes on every status and track tick, and a new
    // Brush each time would throw away the shader. drawWithCache then builds the brush once per
    // size, and only when the station changes.
    val heroBackground = remember(colors.primaryContainer, colors.surface) {
        heroGlowBackground(colors.primaryContainer, colors.surface)
    }

    Box(
        modifier = modifier
            .testTag("hero-panel")
            .clip(MaterialTheme.shapes.extraLarge)
            // The hero is a tinted version of the station surface, never a flat brand slab: a
            // radial glow whose core sits under the station icon and fades out before it reaches
            // the text column.
            .then(heroBackground)
            // The hero is height-starved at 1080p: a 540 dp-tall screen gives it ~242 dp, so the
            // 96 sp name, the fixed 26 sp track block and the status line have to share ~202 dp.
            // The redundant „Radijas" eyebrow was removed to buy the status line its size (it is
            // the copy an elderly user has to read to fix a silent radio), and the later children
            // of a Column are the ones a short hero would squeeze to nothing.
            .padding(horizontal = 44.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                StationName(station.name)
                Spacer(Modifier.height(2.dp))
                TrackTitleLine(trackTitle)
                Spacer(Modifier.height(2.dp))
                StatusLine(status, onScreenOff, onNavigateDown)
            }
            // Same optical normalisation as the tiles, scaled to the hero's 168 dp slot.
            val heroPainter = painterResource(station.iconResId)
            val heroAspect = remember(heroPainter) { logoAspect(heroPainter) }
            val heroLogoWidth = 168.dp * (stationLogoWidthFraction[station.id] ?: 1f)
            Image(
                painter = heroPainter,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                // The logo is a white silhouette on transparency and the brand lives in the glow
                // behind it, so the mark needs no plate.
                colorFilter = ColorFilter.tint(Color.White),
                modifier = Modifier
                    .size(width = heroLogoWidth, height = heroLogoWidth / heroAspect)
                    .testTag("hero-station-logo")
            )
        }
    }
}

/**
 * Paints the station glow behind the hero.
 *
 * The brush is a pixel-space circle scaled vertically, which turns it into the ellipse
 * [HeroGlowRadiusX]/[HeroGlowRadiusY] describe; that keeps the drawn pixels identical to the
 * fraction-space model the contrast test samples. The brush is built inside `drawWithCache`,
 * i.e. once per size and brand, not per recomposition.
 */
/** Where the glow's core sits, as fractions of the hero box: under the station icon. */
private const val HeroCoreX = 0.90f
private const val HeroCoreY = 0.44f
private const val HeroGlowRadiusX = 0.22f
private const val HeroGlowRadiusY = 0.62f

/**
 * Paints the station glow behind the hero: the station's primary container at three alphas over
 * the hero surface, so every colour on screen still comes from the one generated palette.
 */
private fun heroGlowBackground(tint: Color, surface: Color): Modifier = Modifier.drawWithCache {
    val width = size.width
    val height = size.height
    val center = Offset(HeroCoreX * width, HeroCoreY * height)
    val radiusX = HeroGlowRadiusX * width
    val radiusY = HeroGlowRadiusY * height
    val glow = Brush.radialGradient(
        colors = listOf(
            tint.copy(alpha = 0.55f),
            tint.copy(alpha = 0.24f),
            tint.copy(alpha = 0.035f)
        ),
        center = center,
        radius = radiusX
    )
    onDrawBehind {
        drawRect(surface)
        // Pixel-space shader avoids precision artefacts from a one-pixel radial brush on TV GPUs.
        scale(scaleX = 1f, scaleY = radiusY / radiusX, pivot = center) {
            drawCircle(brush = glow, radius = radiusX, center = center)
        }
    }
}

/**
 * The screen-off control, in the status row: a small TV-with-a-slash icon at the end of the line.
 * Its label appears only while it is focused, so the screen never carries a permanent button.
 */
@Composable
private fun ScreenOffControl(onClick: () -> Unit, onNavigateDown: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val actionLabel = stringResource(R.string.screen_off_action)
    var isFocused by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isFocused) {
            Text(
                text = stringResource(R.string.screen_off_button),
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurface.copy(alpha = 0.84f)
            )
            Spacer(Modifier.width(8.dp))
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .testTag("screen-off-button")
                .onFocusChanged { isFocused = it.isFocused }
                .focusable()
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    onClick(label = actionLabel) {
                        onClick()
                        true
                    }
                }
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown &&
                        event.nativeKeyEvent.repeatCount == 0 &&
                        event.key == Key.DirectionDown
                    ) {
                        // Down returns to the station the user came from, not to whatever tile
                        // sits under this control's x position.
                        onNavigateDown()
                        true
                    } else if (event.type == KeyEventType.KeyUp &&
                        (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                    ) {
                        // Fire on release: turning the black frame on during the DOWN would let
                        // the same press's UP wake the screen again.
                        onClick()
                        true
                    } else {
                        false
                    }
                }
                .clip(MaterialTheme.shapes.small)
                // Idle: a quiet glyph over the hero, no pill to float. Focused: a pill so the
                // label and the ring have something to sit on.
                .background(
                    if (isFocused) lerp(colors.surfaceVariant, colors.primary, ControlTint)
                    else Color.Transparent
                )
                .border(
                    width = 3.dp,
                    color = if (isFocused) colors.border else Color.Transparent,
                    shape = MaterialTheme.shapes.small
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(20.dp)) {
                val stroke = 2.dp.toPx()
                drawRoundRect(
                    color = colors.primary,
                    topLeft = Offset(size.width * 0.08f, size.height * 0.20f),
                    size = Size(size.width * 0.84f, size.height * 0.5f),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                    style = Stroke(width = stroke)
                )
                drawLine(
                    color = colors.primary,
                    start = Offset(size.width * 0.18f, size.height * 0.86f),
                    end = Offset(size.width * 0.82f, size.height * 0.12f),
                    strokeWidth = stroke
                )
            }
        }
    }
}

/** Largest hero-name size; comes from the theme's displayLarge. */
private val HeroNameMaxSize = 64.sp

/** Smallest the name may shrink to before an ellipsis is the lesser evil. */
private val HeroNameMinSize = 40.sp

/**
 * The station name, at the theme's 64 sp whenever it fits the hero's text column.
 *
 * The longest station names no longer need the old 96 sp step-down; the loop stays as the guard
 * for a future long name: the name steps down until it fits one line, a cut name is not a name.
 * The floor is 40 sp, below which the ellipsis returns.
 */
@Composable
private fun StationName(name: String) {
    val style = MaterialTheme.typography.displayLarge.copy(
        // The hero is height-starved at 1080p; the font's built-in top and bottom padding
        // would cost ~8 dp and push the status line out.
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )
    BoxWithConstraints(
        // A fixed band, like the track block: a station whose name steps down must not shift
        // the status line. 76 dp is the 64 sp name's natural line box; heightIn lets a larger
        // system font scale grow the band instead of painting the name over the track line.
        modifier = Modifier.heightIn(min = 76.dp),
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
            color = MaterialTheme.colorScheme.onSurface,
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
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatusLine(
    status: PlayerStatus,
    onScreenOff: () -> Unit,
    onNavigateDown: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        // Copy and equalizer keep the hero's text edge; the screen-off control is pinned to the
        // right end of the column so it never shifts as the status copy changes length.
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // The theme's 28 sp semibold: this is the line an elderly user has to read to fix a
            // silent radio ("Nepavyksta prisijungti. Paspausk OK."), so it is the one line that
            // must not be caption-sized. It ellipsises only when the control is focused, the one
            // moment the label needs the room.
            Text(
                text = stringResource(status.textRes()),
                modifier = Modifier.testTag("hero-status"),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.84f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(10.dp))
            EqualizerBars(active = status == PlayerStatus.PLAYING)
        }
        Spacer(Modifier.width(16.dp))
        ScreenOffControl(onScreenOff, onNavigateDown)
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
    // Read outside the draw lambda: a Canvas draw scope is not a composable context.
    val barColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
    Canvas(Modifier.size(18.dp, 18.dp).alpha(if (active) 1f else 0f).testTag("hero-equalizer")) {
        listOf(first, second, third).forEachIndexed { index, level ->
            val barHeight = 3.dp.toPx() + (size.height - 3.dp.toPx()) * level
            drawRoundRect(
                color = barColor,
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
