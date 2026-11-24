package com.ma.droiddeck.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ma.droiddeck.data.model.AccentTheme
import com.ma.droiddeck.data.model.AppFontFamily
import com.ma.droiddeck.data.model.ClockStyle
import com.ma.droiddeck.data.model.DashboardWidgetType
import com.ma.droiddeck.data.model.DroidDeckSettings
import com.ma.droiddeck.data.model.OrientationMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "droiddeck_settings")

/**
 * Manages persisted user settings using Jetpack DataStore Preferences
 * and supports JSON export/import backup.
 */
class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val USE_24_HOUR = booleanPreferencesKey("use_24_hour")
        val SHOW_SECONDS = booleanPreferencesKey("show_seconds")
        val SHOW_DATE = booleanPreferencesKey("show_date")
        val ACCENT_THEME = stringPreferencesKey("accent_theme")
        val APP_FONT = stringPreferencesKey("app_font")
        val CLOCK_STYLE = stringPreferencesKey("clock_style")
        val CLOCK_SCALE = floatPreferencesKey("clock_scale")
        val SECONDARY_TZ = stringPreferencesKey("secondary_tz")
        val SHOW_ON_LOCK_SCREEN = booleanPreferencesKey("show_on_lock_screen")
        val SUPPRESS_HEADS_UP = booleanPreferencesKey("suppress_heads_up")
        val WIDGET_ORDER = stringPreferencesKey("widget_order")
        val LANDSCAPE_LEFT_WIDGETS = stringSetPreferencesKey("landscape_left_widgets")
        val HIDDEN_WIDGETS = stringSetPreferencesKey("hidden_widgets")
        val BLACKLISTED_PACKAGES = stringSetPreferencesKey("blacklisted_packages")
        val QUICK_APP_PACKAGES = stringPreferencesKey("quick_app_packages")
        val WEATHER_CITY = stringPreferencesKey("weather_city")
        val SHOW_WEATHER_IN_CLOCK = booleanPreferencesKey("show_weather_in_clock")
        val SHOW_CALENDAR_IN_CLOCK = booleanPreferencesKey("show_calendar_in_clock")
        val ORIENTATION_MODE = stringPreferencesKey("orientation_mode")
        val PIXEL_SHIFT = booleanPreferencesKey("pixel_shift")
        val ANTI_BURN_IN = booleanPreferencesKey("anti_burn_in")
        val AUTO_DIM = booleanPreferencesKey("auto_dim")
        val DIM_BRIGHTNESS = intPreferencesKey("dim_brightness")
        val PROXIMITY_BLANKING = booleanPreferencesKey("proximity_blanking")
        val WORKING_HOURS_ENABLED = booleanPreferencesKey("working_hours_enabled")
        val WORKING_DAYS = stringSetPreferencesKey("working_days")
        val WORK_START_HOUR = intPreferencesKey("work_start_hour")
        val WORK_START_MINUTE = intPreferencesKey("work_start_minute")
        val WORK_END_HOUR = intPreferencesKey("work_end_hour")
        val WORK_END_MINUTE = intPreferencesKey("work_end_minute")
        val REQUIRE_CHARGING = booleanPreferencesKey("require_charging")
    }

    val settingsFlow: Flow<DroidDeckSettings> = context.dataStore.data.map(::mapPreferences)

    suspend fun updateSettings(transform: (DroidDeckSettings) -> DroidDeckSettings) {
        context.dataStore.edit { prefs ->
            val current = mapPreferences(prefs)
            val updated = transform(current)
            writeToPreferences(prefs, updated)
        }
    }

    suspend fun replaceSettings(settings: DroidDeckSettings) {
        context.dataStore.edit { prefs ->
            writeToPreferences(prefs, settings)
        }
    }

    private fun writeToPreferences(
        prefs: androidx.datastore.preferences.core.MutablePreferences,
        updated: DroidDeckSettings
    ) {
        prefs[Keys.USE_24_HOUR] = updated.use24HourFormat
        prefs[Keys.SHOW_SECONDS] = updated.showSeconds
        prefs[Keys.SHOW_DATE] = updated.showDate
        prefs[Keys.ACCENT_THEME] = updated.accentTheme.name
        prefs[Keys.APP_FONT] = updated.appFont.name
        prefs[Keys.CLOCK_STYLE] = updated.clockStyle.name
        prefs[Keys.CLOCK_SCALE] = updated.clockScale.coerceIn(0.65f, 1.40f)
        prefs[Keys.SECONDARY_TZ] = updated.secondaryTimeZoneId
        prefs[Keys.SHOW_ON_LOCK_SCREEN] = updated.showOnLockScreen
        prefs[Keys.SUPPRESS_HEADS_UP] = updated.suppressHeadsUpWhileActive
        prefs[Keys.WIDGET_ORDER] = serializeWidgetOrder(updated.widgetOrder)
        prefs[Keys.LANDSCAPE_LEFT_WIDGETS] = updated.landscapeLeftWidgets.map { it.name }.toSet()
        prefs[Keys.HIDDEN_WIDGETS] = updated.hiddenWidgets.map { it.name }.toSet()
        prefs[Keys.BLACKLISTED_PACKAGES] = updated.blacklistedNotificationPackages
        prefs[Keys.QUICK_APP_PACKAGES] = updated.quickAppPackages.take(4).joinToString(",")
        prefs[Keys.WEATHER_CITY] = updated.weatherCity
        prefs[Keys.SHOW_WEATHER_IN_CLOCK] = updated.showWeatherInClock
        prefs[Keys.SHOW_CALENDAR_IN_CLOCK] = updated.showCalendarInClock
        prefs[Keys.ORIENTATION_MODE] = updated.orientationMode.name
        prefs[Keys.PIXEL_SHIFT] = updated.pixelShiftEnabled
        prefs[Keys.ANTI_BURN_IN] = updated.antiBurnInDimmingEnabled
        prefs[Keys.AUTO_DIM] = updated.autoDimEnabled
        prefs[Keys.DIM_BRIGHTNESS] = updated.dimBrightnessPercent.coerceIn(1, 100)
        prefs[Keys.PROXIMITY_BLANKING] = updated.proximityBlankingEnabled
        prefs[Keys.WORKING_HOURS_ENABLED] = updated.workingHoursEnabled
        prefs[Keys.WORKING_DAYS] = updated.workingDays.map { it.name }.toSet()
        prefs[Keys.WORK_START_HOUR] = updated.workStartHour.coerceIn(0, 23)
        prefs[Keys.WORK_START_MINUTE] = updated.workStartMinute.coerceIn(0, 59)
        prefs[Keys.WORK_END_HOUR] = updated.workEndHour.coerceIn(0, 23)
        prefs[Keys.WORK_END_MINUTE] = updated.workEndMinute.coerceIn(0, 59)
        prefs[Keys.REQUIRE_CHARGING] = updated.requireChargingForAutoLaunch
    }

    private fun mapPreferences(prefs: Preferences): DroidDeckSettings {
        val defaultDays = setOf(
            DayOfWeek.MONDAY.name,
            DayOfWeek.TUESDAY.name,
            DayOfWeek.WEDNESDAY.name,
            DayOfWeek.THURSDAY.name,
            DayOfWeek.FRIDAY.name
        )
        val parsedDays = (prefs[Keys.WORKING_DAYS] ?: defaultDays)
            .mapNotNull { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }
            .toSet()

        val parsedAccent = prefs[Keys.ACCENT_THEME]
            ?.let { runCatching { AccentTheme.valueOf(it) }.getOrNull() }
            ?: AccentTheme.PURE_WHITE

        val parsedFont = prefs[Keys.APP_FONT]
            ?.let { runCatching { AppFontFamily.valueOf(it) }.getOrNull() }
            ?: AppFontFamily.DEFAULT

        val parsedClockStyle = prefs[Keys.CLOCK_STYLE]
            ?.let { runCatching { ClockStyle.valueOf(it) }.getOrNull() }
            ?: ClockStyle.MODERN_DIGITAL

        val parsedLeft = prefs[Keys.LANDSCAPE_LEFT_WIDGETS]
            ?.mapNotNull { runCatching { DashboardWidgetType.valueOf(it) }.getOrNull() }
            ?.toSet()
            ?: DashboardWidgetType.DEFAULT_LANDSCAPE_LEFT

        val parsedHidden = prefs[Keys.HIDDEN_WIDGETS]
            ?.mapNotNull { runCatching { DashboardWidgetType.valueOf(it) }.getOrNull() }
            ?.toSet()
            ?: setOf(
                DashboardWidgetType.QUICK_APPS
            )

        val parsedOrientation = prefs[Keys.ORIENTATION_MODE]
            ?.let { runCatching { OrientationMode.valueOf(it) }.getOrNull() }
            ?: OrientationMode.AUTO

        val quickApps = prefs[Keys.QUICK_APP_PACKAGES]
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.take(4)
            ?: emptyList()

        return DroidDeckSettings(
            use24HourFormat = prefs[Keys.USE_24_HOUR] ?: false,
            showSeconds = prefs[Keys.SHOW_SECONDS] ?: false,
            showDate = prefs[Keys.SHOW_DATE] ?: true,
            accentTheme = parsedAccent,
            appFont = parsedFont,
            clockStyle = parsedClockStyle,
            clockScale = (prefs[Keys.CLOCK_SCALE] ?: 1.0f).coerceIn(0.65f, 1.40f),
            secondaryTimeZoneId = prefs[Keys.SECONDARY_TZ] ?: "UTC",
            showOnLockScreen = prefs[Keys.SHOW_ON_LOCK_SCREEN] ?: true,
            suppressHeadsUpWhileActive = prefs[Keys.SUPPRESS_HEADS_UP] ?: true,
            widgetOrder = parseWidgetOrder(prefs[Keys.WIDGET_ORDER]),
            landscapeLeftWidgets = parsedLeft,
            hiddenWidgets = parsedHidden,
            blacklistedNotificationPackages = prefs[Keys.BLACKLISTED_PACKAGES] ?: emptySet(),
            quickAppPackages = quickApps,
            weatherCity = prefs[Keys.WEATHER_CITY] ?: "",
            showWeatherInClock = prefs[Keys.SHOW_WEATHER_IN_CLOCK] ?: true,
            showCalendarInClock = prefs[Keys.SHOW_CALENDAR_IN_CLOCK] ?: true,
            orientationMode = parsedOrientation,
            pixelShiftEnabled = prefs[Keys.PIXEL_SHIFT] ?: true,
            antiBurnInDimmingEnabled = prefs[Keys.ANTI_BURN_IN] ?: true,
            autoDimEnabled = prefs[Keys.AUTO_DIM] ?: true,
            dimBrightnessPercent = (prefs[Keys.DIM_BRIGHTNESS] ?: 5).coerceIn(1, 100),
            proximityBlankingEnabled = prefs[Keys.PROXIMITY_BLANKING] ?: true,
            workingHoursEnabled = prefs[Keys.WORKING_HOURS_ENABLED] ?: false,
            workingDays = parsedDays,
            workStartHour = prefs[Keys.WORK_START_HOUR] ?: 9,
            workStartMinute = prefs[Keys.WORK_START_MINUTE] ?: 0,
            workEndHour = prefs[Keys.WORK_END_HOUR] ?: 17,
            workEndMinute = prefs[Keys.WORK_END_MINUTE] ?: 0,
            requireChargingForAutoLaunch = prefs[Keys.REQUIRE_CHARGING] ?: false
        )
    }

    companion object {
        fun serializeWidgetOrder(order: List<DashboardWidgetType>): String =
            order.distinct().joinToString(",") { it.name }

        fun parseWidgetOrder(raw: String?): List<DashboardWidgetType> {
            if (raw.isNullOrBlank()) return DashboardWidgetType.DEFAULT_ORDER
            val parsed = raw.split(",")
                .mapNotNull { token -> runCatching { DashboardWidgetType.valueOf(token.trim()) }.getOrNull() }
                .distinct()
                .toMutableList()
            DashboardWidgetType.DEFAULT_ORDER.forEach { required ->
                if (!parsed.contains(required)) parsed.add(required)
            }
            return parsed
        }

        fun exportSettingsJson(settings: DroidDeckSettings): String {
            return JSONObject().apply {
                put("version", 2)
                put("use24HourFormat", settings.use24HourFormat)
                put("showSeconds", settings.showSeconds)
                put("showDate", settings.showDate)
                put("accentTheme", settings.accentTheme.name)
                put("appFont", settings.appFont.name)
                put("clockStyle", settings.clockStyle.name)
                put("clockScale", settings.clockScale.toDouble())
                put("secondaryTimeZoneId", settings.secondaryTimeZoneId)
                put("showOnLockScreen", settings.showOnLockScreen)
                put("suppressHeadsUpWhileActive", settings.suppressHeadsUpWhileActive)
                put("widgetOrder", JSONArray(settings.widgetOrder.map { it.name }))
                put("landscapeLeftWidgets", JSONArray(settings.landscapeLeftWidgets.map { it.name }))
                put("hiddenWidgets", JSONArray(settings.hiddenWidgets.map { it.name }))
                put("blacklistedNotificationPackages", JSONArray(settings.blacklistedNotificationPackages.toList()))
                put("quickAppPackages", JSONArray(settings.quickAppPackages))
                put("weatherCity", settings.weatherCity)
                put("showWeatherInClock", settings.showWeatherInClock)
                put("showCalendarInClock", settings.showCalendarInClock)
                put("orientationMode", settings.orientationMode.name)
                put("pixelShiftEnabled", settings.pixelShiftEnabled)
                put("antiBurnInDimmingEnabled", settings.antiBurnInDimmingEnabled)
                put("autoDimEnabled", settings.autoDimEnabled)
                put("dimBrightnessPercent", settings.dimBrightnessPercent)
                put("proximityBlankingEnabled", settings.proximityBlankingEnabled)
                put("workingHoursEnabled", settings.workingHoursEnabled)
                put("workingDays", JSONArray(settings.workingDays.map { it.name }))
                put("workStartHour", settings.workStartHour)
                put("workStartMinute", settings.workStartMinute)
                put("workEndHour", settings.workEndHour)
                put("workEndMinute", settings.workEndMinute)
                put("requireChargingForAutoLaunch", settings.requireChargingForAutoLaunch)
            }.toString(2)
        }

        fun importSettingsJson(json: String): DroidDeckSettings {
            val obj = JSONObject(json)
            val defaults = DroidDeckSettings()

            fun jsonArrayToStrings(key: String): List<String>? {
                if (!obj.has(key)) return null
                val arr = obj.getJSONArray(key)
                return List(arr.length()) { idx -> arr.getString(idx) }
            }

            val order = jsonArrayToStrings("widgetOrder")
                ?.mapNotNull { runCatching { DashboardWidgetType.valueOf(it) }.getOrNull() }
                ?.let { list ->
                    val mutable = list.distinct().toMutableList()
                    DashboardWidgetType.DEFAULT_ORDER.forEach { if (!mutable.contains(it)) mutable.add(it) }
                    mutable
                } ?: defaults.widgetOrder

            val leftWidgets = jsonArrayToStrings("landscapeLeftWidgets")
                ?.mapNotNull { runCatching { DashboardWidgetType.valueOf(it) }.getOrNull() }
                ?.toSet() ?: defaults.landscapeLeftWidgets

            val hidden = jsonArrayToStrings("hiddenWidgets")
                ?.mapNotNull { runCatching { DashboardWidgetType.valueOf(it) }.getOrNull() }
                ?.toSet() ?: defaults.hiddenWidgets

            val days = jsonArrayToStrings("workingDays")
                ?.mapNotNull { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }
                ?.toSet() ?: defaults.workingDays

            return DroidDeckSettings(
                use24HourFormat = obj.optBoolean("use24HourFormat", defaults.use24HourFormat),
                showSeconds = obj.optBoolean("showSeconds", defaults.showSeconds),
                showDate = obj.optBoolean("showDate", defaults.showDate),
                accentTheme = runCatching { AccentTheme.valueOf(obj.optString("accentTheme")) }
                    .getOrDefault(defaults.accentTheme),
                appFont = runCatching { AppFontFamily.valueOf(obj.optString("appFont")) }
                    .getOrDefault(defaults.appFont),
                clockStyle = runCatching { ClockStyle.valueOf(obj.optString("clockStyle")) }
                    .getOrDefault(defaults.clockStyle),
                clockScale = obj.optDouble("clockScale", defaults.clockScale.toDouble()).toFloat().coerceIn(0.65f, 1.40f),
                secondaryTimeZoneId = obj.optString("secondaryTimeZoneId", defaults.secondaryTimeZoneId),
                showOnLockScreen = obj.optBoolean("showOnLockScreen", defaults.showOnLockScreen),
                suppressHeadsUpWhileActive = obj.optBoolean("suppressHeadsUpWhileActive", defaults.suppressHeadsUpWhileActive),
                widgetOrder = order,
                landscapeLeftWidgets = leftWidgets,
                hiddenWidgets = hidden,
                blacklistedNotificationPackages = jsonArrayToStrings("blacklistedNotificationPackages")?.toSet()
                    ?: defaults.blacklistedNotificationPackages,
                quickAppPackages = jsonArrayToStrings("quickAppPackages")?.take(4) ?: defaults.quickAppPackages,
                weatherCity = obj.optString("weatherCity", defaults.weatherCity),
                showWeatherInClock = obj.optBoolean("showWeatherInClock", defaults.showWeatherInClock),
                showCalendarInClock = obj.optBoolean("showCalendarInClock", defaults.showCalendarInClock),
                orientationMode = runCatching { OrientationMode.valueOf(obj.optString("orientationMode")) }
                    .getOrDefault(defaults.orientationMode),
                pixelShiftEnabled = obj.optBoolean("pixelShiftEnabled", defaults.pixelShiftEnabled),
                antiBurnInDimmingEnabled = obj.optBoolean("antiBurnInDimmingEnabled", defaults.antiBurnInDimmingEnabled),
                autoDimEnabled = obj.optBoolean("autoDimEnabled", defaults.autoDimEnabled),
                dimBrightnessPercent = obj.optInt("dimBrightnessPercent", defaults.dimBrightnessPercent).coerceIn(1, 100),
                proximityBlankingEnabled = obj.optBoolean("proximityBlankingEnabled", defaults.proximityBlankingEnabled),
                workingHoursEnabled = obj.optBoolean("workingHoursEnabled", defaults.workingHoursEnabled),
                workingDays = days,
                workStartHour = obj.optInt("workStartHour", defaults.workStartHour).coerceIn(0, 23),
                workStartMinute = obj.optInt("workStartMinute", defaults.workStartMinute).coerceIn(0, 59),
                workEndHour = obj.optInt("workEndHour", defaults.workEndHour).coerceIn(0, 23),
                workEndMinute = obj.optInt("workEndMinute", defaults.workEndMinute).coerceIn(0, 59),
                requireChargingForAutoLaunch = obj.optBoolean("requireChargingForAutoLaunch", defaults.requireChargingForAutoLaunch)
            )
        }
    }
}
