package com.ma.droiddeck

import com.ma.droiddeck.util.MediaProgressCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaProgressCalculatorTest {

    @Test
    fun `paused playback keeps base position without extrapolating`() {
        val pos = MediaProgressCalculator.computeCurrentPositionMs(
            basePositionMs = 45_000L,
            durationMs = 180_000L,
            lastUpdateElapsedMs = 10_000L,
            nowElapsedMs = 25_000L,
            playbackSpeed = 1.0f,
            isPlaying = false
        )
        assertEquals(45_000L, pos)
    }

    @Test
    fun `playing media extrapolates elapsed time and clamps to duration`() {
        val pos = MediaProgressCalculator.computeCurrentPositionMs(
            basePositionMs = 30_000L,
            durationMs = 120_000L,
            lastUpdateElapsedMs = 1_000L,
            nowElapsedMs = 11_000L,
            playbackSpeed = 1.5f,
            isPlaying = true
        )
        assertEquals(45_000L, pos)

        val clamped = MediaProgressCalculator.computeCurrentPositionMs(
            basePositionMs = 115_000L,
            durationMs = 120_000L,
            lastUpdateElapsedMs = 1_000L,
            nowElapsedMs = 20_000L,
            playbackSpeed = 1.0f,
            isPlaying = true
        )
        assertEquals(120_000L, clamped)
    }
}
