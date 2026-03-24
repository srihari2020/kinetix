package com.kinetix.controller.v2.system

import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Makes a view draggable and resizable in the editor.
 * Clean separation of drag vs resize via deferred mode detection:
 *   - ACTION_DOWN picks a CANDIDATE mode based on where the finger lands
 *   - Mode only COMMITS after the finger moves a minimum distance
 *   - This prevents accidental mode switches
 */
class DraggableElement(private val targetView: View, private val onLayoutChanged: () -> Unit) {

    private var initialTx = 0f
    private var initialTy = 0f
    private var touchX = 0f
    private var touchY = 0f

    private var scaleFactorX = 1.0f
    private var scaleFactorY = 1.0f

    // Mode detection: PENDING means we know what we WANT to do, but haven't committed yet
    private enum class DragMode { NONE, PENDING_MOVE, PENDING_RESIZE_X, PENDING_RESIZE_Y, MOVE, RESIZE_X, RESIZE_Y }
    private var dragMode = DragMode.NONE

    private var resizeStartX = 0f
    private var resizeStartY = 0f
    private var resizeInitialScaleX = 1.0f
    private var resizeInitialScaleY = 1.0f

    // Long press detection
    private val longPressHandler = Handler(Looper.getMainLooper())
    private var longPressTriggered = false
    private var downRawX = 0f
    private var downRawY = 0f
    private val longPressDuration = 500L
    private val moveCommitThreshold = 12f  // px before mode commits (prevents jitter)

    // Handle visual size and hit detection — TIGHT hit areas
    private val handleRadius = 32f
    private val handleHitRadius = 44f  // actual touch target (tight, only around the circle)
    private val handleColor = Color.parseColor("#00E5FF")
    private val handleColorY = Color.parseColor("#FF4081")

    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val handleStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 3f; color = Color.parseColor("#FFFFFF")
    }
    private val selectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 3f; color = Color.parseColor("#44FFFFFF")
        pathEffect = DashPathEffect(floatArrayOf(15f, 10f), 0f)
    }

    private val scaleGestureDetector = ScaleGestureDetector(targetView.context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val factor = detector.scaleFactor
            scaleFactorX = (scaleFactorX * factor).coerceIn(0.3f, 4.0f)
            scaleFactorY = (scaleFactorY * factor).coerceIn(0.3f, 4.0f)
            targetView.scaleX = scaleFactorX
            targetView.scaleY = scaleFactorY
            onLayoutChanged()
            return true
        }
        override fun onScaleEnd(detector: ScaleGestureDetector) { onLayoutChanged() }
    })

    private var overlayView: HandleOverlayView? = null

    fun attach() {
        val parent = targetView.parent as? android.view.ViewGroup
        if (parent != null && overlayView == null) {
            overlayView = HandleOverlayView(targetView.context).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                )
                element = this@DraggableElement
            }
        }

        targetView.setOnTouchListener { v, event ->
            if (event.pointerCount >= 2) {
                cancelLongPress()
                scaleGestureDetector.onTouchEvent(event)
                return@setOnTouchListener true
            }

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    longPressTriggered = false
                    downRawX = event.rawX
                    downRawY = event.rawY

                    // Store initial state for both move and resize
                    initialTx = v.translationX
                    initialTy = v.translationY
                    touchX = event.rawX
                    touchY = event.rawY
                    resizeStartX = event.rawX
                    resizeStartY = event.rawY
                    resizeInitialScaleX = v.scaleX
                    resizeInitialScaleY = v.scaleY

                    // Determine CANDIDATE mode based on touch position
                    val localX = event.x
                    val localY = event.y
                    val hitSq = handleHitRadius * handleHitRadius

                    val dxR = localX - v.width.toFloat()
                    val dyR = localY - v.height / 2f
                    val dxB = localX - v.width / 2f
                    val dyB = localY - v.height.toFloat()

                    dragMode = when {
                        (dxR * dxR + dyR * dyR) < hitSq -> DragMode.PENDING_RESIZE_X
                        (dxB * dxB + dyB * dyB) < hitSq -> DragMode.PENDING_RESIZE_Y
                        else -> DragMode.PENDING_MOVE
                    }

                    // Long press only for move candidates
                    if (dragMode == DragMode.PENDING_MOVE) {
                        scheduleLongPress(v)
                    }

                    v.bringToFront()
                }

                MotionEvent.ACTION_MOVE -> {
                    if (longPressTriggered) return@setOnTouchListener true

                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    val dist = sqrt(dx * dx + dy * dy)

                    // Cancel long press if moved
                    if (dist > moveCommitThreshold) {
                        cancelLongPress()
                    }

                    // Commit pending mode once past threshold
                    if (dist > moveCommitThreshold) {
                        when (dragMode) {
                            DragMode.PENDING_MOVE -> dragMode = DragMode.MOVE
                            DragMode.PENDING_RESIZE_X -> dragMode = DragMode.RESIZE_X
                            DragMode.PENDING_RESIZE_Y -> dragMode = DragMode.RESIZE_Y
                            else -> {} // already committed
                        }
                    }

                    // Execute committed mode
                    when (dragMode) {
                        DragMode.MOVE -> {
                            v.translationX = initialTx + (event.rawX - touchX)
                            v.translationY = initialTy + (event.rawY - touchY)
                            onLayoutChanged()
                        }
                        DragMode.RESIZE_X -> {
                            val delta = event.rawX - resizeStartX
                            val sensitivity = 200f
                            val newScale = (resizeInitialScaleX + delta / sensitivity).coerceIn(0.3f, 4.0f)
                            scaleFactorX = newScale
                            v.scaleX = newScale
                            onLayoutChanged()
                        }
                        DragMode.RESIZE_Y -> {
                            val delta = event.rawY - resizeStartY
                            val sensitivity = 200f
                            val newScale = (resizeInitialScaleY + delta / sensitivity).coerceIn(0.3f, 4.0f)
                            scaleFactorY = newScale
                            v.scaleY = newScale
                            onLayoutChanged()
                        }
                        else -> {} // still pending, don't move yet
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    cancelLongPress()
                    dragMode = DragMode.NONE
                    onLayoutChanged()
                }
            }
            true
        }
    }

    private fun scheduleLongPress(v: View) {
        longPressHandler.postDelayed({
            longPressTriggered = true
            v.performLongClick()
        }, longPressDuration)
    }

    private fun cancelLongPress() {
        longPressHandler.removeCallbacksAndMessages(null)
    }

    fun detach() {
        cancelLongPress()
        targetView.setOnTouchListener(null)
    }

    fun drawHandles(canvas: Canvas) {
        val v = targetView
        val left = v.left + v.translationX
        val top = v.top + v.translationY
        val w = v.width * v.scaleX
        val h = v.height * v.scaleY
        val cx = left + v.width / 2f
        val cy = top + v.height / 2f
        val l = cx - w / 2f
        val t = cy - h / 2f

        // Selection border
        canvas.drawRect(l, t, l + w, t + h, selectionPaint)

        // Right-center handle (scaleX) — cyan
        handlePaint.color = handleColor
        handlePaint.setShadowLayer(12f, 0f, 0f, handleColor)
        canvas.drawCircle(l + w, cy, handleRadius, handlePaint)
        canvas.drawCircle(l + w, cy, handleRadius, handleStrokePaint)
        handlePaint.clearShadowLayer()
        drawArrowIcon(canvas, l + w, cy, true)

        // Bottom-center handle (scaleY) — pink
        handlePaint.color = handleColorY
        handlePaint.setShadowLayer(12f, 0f, 0f, handleColorY)
        canvas.drawCircle(cx, t + h, handleRadius, handlePaint)
        canvas.drawCircle(cx, t + h, handleRadius, handleStrokePaint)
        handlePaint.clearShadowLayer()
        drawArrowIcon(canvas, cx, t + h, false)
    }

    private fun drawArrowIcon(canvas: Canvas, cx: Float, cy: Float, horizontal: Boolean) {
        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; style = Paint.Style.FILL
        }
        val size = handleRadius * 0.45f
        val path = Path()
        if (horizontal) {
            path.moveTo(cx - size, cy)
            path.lineTo(cx - size * 0.3f, cy - size * 0.5f)
            path.lineTo(cx - size * 0.3f, cy + size * 0.5f)
            path.close()
            path.moveTo(cx + size, cy)
            path.lineTo(cx + size * 0.3f, cy - size * 0.5f)
            path.lineTo(cx + size * 0.3f, cy + size * 0.5f)
            path.close()
        } else {
            path.moveTo(cx, cy - size)
            path.lineTo(cx - size * 0.5f, cy - size * 0.3f)
            path.lineTo(cx + size * 0.5f, cy - size * 0.3f)
            path.close()
            path.moveTo(cx, cy + size)
            path.lineTo(cx - size * 0.5f, cy + size * 0.3f)
            path.lineTo(cx + size * 0.5f, cy + size * 0.3f)
            path.close()
        }
        canvas.drawPath(path, iconPaint)
    }

    fun applyState(tx: Float, ty: Float, scaleX: Float, scaleY: Float) {
        targetView.translationX = tx
        targetView.translationY = ty
        targetView.scaleX = scaleX
        targetView.scaleY = scaleY
        scaleFactorX = scaleX
        scaleFactorY = scaleY
    }

    class HandleOverlayView(context: android.content.Context) : View(context) {
        var element: DraggableElement? = null
        override fun onDraw(canvas: Canvas) { element?.drawHandles(canvas) }
    }
}
