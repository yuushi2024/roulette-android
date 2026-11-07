package com.yuushi2024.roulette.logic

import kotlin.math.ceil
import kotlin.random.Random

/**
 * Turns the raw text entered by the user into a finely divided wheel.
 *
 * Every entry is repeated several times and the copies are scattered around the
 * wheel so that identical labels are never neighbours. The result is a wheel with
 * many small wedges that all look alike, which keeps the outcome hard to guess
 * until the pointer stops.
 */
object WheelFactory {

    /** The wheel is grown to at least this many wedges when possible. */
    const val MIN_SECTORS = 16

    /** A single entry is never repeated more than this many times. */
    const val MAX_REPEATS = 12

    /** Splits raw text into entries: one item per line, blanks removed. */
    fun parseEntries(raw: String): List<String> =
        raw.split('\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    /**
     * How many copies of each entry are needed to reach [MIN_SECTORS], clamped so a
     * short list never explodes into a huge wheel.
     */
    fun repeatsFor(entryCount: Int): Int {
        if (entryCount <= 0) return 0
        val needed = ceil(MIN_SECTORS.toDouble() / entryCount).toInt()
        return needed.coerceIn(1, MAX_REPEATS)
    }

    /**
     * Builds the sectors for [entries].
     *
     * Copies are laid out with a greedy "spread the most frequent label first"
     * strategy, so identical labels end up far apart instead of next to each other.
     * Ties are broken with [random], which makes the wheel layout itself vary a
     * little between builds.
     */
    fun createSectors(entries: List<String>, random: Random = Random.Default): List<RouletteSector> {
        if (entries.isEmpty()) return emptyList()

        val repeats = repeatsFor(entries.size)
        val order = spreadLabels(entries, repeats, random)

        val sweep = 360f / order.size
        return order.mapIndexed { index, item ->
            RouletteSector(
                label = item.label,
                sourceIndex = item.sourceIndex,
                sectorIndex = index,
                startAngle = index * sweep,
                sweepAngle = sweep,
            )
        }
    }

    private data class Placed(val label: String, val sourceIndex: Int)

    private fun spreadLabels(
        entries: List<String>,
        repeats: Int,
        random: Random,
    ): List<Placed> {
        // Group identical lines: two identical lines must keep double the weight.
        val firstIndex = LinkedHashMap<String, Int>()
        val remaining = LinkedHashMap<String, Int>()
        entries.forEachIndexed { index, label ->
            firstIndex.putIfAbsent(label, index)
            remaining[label] = (remaining[label] ?: 0) + repeats
        }

        val total = remaining.values.sum()
        val result = ArrayList<String>(total)
        var lastLabel: String? = null

        repeat(total) {
            val available = remaining.filterValues { it > 0 }
            val notPrevious = available.filterKeys { it != lastLabel }
            val pool = if (notPrevious.isEmpty()) available else notPrevious
            val most = pool.values.maxOrNull() ?: 0
            val top = pool.filterValues { it == most }.keys.toList()
            val chosen = top[random.nextInt(top.size)]

            remaining[chosen] = remaining.getValue(chosen) - 1
            result.add(chosen)
            lastLabel = chosen
        }

        avoidWrapAroundDuplicate(result)
        return result.map { Placed(it, firstIndex.getValue(it)) }
    }

    /**
     * The wheel is a circle, so the last wedge also touches the first one. The greedy
     * pass only guarantees that neighbours inside the list differ; this swaps one
     * wedge when the first and last labels happen to match.
     */
    private fun avoidWrapAroundDuplicate(order: MutableList<String>) {
        if (order.size < 3 || order.first() != order.last()) return

        for (index in 1 until order.size - 1) {
            swap(order, index, order.size - 1)
            if (isRingClean(order)) return
            swap(order, index, order.size - 1)
        }
        for (index in 1 until order.size - 1) {
            swap(order, index, 0)
            if (isRingClean(order)) return
            swap(order, index, 0)
        }
    }

    private fun swap(order: MutableList<String>, a: Int, b: Int) {
        val tmp = order[a]
        order[a] = order[b]
        order[b] = tmp
    }

    private fun isRingClean(order: List<String>): Boolean {
        for (index in order.indices) {
            if (order[index] == order[(index + 1) % order.size]) return false
        }
        return true
    }
}
