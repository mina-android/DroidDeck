package com.ma.droiddeck.util

import android.Manifest
import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import com.ma.droiddeck.MainActivity
import com.ma.droiddeck.data.repository.DroidDeckDashboardRepository

/**
 * Executes incoming/ongoing call actions reliably across Android versions:
 * 1. Sends CallStyle/Notification PendingIntents with background-activity-start options on API 34+.
 * 2. Falls back to TelecomManager (when ANSWER_PHONE_CALLS is granted) for cellular calls.
 */
object CallActionHelper {

    fun sendPendingIntentAllowBg(pendingIntent: PendingIntent?): Boolean {
        if (pendingIntent == null) return false
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val opts = ActivityOptions.makeBasic()
                    .setPendingIntentBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                    )
                pendingIntent.send(opts.toBundle())
            } else {
                pendingIntent.send()
            }
            true
        }.getOrDefault(false)
    }

    @Suppress("DEPRECATION")
    fun answerCall(context: Context, acceptIntent: PendingIntent?, fallbackIntent: PendingIntent?) {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ANSWER_PHONE_CALLS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPerm) {
            val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            val answered = runCatching {
                telecom?.acceptRingingCall()
                true
            }.getOrDefault(false)
            if (answered) {
                bringAppToFrontSequence(context)
                return
            }
        }

        if (sendPendingIntentAllowBg(acceptIntent)) {
            bringAppToFrontSequence(context)
            return
        }

        if (sendPendingIntentAllowBg(fallbackIntent)) {
            bringAppToFrontSequence(context)
        }
    }

    @Suppress("DEPRECATION")
    fun declineOrEndCall(context: Context, declineOrHangUpIntent: PendingIntent?) {
        if (sendPendingIntentAllowBg(declineOrHangUpIntent)) return

        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ANSWER_PHONE_CALLS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            runCatching { telecom?.endCall() }
        }
    }

    fun silenceRinger(context: Context) {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ANSWER_PHONE_CALLS
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            val silenced = runCatching {
                telecom?.silenceRinger()
                true
            }.getOrDefault(false)
            if (silenced) return
        }

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        runCatching {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_RING,
                AudioManager.ADJUST_MUTE,
                0
            )
        }
    }

    fun placeCallOrDial(context: Context, phoneNumber: String) {
        val cleaned = phoneNumber.trim()
        if (cleaned.isEmpty()) return
        val hasCallPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val telUri = Uri.parse("tel:${Uri.encode(cleaned)}")
        var launched = false

        if (hasCallPerm) {
            val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            launched = runCatching {
                telecom?.placeCall(telUri, Bundle())
                true
            }.getOrDefault(false)
        }

        if (!launched) {
            val action = if (hasCallPerm) Intent.ACTION_CALL else Intent.ACTION_DIAL
            val intent = Intent(action, telUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            launched = runCatching {
                context.startActivity(intent)
                true
            }.getOrDefault(false)
        }

        if (launched && hasCallPerm) {
            bringAppToFrontSequence(context)
        }
    }

    fun bringAppToFrontDelayed(context: Context, delayMs: Long = 350L) {
        val appContext = context.applicationContext
        Handler(Looper.getMainLooper()).postDelayed({
            bringAppToFrontOnce(appContext)
        }, delayMs)
    }

    fun bringAppToFrontSequence(context: Context) {
        val appContext = context.applicationContext
        val handler = Handler(Looper.getMainLooper())
        val delays = longArrayOf(150L, 500L, 1000L, 1800L)
        delays.forEach { delayMs ->
            handler.postDelayed({
                val repo = DroidDeckDashboardRepository.getInstance()
                if (repo.isDashboardSessionActive && !repo.isDashboardForeground) {
                    bringAppToFrontOnce(appContext)
                }
            }, delayMs)
        }
    }

    private fun bringAppToFrontOnce(appContext: Context) {
        val repo = DroidDeckDashboardRepository.getInstance()
        if (repo.requestBringToFront()) return
        runCatching {
            val intent = Intent(appContext, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }
            appContext.startActivity(intent)
        }
    }
}
