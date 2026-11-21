package lt.radijas.tv.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import lt.radijas.tv.R
import lt.radijas.tv.data.model.RadioStation
import lt.radijas.tv.ui.theme.logoAspect
import lt.radijas.tv.ui.theme.stationLogoWidthFraction
import lt.radijas.tv.ui.theme.stationScheme

/**
 * One station in the shelf. This is the only focusable element on the screen: the brand-tinted
 * ring marks where the D-pad is, and OK on it selects the station.
 *
 * It is also an accessibility button: the screen has no other control, so a tile that assistive
 * technology cannot press would make the app unusable for a low-vision user. The playing tile
 * says „Groja" (state), uses a medium label weight and carries a small accent dot, while the
 * outer ring belongs exclusively to the focused item.
 */
@Composable
fun StationTile(
    station: RadioStation,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onSelect: () -> Unit = {},
    isPlaying: Boolean = false,
    onFocused: () -> Unit = {},
) {
    var isFocused by remember { mutableStateOf(false) }
    // Resting Nest stone cards stay quiet with a subtle 22% tint of their own palette; an
    // unfocused playing card blooms gently at 65%, while only the focused tile lights up to
    // full primaryContainer with a bright 3 dp ring.
    val colors = remember(station.id) { stationScheme(station.id) }
    val restingBackground = remember(colors) { lerp(colors.surface, colors.primaryContainer, 0.22f) }
    val playingBackground = remember(colors) { lerp(colors.surface, colors.primaryContainer, 0.65f) }
    val background = when {
        isFocused -> colors.primaryContainer
        isPlaying -> playingBackground
        else -> restingBackground
    }
    val accent = colors.border
    val labelColor = if (isFocused || isPlaying) colors.onPrimaryContainer else colors.onSurface.copy(alpha = 0.80f)
    val logoTint = if (isFocused || isPlaying) Color.White else Color.White.copy(alpha = 0.88f)
    val playingLabel = stringResource(R.string.station_playing)
    val selectLabel = stringResource(R.string.station_select)

    Column(
        modifier = modifier
            .width(208.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (focusState.isFocused) onFocused()
            }
            .focusable()
            // Accessibility action, not a click modifier: the raw D-pad key handling below is
            // invisible to an accessibility service, so without this a TalkBack user could focus
            // a station but never start it.
            .semantics(mergeDescendants = true) {
                role = Role.Button
                if (isPlaying) stateDescription = playingLabel
                onClick(label = selectLabel) {
                    onSelect()
                    true
                }
            }
            .onKeyEvent { event ->
                // Only the first KeyDown: a held OK repeats KeyDown and must not queue a
                // second start/retry behind the first one.
                if (event.type == KeyEventType.KeyDown &&
                    event.nativeKeyEvent.repeatCount == 0 &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                ) {
                    onSelect()
                    true
                } else {
                    false
                }
            }
            .testTag("station-tile-${station.id}")
            // The focused tile grows 6 % from its centre immediately, keeping D-pad response crisp.
            .graphicsLayer {
                val scale = if (isFocused) 1.06f else 1f
                scaleX = scale
                scaleY = scale
            }
            .clip(MaterialTheme.shapes.extraLarge)
            .background(background)
            .border(
                width = if (isFocused) 3.dp else 1.dp,
                // Outer 3 dp focus ring belongs strictly to the focused item; resting tiles keep a
                // quiet hairline border matching the smoked Nest card above.
                color = when {
                    isFocused -> accent
                    isPlaying -> colors.onSurface.copy(alpha = 0.14f)
                    else -> colors.onSurface.copy(alpha = 0.09f)
                },
                shape = MaterialTheme.shapes.extraLarge
            )
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Drawn at its optically normalised width, not fitted to the box: stationLogoWidthFraction
        // keeps dense compact marks from outweighing the wide wordmarks.
        val painter = painterResource(station.iconResId)
        val aspect = remember(painter) { logoAspect(painter) }
        val logoWidth = 156.dp * (stationLogoWidthFraction[station.id] ?: 1f)
        Box(
            modifier = Modifier
                .size(width = 164.dp, height = 106.dp)
                .clip(RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(logoTint),
                modifier = Modifier.size(width = logoWidth, height = logoWidth / aspect)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(accent, CircleShape)
                        .testTag("station-tile-playing-dot-${station.id}")
                )
            }
            Text(
                text = station.name,
                modifier = Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = when {
                    isFocused -> FontWeight.SemiBold
                    isPlaying -> FontWeight.Medium
                    else -> FontWeight.Normal
                },
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
