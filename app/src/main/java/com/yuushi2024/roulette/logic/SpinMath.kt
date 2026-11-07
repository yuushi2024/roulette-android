package com.yuushi2024.roulette.logic

import kotlin.math.floor
import kotlin.random.Random

/**
 * Maps the wheel's final rotation back to the wedge sitting under the pointer.
 *
 * Angles use the canvas convention: 0 degrees points right (3 o'clock) and grows
 * clockwise. Sectors are laid out in that same order, so a sector drawn at angle
 * `a` is shown at `a + rotation` once the wheel is turned by `rotation`.
 */
object SpinMath {

    /** Canvas angle of the pointer: straight up (12 o'clock). */
    const val POINTER_ANGLE = 270f

    /**
     * Index of the sector under the pointer, or -1 when there are no sectors.
     *
     * [rotation] is normalised first, so callers can pass an ever-growing spin
     * angle without worrying about overflow.
     */
    fun winningSectorIndex(rotation: Float, sectorCount: Int): Int {
        if (sectorCount <= 0) return -1
        val sweep = 360f / sectorCount
        val normalised = normalise(rotation)
        val relative = normalise(POINTER_ANGLE - normalised)
        return floor(relative / sweep).toInt().coerceIn(0, sectorCount - 1)
    }

    /**
     * The rotation to animate towards: always forwards, always several full turns,
     * plus a random part-turn so the wheel never lands in the same place twice.
     */
    fun nextRotation(
        currentRotation: Float,
        random: Random = Random.Default,
        minTurns: Int = 5,
        maxTurns: Int = 9,
    ): Float {
        val turns = minTurns + random.nextInt(maxTurns - minTurns + 1)
        val extra = random.nextFloat() * 360f
        return currentRotation + turns * 360f + extra
    }

    private fun normalise(degrees: Float): Float {
        val value = degrees % 360f
        return if (value < 0f) value + 360f else value
    }
}
