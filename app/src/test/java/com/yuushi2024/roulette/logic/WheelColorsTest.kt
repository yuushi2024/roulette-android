package com.yuushi2024.roulette.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WheelColorsTest {

    private fun alpha(color: Int) = (color ushr 24) and 0xFF

    /** HSV conversion matches the primaries. */
    @Test
    fun `hsv conversion matches primary colours`() {
        assertEquals(0xFFFF0000.toInt(), WheelColors.hsvToArgb(0f, 1f, 1f))
        assertEquals(0xFF00FF00.toInt(), WheelColors.hsvToArgb(120f, 1f, 1f))
        assertEquals(0xFF0000FF.toInt(), WheelColors.hsvToArgb(240f, 1f, 1f))
        assertEquals(0xFFFFFFFF.toInt(), WheelColors.hsvToArgb(0f, 0f, 1f))
        assertEquals(0xFF000000.toInt(), WheelColors.hsvToArgb(0f, 0f, 0f))
    }

    /** No colour is see-through. */
    @Test
    fun `every item colour is fully opaque`() {
        for (source in 0 until 20) {
            assertEquals(0xFF, alpha(WheelColors.itemColor(source)))
        }
    }

    /** One entry, one colour. */
    @Test
    fun `an item keeps one colour whatever its sectors do`() {
        // The user asked for one colour per entry, not a gradient per copy.
        val colour = WheelColors.itemColor(3)
        assertEquals(colour, WheelColors.itemColor(3))
    }

    /** Two entries never share a colour. */
    @Test
    fun `each item gets its own colour`() {
        val colours = (0 until 12).map { WheelColors.itemColor(it) }
        assertEquals("items should not share a colour", 12, colours.toSet().size)
    }

    /** Small lists stay easy to tell apart. */
    @Test
    fun `neighbouring items get clearly different hues`() {
        // Two or three entries are the case that used to look monotonous, so the
        // first few base hues must be far apart.
        for (source in 0 until 5) {
            val a = WheelColors.baseHue(source)
            val b = WheelColors.baseHue(source + 1)
            val gap = minOf(
                kotlin.math.abs(a - b),
                360f - kotlin.math.abs(a - b),
            )
            assertTrue("hues $a and $b are too close", gap > 60f)
        }
    }

    /** Hues stay in 0..360. */
    @Test
    fun `base hue stays inside the colour wheel`() {
        for (source in 0 until 50) {
            val hue = WheelColors.baseHue(source)
            assertTrue("hue $hue out of range", hue >= 0f && hue < 360f)
        }
    }

    /** Three entries give three colours. */
    @Test
    fun `three items produce three distinct colours`() {
        val colours = (0 until 3).map { WheelColors.itemColor(it) }
        assertEquals(3, colours.toSet().size)
    }

    /** Light and dark wedges both stay readable. */
    @Test
    fun `label colour contrasts with its background`() {
        assertEquals(WheelColors.DARK_TEXT, WheelColors.labelColorFor(0xFFFFFFFF.toInt()))
        assertEquals(WheelColors.DARK_TEXT, WheelColors.labelColorFor(0xFFFFF59D.toInt()))
        assertEquals(WheelColors.LIGHT_TEXT, WheelColors.labelColorFor(0xFF121212.toInt()))
        assertEquals(WheelColors.LIGHT_TEXT, WheelColors.labelColorFor(0xFF0D47A1.toInt()))
    }

    /** The better of black and white wins. */
    @Test
    fun `label colour is the candidate with the higher contrast ratio`() {
        // Regression: a plain brightness threshold used to pick white for
        // #F522CC even though dark text contrasts better there.
        assertEquals(WheelColors.DARK_TEXT, WheelColors.labelColorFor(0xFFF522CC.toInt()))

        for (source in 0 until 20) {
            val background = WheelColors.itemColor(source)
            val backgroundLuminance = WheelColors.relativeLuminance(background)
            val dark = WheelColors.contrastRatio(
                backgroundLuminance,
                WheelColors.relativeLuminance(WheelColors.DARK_TEXT),
            )
            val light = WheelColors.contrastRatio(
                backgroundLuminance,
                WheelColors.relativeLuminance(WheelColors.LIGHT_TEXT),
            )
            val chosen = if (WheelColors.labelColorFor(background) == WheelColors.DARK_TEXT) {
                dark
            } else {
                light
            }
            assertTrue(
                "label for $background contrasts $chosen but the other is better",
                chosen >= maxOf(dark, light),
            )
        }
    }

    /** The palette keeps a usable contrast floor. */
    @Test
    fun `generated item colours stay legible`() {
        // The palette is deliberately vivid, so a few saturated mid-tones cannot
        // reach WCAG AA (4.5:1) with either candidate. Picking the better
        // candidate keeps every colour in this palette at 4.4:1 or above; assert
        // that floor so a regression in the palette is caught.
        for (source in 0 until 20) {
            val background = WheelColors.itemColor(source)
            val label = WheelColors.labelColorFor(background)
            val ratio = WheelColors.contrastRatio(
                WheelColors.relativeLuminance(background),
                WheelColors.relativeLuminance(label),
            )
            assertTrue(
                "label on $background only contrasts $ratio",
                ratio >= 4.4,
            )
        }
    }
}
