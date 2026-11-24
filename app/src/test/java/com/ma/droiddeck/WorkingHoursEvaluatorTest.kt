package com.ma.droiddeck

import com.ma.droiddeck.data.model.DroidDeckSettings
import com.ma.droiddeck.util.WorkingHoursEvaluator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime

class WorkingHoursEvaluatorTest {

    private val zone = ZoneId.of("UTC")

    @Test
    fun `disabled working hours never auto-launches`() {
        val settings = DroidDeckSettings(workingHoursEnabled = false)
        val mondayNoon = ZonedDateTime.of(2026, 11, 23, 12, 0, 0, 0, zone)
        assertFalse(WorkingHoursEvaluator.shouldAutoLaunch(settings, mondayNoon, isCharging = true))
    }

    @Test
    fun `active weekday inside time window auto-launches`() {
        val settings = DroidDeckSettings(
            workingHoursEnabled = true,
            workingDays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY),
            workStartHour = 9,
            workStartMinute = 0,
            workEndHour = 17,
            workEndMinute = 0
        )
        val mondayTenAm = ZonedDateTime.of(2026, 11, 23, 10, 30, 0, 0, zone)
        assertTrue(WorkingHoursEvaluator.shouldAutoLaunch(settings, mondayTenAm, isCharging = false))
    }

    @Test
    fun `require charging blocks auto-launch when unplugged`() {
        val settings = DroidDeckSettings(
            workingHoursEnabled = true,
            workingDays = setOf(DayOfWeek.MONDAY),
            workStartHour = 9,
            workStartMinute = 0,
            workEndHour = 17,
            workEndMinute = 0,
            requireChargingForAutoLaunch = true
        )
        val mondayTenAm = ZonedDateTime.of(2026, 11, 23, 10, 30, 0, 0, zone)
        assertFalse(WorkingHoursEvaluator.shouldAutoLaunch(settings, mondayTenAm, isCharging = false))
        assertTrue(WorkingHoursEvaluator.shouldAutoLaunch(settings, mondayTenAm, isCharging = true))
    }

    @Test
    fun `overnight window evaluates across midnight correctly`() {
        val settings = DroidDeckSettings(
            workingHoursEnabled = true,
            workingDays = setOf(DayOfWeek.MONDAY),
            workStartHour = 22,
            workStartMinute = 0,
            workEndHour = 6,
            workEndMinute = 0
        )
        val mondayNight = ZonedDateTime.of(2026, 11, 23, 23, 15, 0, 0, zone)
        val tuesdayEarly = ZonedDateTime.of(2026, 11, 24, 3, 45, 0, 0, zone)
        val tuesdayMorning = ZonedDateTime.of(2026, 11, 24, 8, 0, 0, 0, zone)

        assertTrue(WorkingHoursEvaluator.shouldAutoLaunch(settings, mondayNight, isCharging = false))
        assertTrue(WorkingHoursEvaluator.shouldAutoLaunch(settings, tuesdayEarly, isCharging = false))
        assertFalse(WorkingHoursEvaluator.shouldAutoLaunch(settings, tuesdayMorning, isCharging = false))
    }
}
