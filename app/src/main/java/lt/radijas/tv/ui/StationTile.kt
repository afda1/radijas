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
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import lt.radijas.tv.data.model.RadioStation

/**
 * One station in the shelf. This is the only focusable element on the screen: the white ring
 * marks where the D-pad is, and OK on it selects the station.
 */
@Composable
fun StationTile(
    station: RadioStation,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onSelect: () -> Unit = {},
) {
    var isFocused by remember { mutableStateOf(false) }

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
            .border(
                width = 3.dp,
                color = if (isFocused) Color.White else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(156.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(station.iconResId),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(156.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = station.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isFocused) Color.White else Color.White.copy(alpha = 0.72f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
