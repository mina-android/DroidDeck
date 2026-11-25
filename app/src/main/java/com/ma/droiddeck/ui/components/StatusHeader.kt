package com.ma.droiddeck.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.DoNotDisturbOff
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ma.droiddeck.data.model.DeviceStatusState
import com.ma.droiddeck.data.model.RingerModeState
import com.ma.droiddeck.ui.theme.OledBorder
import com.ma.droiddeck.ui.theme.OledSubtleSurface
import com.ma.droiddeck.ui.theme.TextSecondary
import java.util.Locale

/**
 * Enlarged OLED top status bar displaying battery percentage, charging state, temperature,
 * network/Bluetooth connectivity, dedicated DND button, Sound/Vibrate/Mute cycling pill,
 * Rotate toggle, and Settings trigger.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StatusHeader(
    status: DeviceStatusState,
    isCarModeEnabled: Boolean,
    onToggleCarMode: () -> Unit,
    onToggleDnd: () -> Unit,
    onCycleRingerMode: () -> Unit,
    onToggleRotate: () -> Unit,
    onResetAutoRotate: () -> Unit,
    onOpenSettings: () -> Unit,
    onExitApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Exit Button + Enlarged Battery & Connectivity indicators
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onExitApp,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Exit DroidDeck",
                    tint = TextSecondary,
                    modifier = Modifier.size(19.dp)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (status.isCharging) {
                        Icons.Rounded.BatteryChargingFull
                    } else {
                        Icons.Rounded.BatteryFull
                    },
                    contentDescription = "Battery",
                    tint = if (status.isCharging) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        TextSecondary
                    },
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = String.format(
                        Locale.US,
                        "%d%% • %.0f°C",
                        status.batteryLevel,
                        status.batteryTempCelsius
                    ),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)
                )
            }

            if (status.isWifiConnected) {
                Icon(
                    imageVector = Icons.Rounded.Wifi,
                    contentDescription = "Wi-Fi Connected",
                    tint = TextSecondary,
                    modifier = Modifier.size(19.dp)
                )
            }

            if (status.isCellularConnected) {
                Icon(
                    imageVector = Icons.Rounded.SignalCellularAlt,
                    contentDescription = "Cellular Connected",
                    tint = TextSecondary,
                    modifier = Modifier.size(19.dp)
                )
            }

            if (status.isBluetoothConnected) {
                Icon(
                    imageVector = Icons.Rounded.Bluetooth,
                    contentDescription = "Bluetooth Enabled",
                    tint = TextSecondary,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        // Right: Car Mode Pill, Dedicated DND Pill, Sound/Vibrate/Mute Pill, Rotate Toggle, Settings
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Car Mode (Max Brightness) Button
            Surface(
                color = if (isCarModeEnabled) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                } else {
                    OledSubtleSurface
                },
                border = BorderStroke(
                    1.dp,
                    if (isCarModeEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else OledBorder
                ),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onToggleCarMode)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DirectionsCar,
                        contentDescription = "Toggle Car Mode (Max Brightness)",
                        tint = if (isCarModeEnabled) MaterialTheme.colorScheme.primary else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Car",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                        color = if (isCarModeEnabled) MaterialTheme.colorScheme.primary else TextSecondary
                    )
                }
            }

            // Dedicated DND Button
            Surface(
                color = if (status.isDndActive) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                } else {
                    OledSubtleSurface
                },
                border = BorderStroke(
                    1.dp,
                    if (status.isDndActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else OledBorder
                ),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onToggleDnd)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (status.isDndActive) {
                            Icons.Rounded.DoNotDisturbOn
                        } else {
                            Icons.Rounded.DoNotDisturbOff
                        },
                        contentDescription = "Toggle Do Not Disturb",
                        tint = if (status.isDndActive) MaterialTheme.colorScheme.primary else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "DND",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                        color = if (status.isDndActive) MaterialTheme.colorScheme.primary else TextSecondary
                    )
                }
            }

            // Sound / Vibrate / Mute Cycling Pill
            val ringerIcon = when (status.ringerMode) {
                RingerModeState.SOUND -> Icons.AutoMirrored.Rounded.VolumeUp
                RingerModeState.VIBRATE -> Icons.Rounded.Vibration
                RingerModeState.MUTE -> Icons.AutoMirrored.Rounded.VolumeOff
            }
            val isMutedOrVibrate = status.ringerMode != RingerModeState.SOUND

            Surface(
                color = if (isMutedOrVibrate) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                } else {
                    OledSubtleSurface
                },
                border = BorderStroke(
                    1.dp,
                    if (isMutedOrVibrate) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f) else OledBorder
                ),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onCycleRingerMode)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = ringerIcon,
                        contentDescription = "Cycle Sound, Vibrate, Mute",
                        tint = if (isMutedOrVibrate) MaterialTheme.colorScheme.primary else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = status.ringerMode.label,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                        color = if (isMutedOrVibrate) MaterialTheme.colorScheme.primary else TextSecondary
                    )
                }
            }

            // Enlarged On-Screen Rotate Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .combinedClickable(
                        onClick = onToggleRotate,
                        onLongClick = onResetAutoRotate
                    )
            ) {
                Icon(
                    imageVector = Icons.Rounded.ScreenRotation,
                    contentDescription = "Rotate Screen",
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Enlarged Settings Button
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Open DroidDeck Settings",
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
