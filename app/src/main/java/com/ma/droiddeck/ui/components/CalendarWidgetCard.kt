package com.ma.droiddeck.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ma.droiddeck.data.model.CalendarEventSummary
import com.ma.droiddeck.ui.theme.OledBorder
import com.ma.droiddeck.ui.theme.PureBlack
import com.ma.droiddeck.ui.theme.TextSecondary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Pure-black OLED widget displaying the next upcoming Calendar event and countdown.
 */
@Composable
fun CalendarWidgetCard(
    event: CalendarEventSummary?,
    isCalendarGranted: Boolean,
    use24HourFormat: Boolean,
    onOpenEvent: (Long) -> Unit,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                if (!isCalendarGranted) {
                    onRequestPermissions()
                } else if (event != null) {
                    onOpenEvent(event.eventId)
                }
            },
        color = PureBlack,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, OledBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarToday,
                    contentDescription = "Next Meeting",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = when {
                            !isCalendarGranted -> "Tap to grant Calendar access"
                            event == null -> "No upcoming events today"
                            else -> event.title
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = if (event != null) MaterialTheme.colorScheme.onSurface else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (event != null) {
                        val pattern = if (use24HourFormat) "HH:mm" else "h:mm a"
                        val startStr = Instant.ofEpochMilli(event.startTimeMs)
                            .atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
                        val minsUntil = ((event.startTimeMs - System.currentTimeMillis()) / 60_000L).coerceAtLeast(0L)
                        val countdownStr = formatCountdown(minsUntil)
                        val subtitle = if (event.location.isNotBlank()) {
                            "$startStr • $countdownStr • ${event.location}"
                        } else {
                            "$startStr • $countdownStr"
                        }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

fun formatCountdown(minutes: Long): String = when {
    minutes <= 0L -> "now"
    minutes < 60L -> "in ${minutes}m"
    minutes % 60L == 0L -> "in ${minutes / 60L}h"
    else -> "in ${minutes / 60L}h ${minutes % 60L}m"
}
