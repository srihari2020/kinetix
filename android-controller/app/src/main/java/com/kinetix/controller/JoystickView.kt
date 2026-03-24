package com.kinetix.controller.v2.ui.components

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.kinetix.controller.v2.system.HapticsManager
import kotlin.math.*

class JoystickView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var onPositionChanged: ((Float, Float) -> Unit)? = null
    var haptics: HapticsManager? = null

    private var centerX = 0f
    private var centerY = 0f
    private var baseRadius = 0f
    private var hatRadius = 0f
    private var hatX = 0f
    private var hatY = 0f
    private var activePointerId = -1
    private val deadZone = 0.08f

    // Glassmorphism and Neon
    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.parseColor("#121220") }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 5f; color = Color.parseColor("#2C2C3E") }
    private val hatPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 8f; color = Color.parseColor("#E94560") }

    private var isAtEdge = false

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)
        centerX = w / 2f
        centerY = h / 2f
        baseRadius = min(w, h) / 2f * 0.85f
        hatRadius = baseRadius * 0.40f
        hatX = centerX
        hatY = centerY
    }

    override fun onDraw(canvas: Canvas) {
        // Base glass shadow
        glowPaint.setShadowLayer(30f, 0f, 15f, Color.parseColor("#05050A"))
        canvas.drawCircle(centerX, centerY, baseRadius, glowPaint)
        glowPaint.clearShadowLayer()

        canvas.drawCircle(centerX, centerY, baseRadius, basePaint)
        canvas.drawCircle(centerX, centerY, baseRadius, strokePaint)

        // Inner Rings
        strokePaint.alpha = 50
        canvas.drawCircle(centerX, centerY, baseRadius * 0.5f, strokePaint)
        strokePaint.alpha = 255

        val isActive = activePointerId != -1
        hatPaint.shader = RadialGradient(
            hatX, hatY, hatRadius,
            if (isActive) Color.parseColor("#E94560") else Color.parseColor("#2C2C3E"),
            Color.parseColor("#0A0A1A"),
            Shader.TileMode.CLAMP
        )

        canvas.drawCircle(hatX, hatY, hatRadius, hatPaint)
        if (isActive) {
            glowPaint.alpha = 150
            canvas.drawCircle(hatX, hatY, hatRadius, glowPaint)
            glowPaint.alpha = 255
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (tag == "editor_mode") return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = event.getPointerId(0)
                updateHat(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val idx = event.findPointerIndex(activePointerId)
                if (idx >= 0) updateHat(event.getX(idx), event.getY(idx))
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activePointerId = -1
                resetHat()
                return true
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val idx = event.actionIndex
                if (event.getPointerId(idx) == activePointerId) {
                    activePointerId = -1
                    resetHat()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateHat(touchX: Float, touchY: Float) {
        var dx = touchX - centerX
        var dy = touchY - centerY
        val dist = sqrt(dx * dx + dy * dy)
        val maxDist = baseRadius - hatRadius

        if (dist > maxDist) {
            dx = dx / dist * maxDist
            dy = dy / dist * maxDist
            if (!isAtEdge) {
                isAtEdge = true
                haptics?.vibrate(HapticsManager.Type.LIGHT_TICK)
            }
        } else {
            isAtEdge = false
        }

        hatX = centerX + dx
        hatY = centerY + dy

        var nx = dx / maxDist
        var ny = -(dy / maxDist)

        if (abs(nx) < deadZone) nx = 0f
        if (abs(ny) < deadZone) ny = 0f

        onPositionChanged?.invoke(nx, ny)
        invalidate()
    }

    private fun resetHat() {
        hatX = centerX
        hatY = centerY
        onPositionChanged?.invoke(0f, 0f)
        invalidate()
    }
}
