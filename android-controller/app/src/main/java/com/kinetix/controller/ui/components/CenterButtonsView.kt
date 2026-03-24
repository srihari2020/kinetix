package com.kinetix.controller.v2.ui.components

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import com.kinetix.controller.v2.system.HapticsManager
import kotlin.math.hypot

class CenterButtonsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var onStateChanged: ((start: Boolean, select: Boolean) -> Unit)? = null
    var haptics: HapticsManager? = null

    private var startPressed = false
    private var selectPressed = false

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#151525"); style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#34344A"); style = Paint.Style.STROKE; strokeWidth = 4f }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 30f; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
    
    // Distinct accent colors
    private val startColor = Color.parseColor("#2ECC71")   // Green
    private val selectColor = Color.parseColor("#3498DB")   // Blue
    private val inactivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#222230"); style = Paint.Style.FILL }
    private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private val rect = RectF()
    private var btnRadius = 0f
    private val selectCenter = PointF()
    private val startCenter = PointF()

    private var selectScale = 1.0f
    private var startScale = 1.0f

    class PointF(var x: Float = 0f, var y: Float = 0f)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        rect.set(10f, 10f, w - 10f, h - 10f)
        val cy = h / 2f
        btnRadius = Math.min(w, h) * 0.35f
        
        selectCenter.x = w * 0.3f
        selectCenter.y = cy
        
        startCenter.x = w * 0.7f
        startCenter.y = cy
        
        textPaint.textSize = btnRadius * 0.6f
    }

    override fun onDraw(canvas: Canvas) {
        // Background pill
        basePaint.setShadowLayer(15f, 0f, 5f, Color.parseColor("#05050A"))
        val corner = height / 2f
        canvas.drawRoundRect(rect, corner, corner, basePaint)
        basePaint.clearShadowLayer()
        canvas.drawRoundRect(rect, corner, corner, strokePaint)

        // Select Button
        drawCenterBtn(canvas, selectCenter, "SEL", selectPressed, selectColor, selectScale)
        // Start Button
        drawCenterBtn(canvas, startCenter, "STR", startPressed, startColor, startScale)
    }

    private fun drawCenterBtn(canvas: Canvas, c: PointF, text: String, pressed: Boolean, color: Int, scale: Float) {
        canvas.save()
        canvas.scale(scale, scale, c.x, c.y)

        if (pressed) {
            activePaint.shader = RadialGradient(c.x, c.y, btnRadius, color, Color.parseColor("#111111"), Shader.TileMode.CLAMP)
            activePaint.setShadowLayer(20f, 0f, 0f, color)
            canvas.drawCircle(c.x, c.y, btnRadius, activePaint)
            activePaint.clearShadowLayer()
            activePaint.shader = null
        } else {
            canvas.drawCircle(c.x, c.y, btnRadius, inactivePaint)
        }

        canvas.drawText(text, c.x, c.y - (textPaint.descent() + textPaint.ascent()) / 2, textPaint)
        canvas.restore()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (tag == "editor_mode") return false
        
        var newStart = false
        var newSelect = false

        for (i in 0 until event.pointerCount) {
            if (event.actionMasked == MotionEvent.ACTION_POINTER_UP || event.actionMasked == MotionEvent.ACTION_UP) {
                if (i == event.actionIndex) continue
            }
            val px = event.getX(i)
            val py = event.getY(i)
            
            if (hypot(px - selectCenter.x, py - selectCenter.y) <= btnRadius * 1.5f) newSelect = true
            if (hypot(px - startCenter.x, py - startCenter.y) <= btnRadius * 1.5f) newStart = true
        }

        // Rubber haptics on both press and release
        if (newStart != startPressed) {
            haptics?.vibrate(if (newStart) HapticsManager.Type.RUBBER_PRESS else HapticsManager.Type.RUBBER_RELEASE)
            animateBtn("start", if (newStart) 0.85f else 1.0f)
        }
        if (newSelect != selectPressed) {
            haptics?.vibrate(if (newSelect) HapticsManager.Type.RUBBER_PRESS else HapticsManager.Type.RUBBER_RELEASE)
            animateBtn("select", if (newSelect) 0.85f else 1.0f)
        }

        if (newStart != startPressed || newSelect != selectPressed) {
            startPressed = newStart
            selectPressed = newSelect
            onStateChanged?.invoke(startPressed, selectPressed)
            invalidate()
        }
        return true
    }

    private fun animateBtn(btn: String, to: Float) {
        val from = if (btn == "start") startScale else selectScale
        ValueAnimator.ofFloat(from, to).apply {
            duration = 120
            interpolator = OvershootInterpolator(3f)
            addUpdateListener {
                val v = it.animatedValue as Float
                if (btn == "start") startScale = v else selectScale = v
                invalidate()
            }
            start()
        }
    }
}
