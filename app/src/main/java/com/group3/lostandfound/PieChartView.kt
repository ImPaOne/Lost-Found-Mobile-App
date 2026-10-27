package com.group3.lostandfound

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator

/**
 * Custom interactive Real Pie / Donut Chart View.
 * Dynamically renders category slices based on live Firebase Realtime Database metrics.
 * Features smooth sweep angle animations and centered summary statistics.
 */
class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class PieSlice(
        val label: String,
        val count: Int,
        val color: Int
    )

    private val slicePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#E5E7EB")
        strokeCap = Paint.Cap.ROUND
    }

    private val centerCountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#003049")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val centerLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#64748B")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    private val rectF = RectF()
    private var slices: List<PieSlice> = emptyList()
    private var totalCount: Int = 0
    private var animationProgress: Float = 1f
    private var animator: ValueAnimator? = null

    init {
        // Density scaling for text and stroke
        val density = context.resources.displayMetrics.density
        slicePaint.strokeWidth = 20f * density
        emptyPaint.strokeWidth = 20f * density
        centerCountPaint.textSize = 20f * density
        centerLabelPaint.textSize = 10f * density
    }

    /**
     * Updates the chart data with real category distributions and triggers an animation.
     */
    fun setData(newSlices: List<PieSlice>) {
        slices = newSlices.filter { it.count > 0 }
        totalCount = slices.sumOf { it.count }

        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 750
            interpolator = DecelerateInterpolator()
            addUpdateListener { va ->
                animationProgress = va.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val stroke = slicePaint.strokeWidth
        val padding = stroke / 2f + 4f
        val w = width.toFloat()
        val h = height.toFloat()
        val diameter = (w.coerceAtMost(h)) - padding * 2
        val left = (w - diameter) / 2f
        val top = (h - diameter) / 2f

        rectF.set(left, top, left + diameter, top + diameter)

        val cx = w / 2f
        val cy = h / 2f

        if (totalCount == 0 || slices.isEmpty()) {
            canvas.drawArc(rectF, 0f, 360f, false, emptyPaint)
            canvas.drawText("0", cx, cy + centerCountPaint.textSize * 0.1f, centerCountPaint)
            canvas.drawText("Items", cx, cy + centerCountPaint.textSize * 0.9f, centerLabelPaint)
            return
        }

        var startAngle = -90f
        val spacingAngle = if (slices.size > 1) 3f else 0f
        val availableDegrees = 360f - (spacingAngle * slices.size)

        for (slice in slices) {
            val sweep = ((slice.count.toFloat() / totalCount) * availableDegrees) * animationProgress
            slicePaint.color = slice.color
            if (sweep > 0f) {
                canvas.drawArc(rectF, startAngle, sweep, false, slicePaint)
            }
            startAngle += sweep + spacingAngle
        }

        // Draw center text
        val countText = "$totalCount"
        val labelText = "Reports"
        canvas.drawText(countText, cx, cy + centerCountPaint.textSize * 0.05f, centerCountPaint)
        canvas.drawText(labelText, cx, cy + centerCountPaint.textSize * 0.85f, centerLabelPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
