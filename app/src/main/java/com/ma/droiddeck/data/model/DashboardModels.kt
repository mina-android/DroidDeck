package com.ma.droiddeck.data.model

import android.app.PendingIntent
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import java.time.DayOfWeek

/**
 * Available OLED-safe accent color themes rendered on the pure #000000 background.
 */
enum class AccentTheme(val label: String) {
    PURE_WHITE("Pure White"),
    WARM_AMBER("Warm Amber"),
    NIGHT_RED("Night Red"),
    SOFT_CYAN("Soft Cyan"),
    DYNAMIC_M3("Material You")
}

/**
 * Selectable clock face styles.
 */
enum class ClockStyle(val label: String) {
    MODERN_DIGITAL("Modern Digital"),
    MINIMAL_ANALOG("Minimal Analog"),
    BOLD_CONDENSED("Bold Condensed"),
    DUAL_TIMEZONE("Dual Timezone")
}

/**
 * Selectable app-wide font families (matching expensy font list + Monospace).
 */
enum class AppFontFamily(val label: String, val googleFontName: String?) {
    DEFAULT("System Default", null),
    PLUS_JAKARTA_SANS("Plus Jakarta Sans", "Plus Jakarta Sans"),
    DM_SANS("DM Sans", "DM Sans"),
    INTER("Inter", "Inter"),
    SPACE_GROTESK("Space Grotesk", "Space Grotesk"),
    OUTFIT("Outfit", "Outfit"),
    SORA("Sora", "Sora"),
    POPPINS("Poppins", "Poppins"),
    NUNITO("Nunito", "Nunito"),
    MONOSPACE("Monospace", null)
}

/**
 * Persisted screen orientation preference.
 */
enum class OrientationMode(val label: String) {
    AUTO("Auto"),
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape")
}

/**
 * Reorderable dashboard widgets in the unified DroidDeck widget zone.
 * Incoming/ongoing call banners are pinned directly above the Clock widget,
 * and Weather is embedded inside the Clock widget.
 */
enum class DashboardWidgetType(val label: String) {
    CLOCK("Clock & Timer"),
    RECENT_CALLS("Recent Calls"),
    MUSIC_CONTROLS("Music & Volume"),
    NOTIFICATIONS("Notifications"),
    QUICK_APPS("Quick Apps");

    companion object {
        val DEFAULT_ORDER: List<DashboardWidgetType> = listOf(
            CLOCK,
            RECENT_CALLS,
            MUSIC_CONTROLS,
            NOTIFICATIONS,
            QUICK_APPS
        )

        val DEFAULT_LANDSCAPE_LEFT: Set<DashboardWidgetType> = setOf(
            CLOCK,
            RECENT_CALLS
        )
    }
}

/**
 * System ringer mode representation for Sound / Vibrate / Mute quick control.
 */
enum class RingerModeState(val label: String) {
    SOUND("Sound"),
    VIBRATE("Vibrate"),
    MUTE("Mute")
}

/**
 * Call log entry direction for the 2 Recent Calls widget.
 */
enum class CallDirection {
    INCOMING,
    OUTGOING,
    MISSED
}

/**
 * Compact recent call item queried from Android CallLog.
 */
data class RecentCallEntry(
    val id: Long,
    val displayName: String,
    val number: String,
    val direction: CallDirection,
    val timestampMs: Long
)

/**
 * Current playback state extracted from the active system MediaController.
 */
data class MediaPlaybackState(
    val isActive: Boolean = false,
    val isPlaying: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val packageName: String = "",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val lastPositionUpdateElapsedMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val artwork: Bitmap? = null
)

/**
 * Compact representation of a non-ongoing user notification for the AOD pill list.
 */
data class NotificationItem(
    val key: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestampMs: Long,
    val smallIcon: Icon? = null,
    val contentIntent: PendingIntent? = null,
    val isClearable: Boolean = true
)

/**
 * Active incoming cellular or VoIP call alert extracted from high-priority call notifications.
 */
data class IncomingCallAlert(
    val key: String,
    val callerName: String,
    val callerSubtitle: String,
    val appName: String,
    val acceptIntent: PendingIntent? = null,
    val declineIntent: PendingIntent? = null,
    val contentIntent: PendingIntent? = null
)

/**
 * Active ongoing cellular or VoIP call rendered inside the dashboard.
 */
data class OngoingCallState(
    val key: String,
    val callerName: String,
    val appName: String,
    val startedAtMs: Long,
    val hangUpIntent: PendingIntent? = null,
    val speakerIntent: PendingIntent? = null,
    val muteIntent: PendingIntent? = null,
    val contentIntent: PendingIntent? = null
)

/**
 * Next upcoming calendar event summary.
 */
data class CalendarEventSummary(
    val eventId: Long,
    val title: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val location: String = ""
)

/**
 * Weather & ambient temperature card state.
 */
data class WeatherState(
    val city: String = "",
    val temperatureCelsius: Float? = null,
    val conditionLabel: String = "No data",
    val isAmbientSensorFallback: Boolean = false,
    val updatedAtMs: Long = 0L
)

/**
 * Installed app shortcut entry for the Quick App Dock.
 */
data class QuickAppEntry(
    val packageName: String,
    val label: String
)

/**
 * Real-time device power, connectivity, audio, and interruption status.
 */
data class DeviceStatusState(
    val batteryLevel: Int = 100,
    val isCharging: Boolean = false,
    val batteryTempCelsius: Float = 25.0f,
    val isWifiConnected: Boolean = false,
    val isCellularConnected: Boolean = false,
    val isBluetoothConnected: Boolean = false,
    val isDndActive: Boolean = false,
    val mediaVolume: Int = 0,
    val mediaMaxVolume: Int = 15,
    val ringerMode: RingerModeState = RingerModeState.SOUND,
    val isSpeakerphoneOn: Boolean = false,
    val isMicMuted: Boolean = false
)

/**
 * Persisted user preferences for clock appearance, font, OLED protection,
 * lockscreen visibility, widget ordering/visibility, and working-hours automation.
 */
data class DroidDeckSettings(
    val use24HourFormat: Boolean = false,
    val showSeconds: Boolean = false,
    val showDate: Boolean = true,
    val accentTheme: AccentTheme = AccentTheme.PURE_WHITE,
    val appFont: AppFontFamily = AppFontFamily.DEFAULT,
    val clockStyle: ClockStyle = ClockStyle.MODERN_DIGITAL,
    val clockScale: Float = 1.0f,
    val secondaryTimeZoneId: String = "UTC",
    val showOnLockScreen: Boolean = true,
    val suppressHeadsUpWhileActive: Boolean = true,
    val widgetOrder: List<DashboardWidgetType> = DashboardWidgetType.DEFAULT_ORDER,
    val landscapeLeftWidgets: Set<DashboardWidgetType> = DashboardWidgetType.DEFAULT_LANDSCAPE_LEFT,
    val hiddenWidgets: Set<DashboardWidgetType> = setOf(
        DashboardWidgetType.QUICK_APPS
    ),
    val blacklistedNotificationPackages: Set<String> = emptySet(),
    val quickAppPackages: List<String> = emptyList(),
    val weatherCity: String = "",
    val showWeatherInClock: Boolean = true,
    val showCalendarInClock: Boolean = true,
    val orientationMode: OrientationMode = OrientationMode.AUTO,
    val pixelShiftEnabled: Boolean = true,
    val antiBurnInDimmingEnabled: Boolean = true,
    val autoDimEnabled: Boolean = true,
    val dimBrightnessPercent: Int = 5,
    val proximityBlankingEnabled: Boolean = true,
    val workingHoursEnabled: Boolean = false,
    val workingDays: Set<DayOfWeek> = setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY
    ),
    val workStartHour: Int = 9,
    val workStartMinute: Int = 0,
    val workEndHour: Int = 17,
    val workEndMinute: Int = 0,
    val requireChargingForAutoLaunch: Boolean = false
)

/**
 * Built-in desk Pomodoro / countdown timer state.
 */
data class DroidDeckTimerState(
    val isRunning: Boolean = false,
    val totalSeconds: Int = 25 * 60,
    val remainingSeconds: Int = 25 * 60,
    val label: String = "Focus"
)
