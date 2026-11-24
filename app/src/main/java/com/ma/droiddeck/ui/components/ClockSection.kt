package com.ma.droiddeck.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ma.droiddeck.data.model.CalendarEventSummary
import com.ma.droiddeck.data.model.ClockStyle
import com.ma.droiddeck.data.model.DroidDeckTimerState
import com.ma.droiddeck.data.model.WeatherState
import com.ma.droiddeck.ui.theme.OledBorder
import com.ma.droiddeck.ui.theme.PureBlack
import com.ma.droiddeck.ui.theme.TextSecondary
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Resizable AOD clock widget supporting 4 clock styles (Modern Digital,
 * Minimal Analog Dial, Bold Condensed, Dual Timezone), date display, and
 * quick Pomodoro focus timer.
 */
@Composable
fun ClockSection(
    currentTime: LocalDateTime,
    use24HourFormat: Boolean,
    showSeconds: Boolean,
    showDate: Boolean,
    clockStyle: ClockStyle,
    clockScale: Float,
    secondaryTimeZoneId: String,
    timerState: DroidDeckTimerState,
    weather: WeatherState,
    showWeatherInClock: Boolean,
    calendarEvent: CalendarEventSummary?,
    isCalendarGranted: Boolean,
    showCalendarInClock: Boolean,
    onRefreshWeather: () -> Unit,
    onOpenCalendarEvent: (Long) -> Unit,
    onRequestCalendarPermission: () -> Unit,
    onToggleTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onCycleTimerPreset: () -> Unit,
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    val scale = clockScale.coerceIn(0.65f, 1.40f)
    val pattern = when {
        use24HourFormat && showSeconds -> "HH:mm:ss"
        use24HourFormat -> "HH:mm"
        showSeconds -> "h:mm:ss"
        else -> "h:mm"
    }
    val timeText = currentTime.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
    val amPmText = if (!use24HourFormat) {
        currentTime.format(DateTimeFormatter.ofPattern("a", Locale.getDefault()))
    } else {
        ""
    }
    val dateText = currentTime.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = if (isLandscape) Alignment.Start else Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
            when (clockStyle) {
                ClockStyle.MINIMAL_ANALOG -> {
                    val dialSize = (140f * scale).dp
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val hourAngle = ((currentTime.hour % 12) + currentTime.minute / 60f) * 30f - 90f
                    val minuteAngle = (currentTime.minute + currentTime.second / 60f) * 6f - 90f
                    val secondAngle = currentTime.second * 6f - 90f

                    Canvas(modifier = Modifier.size(dialSize)) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = size.minDimension / 2f - 4.dp.toPx()

                        drawCircle(
                            color = OledBorder,
                            radius = radius,
                            center = center,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                        )

                        // Hour ticks
                        for (i in 0 until 12) {
                            val rad = Math.toRadians((i * 30.0) - 90.0)
                            val outer = Offset(
                                center.x + (radius * 0.92f * cos(rad)).toFloat(),
                                center.y + (radius * 0.92f * sin(rad)).toFloat()
                            )
                            val inner = Offset(
                                center.x + (radius * 0.80f * cos(rad)).toFloat(),
                                center.y + (radius * 0.80f * sin(rad)).toFloat()
                            )
                            drawLine(
                                color = TextSecondary,
                                start = inner,
                                end = outer,
                                strokeWidth = if (i % 3 == 0) 2.5.dp.toPx() else 1.2.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }

                        // Hour hand
                        val hRad = Math.toRadians(hourAngle.toDouble())
                        drawLine(
                            color = primaryColor,
                            start = center,
                            end = Offset(
                                center.x + (radius * 0.50f * cos(hRad)).toFloat(),
                                center.y + (radius * 0.50f * sin(hRad)).toFloat()
                            ),
                            strokeWidth = 4.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // Minute hand
                        val mRad = Math.toRadians(minuteAngle.toDouble())
                        drawLine(
                            color = primaryColor.copy(alpha = 0.88f),
                            start = center,
                            end = Offset(
                                center.x + (radius * 0.74f * cos(mRad)).toFloat(),
                                center.y + (radius * 0.74f * sin(mRad)).toFloat()
                            ),
                            strokeWidth = 2.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // Optional Second hand
                        if (showSeconds) {
                            val sRad = Math.toRadians(secondAngle.toDouble())
                            drawLine(
                                color = TextSecondary,
                                start = center,
                                end = Offset(
                                    center.x + (radius * 0.82f * cos(sRad)).toFloat(),
                                    center.y + (radius * 0.82f * sin(sRad)).toFloat()
                                ),
                                strokeWidth = 1.2.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }

                        drawCircle(color = primaryColor, radius = 4.dp.toPx(), center = center)
                    }
                }

                ClockStyle.BOLD_CONDENSED,
                ClockStyle.MODERN_DIGITAL,
                ClockStyle.DUAL_TIMEZONE -> {
                    val baseSize = when {
                        showSeconds && isLandscape -> 56f
                        showSeconds -> 66f
                        isLandscape -> 76f
                        else -> 88f
                    }
                    val scaledSp = (baseSize * scale).sp
                    val fontWeight = if (clockStyle == ClockStyle.BOLD_CONDENSED) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Light
                    }

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = scaledSp,
                                lineHeight = (baseSize * scale * 1.06f).sp,
                                fontWeight = fontWeight,
                                letterSpacing = if (clockStyle == ClockStyle.BOLD_CONDENSED) (-3).sp else (-2).sp
                            ),
                            maxLines = 1,
                            softWrap = false,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (amPmText.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = amPmText,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = (15f * scale).sp
                                ),
                                color = TextSecondary,
                                modifier = Modifier.padding(bottom = (14f * scale).dp)
                            )
                        }
                    }

                    if (clockStyle == ClockStyle.DUAL_TIMEZONE) {
                        val zoneId = runCatching { ZoneId.of(secondaryTimeZoneId) }.getOrDefault(ZoneId.of("UTC"))
                        val secondaryTime = ZonedDateTime.now(zoneId).format(
                            DateTimeFormatter.ofPattern(if (use24HourFormat) "HH:mm" else "h:mm a", Locale.getDefault())
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${zoneId.id}: $secondaryTime",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = (14f * scale).sp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            if (showDate) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = (17f * scale).sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
            }

            if (showWeatherInClock) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = PureBlack,
                    border = BorderStroke(1.dp, OledBorder),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.clickable(onClick = onRefreshWeather)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WbSunny,
                            contentDescription = "Weather",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        val tempText = weather.temperatureCelsius?.let {
                            String.format(Locale.US, "%.1f°C", it)
                        } ?: "--°C"
                        Text(
                            text = tempText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (weather.city.isNotBlank()) {
                            Text(
                                text = "• ${weather.city}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                        if (weather.conditionLabel.isNotBlank()) {
                            Text(
                                text = "• ${weather.conditionLabel}",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            if (showCalendarInClock) {
                Spacer(modifier = Modifier.height(8.dp))
                CalendarWidgetCard(
                    event = calendarEvent,
                    isCalendarGranted = isCalendarGranted,
                    use24HourFormat = use24HourFormat,
                    onOpenEvent = onOpenCalendarEvent,
                    onRequestPermissions = onRequestCalendarPermission,
                    modifier = if (isLandscape) Modifier.widthIn(max = 360.dp) else Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick DroidDeck Pomodoro / Focus Timer Pill
            val mins = timerState.remainingSeconds / 60
            val secs = timerState.remainingSeconds % 60
            val timerFormatted = String.format(Locale.US, "%02d:%02d", mins, secs)

            Surface(
                color = PureBlack,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (timerState.isRunning) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    } else {
                        OledBorder
                    }
                ),
                shape = RoundedCornerShape(50)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.clickable(onClick = onCycleTimerPreset),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = "Cycle Timer Preset",
                            tint = TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "${timerState.label} $timerFormatted",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (timerState.isRunning) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            }
                        )
                    }

                    Icon(
                        imageVector = if (timerState.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (timerState.isRunning) "Pause Timer" else "Start Timer",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(19.dp)
                            .clickable(onClick = onToggleTimer)
                    )

                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Reset Timer",
                        tint = TextSecondary,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable(onClick = onResetTimer)
                    )
                }
            }
        }
}
