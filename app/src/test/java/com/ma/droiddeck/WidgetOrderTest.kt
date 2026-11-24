package com.ma.droiddeck

import com.ma.droiddeck.data.model.DashboardWidgetType
import com.ma.droiddeck.data.preferences.UserPreferencesRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetOrderTest {

    @Test
    fun `widget order serialization and parsing preserves custom order`() {
        val custom = listOf(
            DashboardWidgetType.NOTIFICATIONS,
            DashboardWidgetType.CLOCK,
            DashboardWidgetType.RECENT_CALLS,
            DashboardWidgetType.MUSIC_CONTROLS,
            DashboardWidgetType.QUICK_APPS
        )
        val serialized = UserPreferencesRepository.serializeWidgetOrder(custom)
        val parsed = UserPreferencesRepository.parseWidgetOrder(serialized)
        assertEquals(custom, parsed)
    }

    @Test
    fun `parsing partial or invalid widget order restores missing defaults`() {
        val parsed = UserPreferencesRepository.parseWidgetOrder("NOTIFICATIONS,UNKNOWN_WIDGET")
        assertEquals(DashboardWidgetType.NOTIFICATIONS, parsed.first())
        assertTrue(parsed.containsAll(DashboardWidgetType.DEFAULT_ORDER))
        assertEquals(DashboardWidgetType.DEFAULT_ORDER.size, parsed.size)
    }
}
