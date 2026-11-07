package com.yuushi2024.roulette.logic

/**
 * One wedge of the wheel. Several sectors can share the same [label]: the label is
 * duplicated on purpose so the wheel looks finely divided and the landing spot is
 * hard to predict.
 */
data class RouletteSector(
    val label: String,
    val sourceIndex: Int,
    val sectorIndex: Int,
    val startAngle: Float,
    val sweepAngle: Float,
) {
    val centerAngle: Float get() = startAngle + sweepAngle / 2f
}
