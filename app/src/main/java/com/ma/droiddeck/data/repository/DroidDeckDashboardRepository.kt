package com.ma.droiddeck.data.repository

import android.app.NotificationManager
import com.ma.droiddeck.data.model.DeviceStatusState
import com.ma.droiddeck.data.model.IncomingCallAlert
import com.ma.droiddeck.data.model.MediaPlaybackState
import com.ma.droiddeck.data.model.NotificationItem
import com.ma.droiddeck.data.model.OngoingCallState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Thread-safe singleton state holder connecting Android system services
 * (NotificationListenerService, MediaSessionManager, StatusMonitor) to the UI layer.
 */
class DroidDeckDashboardRepository private constructor() {

    private val _mediaState = MutableStateFlow(MediaPlaybackState())
    val mediaState: StateFlow<MediaPlaybackState> = _mediaState.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _incomingCall = MutableStateFlow<IncomingCallAlert?>(null)
    val incomingCall: StateFlow<IncomingCallAlert?> = _incomingCall.asStateFlow()

    private val _ongoingCall = MutableStateFlow<OngoingCallState?>(null)
    val ongoingCall: StateFlow<OngoingCallState?> = _ongoingCall.asStateFlow()

    private val _deviceStatus = MutableStateFlow(DeviceStatusState())
    val deviceStatus: StateFlow<DeviceStatusState> = _deviceStatus.asStateFlow()

    private val _isProximityCovered = MutableStateFlow(false)
    val isProximityCovered: StateFlow<Boolean> = _isProximityCovered.asStateFlow()

    @Volatile
    var isDashboardSessionActive: Boolean = false

    @Volatile
    var isDashboardForeground: Boolean = false

    @Volatile
    var isPopupSuppressionDndActive: Boolean = false

    @Volatile
    var isUserQuietDndActive: Boolean = false

    @Volatile
    var savedSystemNotificationPolicy: NotificationManager.Policy? = null

    @Volatile
    var savedSystemInterruptionFilter: Int? = null

    private var mediaActionHandler: MediaActionHandler? = null
    private var notificationDismissHandler: ((String) -> Unit)? = null
    private var clearAllNotificationsHandler: (() -> Unit)? = null
    private var bringToFrontHandler: (() -> Unit)? = null

    fun updateMediaState(state: MediaPlaybackState) {
        _mediaState.value = state
    }

    fun updateNotifications(items: List<NotificationItem>) {
        _notifications.value = items
    }

    fun updateIncomingCall(alert: IncomingCallAlert?) {
        _incomingCall.value = alert
    }

    fun updateOngoingCall(call: OngoingCallState?) {
        _ongoingCall.value = call
    }

    fun updateDeviceStatus(transform: (DeviceStatusState) -> DeviceStatusState) {
        _deviceStatus.value = transform(_deviceStatus.value)
    }

    fun updateProximityCovered(covered: Boolean) {
        _isProximityCovered.value = covered
    }

    fun registerMediaActionHandler(handler: MediaActionHandler?) {
        mediaActionHandler = handler
    }

    fun registerNotificationDismissHandler(handler: ((String) -> Unit)?) {
        notificationDismissHandler = handler
    }

    fun registerClearAllNotificationsHandler(handler: (() -> Unit)?) {
        clearAllNotificationsHandler = handler
    }

    fun registerBringToFrontHandler(handler: (() -> Unit)?) {
        bringToFrontHandler = handler
    }

    fun requestBringToFront(): Boolean {
        val handler = bringToFrontHandler ?: return false
        handler.invoke()
        return true
    }

    fun togglePlayPause() = mediaActionHandler?.onPlayPause()
    fun skipToNext() = mediaActionHandler?.onNext()
    fun skipToPrevious() = mediaActionHandler?.onPrevious()
    fun seekTo(positionMs: Long) = mediaActionHandler?.onSeekTo(positionMs)
    fun dismissNotification(key: String) = notificationDismissHandler?.invoke(key)
    fun clearAllNotifications() = clearAllNotificationsHandler?.invoke()

    interface MediaActionHandler {
        fun onPlayPause()
        fun onNext()
        fun onPrevious()
        fun onSeekTo(positionMs: Long)
    }

    companion object {
        @Volatile
        private var instance: DroidDeckDashboardRepository? = null

        fun getInstance(): DroidDeckDashboardRepository =
            instance ?: synchronized(this) {
                instance ?: DroidDeckDashboardRepository().also { instance = it }
            }
    }
}
