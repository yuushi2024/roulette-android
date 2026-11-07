package com.yuushi2024.roulette.logic

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Builds the colours of the wheel.
 *
 * Every entry gets a base hue spaced by the golden angle, so even two or three
 * entries land on clearly different hues. The copies of one entry keep that hue
 * but step through lightness and saturation, which gives the finely divided
 * wheel a layered look instead of a flat repeat of the same few colours.
 *
 * Android types are avoided on purpose so this can be unit tested on the JVM.
 */
object WheelColors {

    private const val GOLDEN_ANGLE = 137.508f
    private const val BASE_HUE = 352f

    /** How far a copy's hue drifts from its entry's base hue, in degrees. */
    private const val HUE_SPREAD = 16f

    /** Base hue (degrees) of the entry at [sourceIndex]. */
    fun baseHue(sourceIndex: Int): Float =
        (((BASE_HUE + GOLDEN_ANGLE * sourceIndex) % 360f) + 360f) % 360f

    /**
     * Colour of one copy of an entry. [position] is the copy's order (0-based)
     * and [total] how many copies that entry has on the wheel.
     */
    fun sectorColor(sourceIndex: Int, position: Int, total: Int): Int {
        val hue = baseHue(sourceIndex)
        val t = if (total <= 1) 0f else position.toFloat() / total
        val shifted = (hue + (t - 0.5f) * HUE_SPREAD + 360f) % 360f
        val saturation = if (position % 2 == 0) 0.86f else 0.64f
        val value = 0.96f - 0.30f * t
        return hsvToArgb(shifted, saturation, value)
    }

    /** Black or white, whichever stays legible on [background]. */
    fun labelColorFor(background: Int): Int {
        val r = (background shr 16) and 0xFF
        val g = (background shr 8) and 0xFF
        val b = background and 0xFF
        val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
        return if (luminance > 0.62) DARK_TEXT else LIGHT_TEXT
    }

    /** Converts HSV (hue in degrees, saturation and value in 0..1) to ARGB. */
    fun hsvToArgb(hue: Float, saturation: Float, value: Float): Int {
        val h = ((hue % 360f) + 360f) % 360f
        val c = value * saturation
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = value - c
        val rgb = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return argb(rgb.first + m, rgb.second + m, rgb.third + m)
    }

    private fun argb(r: Float, g: Float, b: Float): Int =
        (0xFF shl 24) or (channel(r) shl 16) or (channel(g) shl 8) or channel(b)

    private fun channel(value: Float): Int =
        (value * 255f).roundToInt().coerceIn(0, 255)

    /** Matches the app's on-surface colour. */
    const val DARK_TEXT = 0xFF1B1B1F.toInt()
    const val LIGHT_TEXT = 0xFFFFFFFF.toInt()
}
