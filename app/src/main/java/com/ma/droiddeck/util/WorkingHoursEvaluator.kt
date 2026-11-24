package com.ma.droiddeck.util

import com.ma.droiddeck.data.model.DroidDeckSettings
import java.time.ZonedDateTime

/**
 * Evaluates whether DroidDeck should automatically launch over the lockscreen
 * based on the user's configured working days, time window, and charging state.
 */
object WorkingHoursEvaluator {

    /**
     * Returns true if [now] falls within [settings]'s working hours schedule and
     * satisfies the optional charging requirement.
     */
    fun shouldAutoLaunch(
        settings: DroidDeckSettings,
        now: ZonedDateTime,
        isCharging: Boolean
    ): Boolean {
        if (!settings.workingHoursEnabled) return false
        if (settings.requireChargingForAutoLaunch && !isCharging) return false
        if (settings.workingDays.isEmpty()) return false

        val startMinutes = settings.workStartHour * 60 + settings.workStartMinute
        val endMinutes = settings.workEndHour * 60 + settings.workEndMinute
        val currentMinutes = now.hour * 60 + now.minute

        if (startMinutes == endMinutes) {
            return settings.workingDays.contains(now.dayOfWeek)
        }

        return if (startMinutes < endMinutes) {
            // Same-day window (e.g., 09:00 to 17:00)
            settings.workingDays.contains(now.dayOfWeek) &&
                currentMinutes in startMinutes until endMinutes
        } else {
            // Overnight window (e.g., 22:00 to 06:00)
            val todayActive = settings.workingDays.contains(now.dayOfWeek) &&
                currentMinutes >= startMinutes
            val yesterday = now.dayOfWeek.minus(1)
            val overnightFromYesterday = settings.workingDays.contains(yesterday) &&
                currentMinutes < endMinutes
            todayActive || overnightFromYesterday
        }
    }
}
