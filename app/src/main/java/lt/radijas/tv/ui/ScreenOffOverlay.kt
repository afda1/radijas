package lt.radijas.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import lt.radijas.tv.R
import lt.radijas.tv.ui.theme.ScreenOffBlack

/**
 * The black frame that stands in for "screen off". Any remote key brings the UI back; see
 * `MainActivity.dispatchKeyEvent`. The hint is shown for a couple of seconds so a black screen
 * does not look like a crash, then the frame is pure black.
 */
@Composable
fun ScreenOffOverlay(hintVisible: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenOffBlack)
            .testTag("screen-off-overlay"),
        contentAlignment = Alignment.Center
    ) {
        if (hintVisible) {
            Text(
                text = stringResource(R.string.screen_off_hint),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 48.dp)
            )
        }
    }
}
