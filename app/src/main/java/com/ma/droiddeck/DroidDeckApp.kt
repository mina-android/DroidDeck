package com.ma.droiddeck

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.ma.droiddeck.data.preferences.UserPreferencesRepository
import com.ma.droiddeck.data.repository.DroidDeckDashboardRepository
import com.ma.droiddeck.service.WorkingHoursLockService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Application entry point for DroidDeck.
 * Initializes notification channels, repositories, and working-hours service state.
 */
class DroidDeckApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var preferencesRepository: UserPreferencesRepository
        private set

    lateinit var dashboardRepository: DroidDeckDashboardRepository
        private set

    override fun onCreate() {
        super.onCreate()
        preferencesRepository = UserPreferencesRepository(applicationContext)
        dashboardRepository = DroidDeckDashboardRepository.getInstance()

        createNotificationChannels()

        appScope.launch {
            val settings = preferencesRepository.settingsFlow.first()
            if (settings.workingHoursEnabled) {
                WorkingHoursLockService.start(applicationContext)
            }
        }
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val workingHoursChannel = NotificationChannel(
            CHANNEL_WORKING_HOURS,
            getString(R.string.channel_working_hours_name),
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = getString(R.string.channel_working_hours_desc)
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
        }
        manager.createNotificationChannel(workingHoursChannel)
    }

    companion object {
        const val CHANNEL_WORKING_HOURS = "droiddeck_working_hours"
    }
}
