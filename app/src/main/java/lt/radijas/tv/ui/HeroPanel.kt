package lt.radijas.tv.ui

import android.os.SystemClock
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.scale
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

/**
 * Now-playing information and the screen-darkening control.
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
    modifier: Modifier = Modifier,
    nowMs: () -> Long = System::currentTimeMillis
) {
    val colors = MaterialTheme.colorScheme
    val animatedSurface by animateColorAsState(
        targetValue = colors.surface,
        animationSpec = tween(durationMillis = 450),
        label = "hero-surface"
    )
    val animatedTint by animateColorAsState(
        targetValue = colors.primaryContainer,
        animationSpec = tween(durationMillis = 450),
        label = "hero-tint"
    )
    val animatedPrimary by animateColorAsState(
        targetValue = colors.primary,
        animationSpec = tween(durationMillis = 450),
        label = "hero-primary"
    )
    // Remembered, not rebuilt: steady-state ticks skip rebuilding the gradient modifier while
    // station changes smoothly crossfade over 450 ms.
    val heroBackground = remember(animatedTint, animatedSurface) {
        heroGlowBackground(animatedTint, animatedSurface.copy(alpha = 0.74f))
    }

    Box(
        modifier = modifier
            .testTag("hero-panel")
            .clip(MaterialTheme.shapes.extraLarge)
            // Smoked translucent surface letting the full-screen atmospheric glow breathe through.
            .then(heroBackground)
            .border(
                width = 1.dp,
                color = colors.onSurface.copy(alpha = 0.09f),
                shape = MaterialTheme.shapes.extraLarge
            )
            .padding(horizontal = 44.dp, vertical = 18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    StationName(station.name)
                    Spacer(Modifier.height(2.dp))
                    TrackTitleLine(trackTitle)
                    Spacer(Modifier.height(8.dp))
                    StatusLine(status, equalizerColor = animatedPrimary)
                }
                Spacer(Modifier.width(24.dp))
                HeroClock(nowMs = nowMs)
            }
            ScreenOffControl(onScreenOff, onNavigateDown)
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
/** Where the glow's core sits, as fractions of the hero box: top-right ambient wash. */
private const val HeroCoreX = 0.86f
private const val HeroCoreY = 0.24f
private const val HeroGlowRadiusX = 0.48f
private const val HeroGlowRadiusY = 0.90f

/**
 * Paints the station glow behind the hero: the station's primary container at three alphas over
 * the translucent hero surface, so every colour on screen still comes from the one generated palette.
 */
private fun heroGlowBackground(tint: Color, surface: Color): Modifier = Modifier.drawWithCache {
    val width = size.width
    val height = size.height
    val center = Offset(HeroCoreX * width, HeroCoreY * height)
    val radiusX = HeroGlowRadiusX * width
    val radiusY = HeroGlowRadiusY * height
    val glow = Brush.radialGradient(
        colors = listOf(
            tint.copy(alpha = 0.38f),
            tint.copy(alpha = 0.15f),
            Color.Transparent
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
 * Google TV pill control for darkening the screen, with a quiet tonal surface at rest and a
 * high-contrast primary fill when focused.
 */
@Composable
private fun ScreenOffControl(onClick: () -> Unit, onNavigateDown: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val actionLabel = stringResource(R.string.screen_off_action)
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .heightIn(min = 44.dp)
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
            .clip(CircleShape)
            .background(
                if (isFocused) colors.primary
                else colors.onSurface.copy(alpha = 0.08f)
            )
            .border(
                width = 1.dp,
                color = if (isFocused) Color.Transparent else colors.onSurface.copy(alpha = 0.10f),
                shape = CircleShape
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(16.dp))
        Image(
            painter = painterResource(R.drawable.ic_screen_dark),
            contentDescription = null,
            colorFilter = ColorFilter.tint(if (isFocused) colors.onPrimary else colors.primary),
            modifier = Modifier
                .size(22.dp)
                .testTag("screen-off-icon")
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.screen_off_button),
            style = MaterialTheme.typography.labelLarge,
            color = if (isFocused) colors.onPrimary else colors.onSurface
        )
        Spacer(Modifier.width(18.dp))
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
        // the status line. 70 dp is the 64 sp name's tight line box; heightIn lets a larger
        // system font scale grow the band instead of painting the name over the track line.
        modifier = Modifier.heightIn(min = 70.dp),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackTitleLine(trackTitle: String?) {
    // A fixed-height block that is composed in both cases: the empty state reserves the
    // headlineSmall line box so the station name and status line never move when a title arrives.
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().heightIn(min = 38.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val style = MaterialTheme.typography.headlineSmall
        val measurer = rememberTextMeasurer()
        val fontScale = LocalDensity.current.fontScale
        val availableWidth = constraints.maxWidth
        val title = trackTitle.orEmpty()
        val titleAlpha = remember { Animatable(1f) }
        var previousTitle by remember { mutableStateOf(title) }
        LaunchedEffect(title) {
            if (title != previousTitle) {
                previousTitle = title
                if (title.isNotEmpty()) {
                    titleAlpha.snapTo(0.15f)
                    titleAlpha.animateTo(1f, tween(durationMillis = 200))
                } else {
                    titleAlpha.snapTo(1f)
                }
            }
        }
        val textWidth = remember(title, style, availableWidth, fontScale) {
            measurer.measure(
                text = AnnotatedString(title),
                style = style,
                maxLines = 1,
                softWrap = false
            ).size.width
        }
        val overflows = textWidth > availableWidth
        // Mask only the right edge so overflowing track titles remain flush-left aligned with the
        // station name and equalizer column.
        val fade = if (overflows) Modifier
            .testTag("hero-track-fade")
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val edge = minOf(28.dp.toPx(), size.width / 2f)
                if (edge > 0f) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            listOf(Color.Black, Color.Transparent),
                            startX = size.width - edge,
                            endX = size.width
                        ),
                        topLeft = Offset(size.width - edge, 0f),
                        size = Size(edge, size.height),
                        blendMode = BlendMode.DstIn
                    )
                }
            } else Modifier
        Box(modifier = fade) {
            key(trackTitle) {
                Text(
                    text = title,
                    modifier = Modifier
                        .graphicsLayer { alpha = titleAlpha.value }
                        .testTag("hero-track-title")
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            initialDelayMillis = 2_000,
                            repeatDelayMillis = 2_000,
                            spacing = MarqueeSpacing(48.dp),
                            velocity = 25.dp
                        ),
                    style = style,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun StatusLine(
    status: PlayerStatus,
    equalizerColor: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Keep instructions readable when playback is not active.
            if (status != PlayerStatus.PLAYING) Text(
                text = stringResource(status.textRes()),
                modifier = Modifier
                    .testTag("hero-status")
                    .semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.84f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (status == PlayerStatus.PLAYING) EqualizerBars(active = true, barColor = equalizerColor)
        }
    }
}

/** Three recent PCM levels; the canvas has fixed bounds so the status copy never moves. */
@Composable
private fun EqualizerBars(active: Boolean, barColor: Color = MaterialTheme.colorScheme.primary) {
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
    Canvas(Modifier.size(28.dp, 22.dp).alpha(if (active) 1f else 0f).testTag("hero-equalizer")) {
        listOf(first, second, third).forEachIndexed { index, level ->
            val barHeight = 4.dp.toPx() + (size.height - 4.dp.toPx()) * level
            drawRoundRect(
                color = barColor,
                topLeft = Offset(index * 11.dp.toPx(), size.height - barHeight),
                size = Size(6.dp.toPx(), barHeight), cornerRadius = CornerRadius(3.dp.toPx())
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
