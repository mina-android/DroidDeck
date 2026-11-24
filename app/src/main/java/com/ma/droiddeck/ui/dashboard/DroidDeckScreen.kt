package com.ma.droiddeck.ui.dashboard

import android.app.PendingIntent
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ma.droiddeck.data.model.DashboardWidgetType
import com.ma.droiddeck.data.model.DroidDeckSettings
import com.ma.droiddeck.data.model.IncomingCallAlert
import com.ma.droiddeck.data.model.OngoingCallState
import com.ma.droiddeck.ui.components.ClockSection
import com.ma.droiddeck.ui.components.IncomingCallBanner
import com.ma.droiddeck.ui.components.MusicControlCard
import com.ma.droiddeck.ui.components.NotificationPillList
import com.ma.droiddeck.ui.components.OngoingCallBanner
import com.ma.droiddeck.ui.components.QuickAppDockCard
import com.ma.droiddeck.ui.components.RecentCallsWidgetCard
import com.ma.droiddeck.ui.components.StatusHeader
import com.ma.droiddeck.ui.settings.SettingsBottomSheet
import com.ma.droiddeck.ui.theme.PureBlack

/**
 * Root pure-black OLED dashboard screen where the entire area below [StatusHeader]
 * is a unified, reorderable widget zone adapting cleanly between Portrait (1-column)
 * and Landscape (2-column cross-column drag-and-drop).
 */
@Composable
fun DroidDeckScreen(
    uiState: DashboardUiState,
    onTapBackground: () -> Unit,
    onUserInteraction: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekMediaTo: (Long) -> Unit,
    onSetMediaVolume: (Int) -> Unit,
    onAdjustMediaVolume: (Int) -> Unit,
    onToggleMediaMute: () -> Unit,
    onAnswerIncomingCall: (IncomingCallAlert) -> Unit,
    onDeclineIncomingCall: (IncomingCallAlert) -> Unit,
    onMuteRinger: () -> Unit,
    onEndOngoingCall: (OngoingCallState) -> Unit,
    onToggleSpeakerphone: () -> Unit,
    onToggleMicMute: () -> Unit,
    onCallRecentNumber: (String) -> Unit,
    onOpenNotification: (PendingIntent?) -> Unit,
    onDismissNotification: (String) -> Unit,
    onClearAllNotifications: () -> Unit,
    onOpenCalendarEvent: (Long) -> Unit,
    onRefreshWeather: (String) -> Unit,
    onLaunchQuickApp: (String) -> Unit,
    onToggleDnd: () -> Unit,
    onCycleRingerMode: () -> Unit,
    onToggleRotate: () -> Unit,
    onResetAutoRotate: () -> Unit,
    onExitApp: () -> Unit,
    onCommitWidgetLayout: (List<DashboardWidgetType>, Set<DashboardWidgetType>) -> Unit,
    onReorderWidgets: (Int, Int) -> Unit,
    onMoveWidgetLandscapeColumn: (DashboardWidgetType, Boolean) -> Unit,
    onToggleWidgetVisibility: (DashboardWidgetType) -> Unit,
    onToggleBlacklistPackage: (String) -> Unit,
    onToggleQuickAppPackage: (String) -> Unit,
    onResetWidgetOrder: () -> Unit,
    onToggleTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onSetTimerPreset: (Int, String) -> Unit,
    onUpdateSettings: ((DroidDeckSettings) -> DroidDeckSettings) -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onRequestDndAccess: () -> Unit,
    isOverlayGranted: Boolean,
    onRequestOverlayAccess: () -> Unit,
    onRequestRuntimePermissions: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    // Proximity blanking: render 100% pure black when covered
    if (uiState.settings.proximityBlankingEnabled && uiState.isProximityCovered) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PureBlack)
        )
        return
    }

    var showSettings by remember { mutableStateOf(false) }
    var presetIndex by remember { mutableIntStateOf(0) }
    val presets = remember {
        listOf(
            25 to "Focus",
            50 to "Deep",
            5 to "Break"
        )
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val (shiftX, shiftY) = uiState.pixelShiftDp
    val contentAlpha = if (uiState.settings.antiBurnInDimmingEnabled && uiState.isIdleBurnInDimmed) {
        0.72f
    } else {
        1.0f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTapBackground
            )
            .displayCutoutPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .alpha(contentAlpha)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = shiftX.dp, y = shiftY.dp)
        ) {
            StatusHeader(
                status = uiState.deviceStatus,
                onToggleDnd = {
                    onUserInteraction()
                    onToggleDnd()
                },
                onCycleRingerMode = {
                    onUserInteraction()
                    onCycleRingerMode()
                },
                onToggleRotate = {
                    onUserInteraction()
                    onToggleRotate()
                },
                onResetAutoRotate = {
                    onUserInteraction()
                    onResetAutoRotate()
                },
                onOpenSettings = {
                    onUserInteraction()
                    showSettings = true
                },
                onExitApp = onExitApp
            )

            Spacer(modifier = Modifier.height(10.dp))

            var localOrder by remember(uiState.settings.widgetOrder) {
                mutableStateOf(uiState.settings.widgetOrder)
            }
            var localLeft by remember(uiState.settings.landscapeLeftWidgets) {
                mutableStateOf(uiState.settings.landscapeLeftWidgets)
            }

            val visibleWidgets = localOrder.filter { type ->
                !uiState.settings.hiddenWidgets.contains(type)
            }

            // Fallback: if Clock is hidden, still show incoming/ongoing call banners at top
            if (!visibleWidgets.contains(DashboardWidgetType.CLOCK)) {
                uiState.incomingCall?.let { call ->
                    IncomingCallBanner(
                        call = call,
                        onAnswerCall = onAnswerIncomingCall,
                        onDeclineCall = onDeclineIncomingCall,
                        onMuteRinger = onMuteRinger
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                uiState.ongoingCall?.let { call ->
                    OngoingCallBanner(
                        call = call,
                        durationSeconds = uiState.liveCallDurationSeconds,
                        isSpeakerphoneOn = uiState.deviceStatus.isSpeakerphoneOn,
                        isMicMuted = uiState.deviceStatus.isMicMuted,
                        onToggleSpeakerphone = onToggleSpeakerphone,
                        onToggleMicMute = onToggleMicMute,
                        onEndCall = onEndOngoingCall
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            val swapLocal: (Int, Int) -> Unit = { from, to ->
                if (from in localOrder.indices && to in localOrder.indices && from != to) {
                    val mutable = localOrder.toMutableList()
                    val moved = mutable.removeAt(from)
                    mutable.add(to, moved)
                    localOrder = mutable
                }
            }
            val moveLocalColumn: (DashboardWidgetType, Boolean) -> Unit = { widget, toLeft ->
                val next = localLeft.toMutableSet()
                if (toLeft) next.add(widget) else next.remove(widget)
                localLeft = next
                onCommitWidgetLayout(localOrder, next)
            }

            if (isLandscape) {
                val leftWidgets = visibleWidgets.filter { localLeft.contains(it) }
                val rightWidgets = visibleWidgets.filter { !localLeft.contains(it) }

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    WidgetColumn(
                        widgets = leftWidgets,
                        fullOrder = localOrder,
                        isLandscape = true,
                        isLeftColumn = true,
                        uiState = uiState,
                        onReorderWidgets = swapLocal,
                        onMoveLandscapeColumn = moveLocalColumn,
                        onDragFinished = { onCommitWidgetLayout(localOrder, localLeft) },
                        onCycleTimerPreset = {
                            presetIndex = (presetIndex + 1) % presets.size
                            val (mins, label) = presets[presetIndex]
                            onSetTimerPreset(mins, label)
                        },
                        onToggleTimer = onToggleTimer,
                        onResetTimer = onResetTimer,
                        onCallRecentNumber = onCallRecentNumber,
                        onRequestRuntimePermissions = onRequestRuntimePermissions,
                        onAnswerIncomingCall = onAnswerIncomingCall,
                        onDeclineIncomingCall = onDeclineIncomingCall,
                        onMuteRinger = onMuteRinger,
                        onEndOngoingCall = onEndOngoingCall,
                        onToggleSpeakerphone = onToggleSpeakerphone,
                        onToggleMicMute = onToggleMicMute,
                        onTogglePlayPause = onTogglePlayPause,
                        onSkipNext = onSkipNext,
                        onSkipPrevious = onSkipPrevious,
                        onSeekMediaTo = onSeekMediaTo,
                        onSetMediaVolume = onSetMediaVolume,
                        onAdjustMediaVolume = onAdjustMediaVolume,
                        onToggleMediaMute = onToggleMediaMute,
                        onOpenNotification = onOpenNotification,
                        onDismissNotification = onDismissNotification,
                        onClearAllNotifications = onClearAllNotifications,
                        onRequestNotificationAccess = onRequestNotificationAccess,
                        onOpenCalendarEvent = onOpenCalendarEvent,
                        onRefreshWeather = onRefreshWeather,
                        onLaunchQuickApp = onLaunchQuickApp,
                        modifier = Modifier
                            .weight(0.46f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                    )

                    WidgetColumn(
                        widgets = rightWidgets,
                        fullOrder = localOrder,
                        isLandscape = true,
                        isLeftColumn = false,
                        uiState = uiState,
                        onReorderWidgets = swapLocal,
                        onMoveLandscapeColumn = moveLocalColumn,
                        onDragFinished = { onCommitWidgetLayout(localOrder, localLeft) },
                        onCycleTimerPreset = {
                            presetIndex = (presetIndex + 1) % presets.size
                            val (mins, label) = presets[presetIndex]
                            onSetTimerPreset(mins, label)
                        },
                        onToggleTimer = onToggleTimer,
                        onResetTimer = onResetTimer,
                        onCallRecentNumber = onCallRecentNumber,
                        onRequestRuntimePermissions = onRequestRuntimePermissions,
                        onAnswerIncomingCall = onAnswerIncomingCall,
                        onDeclineIncomingCall = onDeclineIncomingCall,
                        onMuteRinger = onMuteRinger,
                        onEndOngoingCall = onEndOngoingCall,
                        onToggleSpeakerphone = onToggleSpeakerphone,
                        onToggleMicMute = onToggleMicMute,
                        onTogglePlayPause = onTogglePlayPause,
                        onSkipNext = onSkipNext,
                        onSkipPrevious = onSkipPrevious,
                        onSeekMediaTo = onSeekMediaTo,
                        onSetMediaVolume = onSetMediaVolume,
                        onAdjustMediaVolume = onAdjustMediaVolume,
                        onToggleMediaMute = onToggleMediaMute,
                        onOpenNotification = onOpenNotification,
                        onDismissNotification = onDismissNotification,
                        onClearAllNotifications = onClearAllNotifications,
                        onRequestNotificationAccess = onRequestNotificationAccess,
                        onOpenCalendarEvent = onOpenCalendarEvent,
                        onRefreshWeather = onRefreshWeather,
                        onLaunchQuickApp = onLaunchQuickApp,
                        modifier = Modifier
                            .weight(0.54f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                    )
                }
            } else {
                WidgetColumn(
                    widgets = visibleWidgets,
                    fullOrder = localOrder,
                    isLandscape = false,
                    isLeftColumn = false,
                    uiState = uiState,
                    onReorderWidgets = swapLocal,
                    onMoveLandscapeColumn = null,
                    onDragFinished = { onCommitWidgetLayout(localOrder, localLeft) },
                    onCycleTimerPreset = {
                        presetIndex = (presetIndex + 1) % presets.size
                        val (mins, label) = presets[presetIndex]
                        onSetTimerPreset(mins, label)
                    },
                    onToggleTimer = onToggleTimer,
                    onResetTimer = onResetTimer,
                    onCallRecentNumber = onCallRecentNumber,
                    onRequestRuntimePermissions = onRequestRuntimePermissions,
                    onAnswerIncomingCall = onAnswerIncomingCall,
                    onDeclineIncomingCall = onDeclineIncomingCall,
                    onMuteRinger = onMuteRinger,
                    onEndOngoingCall = onEndOngoingCall,
                    onToggleSpeakerphone = onToggleSpeakerphone,
                    onToggleMicMute = onToggleMicMute,
                    onTogglePlayPause = onTogglePlayPause,
                    onSkipNext = onSkipNext,
                    onSkipPrevious = onSkipPrevious,
                    onSeekMediaTo = onSeekMediaTo,
                    onSetMediaVolume = onSetMediaVolume,
                    onAdjustMediaVolume = onAdjustMediaVolume,
                    onToggleMediaMute = onToggleMediaMute,
                    onOpenNotification = onOpenNotification,
                    onDismissNotification = onDismissNotification,
                    onClearAllNotifications = onClearAllNotifications,
                    onRequestNotificationAccess = onRequestNotificationAccess,
                    onOpenCalendarEvent = onOpenCalendarEvent,
                    onRefreshWeather = onRefreshWeather,
                    onLaunchQuickApp = onLaunchQuickApp,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                )
            }
        }
    }

    if (showSettings) {
        SettingsBottomSheet(
            settings = uiState.settings,
            installedApps = uiState.installedApps,
            isNotificationAccessGranted = uiState.isNotificationAccessGranted,
            isDndAccessGranted = uiState.isDndAccessGranted,
            isCallControlGranted = uiState.isCallControlGranted,
            isCalendarGranted = uiState.isCalendarGranted,
            isCallLogGranted = uiState.isCallLogGranted,
            isOverlayGranted = isOverlayGranted,
            onUpdateSettings = onUpdateSettings,
            onToggleWidgetVisibility = onToggleWidgetVisibility,
            onToggleBlacklistPackage = onToggleBlacklistPackage,
            onToggleQuickAppPackage = onToggleQuickAppPackage,
            onRequestNotificationAccess = onRequestNotificationAccess,
            onRequestDndAccess = onRequestDndAccess,
            onRequestOverlayAccess = onRequestOverlayAccess,
            onRequestRuntimePermissions = onRequestRuntimePermissions,
            onResetAutoRotate = onResetAutoRotate,
            onResetWidgetOrder = onResetWidgetOrder,
            onExportBackup = onExportBackup,
            onImportBackup = onImportBackup,
            onOpenUrl = onOpenUrl,
            onDismiss = { showSettings = false }
        )
    }
}

@Composable
private fun WidgetColumn(
    widgets: List<DashboardWidgetType>,
    fullOrder: List<DashboardWidgetType>,
    isLandscape: Boolean,
    isLeftColumn: Boolean,
    uiState: DashboardUiState,
    onReorderWidgets: (Int, Int) -> Unit,
    onMoveLandscapeColumn: ((DashboardWidgetType, Boolean) -> Unit)?,
    onDragFinished: () -> Unit,
    onCycleTimerPreset: () -> Unit,
    onToggleTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onCallRecentNumber: (String) -> Unit,
    onRequestRuntimePermissions: () -> Unit,
    onAnswerIncomingCall: (IncomingCallAlert) -> Unit,
    onDeclineIncomingCall: (IncomingCallAlert) -> Unit,
    onMuteRinger: () -> Unit,
    onEndOngoingCall: (OngoingCallState) -> Unit,
    onToggleSpeakerphone: () -> Unit,
    onToggleMicMute: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekMediaTo: (Long) -> Unit,
    onSetMediaVolume: (Int) -> Unit,
    onAdjustMediaVolume: (Int) -> Unit,
    onToggleMediaMute: () -> Unit,
    onOpenNotification: (PendingIntent?) -> Unit,
    onDismissNotification: (String) -> Unit,
    onClearAllNotifications: () -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onOpenCalendarEvent: (Long) -> Unit,
    onRefreshWeather: (String) -> Unit,
    onLaunchQuickApp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val spacingPx = with(LocalDensity.current) { 12.dp.toPx() }
    var draggingType by remember { mutableStateOf<DashboardWidgetType?>(null) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val itemHeights = remember { mutableStateMapOf<DashboardWidgetType, Int>() }
    val itemWidths = remember { mutableStateMapOf<DashboardWidgetType, Int>() }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        widgets.forEachIndexed { localIdx, widgetType ->
            key(widgetType) {
                val isDragging = draggingType == widgetType
                val currentWidgets by rememberUpdatedState(widgets)
                val currentFullOrder by rememberUpdatedState(fullOrder)
                val currentLocalIdx by rememberUpdatedState(localIdx)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (isDragging) 2f else 0f)
                        .onSizeChanged {
                            itemHeights[widgetType] = it.height
                            itemWidths[widgetType] = it.width
                        }
                        .graphicsLayer {
                            translationX = if (isDragging) dragOffsetX else 0f
                            translationY = if (isDragging) dragOffsetY else 0f
                            scaleX = if (isDragging) 1.02f else 1f
                            scaleY = if (isDragging) 1.02f else 1f
                        }
                        .border(
                            width = if (isDragging) 1.5.dp else 0.dp,
                            color = if (isDragging) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .pointerInput(widgetType, isLandscape, isLeftColumn) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingType = widgetType
                                    dragOffsetX = 0f
                                    dragOffsetY = 0f
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragEnd = {
                                    draggingType = null
                                    dragOffsetX = 0f
                                    dragOffsetY = 0f
                                    onDragFinished()
                                },
                                onDragCancel = {
                                    draggingType = null
                                    dragOffsetX = 0f
                                    dragOffsetY = 0f
                                    onDragFinished()
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetX += dragAmount.x
                                    dragOffsetY += dragAmount.y

                                    // Cross-column drag in Landscape
                                    if (isLandscape && onMoveLandscapeColumn != null) {
                                        val colWidth = (itemWidths[widgetType] ?: 300).coerceAtLeast(150)
                                        if (isLeftColumn && dragOffsetX > colWidth * 0.45f) {
                                            onMoveLandscapeColumn(widgetType, false)
                                            draggingType = null
                                            dragOffsetX = 0f
                                            dragOffsetY = 0f
                                            return@detectDragGesturesAfterLongPress
                                        } else if (!isLeftColumn && dragOffsetX < -colWidth * 0.45f) {
                                            onMoveLandscapeColumn(widgetType, true)
                                            draggingType = null
                                            dragOffsetX = 0f
                                            dragOffsetY = 0f
                                            return@detectDragGesturesAfterLongPress
                                        }
                                    }

                                    // Vertical reorder within column using live updated state
                                    val liveWidgets = currentWidgets
                                    val liveOrder = currentFullOrder
                                    val idx = currentLocalIdx
                                    if (dragOffsetY > 0f && idx < liveWidgets.lastIndex) {
                                        val nextWidget = liveWidgets[idx + 1]
                                        val nextHeight = (itemHeights[nextWidget] ?: itemHeights[widgetType] ?: 120).coerceAtLeast(60)
                                        val step = nextHeight + spacingPx
                                        if (dragOffsetY > step * 0.5f) {
                                            val fromGlobal = liveOrder.indexOf(widgetType)
                                            val targetGlobal = liveOrder.indexOf(nextWidget)
                                            if (fromGlobal >= 0 && targetGlobal >= 0) {
                                                onReorderWidgets(fromGlobal, targetGlobal)
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                            dragOffsetY -= step
                                        }
                                    } else if (dragOffsetY < 0f && idx > 0) {
                                        val prevWidget = liveWidgets[idx - 1]
                                        val prevHeight = (itemHeights[prevWidget] ?: itemHeights[widgetType] ?: 120).coerceAtLeast(60)
                                        val step = prevHeight + spacingPx
                                        if (dragOffsetY < -step * 0.5f) {
                                            val fromGlobal = liveOrder.indexOf(widgetType)
                                            val targetGlobal = liveOrder.indexOf(prevWidget)
                                            if (fromGlobal >= 0 && targetGlobal >= 0) {
                                                onReorderWidgets(fromGlobal, targetGlobal)
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                            dragOffsetY += step
                                        }
                                    }
                                }
                            )
                        }
                ) {
                    when (widgetType) {
                        DashboardWidgetType.CLOCK -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                uiState.incomingCall?.let { call ->
                                    IncomingCallBanner(
                                        call = call,
                                        onAnswerCall = onAnswerIncomingCall,
                                        onDeclineCall = onDeclineIncomingCall,
                                        onMuteRinger = onMuteRinger
                                    )
                                }
                                uiState.ongoingCall?.let { call ->
                                    OngoingCallBanner(
                                        call = call,
                                        durationSeconds = uiState.liveCallDurationSeconds,
                                        isSpeakerphoneOn = uiState.deviceStatus.isSpeakerphoneOn,
                                        isMicMuted = uiState.deviceStatus.isMicMuted,
                                        onToggleSpeakerphone = onToggleSpeakerphone,
                                        onToggleMicMute = onToggleMicMute,
                                        onEndCall = onEndOngoingCall
                                    )
                                }
                                ClockSection(
                                    currentTime = uiState.currentTime,
                                    use24HourFormat = uiState.settings.use24HourFormat,
                                    showSeconds = uiState.settings.showSeconds,
                                    showDate = uiState.settings.showDate,
                                    clockStyle = uiState.settings.clockStyle,
                                    clockScale = uiState.settings.clockScale,
                                    secondaryTimeZoneId = uiState.settings.secondaryTimeZoneId,
                                    timerState = uiState.timer,
                                    weather = uiState.weather,
                                    showWeatherInClock = uiState.settings.showWeatherInClock,
                                    calendarEvent = uiState.nextCalendarEvent,
                                    isCalendarGranted = uiState.isCalendarGranted,
                                    showCalendarInClock = uiState.settings.showCalendarInClock,
                                    onRefreshWeather = { onRefreshWeather(uiState.settings.weatherCity) },
                                    onOpenCalendarEvent = onOpenCalendarEvent,
                                    onRequestCalendarPermission = onRequestRuntimePermissions,
                                    onToggleTimer = onToggleTimer,
                                    onResetTimer = onResetTimer,
                                    onCycleTimerPreset = onCycleTimerPreset,
                                    isLandscape = isLandscape
                                )
                            }
                        }

                        DashboardWidgetType.RECENT_CALLS -> {
                            RecentCallsWidgetCard(
                                recentCalls = uiState.recentCalls,
                                isCallLogGranted = uiState.isCallLogGranted,
                                onDoubleTapCall = onCallRecentNumber,
                                onRequestPermissions = onRequestRuntimePermissions
                            )
                        }

                        DashboardWidgetType.MUSIC_CONTROLS -> {
                            MusicControlCard(
                                media = uiState.media,
                                livePositionMs = uiState.liveMediaPositionMs,
                                mediaVolume = uiState.deviceStatus.mediaVolume,
                                mediaMaxVolume = uiState.deviceStatus.mediaMaxVolume,
                                onPrevious = onSkipPrevious,
                                onPlayPause = onTogglePlayPause,
                                onNext = onSkipNext,
                                onSeekTo = onSeekMediaTo,
                                onSetMediaVolume = onSetMediaVolume,
                                onAdjustMediaVolume = onAdjustMediaVolume,
                                onToggleMediaMute = onToggleMediaMute
                            )
                        }

                        DashboardWidgetType.NOTIFICATIONS -> {
                            NotificationPillList(
                                notifications = uiState.notifications,
                                isNotificationAccessGranted = uiState.isNotificationAccessGranted,
                                isDndActive = uiState.deviceStatus.isDndActive,
                                onOpenNotification = onOpenNotification,
                                onDismissNotification = onDismissNotification,
                                onClearAllNotifications = onClearAllNotifications,
                                onRequestNotificationAccess = onRequestNotificationAccess
                            )
                        }

                        DashboardWidgetType.QUICK_APPS -> {
                            QuickAppDockCard(
                                selectedPackages = uiState.settings.quickAppPackages,
                                installedApps = uiState.installedApps,
                                onLaunchApp = onLaunchQuickApp
                            )
                        }
                    }
                }
            }
        }
    }
}
