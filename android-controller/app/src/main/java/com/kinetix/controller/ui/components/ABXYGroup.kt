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

class ABXYGroup @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var state = ActionState()
    var onStateChanged: ((ActionState) -> Unit)? = null
    var haptics: HapticsManager? = null

    data class ActionState(var a: Boolean = false, var b: Boolean = false, var x: Boolean = false, var y: Boolean = false)

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#151525"); style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#2C2C3E"); style = Paint.Style.STROKE; strokeWidth = 5f }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 60f; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
    
    // Xbox Colors — vibrant accent gradients
    private val aColors = intArrayOf(Color.parseColor("#2ECC71"), Color.parseColor("#1E8449"))
    private val bColors = intArrayOf(Color.parseColor("#E74C3C"), Color.parseColor("#922B21"))
    private val xColors = intArrayOf(Color.parseColor("#3498DB"), Color.parseColor("#21618C"))
    private val yColors = intArrayOf(Color.parseColor("#F1C40F"), Color.parseColor("#9A7D0A"))
    private val inactiveColors = intArrayOf(Color.parseColor("#2A2A3A"), Color.parseColor("#1A1A2A"))

    private val aPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val xPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val yPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val inactivePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var btnRadius = 0f
    private val aCenter = PointF()
    private val bCenter = PointF()
    private val xCenter = PointF()
    private val yCenter = PointF()

    // Independent scales for rubber bounce
    private var aScale = 1.0f
    private var bScale = 1.0f
    private var xScale = 1.0f
    private var yScale = 1.0f

    class PointF(var x: Float = 0f, var y: Float = 0f)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val cx = w / 2f
        val cy = h / 2f
        val R = (Math.min(w, h) / 2f) * 0.85f
        btnRadius = R * 0.35f

        val offset = R * 0.60f
        aCenter.apply { x = cx; y = cy + offset }
        yCenter.apply { x = cx; y = cy - offset }
        xCenter.apply { x = cx - offset; y = cy }
        bCenter.apply { x = cx + offset; y = cy }
        textPaint.textSize = btnRadius * 0.8f
    }

    override fun onDraw(canvas: Canvas) {
        // Glassmorphism background disk
        basePaint.setShadowLayer(25f, 0f, 10f, Color.parseColor("#05050A"))
        canvas.drawCircle(width / 2f, height / 2f, Math.min(width, height) / 2f * 0.95f, basePaint)
        basePaint.clearShadowLayer()

        canvas.drawCircle(width / 2f, height / 2f, Math.min(width, height) / 2f * 0.95f, strokePaint)

        // Draw buttons
        drawButton(canvas, aCenter, "A", if (state.a) aPaint else inactivePaint, aScale, state.a, aColors[0])
        drawButton(canvas, bCenter, "B", if (state.b) bPaint else inactivePaint, bScale, state.b, bColors[0])
        drawButton(canvas, xCenter, "X", if (state.x) xPaint else inactivePaint, xScale, state.x, xColors[0])
        drawButton(canvas, yCenter, "Y", if (state.y) yPaint else inactivePaint, yScale, state.y, yColors[0])
    }

    private fun drawButton(canvas: Canvas, c: PointF, text: String, paint: Paint, scale: Float, isPressed: Boolean, glowColor: Int) {
        canvas.save()
        canvas.scale(scale, scale, c.x, c.y)

        // Radial gradient for 3D rubber effect
        paint.shader = RadialGradient(c.x - btnRadius*0.3f, c.y - btnRadius*0.3f, btnRadius*1.5f,
            if (isPressed) glowColor else Color.parseColor("#3A3A4A"),
            if (isPressed) Color.parseColor("#111111") else Color.parseColor("#151525"),
            Shader.TileMode.CLAMP)

        // Shadow and glow — enhanced for rubber feel
        if (isPressed) {
            paint.setShadowLayer(35f, 0f, 0f, glowColor)
        } else {
            paint.setShadowLayer(15f, 0f, 5f, Color.parseColor("#05050A"))
        }

        canvas.drawCircle(c.x, c.y, btnRadius, paint)
        paint.clearShadowLayer()
        paint.shader = null

        // Inner rim
        strokePaint.color = if (isPressed) Color.argb(80, Color.red(glowColor), Color.green(glowColor), Color.blue(glowColor)) else Color.parseColor("#22FFFFFF")
        canvas.drawCircle(c.x, c.y, btnRadius, strokePaint)
        strokePaint.color = Color.parseColor("#2C2C3E")

        canvas.drawText(text, c.x, c.y - (textPaint.descent() + textPaint.ascent()) / 2 + 5f, textPaint)
        canvas.restore()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (tag == "editor_mode") return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_MOVE -> updatePointers(event)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> updatePointers(event)
        }
        return true
    }

    private fun updatePointers(event: MotionEvent) {
        val oldA = state.a; val oldB = state.b; val oldX = state.x; val oldY = state.y
        var a = false; var b = false; var x = false; var y = false

        for (i in 0 until event.pointerCount) {
            if (event.actionMasked == MotionEvent.ACTION_POINTER_UP || event.actionMasked == MotionEvent.ACTION_UP) {
                if (i == event.actionIndex) continue
            }
            val px = event.getX(i)
            val py = event.getY(i)

            if (hypot(px - aCenter.x, py - aCenter.y) <= btnRadius * 1.8f) a = true
            if (hypot(px - bCenter.x, py - bCenter.y) <= btnRadius * 1.8f) b = true
            if (hypot(px - xCenter.x, py - xCenter.y) <= btnRadius * 1.8f) x = true
            if (hypot(px - yCenter.x, py - yCenter.y) <= btnRadius * 1.8f) y = true
        }

        // Rubber press + release haptics
        if (a != oldA) { state.a = a; animateScale("a", if (a) 0.82f else 1.0f); haptics?.vibrate(if (a) HapticsManager.Type.RUBBER_PRESS else HapticsManager.Type.RUBBER_RELEASE) }
        if (b != oldB) { state.b = b; animateScale("b", if (b) 0.82f else 1.0f); haptics?.vibrate(if (b) HapticsManager.Type.RUBBER_PRESS else HapticsManager.Type.RUBBER_RELEASE) }
        if (x != oldX) { state.x = x; animateScale("x", if (x) 0.82f else 1.0f); haptics?.vibrate(if (x) HapticsManager.Type.RUBBER_PRESS else HapticsManager.Type.RUBBER_RELEASE) }
        if (y != oldY) { state.y = y; animateScale("y", if (y) 0.82f else 1.0f); haptics?.vibrate(if (y) HapticsManager.Type.RUBBER_PRESS else HapticsManager.Type.RUBBER_RELEASE) }

        if (a != oldA || b != oldB || x != oldX || y != oldY) {
            onStateChanged?.invoke(state)
            invalidate()
        }
    }

    private fun animateScale(btn: String, to: Float) {
        val from = when (btn) { "a" -> aScale; "b" -> bScale; "x" -> xScale; else -> yScale }
        ValueAnimator.ofFloat(from, to).apply {
            duration = 130
            interpolator = OvershootInterpolator(3f) // Bouncier rubber feel
            addUpdateListener {
                val v = it.animatedValue as Float
                when (btn) { "a" -> aScale = v; "b" -> bScale = v; "x" -> xScale = v; else -> yScale = v }
                invalidate()
            }
            start()
        }
    }
}
