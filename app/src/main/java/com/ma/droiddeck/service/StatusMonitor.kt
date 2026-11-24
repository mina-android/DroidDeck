package com.ma.droiddeck.service

import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.BatteryManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.ma.droiddeck.data.model.RingerModeState
import com.ma.droiddeck.data.repository.DroidDeckDashboardRepository

/**
 * Monitors battery status (level, charging, temperature), network connectivity
 * (Wi-Fi, Cellular, Bluetooth), Do Not Disturb filter state, media volume,
 * ringer mode (Sound / Vibrate / Mute), and the proximity sensor for OLED blanking.
 */
class StatusMonitor(private val context: Context) {

    private val repository = DroidDeckDashboardRepository.getInstance()
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val proximitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    private var isStarted = false

    private val commDeviceListener: Any? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        AudioManager.OnCommunicationDeviceChangedListener { device ->
            val isSpeaker = device?.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            repository.updateDeviceStatus { it.copy(isSpeakerphoneOn = isSpeaker) }
        }
    } else {
        null
    }

    private val systemReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_BATTERY_CHANGED -> parseBatteryIntent(intent)
                NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED -> refreshDndState()
                AudioManager.RINGER_MODE_CHANGED_ACTION -> refreshRingerState()
                ACTION_VOLUME_CHANGED -> {
                    refreshAudioVolume()
                    refreshRingerState()
                }
                BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED,
                BluetoothAdapter.ACTION_STATE_CHANGED -> refreshBluetoothState()
            }
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = refreshConnectivity()
        override fun onLost(network: Network) = refreshConnectivity()
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) =
            refreshConnectivity()
    }

    private val proximityListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            val sensor = proximitySensor ?: return
            val distance = event?.values?.firstOrNull() ?: return
            val isCovered = distance < sensor.maximumRange
            repository.updateProximityCovered(isCovered)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    fun start() {
        if (isStarted) return
        isStarted = true

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction(ACTION_VOLUME_CHANGED)
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        val stickyBattery = ContextCompat.registerReceiver(
            context,
            systemReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        stickyBattery?.let(::parseBatteryIntent)

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        runCatching { connectivityManager.registerNetworkCallback(request, networkCallback) }

        proximitySensor?.let {
            sensorManager.registerListener(
                proximityListener,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        refreshConnectivity()
        refreshBluetoothState()
        refreshDndState()
        refreshRingerState()
        refreshAudioVolume()
        refreshSpeakerphoneState()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (commDeviceListener as? AudioManager.OnCommunicationDeviceChangedListener)?.let { listener ->
                runCatching {
                    audioManager.addOnCommunicationDeviceChangedListener(context.mainExecutor, listener)
                }
            }
        }
    }

    fun stop() {
        if (!isStarted) return
        isStarted = false

        runCatching { context.unregisterReceiver(systemReceiver) }
        runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
        runCatching { sensorManager.unregisterListener(proximityListener) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (commDeviceListener as? AudioManager.OnCommunicationDeviceChangedListener)?.let { listener ->
                runCatching { audioManager.removeOnCommunicationDeviceChangedListener(listener) }
            }
        }
        repository.updateProximityCovered(false)
    }

    @Suppress("DEPRECATION")
    fun refreshSpeakerphoneState() {
        val isSpeaker = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.communicationDevice?.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        } else {
            audioManager.isSpeakerphoneOn
        }
        repository.updateDeviceStatus { it.copy(isSpeakerphoneOn = isSpeaker) }
    }

    fun refreshDndState() {
        val filter = notificationManager.currentInterruptionFilter
        val dndActive = filter != NotificationManager.INTERRUPTION_FILTER_ALL
        repository.updateDeviceStatus { it.copy(isDndActive = dndActive) }
    }

    fun refreshRingerState() {
        val isRingMuted = runCatching {
            audioManager.isStreamMute(AudioManager.STREAM_RING) ||
                audioManager.getStreamVolume(AudioManager.STREAM_RING) == 0
        }.getOrDefault(false)
        val mode = when {
            audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT -> RingerModeState.MUTE
            audioManager.ringerMode == AudioManager.RINGER_MODE_VIBRATE -> RingerModeState.VIBRATE
            isRingMuted -> RingerModeState.MUTE
            else -> RingerModeState.SOUND
        }
        repository.updateDeviceStatus { it.copy(ringerMode = mode) }
    }

    fun refreshAudioVolume() {
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        repository.updateDeviceStatus {
            it.copy(mediaVolume = current, mediaMaxVolume = max)
        }
    }

    private fun parseBatteryIntent(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val pct = if (level >= 0) (level * 100) / scale else 100
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 250)

        repository.updateDeviceStatus {
            it.copy(
                batteryLevel = pct.coerceIn(0, 100),
                isCharging = isCharging,
                batteryTempCelsius = tempTenths / 10f
            )
        }
    }

    private fun refreshConnectivity() {
        val activeNetwork = connectivityManager.activeNetwork
        val caps = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
        val wifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val cellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true

        repository.updateDeviceStatus {
            it.copy(
                isWifiConnected = wifi,
                isCellularConnected = cellular
            )
        }
    }

    private fun refreshBluetoothState() {
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val enabled = runCatching { btManager?.adapter?.isEnabled == true }.getOrDefault(false)
        repository.updateDeviceStatus { it.copy(isBluetoothConnected = enabled) }
    }

    companion object {
        private const val ACTION_VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION"
    }
}
