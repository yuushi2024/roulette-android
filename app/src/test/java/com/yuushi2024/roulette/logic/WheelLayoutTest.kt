package com.yuushi2024.roulette.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WheelLayoutTest {

    /** The wheel area is hidden so the input box can use the whole screen. */
    @Test
    fun `the wheel area is hidden while the keyboard is open`() {
        assertFalse(WheelLayout.showWheelArea(imeVisible = true))
    }

    /** Nothing is hidden when the keyboard is closed. */
    @Test
    fun `the wheel area is shown when the keyboard is closed`() {
        assertTrue(WheelLayout.showWheelArea(imeVisible = false))
    }

    /** The keyboard height becomes the bottom padding. */
    @Test
    fun `the keyboard height is used as the bottom padding`() {
        assertEquals(720, WheelLayout.bottomInset(imeVisible = true, imeBottom = 720, systemBarBottom = 48))
    }

    /** Without the keyboard only the navigation bar is padded. */
    @Test
    fun `the navigation bar height is used when the keyboard is closed`() {
        assertEquals(48, WheelLayout.bottomInset(imeVisible = false, imeBottom = 720, systemBarBottom = 48))
    }

    /** A negative inset (some IMEs report one transiently) must not shift content. */
    @Test
    fun `a negative inset is clamped to zero`() {
        assertEquals(0, WheelLayout.bottomInset(imeVisible = true, imeBottom = -1, systemBarBottom = -1))
    }

    /** A keyboard taller than the navigation bar means it is open. */
    @Test
    fun `a taller bottom inset means the keyboard is open`() {
        assertTrue(WheelLayout.imeVisibleFromInsets(imeBottom = 720, systemBarBottom = 48))
    }

    /** Without a keyboard the bottom inset is just the navigation bar. */
    @Test
    fun `a bottom inset equal to the navigation bar means no keyboard`() {
        assertFalse(WheelLayout.imeVisibleFromInsets(imeBottom = 48, systemBarBottom = 48))
    }

    /** The default wheel fraction stays in a sane range. */
    @Test
    fun `the normal wheel fraction is positive and below one`() {
        assertTrue(WheelLayout.NORMAL_HEIGHT_PERCENT > 0f)
        assertTrue(WheelLayout.NORMAL_HEIGHT_PERCENT < 1f)
    }
}
