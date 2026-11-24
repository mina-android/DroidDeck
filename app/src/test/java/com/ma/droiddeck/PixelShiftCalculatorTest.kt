package com.ma.droiddeck

import com.ma.droiddeck.util.PixelShiftCalculator
import org.junit.Assert.assertTrue
import org.junit.Test

class PixelShiftCalculatorTest {

    @Test
    fun `pixel shift offsets stay within bounded OLED safe range`() {
        for (minute in -32L..64L) {
            val (x, y) = PixelShiftCalculator.offsetForMinute(minute)
            assertTrue("X offset out of bounds: $x", x in -PixelShiftCalculator.MAX_SHIFT_DP..PixelShiftCalculator.MAX_SHIFT_DP)
            assertTrue("Y offset out of bounds: $y", y in -PixelShiftCalculator.MAX_SHIFT_DP..PixelShiftCalculator.MAX_SHIFT_DP)
        }
    }
}
