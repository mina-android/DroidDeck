package com.ma.droiddeck.util

/**
 * Pure JVM helper for extrapolating live media playback position from
 * Android MediaSession's lastPositionUpdateTime and playbackSpeed.
 */
object MediaProgressCalculator {

    /**
     * Returns the current playback position in milliseconds bounded to [0, durationMs].
     */
    fun computeCurrentPositionMs(
        basePositionMs: Long,
        durationMs: Long,
        lastUpdateElapsedMs: Long,
        nowElapsedMs: Long,
        playbackSpeed: Float,
        isPlaying: Boolean
    ): Long {
        val clampedBase = basePositionMs.coerceAtLeast(0L)
        val rawPos = if (isPlaying && lastUpdateElapsedMs > 0L) {
            val elapsed = (nowElapsedMs - lastUpdateElapsedMs).coerceAtLeast(0L)
            clampedBase + (elapsed * playbackSpeed.coerceAtLeast(0f)).toLong()
        } else {
            clampedBase
        }
        return if (durationMs > 0L) {
            rawPos.coerceIn(0L, durationMs)
        } else {
            rawPos.coerceAtLeast(0L)
        }
    }
}
