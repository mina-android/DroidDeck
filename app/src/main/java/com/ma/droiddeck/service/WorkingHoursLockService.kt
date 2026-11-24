package com.ma.droiddeck.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.ma.droiddeck.DroidDeckApp
import com.ma.droiddeck.MainActivity
import com.ma.droiddeck.R
import com.ma.droiddeck.util.WorkingHoursEvaluator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/**
 * Lightweight foreground service that listens for screen-off/lock events and
 * automatically launches DroidDeck over the lockscreen when the current day and time
 * fall inside the user's configured Working Hours schedule.
 */
class WorkingHoursLockService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action != Intent.ACTION_SCREEN_OFF) return
            serviceScope.launch {
                val app = application as DroidDeckApp
                val settings = app.preferencesRepository.settingsFlow.first()
                if (!settings.showOnLockScreen) return@launch
                val isCharging = queryIsCharging()
                if (WorkingHoursEvaluator.shouldAutoLaunch(settings, ZonedDateTime.now(), isCharging)) {
                    val launchIntent = Intent(this@WorkingHoursLockService, MainActivity::class.java).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        )
                    }
                    startActivity(launchIntent)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildSilentNotification())
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        ContextCompat.registerReceiver(
            this,
            screenOffReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenOffReceiver) }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun queryIsCharging(): Boolean {
        val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun buildSilentNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, DroidDeckApp.CHANNEL_WORKING_HOURS)
            .setSmallIcon(R.drawable.ic_qs_droiddeck)
            .setContentTitle(getString(R.string.working_hours_notification_title))
            .setContentText(getString(R.string.working_hours_notification_text))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 4102

        fun start(context: Context) {
            val intent = Intent(context, WorkingHoursLockService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, WorkingHoursLockService::class.java))
        }
    }
}
