package com.ma.droiddeck

import android.Manifest
import android.app.KeyguardManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ma.droiddeck.data.model.OrientationMode
import com.ma.droiddeck.data.repository.DroidDeckDashboardRepository
import com.ma.droiddeck.service.StatusMonitor
import com.ma.droiddeck.service.WorkingHoursLockService
import com.ma.droiddeck.ui.dashboard.DashboardViewModel
import com.ma.droiddeck.ui.dashboard.DroidDeckScreen
import com.ma.droiddeck.ui.theme.DroidDeckTheme
import com.ma.droiddeck.util.CallActionHelper
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

/**
 * Main host activity for DroidDeck.
 * Configures lockscreen presentation, KeepScreenOn, immersive pure-black system bars,
 * foreground heads-up notification suppression, orientation switching, and JSON backup launchers.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var statusMonitor: StatusMonitor

    private var savedNotificationPolicy: NotificationManager.Policy? = null
    private var savedInterruptionFilter: Int? = null
    private var isHeadsUpSuppressedByApp = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        applyLockScreenMode(showWhenLocked = true)
        configureImmersiveBars()

        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, insets ->
            if (insets.isVisible(WindowInsetsCompat.Type.statusBars()) && hasWindowFocus()) {
                view.post { configureImmersiveBars() }
            }
            insets
        }

        statusMonitor = StatusMonitor(applicationContext)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val scope = rememberCoroutineScope()

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) {
                viewModel.refreshPermissions()
            }

            val exportLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("application/json")
            ) { uri ->
                if (uri != null) {
                    scope.launch {
                        val json = viewModel.exportSettingsJson()
                        val ok = runCatching {
                            contentResolver.openOutputStream(uri)?.use { out ->
                                out.write(json.toByteArray(Charsets.UTF_8))
                            }
                            true
                        }.getOrDefault(false)
                        Toast.makeText(
                            this@MainActivity,
                            if (ok) "Backup exported" else "Export failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            val importLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    scope.launch {
                        val json = runCatching {
                            contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        }.getOrNull()
                        val ok = if (json != null) viewModel.importSettingsJson(json) else false
                        Toast.makeText(
                            this@MainActivity,
                            if (ok) "Backup restored" else "Invalid backup file",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            LaunchedEffect(uiState.settings.showOnLockScreen) {
                applyLockScreenMode(uiState.settings.showOnLockScreen)
            }

            LaunchedEffect(uiState.settings.orientationMode) {
                requestedOrientation = when (uiState.settings.orientationMode) {
                    OrientationMode.AUTO -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    OrientationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    OrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                }
            }

            LaunchedEffect(uiState.ongoingCall != null, uiState.incomingCall != null) {
                configureImmersiveBars()
            }

            LaunchedEffect(uiState.incomingCall?.key, uiState.settings.suppressHeadsUpWhileActive) {
                if (uiState.incomingCall != null && uiState.settings.suppressHeadsUpWhileActive) {
                    CallActionHelper.bringAppToFrontSequence(this@MainActivity)
                }
            }

            LaunchedEffect(uiState.settings.suppressHeadsUpWhileActive, uiState.isDndAccessGranted) {
                if (uiState.settings.suppressHeadsUpWhileActive && uiState.isDndAccessGranted) {
                    applyHeadsUpSuppression()
                } else {
                    restoreHeadsUpSuppression()
                }
            }

            LaunchedEffect(
                uiState.settings.autoDimEnabled,
                uiState.settings.dimBrightnessPercent,
                uiState.isTemporarilyBrightened,
                uiState.isProximityCovered,
                uiState.settings.proximityBlankingEnabled
            ) {
                val lp = window.attributes
                lp.screenBrightness = when {
                    uiState.settings.proximityBlankingEnabled && uiState.isProximityCovered -> 0.0f
                    uiState.isTemporarilyBrightened -> 0.35f
                    uiState.settings.autoDimEnabled ->
                        (uiState.settings.dimBrightnessPercent / 100f).coerceIn(0.01f, 1.0f)
                    else -> WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
                window.attributes = lp
            }

            DroidDeckTheme(
                accentTheme = uiState.settings.accentTheme,
                appFont = uiState.settings.appFont
            ) {
                DroidDeckScreen(
                    uiState = uiState,
                    onTapBackground = { viewModel.triggerTemporaryBrighten() },
                    onUserInteraction = { viewModel.onUserInteraction() },
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onSkipNext = { viewModel.skipToNext() },
                    onSkipPrevious = { viewModel.skipToPrevious() },
                    onSeekMediaTo = { posMs -> viewModel.seekMediaTo(posMs) },
                    onSetMediaVolume = { vol -> viewModel.setMediaVolume(vol) },
                    onAdjustMediaVolume = { delta -> viewModel.adjustMediaVolume(delta) },
                    onToggleMediaMute = { viewModel.toggleMediaMute() },
                    onAnswerIncomingCall = { call ->
                        CallActionHelper.answerCall(this@MainActivity, call.acceptIntent, call.contentIntent)
                    },
                    onDeclineIncomingCall = { call ->
                        CallActionHelper.declineOrEndCall(this@MainActivity, call.declineIntent)
                    },
                    onMuteRinger = { CallActionHelper.silenceRinger(this@MainActivity) },
                    onEndOngoingCall = { call ->
                        CallActionHelper.declineOrEndCall(this@MainActivity, call.hangUpIntent)
                    },
                    onToggleSpeakerphone = {
                        if (!viewModel.toggleSpeakerphone()) {
                            Toast.makeText(
                                this@MainActivity,
                                "Speaker routing is controlled by the Phone app on this device",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onToggleMicMute = { viewModel.toggleMicMute() },
                    onCallRecentNumber = { number ->
                        CallActionHelper.placeCallOrDial(this@MainActivity, number)
                    },
                    onOpenNotification = { pendingIntent -> openNotificationSafely(pendingIntent) },
                    onDismissNotification = { key -> viewModel.dismissNotification(key) },
                    onClearAllNotifications = { viewModel.clearAllNotifications() },
                    onOpenCalendarEvent = { eventId -> openCalendarEventSafely(eventId) },
                    onRefreshWeather = { city -> viewModel.refreshWeather(city, forceRefresh = true) },
                    onLaunchQuickApp = { pkg -> launchPackageSafely(pkg) },
                    onToggleDnd = {
                        if (!viewModel.toggleDnd()) {
                            Toast.makeText(
                                this@MainActivity,
                                "Grant Do Not Disturb access in Settings first.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onCycleRingerMode = {
                        val ok = viewModel.cycleRingerMode()
                        if (!ok) {
                            Toast.makeText(
                                this@MainActivity,
                                "Grant Do Not Disturb access to use Mute mode.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onToggleRotate = { toggleScreenOrientation() },
                    onResetAutoRotate = {
                        viewModel.updateSettings { it.copy(orientationMode = OrientationMode.AUTO) }
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        Toast.makeText(this@MainActivity, "Orientation set to Auto-Rotate", Toast.LENGTH_SHORT).show()
                    },
                    onExitApp = { exitAppCompletely() },
                    onCommitWidgetLayout = { order, left -> viewModel.commitWidgetLayout(order, left) },
                    onReorderWidgets = { from, to -> viewModel.reorderWidgets(from, to) },
                    onMoveWidgetLandscapeColumn = { widget, toLeft ->
                        viewModel.moveWidgetLandscapeColumn(widget, toLeft)
                    },
                    onToggleWidgetVisibility = { widget -> viewModel.toggleWidgetVisibility(widget) },
                    onToggleBlacklistPackage = { pkg -> viewModel.toggleBlacklistPackage(pkg) },
                    onToggleQuickAppPackage = { pkg -> viewModel.toggleQuickAppPackage(pkg) },
                    onResetWidgetOrder = { viewModel.resetWidgetOrder() },
                    onToggleTimer = { viewModel.toggleTimerRunning() },
                    onResetTimer = { viewModel.resetTimer() },
                    onSetTimerPreset = { mins, label -> viewModel.setTimerPreset(mins, label) },
                    onUpdateSettings = { transform -> viewModel.updateSettings(transform) },
                    onRequestNotificationAccess = {
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                    onRequestDndAccess = {
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                    },
                    isOverlayGranted = Settings.canDrawOverlays(this@MainActivity),
                    onRequestOverlayAccess = {
                        runCatching {
                            startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:$packageName")
                                )
                            )
                        }
                    },
                    onRequestRuntimePermissions = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ANSWER_PHONE_CALLS,
                                Manifest.permission.READ_CALL_LOG,
                                Manifest.permission.CALL_PHONE,
                                Manifest.permission.READ_CALENDAR
                            )
                        )
                    },
                    onExportBackup = { exportLauncher.launch("droiddeck_backup.json") },
                    onImportBackup = { importLauncher.launch(arrayOf("application/json")) },
                    onOpenUrl = { url ->
                        runCatching {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        DroidDeckDashboardRepository.getInstance().isDashboardSessionActive = true
        statusMonitor.start()
        viewModel.refreshCalendarAndRecentCalls()
    }

    override fun onResume() {
        super.onResume()
        DroidDeckDashboardRepository.getInstance().isDashboardForeground = true
        viewModel.refreshPermissions()
        statusMonitor.refreshDndState()
        statusMonitor.refreshRingerState()
        statusMonitor.refreshAudioVolume()
        statusMonitor.refreshSpeakerphoneState()
        configureImmersiveBars()
    }

    override fun onPause() {
        DroidDeckDashboardRepository.getInstance().isDashboardForeground = false
        super.onPause()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        DroidDeckDashboardRepository.getInstance().isDashboardSessionActive = false
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            configureImmersiveBars()
        }
    }

    override fun onStop() {
        restoreHeadsUpSuppression()
        statusMonitor.stop()
        super.onStop()
    }

    private fun exitAppCompletely() {
        val repo = DroidDeckDashboardRepository.getInstance()
        repo.isDashboardSessionActive = false
        repo.isDashboardForeground = false
        restoreHeadsUpSuppression()
        statusMonitor.stop()
        WorkingHoursLockService.stop(applicationContext)
        finishAndRemoveTask()
        android.os.Process.killProcess(android.os.Process.myPid())
        exitProcess(0)
    }

    private fun applyHeadsUpSuppression() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (!nm.isNotificationPolicyAccessGranted) return
        if (!isHeadsUpSuppressedByApp) {
            savedNotificationPolicy = nm.notificationPolicy
            savedInterruptionFilter = nm.currentInterruptionFilter
        }
        runCatching {
            val base = nm.notificationPolicy
            val suppressedEffects = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                base.suppressedVisualEffects or
                    NotificationManager.Policy.SUPPRESSED_EFFECT_PEEK or
                    NotificationManager.Policy.SUPPRESSED_EFFECT_FULL_SCREEN_INTENT
            } else {
                base.suppressedVisualEffects
            }
            val mediaCategory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                NotificationManager.Policy.PRIORITY_CATEGORY_MEDIA or
                    NotificationManager.Policy.PRIORITY_CATEGORY_SYSTEM
            } else {
                0
            }
            val newPolicy = NotificationManager.Policy(
                base.priorityCategories or
                    NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS or
                    mediaCategory,
                base.priorityCallSenders,
                base.priorityMessageSenders,
                suppressedEffects
            )
            nm.notificationPolicy = newPolicy
            // Never auto-enable DND (INTERRUPTION_FILTER_PRIORITY) on launch so DND and media stay untouched.
            isHeadsUpSuppressedByApp = true
        }
    }

    private fun restoreHeadsUpSuppression() {
        if (!isHeadsUpSuppressedByApp) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (!nm.isNotificationPolicyAccessGranted) {
            isHeadsUpSuppressedByApp = false
            return
        }
        runCatching {
            savedNotificationPolicy?.let { nm.notificationPolicy = it }
        }
        isHeadsUpSuppressedByApp = false
    }

    private fun applyLockScreenMode(showWhenLocked: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(showWhenLocked)
            setTurnScreenOn(showWhenLocked)
        } else {
            if (showWhenLocked) {
                @Suppress("DEPRECATION")
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
            } else {
                @Suppress("DEPRECATION")
                window.clearFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
            }
        }
    }

    private fun toggleScreenOrientation() {
        val isCurrentlyLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val nextMode = if (isCurrentlyLandscape) {
            OrientationMode.PORTRAIT
        } else {
            OrientationMode.LANDSCAPE
        }
        viewModel.updateSettings { it.copy(orientationMode = nextMode) }
        requestedOrientation = if (nextMode == OrientationMode.PORTRAIT) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    @Suppress("DEPRECATION")
    private fun configureImmersiveBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val lp = window.attributes
            lp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            window.attributes = lp
        }
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
    }

    private fun launchWithKeyguardCheck(action: () -> Unit) {
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (keyguardManager?.isKeyguardLocked == true) {
            keyguardManager.requestDismissKeyguard(
                this,
                object : KeyguardManager.KeyguardDismissCallback() {
                    override fun onDismissSucceeded() {
                        action()
                    }
                }
            )
        } else {
            action()
        }
    }

    private fun openNotificationSafely(pendingIntent: PendingIntent?) {
        if (pendingIntent == null) return
        launchWithKeyguardCheck {
            CallActionHelper.sendPendingIntentAllowBg(pendingIntent)
        }
    }

    private fun openCalendarEventSafely(eventId: Long) {
        launchWithKeyguardCheck {
            runCatching {
                val uri = Uri.withAppendedPath(CalendarContract.Events.CONTENT_URI, eventId.toString())
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
        }
    }

    private fun launchPackageSafely(packageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        launchWithKeyguardCheck {
            runCatching { startActivity(launchIntent) }
        }
    }
}
