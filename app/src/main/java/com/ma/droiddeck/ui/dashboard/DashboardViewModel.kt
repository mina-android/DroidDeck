package com.ma.droiddeck.ui.dashboard

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ma.droiddeck.DroidDeckApp
import com.ma.droiddeck.data.model.CalendarEventSummary
import com.ma.droiddeck.data.model.DashboardWidgetType
import com.ma.droiddeck.data.model.DroidDeckSettings
import com.ma.droiddeck.data.model.DroidDeckTimerState
import com.ma.droiddeck.data.model.DeviceStatusState
import com.ma.droiddeck.data.model.IncomingCallAlert
import com.ma.droiddeck.data.model.MediaPlaybackState
import com.ma.droiddeck.data.model.NotificationItem
import com.ma.droiddeck.data.model.OngoingCallState
import com.ma.droiddeck.data.model.QuickAppEntry
import com.ma.droiddeck.data.model.RecentCallEntry
import com.ma.droiddeck.data.model.RingerModeState
import com.ma.droiddeck.data.model.WeatherState
import com.ma.droiddeck.data.preferences.UserPreferencesRepository
import com.ma.droiddeck.data.repository.CalendarWeatherRepository
import com.ma.droiddeck.service.DroidDeckNotificationService
import com.ma.droiddeck.service.WorkingHoursLockService
import com.ma.droiddeck.util.CallActionHelper
import com.ma.droiddeck.util.MediaProgressCalculator
import com.ma.droiddeck.util.PixelShiftCalculator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime

data class DashboardUiState(
    val currentTime: LocalDateTime = LocalDateTime.now(),
    val pixelShiftDp: Pair<Int, Int> = 0 to 0,
    val settings: DroidDeckSettings = DroidDeckSettings(),
    val media: MediaPlaybackState = MediaPlaybackState(),
    val liveMediaPositionMs: Long = 0L,
    val notifications: List<NotificationItem> = emptyList(),
    val incomingCall: IncomingCallAlert? = null,
    val ongoingCall: OngoingCallState? = null,
    val liveCallDurationSeconds: Long = 0L,
    val recentCalls: List<RecentCallEntry> = emptyList(),
    val nextCalendarEvent: CalendarEventSummary? = null,
    val weather: WeatherState = WeatherState(),
    val installedApps: List<QuickAppEntry> = emptyList(),
    val deviceStatus: DeviceStatusState = DeviceStatusState(),
    val timer: DroidDeckTimerState = DroidDeckTimerState(),
    val isProximityCovered: Boolean = false,
    val isNotificationAccessGranted: Boolean = false,
    val isDndAccessGranted: Boolean = false,
    val isCallControlGranted: Boolean = false,
    val isCalendarGranted: Boolean = false,
    val isCallLogGranted: Boolean = false,
    val isTemporarilyBrightened: Boolean = false,
    val isIdleBurnInDimmed: Boolean = false
)

/**
 * ViewModel driving the pure-black DroidDeck dashboard.
 * Uses aligned minute-boundary delays when seconds are hidden so Compose performs
 * zero idle recompositions between minutes, and ticks media/call timers only while active.
 */
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DroidDeckApp
    private val preferences = app.preferencesRepository
    private val repository = app.dashboardRepository
    private val calendarWeatherRepo = CalendarWeatherRepository(app)
    private val audioManager = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _currentTime = MutableStateFlow(LocalDateTime.now())
    private val _liveMediaPositionMs = MutableStateFlow(0L)
    private val _liveCallDurationSeconds = MutableStateFlow(0L)
    private val _recentCalls = MutableStateFlow<List<RecentCallEntry>>(emptyList())
    private val _nextCalendarEvent = MutableStateFlow<CalendarEventSummary?>(null)
    private val _weather = MutableStateFlow(WeatherState())
    private val _installedApps = MutableStateFlow<List<QuickAppEntry>>(emptyList())
    private val _timerState = MutableStateFlow(DroidDeckTimerState())
    private val _hasNotificationAccess = MutableStateFlow(checkNotificationListenerEnabled())
    private val _hasDndAccess = MutableStateFlow(checkDndPolicyAccess())
    private val _hasCallControl = MutableStateFlow(hasPermission(Manifest.permission.ANSWER_PHONE_CALLS))
    private val _hasCalendar = MutableStateFlow(hasPermission(Manifest.permission.READ_CALENDAR))
    private val _hasCallLog = MutableStateFlow(hasPermission(Manifest.permission.READ_CALL_LOG))
    private val _isTemporarilyBrightened = MutableStateFlow(false)
    private val _isIdleBurnInDimmed = MutableStateFlow(false)

    private var clockJob: Job? = null
    private var mediaProgressJob: Job? = null
    private var callTimerJob: Job? = null
    private var timerJob: Job? = null
    private var brightenJob: Job? = null
    private var burnInIdleJob: Job? = null
    private var lastNonZeroMediaVolume: Int = 5
    private var lastNonZeroRingVolume: Int = 5
    private var lastLoadedWeatherCity: String? = null

    private val permissionsAndExtrasFlow = combine(
        _hasNotificationAccess,
        _hasDndAccess,
        _hasCallControl,
        _hasCalendar,
        _hasCallLog,
        _isTemporarilyBrightened,
        _isIdleBurnInDimmed,
        _recentCalls,
        _nextCalendarEvent,
        _weather,
        _installedApps
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        ExtrasBundle(
            notif = arr[0] as Boolean,
            dnd = arr[1] as Boolean,
            callCtrl = arr[2] as Boolean,
            calendar = arr[3] as Boolean,
            callLog = arr[4] as Boolean,
            brightened = arr[5] as Boolean,
            burnInDimmed = arr[6] as Boolean,
            recentCalls = arr[7] as List<RecentCallEntry>,
            nextEvent = arr[8] as CalendarEventSummary?,
            weather = arr[9] as WeatherState,
            installedApps = arr[10] as List<QuickAppEntry>
        )
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        _currentTime,
        preferences.settingsFlow,
        repository.mediaState,
        _liveMediaPositionMs,
        repository.notifications,
        repository.incomingCall,
        repository.ongoingCall,
        _liveCallDurationSeconds,
        repository.deviceStatus,
        _timerState,
        repository.isProximityCovered,
        permissionsAndExtrasFlow
    ) { values ->
        val time = values[0] as LocalDateTime
        val settings = values[1] as DroidDeckSettings
        @Suppress("UNCHECKED_CAST")
        val allNotifications = values[4] as List<NotificationItem>
        val filteredNotifications = allNotifications
            .filter { !settings.blacklistedNotificationPackages.contains(it.packageName) }
            .take(5)

        val epochMinute = System.currentTimeMillis() / 60_000L
        val shift = if (settings.pixelShiftEnabled) {
            PixelShiftCalculator.offsetForMinute(epochMinute)
        } else {
            0 to 0
        }
        val extras = values[11] as ExtrasBundle

        DashboardUiState(
            currentTime = time,
            pixelShiftDp = shift,
            settings = settings,
            media = values[2] as MediaPlaybackState,
            liveMediaPositionMs = values[3] as Long,
            notifications = filteredNotifications,
            incomingCall = values[5] as IncomingCallAlert?,
            ongoingCall = values[6] as OngoingCallState?,
            liveCallDurationSeconds = values[7] as Long,
            recentCalls = extras.recentCalls,
            nextCalendarEvent = extras.nextEvent,
            weather = extras.weather,
            installedApps = extras.installedApps,
            deviceStatus = values[8] as DeviceStatusState,
            timer = values[9] as DroidDeckTimerState,
            isProximityCovered = values[10] as Boolean,
            isNotificationAccessGranted = extras.notif,
            isDndAccessGranted = extras.dnd,
            isCallControlGranted = extras.callCtrl,
            isCalendarGranted = extras.calendar,
            isCallLogGranted = extras.callLog,
            isTemporarilyBrightened = extras.brightened,
            isIdleBurnInDimmed = extras.burnInDimmed
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState()
    )

    init {
        viewModelScope.launch {
            preferences.settingsFlow.collect { settings ->
                restartClockTicker(showSeconds = settings.showSeconds)
                if (lastLoadedWeatherCity != settings.weatherCity) {
                    lastLoadedWeatherCity = settings.weatherCity
                    refreshWeather(settings.weatherCity, forceRefresh = false)
                }
                if (!settings.antiBurnInDimmingEnabled) {
                    _isIdleBurnInDimmed.value = false
                }
            }
        }
        viewModelScope.launch {
            repository.mediaState.collect { media ->
                restartMediaProgressTicker(media)
            }
        }
        viewModelScope.launch {
            repository.ongoingCall.collect { call ->
                restartCallDurationTicker(call)
            }
        }
        viewModelScope.launch {
            _installedApps.value = calendarWeatherRepo.queryInstalledLauncherApps()
        }
        resetBurnInIdleTimer()
    }

    private fun restartClockTicker(showSeconds: Boolean) {
        clockJob?.cancel()
        clockJob = viewModelScope.launch {
            while (isActive) {
                _currentTime.value = LocalDateTime.now()
                refreshCalendarAndRecentCalls()
                val nowMs = System.currentTimeMillis()
                val nextTickDelay = if (showSeconds) {
                    1_000L - (nowMs % 1_000L)
                } else {
                    60_000L - (nowMs % 60_000L)
                }
                delay(nextTickDelay.coerceAtLeast(50L))
            }
        }
    }

    private fun restartMediaProgressTicker(media: MediaPlaybackState) {
        mediaProgressJob?.cancel()
        val initialPos = MediaProgressCalculator.computeCurrentPositionMs(
            basePositionMs = media.positionMs,
            durationMs = media.durationMs,
            lastUpdateElapsedMs = media.lastPositionUpdateElapsedMs,
            nowElapsedMs = SystemClock.elapsedRealtime(),
            playbackSpeed = media.playbackSpeed,
            isPlaying = media.isPlaying
        )
        _liveMediaPositionMs.value = initialPos

        if (!media.isActive || !media.isPlaying) return

        mediaProgressJob = viewModelScope.launch {
            while (isActive) {
                _liveMediaPositionMs.value = MediaProgressCalculator.computeCurrentPositionMs(
                    basePositionMs = media.positionMs,
                    durationMs = media.durationMs,
                    lastUpdateElapsedMs = media.lastPositionUpdateElapsedMs,
                    nowElapsedMs = SystemClock.elapsedRealtime(),
                    playbackSpeed = media.playbackSpeed,
                    isPlaying = true
                )
                delay(1_000L)
            }
        }
    }

    private fun restartCallDurationTicker(call: OngoingCallState?) {
        callTimerJob?.cancel()
        if (call == null) {
            _liveCallDurationSeconds.value = 0L
            return
        }
        callTimerJob = viewModelScope.launch {
            while (isActive) {
                val elapsedSec = ((System.currentTimeMillis() - call.startedAtMs) / 1000L).coerceAtLeast(0L)
                _liveCallDurationSeconds.value = elapsedSec
                delay(1_000L)
            }
        }
    }

    fun refreshPermissions() {
        _hasNotificationAccess.value = checkNotificationListenerEnabled()
        _hasDndAccess.value = checkDndPolicyAccess()
        _hasCallControl.value = hasPermission(Manifest.permission.ANSWER_PHONE_CALLS)
        _hasCalendar.value = hasPermission(Manifest.permission.READ_CALENDAR)
        _hasCallLog.value = hasPermission(Manifest.permission.READ_CALL_LOG)
        refreshCalendarAndRecentCalls()
    }

    fun refreshCalendarAndRecentCalls() {
        _nextCalendarEvent.value = calendarWeatherRepo.fetchNextCalendarEvent()
        _recentCalls.value = calendarWeatherRepo.fetchRecentCalls(limit = 4)
    }

    fun refreshWeather(city: String, forceRefresh: Boolean = true) {
        viewModelScope.launch {
            _weather.value = calendarWeatherRepo.fetchWeather(city, forceRefresh = forceRefresh)
        }
    }

    fun onUserInteraction() {
        _isIdleBurnInDimmed.value = false
        resetBurnInIdleTimer()
    }

    private fun resetBurnInIdleTimer() {
        burnInIdleJob?.cancel()
        burnInIdleJob = viewModelScope.launch {
            delay(30L * 60L * 1000L) // 30 minutes continuous idle
            if (preferences.settingsFlow.first().antiBurnInDimmingEnabled) {
                _isIdleBurnInDimmed.value = true
            }
        }
    }

    fun triggerTemporaryBrighten() {
        onUserInteraction()
        brightenJob?.cancel()
        brightenJob = viewModelScope.launch {
            _isTemporarilyBrightened.value = true
            delay(10_000L)
            _isTemporarilyBrightened.value = false
        }
    }

    fun togglePlayPause() = repository.togglePlayPause()
    fun skipToNext() = repository.skipToNext()
    fun skipToPrevious() = repository.skipToPrevious()
    fun seekMediaTo(positionMs: Long) {
        _liveMediaPositionMs.value = positionMs.coerceAtLeast(0L)
        repository.seekTo(positionMs)
    }

    fun dismissNotification(key: String) = repository.dismissNotification(key)
    fun clearAllNotifications() = repository.clearAllNotifications()

    fun setMediaVolume(volume: Int) {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val clamped = volume.coerceIn(0, max)
        if (clamped > 0) lastNonZeroMediaVolume = clamped
        runCatching {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, clamped, 0)
        }
        repository.updateDeviceStatus {
            it.copy(mediaVolume = clamped, mediaMaxVolume = max)
        }
    }

    fun adjustMediaVolume(delta: Int) {
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        setMediaVolume(current + delta)
    }

    fun toggleMediaMute() {
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (current > 0) {
            lastNonZeroMediaVolume = current
            setMediaVolume(0)
        } else {
            setMediaVolume(lastNonZeroMediaVolume.coerceAtLeast(1))
        }
    }

    fun toggleDnd(): Boolean {
        if (!checkDndPolicyAccess()) return false
        val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return false
        val nextFilter = if (nm.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL) {
            NotificationManager.INTERRUPTION_FILTER_PRIORITY
        } else {
            NotificationManager.INTERRUPTION_FILTER_ALL
        }
        nm.setInterruptionFilter(nextFilter)
        repository.updateDeviceStatus {
            it.copy(isDndActive = nextFilter != NotificationManager.INTERRUPTION_FILTER_ALL)
        }
        return true
    }

    fun cycleRingerMode(): Boolean {
        val nextMode = when (repository.deviceStatus.value.ringerMode) {
            RingerModeState.SOUND -> RingerModeState.VIBRATE
            RingerModeState.VIBRATE -> RingerModeState.MUTE
            RingerModeState.MUTE -> RingerModeState.SOUND
        }

        val applied = runCatching {
            when (nextMode) {
                RingerModeState.SOUND -> {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    audioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_UNMUTE, 0)
                    audioManager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_UNMUTE, 0)
                    if (audioManager.getStreamVolume(AudioManager.STREAM_RING) == 0) {
                        val maxRing = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING).coerceAtLeast(1)
                        audioManager.setStreamVolume(
                            AudioManager.STREAM_RING,
                            lastNonZeroRingVolume.coerceIn(1, maxRing),
                            0
                        )
                    }
                }
                RingerModeState.VIBRATE -> {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_UNMUTE, 0)
                    audioManager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_UNMUTE, 0)
                    audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                }
                RingerModeState.MUTE -> {
                    val curRing = audioManager.getStreamVolume(AudioManager.STREAM_RING)
                    if (curRing > 0) lastNonZeroRingVolume = curRing
                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    audioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_MUTE, 0)
                    audioManager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_MUTE, 0)
                    if (audioManager.getStreamVolume(AudioManager.STREAM_RING) > 0) {
                        audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
                    }
                }
            }
            true
        }.getOrDefault(false)

        if (applied) {
            repository.updateDeviceStatus { it.copy(ringerMode = nextMode) }
        }
        return applied
    }

    @Suppress("DEPRECATION")
    fun toggleSpeakerphone(): Boolean {
        val next = !repository.deviceStatus.value.isSpeakerphoneOn
        val sentNotificationAction = CallActionHelper.sendPendingIntentAllowBg(
            repository.ongoingCall.value?.speakerIntent
        )
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val applied = if (next) {
                    val speaker = audioManager.availableCommunicationDevices
                        .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                    if (speaker != null) audioManager.setCommunicationDevice(speaker) else false
                } else {
                    audioManager.clearCommunicationDevice()
                    true
                }
                if (!applied) {
                    audioManager.isSpeakerphoneOn = next
                }
            } else {
                audioManager.isSpeakerphoneOn = next
            }
        }
        val actual = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.communicationDevice?.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        } else {
            audioManager.isSpeakerphoneOn
        }
        val effective = if (sentNotificationAction) next else actual
        repository.updateDeviceStatus { it.copy(isSpeakerphoneOn = effective) }
        return sentNotificationAction || actual == next
    }

    fun toggleMicMute() {
        val next = !repository.deviceStatus.value.isMicMuted
        CallActionHelper.sendPendingIntentAllowBg(repository.ongoingCall.value?.muteIntent)
        runCatching { audioManager.isMicrophoneMute = next }
        repository.updateDeviceStatus { it.copy(isMicMuted = next) }
    }

    fun commitWidgetLayout(order: List<DashboardWidgetType>, left: Set<DashboardWidgetType>) {
        updateSettings { current ->
            current.copy(
                widgetOrder = order.distinct(),
                landscapeLeftWidgets = left
            )
        }
    }

    fun reorderWidgets(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        updateSettings { current ->
            val mutable = current.widgetOrder.toMutableList()
            if (fromIndex !in mutable.indices || toIndex !in mutable.indices) return@updateSettings current
            val moved = mutable.removeAt(fromIndex)
            mutable.add(toIndex, moved)
            current.copy(widgetOrder = mutable)
        }
    }

    fun moveWidgetLandscapeColumn(widget: DashboardWidgetType, toLeftColumn: Boolean) {
        updateSettings { current ->
            val nextLeft = current.landscapeLeftWidgets.toMutableSet()
            if (toLeftColumn) nextLeft.add(widget) else nextLeft.remove(widget)
            current.copy(landscapeLeftWidgets = nextLeft)
        }
    }

    fun toggleWidgetVisibility(widget: DashboardWidgetType) {
        updateSettings { current ->
            val nextHidden = current.hiddenWidgets.toMutableSet()
            if (nextHidden.contains(widget)) nextHidden.remove(widget) else nextHidden.add(widget)
            current.copy(hiddenWidgets = nextHidden)
        }
    }

    fun toggleBlacklistPackage(packageName: String) {
        updateSettings { current ->
            val next = current.blacklistedNotificationPackages.toMutableSet()
            if (next.contains(packageName)) next.remove(packageName) else next.add(packageName)
            current.copy(blacklistedNotificationPackages = next)
        }
    }

    fun toggleQuickAppPackage(packageName: String) {
        updateSettings { current ->
            val next = current.quickAppPackages.toMutableList()
            if (next.contains(packageName)) {
                next.remove(packageName)
            } else if (next.size < 4) {
                next.add(packageName)
            }
            current.copy(quickAppPackages = next)
        }
    }

    fun resetWidgetOrder() {
        updateSettings {
            it.copy(
                widgetOrder = DashboardWidgetType.DEFAULT_ORDER,
                landscapeLeftWidgets = DashboardWidgetType.DEFAULT_LANDSCAPE_LEFT
            )
        }
    }

    fun toggleTimerRunning() {
        val current = _timerState.value
        if (current.isRunning) {
            timerJob?.cancel()
            _timerState.value = current.copy(isRunning = false)
        } else {
            val startFrom = if (current.remainingSeconds <= 0) current.totalSeconds else current.remainingSeconds
            _timerState.value = current.copy(isRunning = true, remainingSeconds = startFrom)
            timerJob?.cancel()
            timerJob = viewModelScope.launch {
                while (isActive && _timerState.value.remainingSeconds > 0) {
                    delay(1_000L)
                    _timerState.value = _timerState.value.copy(
                        remainingSeconds = (_timerState.value.remainingSeconds - 1).coerceAtLeast(0)
                    )
                }
                _timerState.value = _timerState.value.copy(isRunning = false)
            }
        }
    }

    fun setTimerPreset(minutes: Int, label: String) {
        timerJob?.cancel()
        val seconds = minutes * 60
        _timerState.value = DroidDeckTimerState(
            isRunning = false,
            totalSeconds = seconds,
            remainingSeconds = seconds,
            label = label
        )
    }

    fun resetTimer() {
        timerJob?.cancel()
        val current = _timerState.value
        _timerState.value = current.copy(
            isRunning = false,
            remainingSeconds = current.totalSeconds
        )
    }

    fun updateSettings(transform: (DroidDeckSettings) -> DroidDeckSettings) {
        viewModelScope.launch {
            preferences.updateSettings(transform)
            val latest = preferences.settingsFlow.first()
            if (latest.workingHoursEnabled) {
                WorkingHoursLockService.start(app)
            } else {
                WorkingHoursLockService.stop(app)
            }
        }
    }

    suspend fun exportSettingsJson(): String {
        val latest = preferences.settingsFlow.first()
        return UserPreferencesRepository.exportSettingsJson(latest)
    }

    suspend fun importSettingsJson(json: String): Boolean {
        return runCatching {
            val parsed = UserPreferencesRepository.importSettingsJson(json)
            preferences.replaceSettings(parsed)
            true
        }.getOrDefault(false)
    }

    private fun checkNotificationListenerEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            app.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        val expected = ComponentName(app, DroidDeckNotificationService::class.java).flattenToString()
        return enabledListeners.contains(expected)
    }

    private fun checkDndPolicyAccess(): Boolean {
        val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return false
        return nm.isNotificationPolicyAccessGranted
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(app, permission) == PackageManager.PERMISSION_GRANTED

    private data class ExtrasBundle(
        val notif: Boolean,
        val dnd: Boolean,
        val callCtrl: Boolean,
        val calendar: Boolean,
        val callLog: Boolean,
        val brightened: Boolean,
        val burnInDimmed: Boolean,
        val recentCalls: List<RecentCallEntry>,
        val nextEvent: CalendarEventSummary?,
        val weather: WeatherState,
        val installedApps: List<QuickAppEntry>
    )
}
