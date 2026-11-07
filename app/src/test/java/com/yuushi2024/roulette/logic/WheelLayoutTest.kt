package com.yuushi2024.roulette.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WheelLayoutTest {

    /** The wheel keeps its full height when nothing is open. */
    @Test
    fun `the wheel uses its normal height without the keyboard`() {
        assertEquals(0.40f, WheelLayout.wheelHeightPercent(imeVisible = false), 0.0001f)
    }

    /** The wheel gives up height to the keyboard. */
    @Test
    fun `the wheel shrinks while the keyboard is open`() {
        assertEquals(0.16f, WheelLayout.wheelHeightPercent(imeVisible = true), 0.0001f)
    }

    /** The shrink must actually free space. */
    @Test
    fun `the keyboard height leaves most of the screen to the input box`() {
        assertTrue(
            WheelLayout.wheelHeightPercent(imeVisible = true) <
                WheelLayout.wheelHeightPercent(imeVisible = false),
        )
    }
}
