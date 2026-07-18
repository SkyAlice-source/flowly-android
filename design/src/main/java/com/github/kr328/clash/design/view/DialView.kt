package com.github.kr328.clash.design.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.provider.Settings
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.github.kr328.clash.design.R

class DialView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var animator: ValueAnimator? = null
    private val density = resources.displayMetrics.density
    private val dialSizePx = density * 175f
    private val strokeWidth = density * 10f
    private val pad = strokeWidth / 2f + density * 4f

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        this.strokeWidth = this@DialView.strokeWidth
        val base = resolveColor(android.R.attr.colorControlNormal, -16777216)
        color = (0xFFFFFF.toLong() and base.toLong()).toInt() or 0x2E000000
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        this.strokeWidth = this@DialView.strokeWidth
        strokeCap = Paint.Cap.ROUND
        color = -11965201
    }

    private val statePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = density * 13f
        color = resolveColor(android.R.attr.colorControlNormal, -6710887)
    }

    private val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = density * 22f
        color = resolveColor(com.google.android.material.R.attr.colorOnSurface, -13421773)
        isFakeBoldText = true
    }

    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = density * 12f
        color = resolveColor(android.R.attr.colorControlNormal, -6710887)
        alpha = 178
    }

    private val rectF = RectF()
    private var stateText: String = "Stopped"
    private var timeText: String = ""
    private var hintText: String = ""
    private var progress = 0f
    private var running = false
    private val startAngle = -90f

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        val mono = ResourcesCompat.getFont(context, R.font.flowly_mono)
        statePaint.typeface = mono
        timePaint.typeface = mono
        hintPaint.typeface = mono
    }

    fun setRunning(running: Boolean, forwarded: String) {
        this.running = running
        if (running) {
            stateText = context.getString(R.string.running)
            hintText = ""
            progressPaint.color = resolveColor(R.attr.colorConnected, -15681151)
            statePaint.color = resolveColor(R.attr.colorConnected, -15681151)
            animateProgress(360f)
        } else {
            stateText = context.getString(R.string.stopped)
            timeText = ""
            hintText = context.getString(R.string.tap_to_start)
            progressPaint.color = resolveColor(android.R.attr.colorPrimary, -11965201)
            statePaint.color = resolveColor(R.attr.colorClashStopped, -11965201)
            animateProgress(0f)
        }
        invalidate()
    }

    fun setTimeText(time: String) {
        this.timeText = time
        invalidate()
    }

    private fun isReduceMotion(): Boolean {
        return try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        } catch (_: Exception) {
            false
        }
    }

    private fun animateProgress(target: Float) {
        animator?.cancel()
        if (isReduceMotion()) {
            progress = target
            invalidate()
            return
        }
        val animator = ValueAnimator.ofFloat(progress, target).apply {
            duration = 800L
            addUpdateListener {
                progress = (it.animatedValue as Float)
                invalidate()
            }
            start()
        }
        this.animator = animator
    }

    private fun resolveColor(attr: Int, fallback: Int): Int {
        val ta = context.obtainStyledAttributes(intArrayOf(attr))
        val color = ta.getColor(0, fallback)
        ta.recycle()
        return color
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = dialSizePx.toInt()
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val radius = (kotlin.math.min(width, height) / 2f) - pad

        rectF.set(cx - radius, cy - radius, cx + radius, cy + radius)

        canvas.drawCircle(cx, cy, radius, trackPaint)

        if (progress > 0f) {
            canvas.drawArc(rectF, startAngle, progress, false, progressPaint)
        }

        if (running) {
            // Status label near top
            canvas.drawText(stateText, cx, cy - radius * 0.42f, statePaint)

            // Big connection time as the focal element
            val maxTextWidth = 2f * radius * 0.85f
            var ts = density * 22f
            while (timePaint.measureText(timeText) > maxTextWidth && ts > density * 12f) {
                ts -= 1f
                timePaint.textSize = ts
            }
            canvas.drawText(timeText, cx, cy + radius * 0.10f, timePaint)
        } else {
            // Status centered
            canvas.drawText(stateText, cx, cy - density * 6f, statePaint)
            // Tap hint below
            canvas.drawText(hintText, cx, cy + density * 18f, hintPaint)
        }
    }
}
