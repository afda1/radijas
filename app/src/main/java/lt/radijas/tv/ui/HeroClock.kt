package lt.radijas.tv.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.delay

@Immutable
data class ClockDateText(
    val time: String,
    val date: String
)

/**
 * Formats [epochMs] into 24-hour `HH:mm` time and a Lithuanian date string
 * (e.g. `Šeštadienis, lapkričio 14 d.`) using `Calendar` for API 24 compatibility.
 */
internal fun formatClockDate(
    epochMs: Long,
    timeZone: TimeZone = TimeZone.getDefault()
): ClockDateText {
    val calendar = Calendar.getInstance(timeZone).apply { timeInMillis = epochMs }
    val time = String.format(
        Locale.ROOT,
        "%02d:%02d",
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE)
    )
    val weekday = lithuanianWeekday(calendar.get(Calendar.DAY_OF_WEEK))
    val month = lithuanianGenitiveMonth(calendar.get(Calendar.MONTH))
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    return ClockDateText(
        time = time,
        date = "$weekday, $month $day d."
    )
}

private fun lithuanianWeekday(dayOfWeek: Int): String = when (dayOfWeek) {
    Calendar.MONDAY -> "Pirmadienis"
    Calendar.TUESDAY -> "Antradienis"
    Calendar.WEDNESDAY -> "Trečiadienis"
    Calendar.THURSDAY -> "Ketvirtadienis"
    Calendar.FRIDAY -> "Penktadienis"
    Calendar.SATURDAY -> "Šeštadienis"
    Calendar.SUNDAY -> "Sekmadienis"
    else -> error("Unexpected day of week: $dayOfWeek")
}

private fun lithuanianGenitiveMonth(month: Int): String = when (month) {
    Calendar.JANUARY -> "sausio"
    Calendar.FEBRUARY -> "vasario"
    Calendar.MARCH -> "kovo"
    Calendar.APRIL -> "balandžio"
    Calendar.MAY -> "gegužės"
    Calendar.JUNE -> "birželio"
    Calendar.JULY -> "liepos"
    Calendar.AUGUST -> "rugpjūčio"
    Calendar.SEPTEMBER -> "rugsėjo"
    Calendar.OCTOBER -> "spalio"
    Calendar.NOVEMBER -> "lapkričio"
    Calendar.DECEMBER -> "gruodžio"
    else -> error("Unexpected month: $month")
}

/**
 * Tracks the current clock time and date, refreshing on minute boundaries and system
 * time/timezone/date broadcasts.
 */
@Composable
internal fun rememberClockDate(nowMs: () -> Long): ClockDateText {
    val context = LocalContext.current
    var clockDate by remember(nowMs) { mutableStateOf(formatClockDate(nowMs())) }

    LaunchedEffect(nowMs) {
        while (true) {
            val now = nowMs()
            clockDate = formatClockDate(now)
            val remainder = Math.floorMod(now, 60_000L)
            val delayMs = (60_000L - remainder).coerceIn(1_000L, 60_000L)
            delay(delayMs)
        }
    }

    DisposableEffect(context, nowMs) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                clockDate = formatClockDate(nowMs())
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    return clockDate
}

/**
 * Displays the current Lithuanian date and 24-hour clock in the hero's bottom-right footer band,
 * matching the screen-off control height and station palette contrast tokens.
 */
@Composable
internal fun HeroClock(
    modifier: Modifier = Modifier,
    nowMs: () -> Long = System::currentTimeMillis
) {
    val clockDate = rememberClockDate(nowMs)
    val colors = MaterialTheme.colorScheme
    val noFontPad = PlatformTextStyle(includeFontPadding = false)

    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .testTag("hero-clock-container"),
        contentAlignment = Alignment.CenterEnd
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = clockDate.date,
                modifier = Modifier
                    .alignByBaseline()
                    .testTag("hero-date"),
                style = MaterialTheme.typography.labelLarge.copy(platformStyle = noFontPad),
                color = colors.onSurface.copy(alpha = 0.66f),
                maxLines = 1,
                softWrap = false
            )
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(4.dp)
                    .background(colors.primary.copy(alpha = 0.84f), CircleShape)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = clockDate.time,
                modifier = Modifier
                    .alignByBaseline()
                    .testTag("hero-clock"),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFeatureSettings = "tnum",
                    platformStyle = noFontPad
                ),
                color = colors.onSurface.copy(alpha = 0.84f),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
