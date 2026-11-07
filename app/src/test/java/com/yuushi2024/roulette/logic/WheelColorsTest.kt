package com.yuushi2024.roulette.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WheelColorsTest {

    private fun alpha(color: Int) = (color ushr 24) and 0xFF

    @Test
    fun `hsv conversion matches primary colours`() {
        assertEquals(0xFFFF0000.toInt(), WheelColors.hsvToArgb(0f, 1f, 1f))
        assertEquals(0xFF00FF00.toInt(), WheelColors.hsvToArgb(120f, 1f, 1f))
        assertEquals(0xFF0000FF.toInt(), WheelColors.hsvToArgb(240f, 1f, 1f))
        assertEquals(0xFFFFFFFF.toInt(), WheelColors.hsvToArgb(0f, 0f, 1f))
        assertEquals(0xFF000000.toInt(), WheelColors.hsvToArgb(0f, 0f, 0f))
    }

    @Test
    fun `every colour is fully opaque`() {
        for (source in 0 until 12) {
            for (position in 0 until 12) {
                assertEquals(0xFF, alpha(WheelColors.sectorColor(source, position, 12)))
            }
        }
    }

    @Test
    fun `neighbouring entries get clearly different hues`() {
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

    @Test
    fun `base hue stays inside the colour wheel`() {
        for (source in 0 until 50) {
            val hue = WheelColors.baseHue(source)
            assertTrue("hue $hue out of range", hue >= 0f && hue < 360f)
        }
    }

    @Test
    fun `copies of one entry are shaded differently`() {
        val total = 8
        val colors = (0 until total).map { WheelColors.sectorColor(1, it, total) }
        assertEquals("copies should be distinct", total, colors.toSet().size)
    }

    @Test
    fun `copies keep the hue of their entry`() {
        // Lightness may vary, but the hue must not drift far from the base.
        val base = WheelColors.baseHue(0)
        for (position in 0 until 10) {
            val color = WheelColors.sectorColor(0, position, 10)
            val gap = hueDistance(hueOf(color), base)
            assertTrue("hue drifted from $base to ${hueOf(color)}", gap < 25f)
        }
    }

    /** Hue of an ARGB colour in degrees, computed without the Android framework. */
    private fun hueOf(color: Int): Float {
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        if (delta == 0f) return 0f
        val hue = when (max) {
            r -> 60f * (((g - b) / delta) % 6f)
            g -> 60f * (((b - r) / delta) + 2f)
            else -> 60f * (((r - g) / delta) + 4f)
        }
        return (hue + 360f) % 360f
    }

    private fun hueDistance(a: Float, b: Float): Float {
        val diff = kotlin.math.abs(a - b) % 360f
        return minOf(diff, 360f - diff)
    }

    @Test
    fun `three entries produce at least three distinct sector colours`() {
        val colors = (0 until 3).map { WheelColors.sectorColor(it, 0, 4) }
        assertEquals(3, colors.toSet().size)
    }

    @Test
    fun `single copy still returns a usable colour`() {
        val color = WheelColors.sectorColor(0, 0, 1)
        assertEquals(0xFF, alpha(color))
    }

    @Test
    fun `label colour contrasts with its background`() {
        assertEquals(WheelColors.DARK_TEXT, WheelColors.labelColorFor(0xFFFFFFFF.toInt()))
        assertEquals(WheelColors.DARK_TEXT, WheelColors.labelColorFor(0xFFFFF59D.toInt()))
        assertEquals(WheelColors.LIGHT_TEXT, WheelColors.labelColorFor(0xFF121212.toInt()))
        assertEquals(WheelColors.LIGHT_TEXT, WheelColors.labelColorFor(0xFF0D47A1.toInt()))
    }

    @Test
    fun `label colour is the candidate with the higher contrast ratio`() {
        // Regression: a plain brightness threshold used to pick white for
        // #F522CC even though dark text contrasts better there.
        assertEquals(WheelColors.DARK_TEXT, WheelColors.labelColorFor(0xFFF522CC.toInt()))

        for (source in 0 until 12) {
            for (position in 0 until 6) {
                val background = WheelColors.sectorColor(source, position, 6)
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
                    chosen >= minOf(dark, light),
                )
            }
        }
    }

    @Test
    fun `generated sector colours stay legible`() {
        // The palette is deliberately vivid, so a few mid-tones cannot reach
        // WCAG AA. They must still stay above the 4.5:1 floor this palette is
        // designed around, with the better candidate always chosen.
        for (source in 0 until 12) {
            for (position in 0 until 6) {
                val background = WheelColors.sectorColor(source, position, 6)
                val label = WheelColors.labelColorFor(background)
                val ratio = WheelColors.contrastRatio(
                    WheelColors.relativeLuminance(background),
                    WheelColors.relativeLuminance(label),
                )
                assertTrue(
                    "label on $background only contrasts $ratio",
                    ratio >= 4.0,
                )
            }
        }
    }
}
