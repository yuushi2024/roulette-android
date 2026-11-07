package com.yuushi2024.roulette.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SpinMathTest {

    @Test
    fun `returns minus one when there are no sectors`() {
        assertEquals(-1, SpinMath.winningSectorIndex(0f, 0))
    }

    @Test
    fun `pointer at rest selects the last sector`() {
        // With four sectors the pointer at 270 degrees sits in the fourth one.
        assertEquals(3, SpinMath.winningSectorIndex(0f, 4))
    }

    @Test
    fun `a quarter turn moves the pointer one sector back`() {
        assertEquals(2, SpinMath.winningSectorIndex(90f, 4))
        assertEquals(1, SpinMath.winningSectorIndex(180f, 4))
        assertEquals(0, SpinMath.winningSectorIndex(270f, 4))
    }

    @Test
    fun `full turns do not change the result`() {
        val base = SpinMath.winningSectorIndex(37f, 7)
        assertEquals(base, SpinMath.winningSectorIndex(37f + 360f * 12, 7))
    }

    @Test
    fun `negative rotation is normalised`() {
        assertEquals(SpinMath.winningSectorIndex(350f, 6), SpinMath.winningSectorIndex(-10f, 6))
    }

    @Test
    fun `result is always a valid sector index`() {
        val random = Random(7)
        repeat(500) {
            val rotation = random.nextFloat() * 5000f - 2500f
            val count = 1 + random.nextInt(40)
            val index = SpinMath.winningSectorIndex(rotation, count)
            assertTrue("index $index out of range for $count", index in 0 until count)
        }
    }

    @Test
    fun `next rotation always moves forward by several turns`() {
        val random = Random(11)
        repeat(200) {
            val current = random.nextFloat() * 10000f
            val next = SpinMath.nextRotation(current, random)
            assertTrue(next > current)
            assertTrue(next - current >= 5 * 360f)
            assertTrue(next - current < 10 * 360f)
        }
    }
}
