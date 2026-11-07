package com.yuushi2024.roulette.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.yuushi2024.roulette.logic.RouletteSector
import com.yuushi2024.roulette.logic.SpinMath
import com.yuushi2024.roulette.logic.WheelColors
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Draws the wheel and animates the spin.
 *
 * The view only knows how to render sectors and how far the wheel has turned; the
 * decision of where it stops is taken by the caller, which passes the final angle.
 */
class RouletteView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private var sectors: List<RouletteSector> = emptyList()
    private var colors: IntArray = IntArray(0)

    private val sectorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.argb(90, 255, 255, 255)
        strokeWidth = 2f
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val hubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }
    private val hubRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#FFC107")
        strokeWidth = 8f
    }
    private val pointerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#D32F2F")
    }

    private val oval = RectF()
    private val pointerPath = Path()

    private var rotationDegrees = 0f
    private var animator: ValueAnimator? = null
    private var spinListener: ((Int) -> Unit)? = null

    /** Called with the winning sector index once the wheel comes to rest. */
    fun setOnSpinEndListener(listener: (Int) -> Unit) {
        spinListener = listener
    }

    /** Replaces the wheel contents and rebuilds the sector colours. */
    fun setSectors(newSectors: List<RouletteSector>) {
        sectors = newSectors
        colors = buildColors(newSectors)
        invalidate()
    }

    /** True while a spin animation is running. */
    fun isSpinning(): Boolean = animator?.isRunning == true

    /**
     * Spins from the current angle to [targetRotation] (degrees, growing clockwise)
     * and reports the resulting sector index when the animation ends.
     */
    fun spinTo(targetRotation: Float, durationMillis: Long) {
        animator?.cancel()
        val start = rotationDegrees
        val animator = ValueAnimator.ofFloat(start, targetRotation).apply {
            duration = durationMillis
            interpolator = DecelerateInterpolator(1.8f)
            addUpdateListener {
                rotationDegrees = it.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                private var canceled = false

                override fun onAnimationCancel(animation: Animator) {
                    canceled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    // onAnimationEnd also fires after cancel(); only report a
                    // winner for a spin that actually reached the target.
                    if (canceled) return
                    rotationDegrees = targetRotation
                    invalidate()
                    spinListener?.invoke(
                        SpinMath.winningSectorIndex(targetRotation, sectors.size),
                    )
                }
            })
        }
        this.animator = animator
        animator.start()
    }

    /** Stops the animation so a detached view cannot leak its listener. */
    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (sectors.isEmpty()) return

        val size = min(width, height).toFloat()
        val cx = width / 2f
        val cy = height / 2f
        val radius = size / 2f - size * 0.06f
        oval.set(cx - radius, cy - radius, cx + radius, cy + radius)

        val textSize = (radius * 0.085f).coerceIn(18f, 42f)
        labelPaint.textSize = textSize

        sectors.forEachIndexed { index, sector ->
            sectorPaint.color = colors[index]
            val start = sector.startAngle + rotationDegrees
            canvas.drawArc(oval, start, sector.sweepAngle, true, sectorPaint)
            canvas.drawArc(oval, start, sector.sweepAngle, true, dividerPaint)
        }

        drawLabels(canvas, cx, cy, radius, textSize)
        drawHub(canvas, cx, cy, radius)
        drawPointer(canvas, cx, cy, radius)
    }

    /** Draws every sector label, picking a readable colour per wedge. */
    private fun drawLabels(canvas: Canvas, cx: Float, cy: Float, radius: Float, textSize: Float) {
        if (sectors.size > 40) return
        val textRadius = radius * 0.66f
        sectors.forEachIndexed { index, sector ->
            // Light wedges need dark text and vice versa, so the palette can use
            // both bright and deep colours without losing the labels.
            labelPaint.color = WheelColors.labelColorFor(colors[index])
            val angle = Math.toRadians((sector.centerAngle + rotationDegrees).toDouble())
            val x = cx + (textRadius * cos(angle)).toFloat()
            val y = cy + (textRadius * sin(angle)).toFloat()
            canvas.save()
            canvas.rotate(sector.centerAngle + rotationDegrees + 90f, x, y)
            canvas.drawText(sector.label, x, y + textSize / 3f, labelPaint)
            canvas.restore()
        }
    }

    /** Draws the centre hub and its ring. */
    private fun drawHub(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val hubRadius = radius * 0.16f
        canvas.drawCircle(cx, cy, hubRadius, hubPaint)
        canvas.drawCircle(cx, cy, hubRadius, hubRingPaint)
    }

    /** Draws the fixed pointer at the top of the wheel. */
    private fun drawPointer(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val tipLength = radius * 0.30f
        val halfWidth = radius * 0.09f
        pointerPath.reset()
        pointerPath.moveTo(cx, cy - radius + tipLength)
        pointerPath.lineTo(cx - halfWidth, cy - radius - radius * 0.02f)
        pointerPath.lineTo(cx + halfWidth, cy - radius - radius * 0.02f)
        pointerPath.close()
        canvas.drawPath(pointerPath, pointerPaint)
    }

    /** Assigns a colour to every sector, shading the copies of one entry. */
    private fun buildColors(items: List<RouletteSector>): IntArray {
        // Copies of one entry share a hue; their order among the copies sets how
        // light or dark that copy is, so the wheel shows a smooth gradient per
        // entry rather than a single flat colour.
        val totals = IntArray((items.maxOfOrNull { it.sourceIndex } ?: 0) + 1)
        items.forEach { totals[it.sourceIndex]++ }
        val seen = IntArray(totals.size)
        return IntArray(items.size) { index ->
            val source = items[index].sourceIndex
            WheelColors.sectorColor(source, seen[source]++, totals[source])
        }
    }
}
