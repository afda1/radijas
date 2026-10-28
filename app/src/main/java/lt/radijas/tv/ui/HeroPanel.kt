package lt.radijas.tv.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 */
@Composable
fun HeroPanel(station: RadioStation, modifier: Modifier = Modifier) {
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
