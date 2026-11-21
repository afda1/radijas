package lt.radijas.tv.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
) {
    val weekday: String get() = date.substringBefore(", ")
    val monthDay: String get() = date.substringAfter(", ", "")
}

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
 * Displays the current 24-hour clock and two-line Lithuanian date in the hero's top-right corner,
 * keeping station identity clean while pairing time and date naturally with what is playing.
 */
@Composable
internal fun HeroClock(
    modifier: Modifier = Modifier,
    nowMs: () -> Long = System::currentTimeMillis
) {
    val clockDate = rememberClockDate(nowMs)
    val colors = MaterialTheme.colorScheme
    val noFontPad = PlatformTextStyle(includeFontPadding = false)

    Column(
        modifier = modifier.testTag("hero-clock-container"),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = clockDate.time,
            modifier = Modifier.testTag("hero-clock"),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 60.sp,
                lineHeight = 64.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = (-2.0).sp,
                fontFeatureSettings = "tnum",
                platformStyle = noFontPad
            ),
            color = colors.onSurface.copy(alpha = 0.92f),
            maxLines = 1,
            softWrap = false
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "${clockDate.weekday}\n${clockDate.monthDay}",
            modifier = Modifier.testTag("hero-date"),
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Normal,
                platformStyle = noFontPad
            ),
            color = colors.onSurface.copy(alpha = 0.74f),
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
