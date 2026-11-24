package com.ma.droiddeck.util

/**
 * Deterministic OLED burn-in protection calculator.
 * Cycles through bounded X/Y dp offsets within [-MAX_SHIFT_DP, +MAX_SHIFT_DP]
 * based on the current epoch minute.
 */
object PixelShiftCalculator {
    const val MAX_SHIFT_DP = 4

    /**
     * Computes a subtle (xDp, yDp) offset for the given [epochMinute].
     */
    fun offsetForMinute(epochMinute: Long): Pair<Int, Int> {
        val phase = ((epochMinute % 16L) + 16L) % 16L
        val x = when (phase.toInt()) {
            0, 1, 2 -> 0
            3, 4, 5 -> 2
            6, 7 -> 4
            8, 9, 10 -> 0
            11, 12, 13 -> -2
            else -> -4
        }
        val y = when ((phase.toInt() + 4) % 16) {
            0, 1, 2 -> 0
            3, 4, 5 -> 2
            6, 7 -> 4
            8, 9, 10 -> 0
            11, 12, 13 -> -2
            else -> -4
        }
        return x.coerceIn(-MAX_SHIFT_DP, MAX_SHIFT_DP) to y.coerceIn(-MAX_SHIFT_DP, MAX_SHIFT_DP)
    }
}
