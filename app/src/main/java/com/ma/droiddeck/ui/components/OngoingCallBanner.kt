package com.ma.droiddeck.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ma.droiddeck.data.model.OngoingCallState
import com.ma.droiddeck.ui.theme.CallAcceptGreen
import com.ma.droiddeck.ui.theme.CallDeclineRed
import com.ma.droiddeck.ui.theme.OledBorder
import com.ma.droiddeck.ui.theme.PureBlack
import com.ma.droiddeck.ui.theme.TextSecondary
import java.util.Locale

/**
 * In-app active/ongoing call widget displaying caller name, live call duration,
 * Speakerphone toggle, Mic Mute toggle, and End Call button.
 */
@Composable
fun OngoingCallBanner(
    call: OngoingCallState,
    durationSeconds: Long,
    isSpeakerphoneOn: Boolean,
    isMicMuted: Boolean,
    onToggleSpeakerphone: () -> Unit,
    onToggleMicMute: () -> Unit,
    onEndCall: (OngoingCallState) -> Unit,
    modifier: Modifier = Modifier
) {
    val mins = durationSeconds / 60L
    val secs = durationSeconds % 60L
    val timerText = String.format(Locale.US, "%02d:%02d", mins, secs)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = PureBlack,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CallAcceptGreen.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${call.appName} • $timerText",
                    style = MaterialTheme.typography.labelMedium,
                    color = CallAcceptGreen
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = call.callerName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mic Mute
                Surface(
                    shape = CircleShape,
                    color = if (isMicMuted) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else PureBlack,
                    border = BorderStroke(
                        1.dp,
                        if (isMicMuted) MaterialTheme.colorScheme.primary else OledBorder
                    )
                ) {
                    IconButton(
                        onClick = onToggleMicMute,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isMicMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                            contentDescription = "Toggle Mic Mute",
                            tint = if (isMicMuted) MaterialTheme.colorScheme.primary else TextSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                // Speakerphone
                Surface(
                    shape = CircleShape,
                    color = if (isSpeakerphoneOn) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else PureBlack,
                    border = BorderStroke(
                        1.dp,
                        if (isSpeakerphoneOn) MaterialTheme.colorScheme.primary else OledBorder
                    )
                ) {
                    IconButton(
                        onClick = onToggleSpeakerphone,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = "Toggle Speakerphone",
                            tint = if (isSpeakerphoneOn) MaterialTheme.colorScheme.primary else TextSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                // End Call
                Surface(
                    shape = CircleShape,
                    color = CallDeclineRed.copy(alpha = 0.22f),
                    border = BorderStroke(1.dp, CallDeclineRed)
                ) {
                    IconButton(
                        onClick = { onEndCall(call) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CallEnd,
                            contentDescription = "End Call",
                            tint = CallDeclineRed,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}
