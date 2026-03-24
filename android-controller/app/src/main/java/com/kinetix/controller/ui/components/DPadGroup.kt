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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * PPSSPP-style circular D-Pad with directional arrow buttons.
 * Circular disc base with 4 triangular arrow indicators.
 * Each direction has a unique neon accent color with glow effect.
 */
class DPadGroup @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var onDpadChanged: ((String) -> Unit)? = null
    var haptics: HapticsManager? = null

    private var pressedDirection: String? = null

    // Per-direction press scales for rubber animation
    private var upScale = 1.0f
    private var downScale = 1.0f
    private var leftScale = 1.0f
    private var rightScale = 1.0f

    // Direction accent colors
    private val upColor = Color.parseColor("#00E5FF")      // Cyan
    private val downColor = Color.parseColor("#FF4081")     // Magenta/Pink
    private val leftColor = Color.parseColor("#FF9100")     // Orange
    private val rightColor = Color.parseColor("#76FF03")    // Lime Green

    // Paints
    private val discPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val discStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 3f; color = Color.parseColor("#3A3A50")
    }
    private val innerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 2f; color = Color.parseColor("#22FFFFFF")
    }
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val centerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL; color = Color.parseColor("#2A2A3A")
    }
    private val separatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 1.5f; color = Color.parseColor("#1A1A2E")
    }

    // Geometry
    private var cx = 0f
    private var cy = 0f
    private var discRadius = 0f
    private var arrowSize = 0f

    // Arrow paths for each direction
    private val upArrow = Path()
    private val downArrow = Path()
    private val leftArrow = Path()
    private val rightArrow = Path()

    // Sector paths for hit testing and glow
    private val upSector = Path()
    private val downSector = Path()
    private val leftSector = Path()
    private val rightSector = Path()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cx = w / 2f
        cy = h / 2f
        discRadius = min(cx, cy) * 0.92f
        arrowSize = discRadius * 0.22f

        val innerR = discRadius * 0.30f   // deadzone circle radius
        val outerR = discRadius * 0.95f
        val arrowDist = discRadius * 0.62f // distance from center to arrow tip

        // Build arrow triangles (pointing outward)
        buildArrow(upArrow, cx, cy - arrowDist, 0f)    // UP: pointing up
        buildArrow(downArrow, cx, cy + arrowDist, 180f) // DOWN: pointing down
        buildArrow(leftArrow, cx - arrowDist, cy, 90f)  // LEFT: pointing left
        buildArrow(rightArrow, cx + arrowDist, cy, -90f) // RIGHT: pointing right

        // Build sector regions for glow fill
        buildSector(upSector, cx, cy, innerR, outerR, 225f, 315f)
        buildSector(rightSector, cx, cy, innerR, outerR, 315f, 405f)
        buildSector(downSector, cx, cy, innerR, outerR, 45f, 135f)
        buildSector(leftSector, cx, cy, innerR, outerR, 135f, 225f)
    }

    private fun buildArrow(path: Path, tipX: Float, tipY: Float, rotation: Float) {
        path.reset()
        val s = arrowSize
        // Triangle pointing UP by default, then rotated
        path.moveTo(0f, -s * 0.9f)    // tip
        path.lineTo(-s * 0.7f, s * 0.4f)  // bottom-left
        path.lineTo(s * 0.7f, s * 0.4f)   // bottom-right
        path.close()

        val matrix = Matrix()
        matrix.postRotate(rotation)
        matrix.postTranslate(tipX, tipY)
        path.transform(matrix)
    }

    private fun buildSector(path: Path, cx: Float, cy: Float, innerR: Float, outerR: Float, startAngle: Float, endAngle: Float) {
        path.reset()
        val rect = RectF(cx - outerR, cy - outerR, cx + outerR, cy + outerR)
        val innerRect = RectF(cx - innerR, cy - innerR, cx + innerR, cy + innerR)
        val sweep = endAngle - startAngle
        path.arcTo(rect, startAngle, sweep, true)
        path.arcTo(innerRect, endAngle, -sweep)
        path.close()
    }

    override fun onDraw(canvas: Canvas) {
        // ── Outer disc shadow ──
        discPaint.color = Color.parseColor("#0A0A18")
        discPaint.setShadowLayer(35f, 0f, 12f, Color.parseColor("#05050A"))
        canvas.drawCircle(cx, cy, discRadius, discPaint)
        discPaint.clearShadowLayer()

        // ── Disc radial gradient ──
        discPaint.shader = RadialGradient(
            cx - discRadius * 0.15f, cy - discRadius * 0.15f, discRadius * 1.4f,
            Color.parseColor("#252535"), Color.parseColor("#0D0D1A"), Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, discRadius, discPaint)
        discPaint.shader = null

        // ── Outer stroke ──
        canvas.drawCircle(cx, cy, discRadius, discStrokePaint)

        // ── Inner concentric rings ──
        canvas.drawCircle(cx, cy, discRadius * 0.60f, innerRingPaint)
        canvas.drawCircle(cx, cy, discRadius * 0.30f, innerRingPaint)

        // ── Separator lines (cross dividers, subtle) ──
        val diagR = discRadius * 0.92f
        val angle45 = Math.toRadians(45.0).toFloat()
        canvas.drawLine(cx + cos(angle45) * discRadius * 0.28f, cy + sin(angle45) * discRadius * 0.28f,
            cx + cos(angle45) * diagR, cy + sin(angle45) * diagR, separatorPaint)
        canvas.drawLine(cx - cos(angle45) * discRadius * 0.28f, cy + sin(angle45) * discRadius * 0.28f,
            cx - cos(angle45) * diagR, cy + sin(angle45) * diagR, separatorPaint)
        canvas.drawLine(cx + cos(angle45) * discRadius * 0.28f, cy - sin(angle45) * discRadius * 0.28f,
            cx + cos(angle45) * diagR, cy - sin(angle45) * diagR, separatorPaint)
        canvas.drawLine(cx - cos(angle45) * discRadius * 0.28f, cy - sin(angle45) * discRadius * 0.28f,
            cx - cos(angle45) * diagR, cy - sin(angle45) * diagR, separatorPaint)

        // ── Draw sector glow + arrows ──
        drawDirectionButton(canvas, "UP", upSector, upArrow, upColor, upScale)
        drawDirectionButton(canvas, "DOWN", downSector, downArrow, downColor, downScale)
        drawDirectionButton(canvas, "LEFT", leftSector, leftArrow, leftColor, leftScale)
        drawDirectionButton(canvas, "RIGHT", rightSector, rightArrow, rightColor, rightScale)

        // ── Center dot ──
        centerDotPaint.shader = RadialGradient(cx, cy, discRadius * 0.15f,
            Color.parseColor("#3A3A4A"), Color.parseColor("#1A1A2A"), Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, discRadius * 0.14f, centerDotPaint)
        centerDotPaint.shader = null
    }

    private fun drawDirectionButton(canvas: Canvas, dir: String, sector: Path, arrow: Path, color: Int, scale: Float) {
        val isPressed = pressedDirection == dir

        canvas.save()
        if (isPressed) {
            // Subtle scale toward that direction
            val pivotX = when (dir) { "LEFT" -> cx - discRadius * 0.5f; "RIGHT" -> cx + discRadius * 0.5f; else -> cx }
            val pivotY = when (dir) { "UP" -> cy - discRadius * 0.5f; "DOWN" -> cy + discRadius * 0.5f; else -> cy }
            canvas.scale(scale, scale, pivotX, pivotY)
        }

        if (isPressed) {
            // Sector glow
            glowPaint.color = Color.argb(60, Color.red(color), Color.green(color), Color.blue(color))
            glowPaint.setShadowLayer(30f, 0f, 0f, color)
            canvas.drawPath(sector, glowPaint)
            glowPaint.clearShadowLayer()
        }

        // Arrow fill
        if (isPressed) {
            arrowPaint.color = color
            arrowPaint.setShadowLayer(20f, 0f, 0f, color)
        } else {
            arrowPaint.color = Color.parseColor("#555570")
            arrowPaint.clearShadowLayer()
        }
        canvas.drawPath(arrow, arrowPaint)
        arrowPaint.clearShadowLayer()

        canvas.restore()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (tag == "editor_mode") return false

        val dx = event.x - cx
        val dy = event.y - cy
        val dist = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
        val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                var newDir = "CENTER"
                if (dist > discRadius * 0.15f) { // Deadzone
                    newDir = when {
                        angle in -45.0..45.0 -> "RIGHT"
                        angle in 45.0..135.0 -> "DOWN"
                        angle in -135.0..-45.0 -> "UP"
                        else -> "LEFT"
                    }
                }

                if (newDir != pressedDirection) {
                    // Release haptic for previous direction
                    if (pressedDirection != null && pressedDirection != "CENTER") {
                        haptics?.vibrate(HapticsManager.Type.RUBBER_RELEASE)
                        animateDirectionScale(pressedDirection!!, 1.0f)
                    }
                    pressedDirection = newDir
                    if (newDir != "CENTER") {
                        haptics?.vibrate(HapticsManager.Type.RUBBER_PRESS)
                        animateDirectionScale(newDir, 0.90f)
                    }
                    onDpadChanged?.invoke(newDir)
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (pressedDirection != null && pressedDirection != "CENTER") {
                    haptics?.vibrate(HapticsManager.Type.RUBBER_RELEASE)
                    animateDirectionScale(pressedDirection!!, 1.0f)
                }
                pressedDirection = null
                onDpadChanged?.invoke("CENTER")
                invalidate()
            }
        }
        return true
    }

    private fun animateDirectionScale(dir: String, target: Float) {
        val current = when (dir) { "UP" -> upScale; "DOWN" -> downScale; "LEFT" -> leftScale; else -> rightScale }
        ValueAnimator.ofFloat(current, target).apply {
            duration = 120
            interpolator = OvershootInterpolator(3f)
            addUpdateListener {
                val v = it.animatedValue as Float
                when (dir) { "UP" -> upScale = v; "DOWN" -> downScale = v; "LEFT" -> leftScale = v; "RIGHT" -> rightScale = v }
                invalidate()
            }
            start()
        }
    }
}
