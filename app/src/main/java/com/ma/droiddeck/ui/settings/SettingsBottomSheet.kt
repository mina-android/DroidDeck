package com.ma.droiddeck.ui.settings

import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ma.droiddeck.BuildConfig
import com.ma.droiddeck.data.model.AccentTheme
import com.ma.droiddeck.data.model.AppFontFamily
import com.ma.droiddeck.data.model.ClockStyle
import com.ma.droiddeck.data.model.DashboardWidgetType
import com.ma.droiddeck.data.model.DroidDeckSettings
import com.ma.droiddeck.data.model.QuickAppEntry
import com.ma.droiddeck.ui.theme.CallAcceptGreen
import com.ma.droiddeck.ui.theme.OledBorder
import com.ma.droiddeck.ui.theme.PureBlack
import com.ma.droiddeck.ui.theme.TextSecondary
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/**
 * Pure-black Modal Bottom Sheet for configuring permissions, clock style/size,
 * expensy-style App Font selector, brightness dimming levels, widget visibility,
 * notification blacklist, quick apps, weather city, backup/restore, and About section.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsBottomSheet(
    settings: DroidDeckSettings,
    installedApps: List<QuickAppEntry>,
    isNotificationAccessGranted: Boolean,
    isDndAccessGranted: Boolean,
    isCallControlGranted: Boolean,
    isCalendarGranted: Boolean,
    isCallLogGranted: Boolean,
    isOverlayGranted: Boolean,
    onUpdateSettings: ((DroidDeckSettings) -> DroidDeckSettings) -> Unit,
    onToggleWidgetVisibility: (DashboardWidgetType) -> Unit,
    onToggleBlacklistPackage: (String) -> Unit,
    onToggleQuickAppPackage: (String) -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onRequestDndAccess: () -> Unit,
    onRequestOverlayAccess: () -> Unit,
    onRequestRuntimePermissions: () -> Unit,
    onResetAutoRotate: () -> Unit,
    onResetWidgetOrder: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var showDeveloperDialog by remember { mutableStateOf(false) }
    var showAppPickerDialog by remember { mutableStateOf(false) }
    var showBlacklistDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PureBlack,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "DroidDeck Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // ── System Permissions ─────────────────────────────────────────
            SectionTitle("System Permissions")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRequestNotificationAccess,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, OledBorder)
                ) {
                    Text(
                        text = if (isNotificationAccessGranted) "Notifications: OK" else "Notifications",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                OutlinedButton(
                    onClick = onRequestDndAccess,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, OledBorder)
                ) {
                    Text(
                        text = if (isDndAccessGranted) "DND Policy: OK" else "DND Policy",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            OutlinedButton(
                onClick = onRequestRuntimePermissions,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, OledBorder)
            ) {
                val allGranted = isCallControlGranted && isCalendarGranted && isCallLogGranted
                Text(
                    text = if (allGranted) "Calls, Call Log & Calendar: Granted" else "Grant Calls, Call Log & Calendar Access",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            OutlinedButton(
                onClick = onRequestOverlayAccess,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, OledBorder)
            ) {
                Text(
                    text = if (isOverlayGranted) {
                        "Display Over Other Apps (Call Shield): Granted"
                    } else {
                        "Grant Display Over Other Apps (Call Shield)"
                    },
                    style = MaterialTheme.typography.labelMedium
                )
            }

            HorizontalDivider(color = OledBorder)

            // ── Clock Style, Size & Theme ──────────────────────────────────
            SectionTitle("Clock & OLED Theme")
            SettingSwitchRow(
                title = "24-Hour Clock Format",
                checked = settings.use24HourFormat,
                onCheckedChange = { v -> onUpdateSettings { it.copy(use24HourFormat = v) } }
            )
            SettingSwitchRow(
                title = "Show Seconds",
                checked = settings.showSeconds,
                onCheckedChange = { v -> onUpdateSettings { it.copy(showSeconds = v) } }
            )
            SettingSwitchRow(
                title = "Show Date Line",
                checked = settings.showDate,
                onCheckedChange = { v -> onUpdateSettings { it.copy(showDate = v) } }
            )

            Text("Clock Face Style", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ClockStyle.entries.forEach { style ->
                    FilterChip(
                        selected = settings.clockStyle == style,
                        onClick = { onUpdateSettings { it.copy(clockStyle = style) } },
                        label = { Text(style.label, style = MaterialTheme.typography.labelMedium) },
                        colors = chipColors()
                    )
                }
            }

            Text(
                text = "Clock Size (${(settings.clockScale * 100).toInt()}%)",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("S" to 0.75f, "M" to 0.95f, "L" to 1.15f, "XL" to 1.35f).forEach { (label, scaleVal) ->
                    FilterChip(
                        selected = kotlin.math.abs(settings.clockScale - scaleVal) < 0.06f,
                        onClick = { onUpdateSettings { it.copy(clockScale = scaleVal) } },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                        colors = chipColors()
                    )
                }
            }
            Slider(
                value = settings.clockScale,
                onValueChange = { v -> onUpdateSettings { it.copy(clockScale = v) } },
                valueRange = 0.65f..1.40f
            )

            Text("Accent Color", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AccentTheme.entries.forEach { theme ->
                    FilterChip(
                        selected = settings.accentTheme == theme,
                        onClick = { onUpdateSettings { it.copy(accentTheme = theme) } },
                        label = { Text(theme.label, style = MaterialTheme.typography.labelMedium) },
                        colors = chipColors()
                    )
                }
            }

            HorizontalDivider(color = OledBorder)

            // ── App Font (expensy-style) ───────────────────────────────────
            SectionTitle("App Font")
            Surface(
                color = PureBlack,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, OledBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Active: ${settings.appFont.label}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AppFontFamily.entries.forEach { font ->
                            FilterChip(
                                selected = settings.appFont == font,
                                onClick = { onUpdateSettings { it.copy(appFont = font) } },
                                label = { Text(font.label, style = MaterialTheme.typography.labelMedium) },
                                colors = chipColors()
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = OledBorder)

            // ── Lockscreen & Popups ────────────────────────────────────────
            SectionTitle("Lockscreen & Notifications")
            SettingSwitchRow(
                title = "Show Over Lockscreen",
                subtitle = "Allow DroidDeck to appear when the phone is locked.",
                checked = settings.showOnLockScreen,
                onCheckedChange = { v -> onUpdateSettings { it.copy(showOnLockScreen = v) } }
            )
            SettingSwitchRow(
                title = "Suppress System Heads-Up Popups",
                subtitle = "Hides Android banners & call popups over DroidDeck (requires DND Policy).",
                checked = settings.suppressHeadsUpWhileActive,
                onCheckedChange = { v -> onUpdateSettings { it.copy(suppressHeadsUpWhileActive = v) } }
            )
            OutlinedButton(
                onClick = { showBlacklistDialog = true },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, OledBorder)
            ) {
                Text(
                    "Notification App Filter (${settings.blacklistedNotificationPackages.size} muted)",
                    style = MaterialTheme.typography.labelMedium
                )
            }

            HorizontalDivider(color = OledBorder)

            // ── Widgets, Quick Apps & Weather ──────────────────────────────
            SectionTitle("Widgets & Dashboard")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DashboardWidgetType.entries.forEach { widget ->
                    val isVisible = !settings.hiddenWidgets.contains(widget)
                    FilterChip(
                        selected = isVisible,
                        onClick = { onToggleWidgetVisibility(widget) },
                        label = { Text(widget.label, style = MaterialTheme.typography.labelMedium) },
                        colors = chipColors()
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showAppPickerDialog = true },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, OledBorder)
                ) {
                    Text(
                        "Quick Apps (${settings.quickAppPackages.size}/4)",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                OutlinedButton(
                    onClick = onResetWidgetOrder,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, OledBorder)
                ) {
                    Text("Reset Layout", style = MaterialTheme.typography.labelMedium)
                }
            }

            var cityInput by remember { mutableStateOf(settings.weatherCity) }
            LaunchedEffect(settings.weatherCity) {
                if (settings.weatherCity != cityInput && cityInput.isEmpty()) {
                    cityInput = settings.weatherCity
                }
            }
            OutlinedTextField(
                value = cityInput,
                onValueChange = {
                    cityInput = it
                    onUpdateSettings { s -> s.copy(weatherCity = it.trim()) }
                },
                label = { Text("Weather City (Open-Meteo)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            SettingSwitchRow(
                title = "Show Weather in Clock",
                subtitle = "Displays temperature and condition inside the Clock widget.",
                checked = settings.showWeatherInClock,
                onCheckedChange = { v -> onUpdateSettings { it.copy(showWeatherInClock = v) } }
            )
            SettingSwitchRow(
                title = "Show Calendar in Clock",
                subtitle = "Displays next upcoming meeting inside the Clock widget.",
                checked = settings.showCalendarInClock,
                onCheckedChange = { v -> onUpdateSettings { it.copy(showCalendarInClock = v) } }
            )

            HorizontalDivider(color = OledBorder)

            // ── Working Hours Lockscreen Auto-Launch ───────────────────────
            SectionTitle("Working Hours Auto-Launch")
            SettingSwitchRow(
                title = "Auto-Open When Screen Locks",
                subtitle = "Opens DroidDeck during selected days and hours.",
                checked = settings.workingHoursEnabled,
                onCheckedChange = { v -> onUpdateSettings { it.copy(workingHoursEnabled = v) } }
            )

            if (settings.workingHoursEnabled) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DayOfWeek.entries.forEach { day ->
                        val selected = settings.workingDays.contains(day)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                onUpdateSettings { current ->
                                    val next = current.workingDays.toMutableSet()
                                    if (selected) next.remove(day) else next.add(day)
                                    current.copy(workingDays = next)
                                }
                            },
                            label = {
                                Text(
                                    day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = chipColors()
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = PureBlack,
                        border = BorderStroke(1.dp, OledBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        onUpdateSettings { it.copy(workStartHour = h, workStartMinute = m) }
                                    },
                                    settings.workStartHour,
                                    settings.workStartMinute,
                                    settings.use24HourFormat
                                ).show()
                            }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Start Time", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            Text(
                                String.format(Locale.US, "%02d:%02d", settings.workStartHour, settings.workStartMinute),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Surface(
                        color = PureBlack,
                        border = BorderStroke(1.dp, OledBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        onUpdateSettings { it.copy(workEndHour = h, workEndMinute = m) }
                                    },
                                    settings.workEndHour,
                                    settings.workEndMinute,
                                    settings.use24HourFormat
                                ).show()
                            }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("End Time", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            Text(
                                String.format(Locale.US, "%02d:%02d", settings.workEndHour, settings.workEndMinute),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                SettingSwitchRow(
                    title = "Only When Charging / Docked",
                    checked = settings.requireChargingForAutoLaunch,
                    onCheckedChange = { v ->
                        onUpdateSettings { it.copy(requireChargingForAutoLaunch = v) }
                    }
                )
            }

            HorizontalDivider(color = OledBorder)

            // ── OLED & Brightness Dimming ──────────────────────────────────
            SectionTitle("OLED & Brightness Levels")
            SettingSwitchRow(
                title = "OLED Pixel Shifting",
                subtitle = "Subtle 2-4dp shift every minute to prevent burn-in.",
                checked = settings.pixelShiftEnabled,
                onCheckedChange = { v -> onUpdateSettings { it.copy(pixelShiftEnabled = v) } }
            )
            SettingSwitchRow(
                title = "Sub-Pixel Anti-Burn-In Dimming",
                subtitle = "Dims peak white after 30 minutes of continuous idle display.",
                checked = settings.antiBurnInDimmingEnabled,
                onCheckedChange = { v -> onUpdateSettings { it.copy(antiBurnInDimmingEnabled = v) } }
            )
            SettingSwitchRow(
                title = "Proximity Face-Down Blanking",
                subtitle = "Turns pixels pure black when proximity sensor is covered.",
                checked = settings.proximityBlankingEnabled,
                onCheckedChange = { v -> onUpdateSettings { it.copy(proximityBlankingEnabled = v) } }
            )
            SettingSwitchRow(
                title = "Custom Brightness Dimming",
                subtitle = "Tap background anytime to temporarily brighten.",
                checked = settings.autoDimEnabled,
                onCheckedChange = { v -> onUpdateSettings { it.copy(autoDimEnabled = v) } }
            )
            if (settings.autoDimEnabled) {
                Text(
                    text = "Brightness Level: ${settings.dimBrightnessPercent}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(1, 5, 15, 30, 50, 75, 100).forEach { pct ->
                        FilterChip(
                            selected = settings.dimBrightnessPercent == pct,
                            onClick = { onUpdateSettings { it.copy(dimBrightnessPercent = pct) } },
                            label = { Text("$pct%", style = MaterialTheme.typography.labelMedium) },
                            colors = chipColors()
                        )
                    }
                }
                Slider(
                    value = settings.dimBrightnessPercent.toFloat(),
                    onValueChange = { value ->
                        onUpdateSettings { it.copy(dimBrightnessPercent = value.toInt()) }
                    },
                    valueRange = 1f..100f
                )
            }

            HorizontalDivider(color = OledBorder)

            // ── Orientation & Backup ───────────────────────────────────────
            SectionTitle("Orientation & Backup")
            OutlinedButton(
                onClick = onResetAutoRotate,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, OledBorder)
            ) {
                Text("Reset Orientation to Auto-Rotate", style = MaterialTheme.typography.labelMedium)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onExportBackup,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, OledBorder)
                ) {
                    Text("Export JSON Backup", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = onImportBackup,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, OledBorder)
                ) {
                    Text("Import JSON Backup", style = MaterialTheme.typography.labelMedium)
                }
            }

            HorizontalDivider(color = OledBorder)

            // ── About (expensy-style) ──────────────────────────────────────
            SectionTitle("About")
            Surface(
                color = PureBlack,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, OledBorder)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Version Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Version", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = OledBorder)

                    // Privacy Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Privacy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("100% On-Device Storage • Zero Telemetry", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            }
                        }
                        Icon(Icons.Rounded.CheckCircleOutline, contentDescription = "Verified", tint = CallAcceptGreen, modifier = Modifier.size(20.dp))
                    }

                    HorizontalDivider(color = OledBorder)

                    // Source Code Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenUrl("https://github.com/mina-android/DroidDeck") }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("GitHub", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Open Source (GPL-3.0)", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            }
                        }
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }

                    HorizontalDivider(color = OledBorder)

                    // Developer Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDeveloperDialog = true }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.PersonOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Developer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Mina Ashraf (mina-android)", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            }
                        }
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showDeveloperDialog) {
        AlertDialog(
            onDismissRequest = { showDeveloperDialog = false },
            containerColor = PureBlack,
            title = { Text("Developer", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showDeveloperDialog = false
                                onOpenUrl("https://github.com/mina-android")
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("GitHub Profile")
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showDeveloperDialog = false
                                onOpenUrl("https://portfolio.minaashraf285.workers.dev")
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Developer Website")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDeveloperDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showAppPickerDialog) {
        AppSelectionDialog(
            title = "Select Quick Apps (Max 4)",
            apps = installedApps,
            selectedPackages = settings.quickAppPackages.toSet(),
            onTogglePackage = onToggleQuickAppPackage,
            onDismiss = { showAppPickerDialog = false }
        )
    }

    if (showBlacklistDialog) {
        AppSelectionDialog(
            title = "Mute Notifications From Apps",
            apps = installedApps,
            selectedPackages = settings.blacklistedNotificationPackages,
            onTogglePackage = onToggleBlacklistPackage,
            onDismiss = { showBlacklistDialog = false }
        )
    }
}

@Composable
private fun AppSelectionDialog(
    title: String,
    apps: List<QuickAppEntry>,
    selectedPackages: Set<String>,
    onTogglePackage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PureBlack,
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                apps.forEach { app ->
                    val checked = selectedPackages.contains(app.packageName)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTogglePackage(app.packageName) }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(checked = checked, onCheckedChange = { onTogglePackage(app.packageName) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    containerColor = PureBlack,
    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
    selectedLabelColor = MaterialTheme.colorScheme.primary
)

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
