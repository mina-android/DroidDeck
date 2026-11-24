package com.ma.droiddeck.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ma.droiddeck.DroidDeckApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Restores [WorkingHoursLockService] after device reboot if Working Hours auto-launch is enabled.
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val app = context.applicationContext as DroidDeckApp
                val settings = app.preferencesRepository.settingsFlow.first()
                if (settings.workingHoursEnabled) {
                    WorkingHoursLockService.start(context.applicationContext)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
