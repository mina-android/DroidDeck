package com.ma.droiddeck

import com.ma.droiddeck.data.model.AccentTheme
import com.ma.droiddeck.data.model.AppFontFamily
import com.ma.droiddeck.data.model.ClockStyle
import com.ma.droiddeck.data.model.DashboardWidgetType
import com.ma.droiddeck.data.model.DroidDeckSettings
import com.ma.droiddeck.data.model.OrientationMode
import com.ma.droiddeck.data.preferences.UserPreferencesRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsBackupTest {

    @Test
    fun `json export and import round-trips all key settings`() {
        val original = DroidDeckSettings(
            use24HourFormat = true,
            showSeconds = true,
            accentTheme = AccentTheme.SOFT_CYAN,
            appFont = AppFontFamily.SPACE_GROTESK,
            clockStyle = ClockStyle.MINIMAL_ANALOG,
            clockScale = 1.25f,
            showOnLockScreen = false,
            suppressHeadsUpWhileActive = true,
            dimBrightnessPercent = 45,
            weatherCity = "Cairo",
            showWeatherInClock = false,
            showCalendarInClock = false,
            orientationMode = OrientationMode.LANDSCAPE,
            quickAppPackages = listOf("com.android.calculator2"),
            blacklistedNotificationPackages = setOf("com.example.noisy"),
            hiddenWidgets = setOf(DashboardWidgetType.QUICK_APPS)
        )

        val json = UserPreferencesRepository.exportSettingsJson(original)
        val restored = UserPreferencesRepository.importSettingsJson(json)

        assertEquals(original.use24HourFormat, restored.use24HourFormat)
        assertEquals(original.showSeconds, restored.showSeconds)
        assertEquals(original.accentTheme, restored.accentTheme)
        assertEquals(original.appFont, restored.appFont)
        assertEquals(original.clockStyle, restored.clockStyle)
        assertEquals(original.clockScale, restored.clockScale, 0.01f)
        assertEquals(original.showOnLockScreen, restored.showOnLockScreen)
        assertEquals(original.dimBrightnessPercent, restored.dimBrightnessPercent)
        assertEquals(original.weatherCity, restored.weatherCity)
        assertEquals(original.showWeatherInClock, restored.showWeatherInClock)
        assertEquals(original.showCalendarInClock, restored.showCalendarInClock)
        assertEquals(original.orientationMode, restored.orientationMode)
        assertEquals(original.quickAppPackages, restored.quickAppPackages)
        assertEquals(original.blacklistedNotificationPackages, restored.blacklistedNotificationPackages)
        assertEquals(original.hiddenWidgets, restored.hiddenWidgets)
    }
}
