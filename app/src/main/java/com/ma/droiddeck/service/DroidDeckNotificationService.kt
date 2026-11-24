package com.ma.droiddeck.service

import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.ma.droiddeck.MainActivity
import com.ma.droiddeck.data.model.IncomingCallAlert
import com.ma.droiddeck.data.model.MediaPlaybackState
import com.ma.droiddeck.data.model.NotificationItem
import com.ma.droiddeck.data.model.OngoingCallState
import com.ma.droiddeck.data.repository.DroidDeckDashboardRepository
import com.ma.droiddeck.util.CallActionHelper

/**
 * System NotificationListenerService that bridges active Android notifications,
 * incoming & ongoing call alerts (including CallStyle extras), and MediaSessionManager
 * transport/seek controls into [DroidDeckDashboardRepository].
 */
class DroidDeckNotificationService : NotificationListenerService(),
    DroidDeckDashboardRepository.MediaActionHandler {

    private val repository = DroidDeckDashboardRepository.getInstance()
    private var mediaSessionManager: MediaSessionManager? = null
    private var activeController: MediaController? = null

    private val sessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            bindPrimaryMediaController(controllers)
        }

    private val mediaCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            publishMediaState()
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            publishMediaState()
        }

        override fun onSessionDestroyed() {
            activeController?.unregisterCallback(this)
            activeController = null
            repository.updateMediaState(MediaPlaybackState())
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        repository.registerMediaActionHandler(this)
        repository.registerNotificationDismissHandler { key ->
            runCatching { cancelNotification(key) }
        }
        repository.registerClearAllNotificationsHandler {
            runCatching {
                val active = activeNotifications ?: return@runCatching
                active.forEach { sbn ->
                    if (sbn.packageName != packageName && sbn.isClearable) {
                        cancelNotification(sbn.key)
                    }
                }
            }
        }
        repository.registerBringToFrontHandler {
            runCatching {
                val intent = Intent(this, MainActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                }
                startActivity(intent)
            }
        }

        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
        val component = ComponentName(this, DroidDeckNotificationService::class.java)
        runCatching {
            mediaSessionManager?.addOnActiveSessionsChangedListener(sessionsChangedListener, component)
            val controllers = mediaSessionManager?.getActiveSessions(component)
            bindPrimaryMediaController(controllers)
        }

        refreshNotifications()
    }

    override fun onListenerDisconnected() {
        activeController?.unregisterCallback(mediaCallback)
        activeController = null
        runCatching {
            mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionsChangedListener)
        }
        repository.registerMediaActionHandler(null)
        repository.registerNotificationDismissHandler(null)
        repository.registerClearAllNotificationsHandler(null)
        repository.registerBringToFrontHandler(null)
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refreshNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refreshNotifications()
    }

    override fun onPlayPause() {
        val controller = activeController ?: return
        val state = controller.playbackState?.state
        if (state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING) {
            controller.transportControls.pause()
        } else {
            controller.transportControls.play()
        }
    }

    override fun onNext() {
        activeController?.transportControls?.skipToNext()
    }

    override fun onPrevious() {
        activeController?.transportControls?.skipToPrevious()
    }

    override fun onSeekTo(positionMs: Long) {
        activeController?.transportControls?.seekTo(positionMs.coerceAtLeast(0L))
    }

    private fun bindPrimaryMediaController(controllers: List<MediaController>?) {
        val preferred = controllers
            ?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers?.firstOrNull()

        if (preferred?.sessionToken == activeController?.sessionToken) {
            publishMediaState()
            return
        }

        activeController?.unregisterCallback(mediaCallback)
        activeController = preferred
        activeController?.registerCallback(mediaCallback)
        publishMediaState()
    }

    private fun publishMediaState() {
        val controller = activeController
        if (controller == null) {
            repository.updateMediaState(MediaPlaybackState())
            return
        }

        val metadata = controller.metadata
        val playback = controller.playbackState
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: ""
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: ""
        val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
        val artwork = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val rawPosition = playback?.position ?: 0L
        val speed = playback?.playbackSpeed?.takeIf { it > 0f } ?: 1.0f
        val updateElapsed = playback?.lastPositionUpdateTime?.takeIf { it > 0L }
            ?: SystemClock.elapsedRealtime()
        val isPlaying = playback?.state == PlaybackState.STATE_PLAYING ||
            playback?.state == PlaybackState.STATE_BUFFERING

        repository.updateMediaState(
            MediaPlaybackState(
                isActive = title.isNotBlank() || playback != null,
                isPlaying = isPlaying,
                title = title.ifBlank { "Media Playing" },
                artist = artist,
                album = album,
                packageName = controller.packageName,
                positionMs = rawPosition.coerceAtLeast(0L),
                durationMs = duration.coerceAtLeast(0L),
                lastPositionUpdateElapsedMs = updateElapsed,
                playbackSpeed = speed,
                artwork = artwork
            )
        )
    }

    private fun refreshNotifications() {
        val active = runCatching { activeNotifications }.getOrNull() ?: return
        var activeIncomingCall: IncomingCallAlert? = null
        var activeOngoingCall: OngoingCallState? = null
        val filteredItems = mutableListOf<NotificationItem>()

        for (sbn in active) {
            if (sbn.packageName == packageName) continue
            val notification = sbn.notification ?: continue

            if (notification.category == Notification.CATEGORY_CALL) {
                val answerExtra = getPendingIntentExtra(notification, "android.answerIntent")
                val declineExtra = getPendingIntentExtra(notification, "android.declineIntent")
                val hangUpExtra = getPendingIntentExtra(notification, "android.hangUpIntent")
                val actions = notification.actions.orEmpty()
                val usesChronometer = notification.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false)

                val isIncoming = answerExtra != null ||
                    declineExtra != null ||
                    (!usesChronometer && (actions.size >= 2 || notification.fullScreenIntent != null))

                if (isIncoming) {
                    activeIncomingCall = extractIncomingCall(sbn, notification, answerExtra, declineExtra)
                    continue
                }

                val isOngoingCall = hangUpExtra != null ||
                    usesChronometer ||
                    (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0

                if (isOngoingCall) {
                    activeOngoingCall = extractOngoingCall(sbn, notification, hangUpExtra)
                    continue
                }
            }

            // Filter out ongoing noise, group summaries, and media notifications
            val isOngoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
            val isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
            val isMediaStyle = notification.extras.containsKey(Notification.EXTRA_MEDIA_SESSION)
            if (isOngoing || isGroupSummary || isMediaStyle) continue

            val title = notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
            val text = (notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: notification.extras.getCharSequence(Notification.EXTRA_TEXT))
                ?.toString()?.trim()

            if (title.isNullOrBlank() && text.isNullOrBlank()) continue

            filteredItems.add(
                NotificationItem(
                    key = sbn.key,
                    packageName = sbn.packageName,
                    appName = resolveAppName(sbn.packageName),
                    title = title ?: resolveAppName(sbn.packageName),
                    text = text.orEmpty(),
                    timestampMs = sbn.postTime,
                    smallIcon = notification.smallIcon,
                    contentIntent = notification.contentIntent,
                    isClearable = sbn.isClearable
                )
            )
        }

        val prevIncomingKey = repository.incomingCall.value?.key
        val prevOngoingKey = repository.ongoingCall.value?.key

        repository.updateIncomingCall(activeIncomingCall)
        repository.updateOngoingCall(activeOngoingCall)
        repository.updateNotifications(
            filteredItems.sortedByDescending { it.timestampMs }
        )

        val newCallAppeared = (activeIncomingCall != null && activeIncomingCall.key != prevIncomingKey) ||
            (activeOngoingCall != null && activeOngoingCall.key != prevOngoingKey)
        if (newCallAppeared && repository.isDashboardSessionActive && !repository.isDashboardForeground) {
            CallActionHelper.bringAppToFrontSequence(this)
        }
    }

    @Suppress("DEPRECATION")
    private fun getPendingIntentExtra(notification: Notification, key: String): PendingIntent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notification.extras.getParcelable(key, PendingIntent::class.java)
        } else {
            notification.extras.getParcelable(key)
        }
    }

    private fun extractIncomingCall(
        sbn: StatusBarNotification,
        notification: Notification,
        answerExtra: PendingIntent?,
        declineExtra: PendingIntent?
    ): IncomingCallAlert {
        val callerName = notification.extras.getCharSequence(Notification.EXTRA_TITLE)
            ?.toString()?.trim().orEmpty().ifBlank { "Incoming Call" }
        val subtitle = notification.extras.getCharSequence(Notification.EXTRA_TEXT)
            ?.toString()?.trim().orEmpty().ifBlank { "Ringing" }

        val actions = notification.actions.orEmpty()
        var acceptIntent = answerExtra ?: actions.firstOrNull { action ->
            val label = action.title?.toString()?.lowercase().orEmpty()
            label.contains("answer") || label.contains("accept") || label.contains("pick")
        }?.actionIntent

        var declineIntent = declineExtra ?: actions.firstOrNull { action ->
            val label = action.title?.toString()?.lowercase().orEmpty()
            label.contains("decline") || label.contains("reject") || label.contains("dismiss") || label.contains("hang")
        }?.actionIntent

        if (acceptIntent == null && declineIntent == null && actions.size >= 2) {
            declineIntent = actions[0].actionIntent
            acceptIntent = actions[1].actionIntent
        }

        return IncomingCallAlert(
            key = sbn.key,
            callerName = callerName,
            callerSubtitle = subtitle,
            appName = resolveAppName(sbn.packageName),
            acceptIntent = acceptIntent,
            declineIntent = declineIntent,
            contentIntent = notification.contentIntent
        )
    }

    private fun extractOngoingCall(
        sbn: StatusBarNotification,
        notification: Notification,
        hangUpExtra: PendingIntent?
    ): OngoingCallState {
        val callerName = notification.extras.getCharSequence(Notification.EXTRA_TITLE)
            ?.toString()?.trim().orEmpty().ifBlank { "Active Call" }

        val actions = notification.actions.orEmpty()
        val hangUpIntent = hangUpExtra ?: actions.firstOrNull { action ->
            val label = action.title?.toString()?.lowercase().orEmpty()
            label.contains("hang") || label.contains("end") || label.contains("disconnect") || label.contains("leave")
        }?.actionIntent ?: actions.lastOrNull()?.actionIntent

        val speakerIntent = actions.firstOrNull { action ->
            val label = action.title?.toString()?.lowercase().orEmpty()
            label.contains("speaker") || label.contains("audio") || label.contains("earpiece")
        }?.actionIntent

        val muteIntent = actions.firstOrNull { action ->
            val label = action.title?.toString()?.lowercase().orEmpty()
            label.contains("mute") || label.contains("mic")
        }?.actionIntent

        val startedAt = notification.`when`.takeIf { it > 0L } ?: sbn.postTime

        return OngoingCallState(
            key = sbn.key,
            callerName = callerName,
            appName = resolveAppName(sbn.packageName),
            startedAtMs = startedAt,
            hangUpIntent = hangUpIntent,
            speakerIntent = speakerIntent,
            muteIntent = muteIntent,
            contentIntent = notification.contentIntent
        )
    }

    private fun resolveAppName(pkg: String): String {
        return runCatching {
            val appInfo = packageManager.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
            packageManager.getApplicationLabel(appInfo).toString()
        }.getOrElse {
            runCatching {
                @Suppress("DEPRECATION")
                val appInfo = packageManager.getApplicationInfo(pkg, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            }.getOrDefault(pkg.substringAfterLast('.'))
        }
    }
}
