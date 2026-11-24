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
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
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
import com.ma.droiddeck.data.model.IncomingCallAlert
import com.ma.droiddeck.ui.theme.CallAcceptGreen
import com.ma.droiddeck.ui.theme.CallDeclineRed
import com.ma.droiddeck.ui.theme.PureBlack
import com.ma.droiddeck.ui.theme.TextSecondary

/**
 * Interactive incoming call alert banner supporting cellular and VoIP calls
 * with reliable CallStyle + TelecomManager Answer, Decline, and Mute Ringer controls.
 */
@Composable
fun IncomingCallBanner(
    call: IncomingCallAlert,
    onAnswerCall: (IncomingCallAlert) -> Unit,
    onDeclineCall: (IncomingCallAlert) -> Unit,
    onMuteRinger: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = PureBlack,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, CallAcceptGreen.copy(alpha = 0.7f))
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
                    text = "${call.appName} • Incoming Call",
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
                if (call.callerSubtitle.isNotBlank()) {
                    Text(
                        text = call.callerSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = PureBlack,
                    border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.4f))
                ) {
                    IconButton(
                        onClick = onMuteRinger,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.VolumeOff,
                            contentDescription = "Mute Ringer",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = CallDeclineRed.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, CallDeclineRed)
                ) {
                    IconButton(
                        onClick = { onDeclineCall(call) },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CallEnd,
                            contentDescription = "Decline Call",
                            tint = CallDeclineRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = CallAcceptGreen.copy(alpha = 0.22f),
                    border = BorderStroke(1.dp, CallAcceptGreen)
                ) {
                    IconButton(
                        onClick = { onAnswerCall(call) },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Call,
                            contentDescription = "Answer Call",
                            tint = CallAcceptGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
