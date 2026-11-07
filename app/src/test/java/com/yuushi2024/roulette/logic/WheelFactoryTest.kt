package com.yuushi2024.roulette.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class WheelFactoryTest {

    @Test
    fun `parseEntries trims blanks and keeps order`() {
        val entries = WheelFactory.parseEntries(" A \n\nB\n  \nC\n")
        assertEquals(listOf("A", "B", "C"), entries)
    }

    @Test
    fun `parseEntries handles empty input`() {
        assertTrue(WheelFactory.parseEntries("").isEmpty())
        assertTrue(WheelFactory.parseEntries("   \n \n").isEmpty())
    }

    @Test
    fun `createSectors returns nothing for empty entries`() {
        assertTrue(WheelFactory.createSectors(emptyList()).isEmpty())
    }

    @Test
    fun `single entry fills the wheel with one sector when capped`() {
        val sectors = WheelFactory.createSectors(listOf("A"))
        assertEquals(WheelFactory.MAX_REPEATS, sectors.size)
        assertTrue(sectors.all { it.label == "A" })
    }

    @Test
    fun `wheel is grown to at least the minimum sector count`() {
        val sectors = WheelFactory.createSectors(listOf("A", "B", "C"))
        assertTrue(sectors.size >= WheelFactory.MIN_SECTORS)
    }

    @Test
    fun `angles cover the full circle without gaps`() {
        val sectors = WheelFactory.createSectors(listOf("A", "B", "C", "D"))
        val sweep = 360f / sectors.size
        sectors.forEachIndexed { index, sector ->
            assertEquals(index * sweep, sector.startAngle, 0.001f)
            assertEquals(sweep, sector.sweepAngle, 0.001f)
        }
        val last = sectors.last()
        assertEquals(360f, last.startAngle + last.sweepAngle, 0.01f)
    }

    @Test
    fun `each label keeps its weight`() {
        val entries = listOf("A", "B", "C")
        val sectors = WheelFactory.createSectors(entries)
        val repeats = WheelFactory.repeatsFor(entries.size)
        entries.forEach { label ->
            assertEquals(repeats, sectors.count { it.label == label })
        }
    }

    @Test
    fun `duplicate labels double their weight`() {
        val sectors = WheelFactory.createSectors(listOf("A", "A", "B"))
        assertEquals(sectors.count { it.label == "B" } * 2, sectors.count { it.label == "A" })
    }

    @Test
    fun `identical labels are never neighbours`() {
        repeat(50) { seed ->
            val sectors = WheelFactory.createSectors(
                listOf("A", "B", "C", "D"),
                Random(seed),
            )
            sectors.forEachIndexed { index, sector ->
                val next = sectors[(index + 1) % sectors.size]
                assertNotEquals(sector.label, next.label)
            }
        }
    }

    @Test
    fun `labels are also spread across the wrap-around point`() {
        repeat(200) { seed ->
            val sectors = WheelFactory.createSectors(
                listOf("A", "A", "B", "B", "C"),
                Random(seed),
            )
            val first = sectors.first().label
            val last = sectors.last().label
            assertNotEquals(first, last)
        }
    }

    @Test
    fun `repeatsFor stays within bounds`() {
        assertEquals(0, WheelFactory.repeatsFor(0))
        assertEquals(1, WheelFactory.repeatsFor(100))
        assertTrue(WheelFactory.repeatsFor(2) <= WheelFactory.MAX_REPEATS)
    }
}
