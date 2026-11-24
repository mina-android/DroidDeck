package com.ma.droiddeck.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallMissed
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ma.droiddeck.data.model.CallDirection
import com.ma.droiddeck.data.model.RecentCallEntry
import com.ma.droiddeck.ui.theme.CallDeclineRed
import com.ma.droiddeck.ui.theme.OledBorder
import com.ma.droiddeck.ui.theme.PureBlack
import com.ma.droiddeck.ui.theme.TextSecondary

/**
 * Compact movable widget showing the 2 most recent calls as standalone circular widgets.
 * Double-tapping a circle places the call inside DroidDeck.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentCallsWidgetCard(
    recentCalls: List<RecentCallEntry>,
    isCallLogGranted: Boolean,
    onDoubleTapCall: (String) -> Unit,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isCallLogGranted) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = PureBlack,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, OledBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = onRequestPermissions)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Call,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tap to allow Call Log for Recent Calls",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }
        }
        return
    }

    if (recentCalls.isEmpty()) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = PureBlack,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, OledBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Call,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "No recent calls",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }
        }
        return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Top
    ) {
        recentCalls.take(4).forEach { call ->
            val dirIcon = when (call.direction) {
                CallDirection.INCOMING -> Icons.AutoMirrored.Rounded.CallReceived
                CallDirection.OUTGOING -> Icons.AutoMirrored.Rounded.CallMade
                CallDirection.MISSED -> Icons.AutoMirrored.Rounded.CallMissed
            }
            val iconTint = if (call.direction == CallDirection.MISSED) {
                CallDeclineRed
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
            }
            val initials = computeInitials(call.displayName)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Surface(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .combinedClickable(
                                onClick = {},
                                onDoubleClick = { onDoubleTapCall(call.number) }
                            ),
                        color = PureBlack,
                        shape = CircleShape,
                        border = BorderStroke(
                            1.dp,
                            if (call.direction == CallDirection.MISSED) {
                                CallDeclineRed.copy(alpha = 0.6f)
                            } else {
                                OledBorder
                            }
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = initials,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .size(18.dp)
                            .background(PureBlack, CircleShape),
                        shape = CircleShape,
                        color = PureBlack,
                        border = BorderStroke(1.dp, OledBorder)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = dirIcon,
                                contentDescription = call.direction.name,
                                tint = iconTint,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = call.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun computeInitials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> "${parts.first().first()}${parts.last().first()}".uppercase()
    }
}
