package com.ma.droiddeck.util

import android.app.NotificationManager
import android.os.Build

/**
 * Builds Android [NotificationManager.Policy] configurations for DroidDeck's
 * sound-preserving popup-hiding DND mode.
 */
object DndPolicyHelper {

    /**
     * Returns a [NotificationManager.Policy] that keeps sound enabled for all
     * supported priority categories (Calls, Messages, Conversations, Events,
     * Reminders, Alarms, Media, System) while suppressing heads-up peek banners,
     * full-screen intents, and ambient popups. Notifications remain visible in
     * the system shade and DroidDeck's in-app notification list.
     */
    fun buildPopupSuppressionPolicy(base: NotificationManager.Policy): NotificationManager.Policy {
        var categories = base.priorityCategories or
            NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS or
            NotificationManager.Policy.PRIORITY_CATEGORY_EVENTS or
            NotificationManager.Policy.PRIORITY_CATEGORY_REMINDERS or
            NotificationManager.Policy.PRIORITY_CATEGORY_CALLS or
            NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            categories = categories or
                NotificationManager.Policy.PRIORITY_CATEGORY_MEDIA or
                NotificationManager.Policy.PRIORITY_CATEGORY_SYSTEM
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            categories = categories or NotificationManager.Policy.PRIORITY_CATEGORY_CONVERSATIONS
        }

        val suppressedEffects = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            (base.suppressedVisualEffects or
                NotificationManager.Policy.SUPPRESSED_EFFECT_PEEK or
                NotificationManager.Policy.SUPPRESSED_EFFECT_FULL_SCREEN_INTENT or
                NotificationManager.Policy.SUPPRESSED_EFFECT_AMBIENT) and
                NotificationManager.Policy.SUPPRESSED_EFFECT_NOTIFICATION_LIST.inv()
        } else {
            base.suppressedVisualEffects
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            NotificationManager.Policy(
                categories,
                NotificationManager.Policy.PRIORITY_SENDERS_ANY,
                NotificationManager.Policy.PRIORITY_SENDERS_ANY,
                suppressedEffects,
                NotificationManager.Policy.CONVERSATION_SENDERS_ANYONE
            )
        } else {
            NotificationManager.Policy(
                categories,
                NotificationManager.Policy.PRIORITY_SENDERS_ANY,
                NotificationManager.Policy.PRIORITY_SENDERS_ANY,
                suppressedEffects
            )
        }
    }
}
