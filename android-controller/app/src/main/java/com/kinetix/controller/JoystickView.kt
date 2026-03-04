package com.kinetix.controller

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

/**
 * Custom analog joystick view.
 *
 * Reports normalised values in the range -1.0 … 1.0 via [onPositionChanged].
 * Dead-zone and boundary clamping are handled internally.
 */
class JoystickView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    // ── Callbacks ────────────────────────────────────────────────────
    var onPositionChanged: ((x: Float, y: Float) -> Unit)? = null

    // ── Geometry ─────────────────────────────────────────────────────
    private var centerX = 0f
    private var centerY = 0f
    private var baseRadius = 0f
    private var hatRadius = 0f

    private var hatX = 0f
    private var hatY = 0f

    private var activePointerId = -1

    // ── Dead-zone ────────────────────────────────────────────────────
    private val deadZone = 0.08f

    // ── Paint ────────────────────────────────────────────────────────
    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#1A1A2E")
    }
    private val baseRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.parseColor("#3A3A5E")
    }
    private val hatPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val hatHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#80FFFFFF")
    }

    // ── Gradient colours ─────────────────────────────────────────────
    private val hatColorIdle = Color.parseColor("#16213E")
    private val hatColorActive = Color.parseColor("#0F3460")
    private val hatGlowColor = Color.parseColor("#E94560")

    // ── Lifecycle ────────────────────────────────────────────────────

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)
        centerX = w / 2f
        centerY = h / 2f
        baseRadius = min(w, h) / 2f * 0.85f
        hatRadius = baseRadius * 0.45f
        hatX = centerX
        hatY = centerY
    }

    // ── Drawing ──────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Base circle
        canvas.drawCircle(centerX, centerY, baseRadius, basePaint)
        canvas.drawCircle(centerX, centerY, baseRadius, baseRingPaint)

        // Inner guide ring
        baseRingPaint.alpha = 60
        canvas.drawCircle(centerX, centerY, baseRadius * 0.5f, baseRingPaint)
        baseRingPaint.alpha = 255

        // Hat (thumb)
        val isActive = activePointerId != -1
        hatPaint.shader = RadialGradient(
            hatX, hatY, hatRadius,
            if (isActive) hatColorActive else hatColorIdle,
            Color.parseColor("#0A0A1A"),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(hatX, hatY, hatRadius, hatPaint)

        // Glow ring when active
        if (isActive) {
            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 4f
                color = hatGlowColor
                maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.drawCircle(hatX, hatY, hatRadius + 4f, glowPaint)
        }

        // Subtle highlight on hat
        canvas.drawCircle(hatX, hatY, hatRadius, hatHighlightPaint)
    }

    // ── Touch ────────────────────────────────────────────────────────

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = event.getPointerId(0)
                updateHat(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val idx = event.findPointerIndex(activePointerId)
                if (idx >= 0) {
                    updateHat(event.getX(idx), event.getY(idx))
                }
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

    // ── Internal ─────────────────────────────────────────────────────

    private fun updateHat(touchX: Float, touchY: Float) {
        var dx = touchX - centerX
        var dy = touchY - centerY
        val dist = sqrt(dx * dx + dy * dy)
        val maxDist = baseRadius - hatRadius

        if (dist > maxDist) {
            dx = dx / dist * maxDist
            dy = dy / dist * maxDist
        }

        hatX = centerX + dx
        hatY = centerY + dy

        var nx = dx / maxDist   // -1 … 1
        var ny = -(dy / maxDist) // invert Y for standard gamepad convention

        // Apply dead-zone
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
