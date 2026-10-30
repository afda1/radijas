package lt.radijas.tv.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import lt.radijas.tv.data.model.RadioStation
import lt.radijas.tv.ui.theme.RadijasContent
import lt.radijas.tv.ui.theme.onBrand
import lt.radijas.tv.ui.theme.tonalSurface

/**
 * One station in the shelf. This is the only focusable element on the screen: the brand-tinted
 * ring marks where the D-pad is, and OK on it selects the station.
 */
@Composable
fun StationTile(
    station: RadioStation,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onSelect: () -> Unit = {},
) {
    var isFocused by remember { mutableStateOf(false) }
    // Derived colours are remembered: the shelf recomposes on every status tick, and each
    // tonalSurface/onBrand call is a real computation, not a field read.
    val background = remember(station.brandColor, isFocused) {
        tonalSurface(station.brandColor, if (isFocused) 0.34f else 0.15f)
    }
    val accent = remember(station.brandColor) { onBrand(station.brandColor) }
    val labelColor = remember(station.brandColor, isFocused) {
        if (isFocused) accent else RadijasContent.copy(alpha = 0.72f)
    }

    Column(
        modifier = modifier
            .width(176.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
            }
            .focusable()
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
            // The focused tile grows 6 % from its centre, exactly as the design asks. No
            // animation: focus is still the only thing on screen that moves, and it moves
            // immediately, never as a delayed flourish.
            .graphicsLayer {
                val scale = if (isFocused) 1.06f else 1f
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(28.dp))
            .background(background)
            .border(
                width = 3.dp,
                // The ring is the brand itself, lightened until it is readable on the focused
                // surface: a white ring would be the plainest thing on a screen about colour.
                color = if (isFocused) accent else Color.Transparent,
                shape = RoundedCornerShape(28.dp)
            )
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(156.dp)
                .clip(RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(station.iconResId),
                contentDescription = null,
                // Every station asset is a white silhouette on transparency, already trimmed to
                // its mark with a 6 % breathing margin, so Fit fills the square without cropping
                // a wide mark and the brand only ever comes from the surface around it.
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(Color.White),
                modifier = Modifier.size(156.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = station.name,
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 16.sp,
                lineHeight = 22.sp
            ),
            fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
