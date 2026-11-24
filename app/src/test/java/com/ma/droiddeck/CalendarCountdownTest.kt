package com.ma.droiddeck

import com.ma.droiddeck.ui.components.formatCountdown
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarCountdownTest {

    @Test
    fun `formatCountdown handles zero, minutes, exact hours, and hours with minutes`() {
        assertEquals("now", formatCountdown(0L))
        assertEquals("now", formatCountdown(-5L))
        assertEquals("in 25m", formatCountdown(25L))
        assertEquals("in 1h", formatCountdown(60L))
        assertEquals("in 2h", formatCountdown(120L))
        assertEquals("in 1h 25m", formatCountdown(85L))
    }
}
