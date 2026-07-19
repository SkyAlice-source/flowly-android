package com.github.kr328.clash.design.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import com.github.kr328.clash.design.R
import kotlin.collections.ArrayDeque

/**
 * Lightweight real-time speed sparkline. Keeps a rolling buffer of the last
 * [maxSamples] (down, up) speed samples (fed once per second from MainDesign)
 * and draws two themed lines: download (connected color) and upload (primary).
 */
class SpeedChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val maxSamples = 60
    private val downSamples = ArrayDeque<Long>()
    private val upSamples = ArrayDeque<Long>()

    private val pad = density * 10f

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = density * 1f
        color = resolveColor(android.R.attr.colorControlNormal, -6710887)
        alpha = 36
    }

    private val downPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = density * 2f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = resolveColor(R.attr.colorConnected, -15681151)
    }

    private val upPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = density * 2f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = resolveColor(android.R.attr.colorPrimary, -11965201)
    }

    private val fillDownPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = resolveColor(R.attr.colorConnected, -15681151)
        alpha = 30
    }

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    fun appendSample(down: Long, up: Long) {
        downSamples.addLast(down)
        upSamples.addLast(up)
        while (downSamples.size > maxSamples) downSamples.removeFirst()
        while (upSamples.size > maxSamples) upSamples.removeFirst()
        invalidate()
    }

    fun clear() {
        if (downSamples.isNotEmpty() || upSamples.isNotEmpty()) {
            downSamples.clear()
            upSamples.clear()
            invalidate()
        }
    }

    private fun resolveColor(attr: Int, fallback: Int): Int {
        val ta = context.obtainStyledAttributes(intArrayOf(attr))
        val color = ta.getColor(0, fallback)
        ta.recycle()
        return color
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (downSamples.isEmpty()) return

        val left = pad
        val right = width - pad
        val top = pad
        val bottom = height - pad
        val plotH = bottom - top

        // baseline grid
        canvas.drawLine(left, bottom, right, bottom, gridPaint)

        val maxVal = maxOf(
            downSamples.maxOrNull() ?: 0L,
            upSamples.maxOrNull() ?: 0L,
            1L
        ).toFloat()

        drawSeries(canvas, downSamples, left, right, bottom, plotH, maxVal, downPaint, fillDownPaint)
        drawSeries(canvas, upSamples, left, right, bottom, plotH, maxVal, upPaint, null)
    }

    private fun drawSeries(
        canvas: Canvas,
        samples: ArrayDeque<Long>,
        left: Float,
        right: Float,
        bottom: Float,
        plotH: Float,
        maxVal: Float,
        linePaint: Paint,
        fillPaint: Paint?
    ) {
        val n = samples.size
        if (n < 2) return

        val stepX = (right - left) / (maxSamples - 1)
        val startIdx = maxSamples - n

        val pts = FloatArray(n * 2)
        for (i in 0 until n) {
            pts[i * 2] = left + (startIdx + i) * stepX
            pts[i * 2 + 1] = bottom - (samples[i].toFloat() / maxVal) * plotH
        }

        if (fillPaint != null) {
            val fillPath = Path().apply {
                moveTo(pts[0], bottom)
                for (i in 0 until n) lineTo(pts[i * 2], pts[i * 2 + 1])
                lineTo(pts[(n - 1) * 2], bottom)
                close()
            }
            canvas.drawPath(fillPath, fillPaint)
        }

        val linePath = Path().apply {
            moveTo(pts[0], pts[1])
            for (i in 1 until n) lineTo(pts[i * 2], pts[i * 2 + 1])
        }
        canvas.drawPath(linePath, linePaint)
    }
}
