package com.ma.droiddeck

import android.media.AudioManager
import com.ma.droiddeck.data.model.RingerModeState
import com.ma.droiddeck.service.StatusMonitor
import org.junit.Assert.assertEquals
import org.junit.Test

class StatusMonitorRingerTest {

    @Test
    fun dndActive_ignoresStreamRingMuteAndKeepsSound() {
        val result = StatusMonitor.resolveRingerMode(
            systemRingerMode = AudioManager.RINGER_MODE_NORMAL,
            isStreamRingMuted = true,
            isDndActive = true
        )
        assertEquals(RingerModeState.SOUND, result)
    }

    @Test
    fun dndInactive_treatsStreamRingMuteAsMute() {
        val result = StatusMonitor.resolveRingerMode(
            systemRingerMode = AudioManager.RINGER_MODE_NORMAL,
            isStreamRingMuted = true,
            isDndActive = false
        )
        assertEquals(RingerModeState.MUTE, result)
    }

    @Test
    fun dndActive_respectsExplicitVibrateAndSilentModes() {
        assertEquals(
            RingerModeState.VIBRATE,
            StatusMonitor.resolveRingerMode(
                systemRingerMode = AudioManager.RINGER_MODE_VIBRATE,
                isStreamRingMuted = true,
                isDndActive = true
            )
        )
        assertEquals(
            RingerModeState.MUTE,
            StatusMonitor.resolveRingerMode(
                systemRingerMode = AudioManager.RINGER_MODE_SILENT,
                isStreamRingMuted = true,
                isDndActive = true
            )
        )
    }
}
