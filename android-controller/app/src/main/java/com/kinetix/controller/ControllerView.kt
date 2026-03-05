package com.kinetix.controller

import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

/**
 * Full gamepad overlay – draws and handles touch for:
 *   A / B / X / Y,  LB / RB,  LT / RT,  Start / Select,  D-pad
 *
 * The two analog sticks are separate [JoystickView] instances in the layout.
 */
class ControllerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    // ── Callback ─────────────────────────────────────────────────────
    var onStateChanged: ((ButtonState) -> Unit)? = null

    var layoutOverrides = mutableMapOf<String, FloatArray>()
        set(value) {
            field = value.toMutableMap()
            if (width > 0 && height > 0) {
                buttons.clear()
                dpadButtons.clear()
                layoutButtons(width.toFloat(), height.toFloat())
                invalidate()
            }
        }
    var isEditMode = false
    var onLayoutChanged: ((Map<String, FloatArray>) -> Unit)? = null
    private var draggedButton: String? = null

    data class ButtonState(
        val a: Boolean = false,
        val b: Boolean = false,
        val x: Boolean = false,
        val y: Boolean = false,
        val lb: Boolean = false,
        val rb: Boolean = false,
        val lt: Float = 0f,
        val rt: Float = 0f,
        val start: Boolean = false,
        val select: Boolean = false,
        val dpad: String = "none"
    )

    // ── Internal button model ────────────────────────────────────────
    private data class GameButton(
        val id: String,
        val label: String,
        var rect: RectF = RectF(),
        var pressed: Boolean = false,
        val color: Int,
        val pressedColor: Int,
        val textColor: Int = Color.WHITE,
        val isCircle: Boolean = false,
        val isRound: Boolean = true
    )

    // ── Buttons ──────────────────────────────────────────────────────
    private val buttons = mutableListOf<GameButton>()
    private val dpadButtons = mutableListOf<GameButton>()

    // Trigger state (analog)
    private var ltValue = 0f
    private var rtValue = 0f
    private var ltRect = RectF()
    private var rtRect = RectF()
    private var ltPressed = false
    private var rtPressed = false

    // Touch tracking (support multi-touch)
    private val touchMap = mutableMapOf<Int, MutableSet<String>>()  // pointerId → set of pressed ids

    // ── Vibration ────────────────────────────────────────────────────
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    // ── Paints ───────────────────────────────────────────────────────
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.TRANSPARENT
    }
    private val btnPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        color = Color.WHITE
    }
    private val triggerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A1A2E")
    }
    private val triggerFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E94560")
    }
    private val triggerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        color = Color.WHITE
        textSize = 28f
    }

    // ── Colours ──────────────────────────────────────────────────────
    private val colGreen      = Color.parseColor("#2ECC71")
    private val colGreenDark  = Color.parseColor("#27AE60")
    private val colRed        = Color.parseColor("#E74C3C")
    private val colRedDark    = Color.parseColor("#C0392B")
    private val colBlue       = Color.parseColor("#3498DB")
    private val colBlueDark   = Color.parseColor("#2980B9")
    private val colYellow     = Color.parseColor("#F1C40F")
    private val colYellowDark = Color.parseColor("#F39C12")
    private val colBumper     = Color.parseColor("#2C2C54")
    private val colBumperDark = Color.parseColor("#474787")
    private val colMenu       = Color.parseColor("#2C2C3A")
    private val colMenuDark   = Color.parseColor("#3A3A5E")
    private val colDpad       = Color.parseColor("#1E1E3A")
    private val colDpadDark   = Color.parseColor("#3A3A6E")

    // ── Layout ───────────────────────────────────────────────────────

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)
        buttons.clear()
        dpadButtons.clear()
        layoutButtons(w.toFloat(), h.toFloat())
    }

    private fun layoutButtons(w: Float, h: Float) {
        // Face buttons (ABXY) – right side, diamond layout
        val fbCx = w * 0.82f
        val fbCy = h * 0.52f
        val fbR = min(w, h) * 0.065f
        val fbSpacing = fbR * 2.6f

        buttons += GameButton("a", "A", circleRect(fbCx, fbCy + fbSpacing, fbR), color = colGreen, pressedColor = colGreenDark, isCircle = true)
        buttons += GameButton("b", "B", circleRect(fbCx + fbSpacing, fbCy, fbR), color = colRed, pressedColor = colRedDark, isCircle = true)
        buttons += GameButton("x", "X", circleRect(fbCx - fbSpacing, fbCy, fbR), color = colBlue, pressedColor = colBlueDark, isCircle = true)
        buttons += GameButton("y", "Y", circleRect(fbCx, fbCy - fbSpacing, fbR), color = colYellow, pressedColor = colYellowDark, textColor = Color.parseColor("#333333"), isCircle = true)

        // Bumpers (LB / RB)
        val bumpW = w * 0.13f
        val bumpH = h * 0.09f
        val bumpY = h * 0.02f
        buttons += GameButton("lb", "LB", RectF(w * 0.05f, bumpY, w * 0.05f + bumpW, bumpY + bumpH), color = colBumper, pressedColor = colBumperDark)
        buttons += GameButton("rb", "RB", RectF(w * 0.82f, bumpY, w * 0.82f + bumpW, bumpY + bumpH), color = colBumper, pressedColor = colBumperDark)

        // Triggers (LT / RT) – drawn as analog bars below bumpers
        val trigW = w * 0.13f
        val trigH = h * 0.16f
        val trigY = bumpY + bumpH + h * 0.02f
        ltRect = RectF(w * 0.05f, trigY, w * 0.05f + trigW, trigY + trigH)
        rtRect = RectF(w * 0.82f, trigY, w * 0.82f + trigW, trigY + trigH)

        // Start / Select
        val menuBtnW = w * 0.08f
        val menuBtnH = h * 0.065f
        val menuY = h * 0.42f
        buttons += GameButton("select", "⏪", RectF(w * 0.39f, menuY, w * 0.39f + menuBtnW, menuY + menuBtnH), color = colMenu, pressedColor = colMenuDark)
        buttons += GameButton("start", "⏩", RectF(w * 0.53f, menuY, w * 0.53f + menuBtnW, menuY + menuBtnH), color = colMenu, pressedColor = colMenuDark)

        // D-pad – left side
        val dpCx = w * 0.18f
        val dpCy = h * 0.68f
        val dpSize = min(w, h) * 0.075f
        val dpGap = dpSize * 0.15f

        dpadButtons += GameButton("dpad_up", "▲",
            RectF(dpCx - dpSize, dpCy - dpSize * 2 - dpGap, dpCx + dpSize, dpCy - dpGap),
            color = colDpad, pressedColor = colDpadDark)
        dpadButtons += GameButton("dpad_down", "▼",
            RectF(dpCx - dpSize, dpCy + dpGap, dpCx + dpSize, dpCy + dpSize * 2 + dpGap),
            color = colDpad, pressedColor = colDpadDark)
        dpadButtons += GameButton("dpad_left", "◀",
            RectF(dpCx - dpSize * 2 - dpGap, dpCy - dpSize, dpCx - dpGap, dpCy + dpSize),
            color = colDpad, pressedColor = colDpadDark)
        dpadButtons += GameButton("dpad_right", "▶",
            RectF(dpCx + dpGap, dpCy - dpSize, dpCx + dpSize * 2 + dpGap, dpCy + dpSize),
            color = colDpad, pressedColor = colDpadDark)

        // Apply overrides
        for (btn in buttons + dpadButtons) {
            layoutOverrides[btn.id]?.let { override ->
                val newCx = w * override[0]
                val newCy = h * override[1]
                val dx = newCx - btn.rect.centerX()
                val dy = newCy - btn.rect.centerY()
                btn.rect.offset(dx, dy)
            }
        }
        layoutOverrides["lt_zone"]?.let { override ->
            val newCx = w * override[0]
            val newCy = h * override[1]
            ltRect.offset(newCx - ltRect.centerX(), newCy - ltRect.centerY())
        }
        layoutOverrides["rt_zone"]?.let { override ->
            val newCx = w * override[0]
            val newCy = h * override[1]
            rtRect.offset(newCx - rtRect.centerX(), newCy - rtRect.centerY())
        }
    }

    private fun circleRect(cx: Float, cy: Float, r: Float) =
        RectF(cx - r, cy - r, cx + r, cy + r)

    // ── Drawing ──────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Face, bump, menu buttons
        for (btn in buttons) drawButton(canvas, btn)

        // D-pad
        for (btn in dpadButtons) drawButton(canvas, btn)

        // Triggers (analog bars)
        drawTrigger(canvas, ltRect, ltValue, "LT", ltPressed)
        drawTrigger(canvas, rtRect, rtValue, "RT", rtPressed)
    }

    private fun drawButton(canvas: Canvas, btn: GameButton) {
        btnPaint.color = if (btn.pressed) btn.pressedColor else btn.color

        if (btn.isCircle) {
            val cx = btn.rect.centerX()
            val cy = btn.rect.centerY()
            val r = btn.rect.width() / 2f

            // Shadow
            btnPaint.setShadowLayer(if (btn.pressed) 2f else 6f, 0f, 3f, Color.parseColor("#40000000"))
            canvas.drawCircle(cx, cy, r, btnPaint)
            btnPaint.clearShadowLayer()

            // Glow ring
            if (btn.pressed) {
                val glowP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE; strokeWidth = 3f; color = btn.color
                    maskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.NORMAL)
                }
                canvas.drawCircle(cx, cy, r + 3f, glowP)
            }

            textPaint.color = btn.textColor
            textPaint.textSize = r * 0.9f
            canvas.drawText(btn.label, cx, cy + textPaint.textSize * 0.33f, textPaint)
        } else {
            val cornerR = btn.rect.height() * 0.35f
            btnPaint.setShadowLayer(if (btn.pressed) 1f else 4f, 0f, 2f, Color.parseColor("#40000000"))
            canvas.drawRoundRect(btn.rect, cornerR, cornerR, btnPaint)
            btnPaint.clearShadowLayer()

            textPaint.color = btn.textColor
            textPaint.textSize = btn.rect.height() * 0.5f
            canvas.drawText(btn.label, btn.rect.centerX(), btn.rect.centerY() + textPaint.textSize * 0.33f, textPaint)
        }
    }

    private fun drawTrigger(canvas: Canvas, rect: RectF, value: Float, label: String, pressed: Boolean) {
        val cornerR = rect.height() * 0.15f
        canvas.drawRoundRect(rect, cornerR, cornerR, triggerBgPaint)

        // Filled portion
        val fillRect = RectF(rect.left, rect.top + rect.height() * (1f - value), rect.right, rect.bottom)
        if (value > 0.01f) {
            triggerFillPaint.alpha = (180 + 75 * value).toInt()
            canvas.drawRoundRect(fillRect, cornerR, cornerR, triggerFillPaint)
        }

        // Border
        val borderP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 2f
            color = if (pressed) Color.parseColor("#E94560") else Color.parseColor("#3A3A5E")
        }
        canvas.drawRoundRect(rect, cornerR, cornerR, borderP)

        // Label
        triggerTextPaint.textSize = rect.width() * 0.3f
        canvas.drawText(label, rect.centerX(), rect.centerY() + triggerTextPaint.textSize * 0.35f, triggerTextPaint)
    }

    // ── Touch handling ───────────────────────────────────────────────

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isEditMode) return handleEditTouch(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val idx = event.actionIndex
                val pid = event.getPointerId(idx)
                val tx = event.getX(idx)
                val ty = event.getY(idx)
                val hits = hitTest(tx, ty)
                touchMap[pid] = hits.toMutableSet()
                applyPresses()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val pid = event.getPointerId(i)
                    val tx = event.getX(i)
                    val ty = event.getY(i)

                    // Update trigger analog values
                    if (ltRect.contains(tx, ty)) {
                        ltValue = ((ltRect.bottom - ty) / ltRect.height()).coerceIn(0f, 1f)
                        ltPressed = true
                    }
                    if (rtRect.contains(tx, ty)) {
                        rtValue = ((rtRect.bottom - ty) / rtRect.height()).coerceIn(0f, 1f)
                        rtPressed = true
                    }

                    val hits = hitTest(tx, ty)
                    touchMap[pid] = hits.toMutableSet()
                }
                applyPresses()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                touchMap.clear()
                ltValue = 0f; rtValue = 0f; ltPressed = false; rtPressed = false
                applyPresses()
                return true
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val idx = event.actionIndex
                val pid = event.getPointerId(idx)
                touchMap.remove(pid)

                // Check if this pointer was the one controlling a trigger
                val tx = event.getX(idx)
                val ty = event.getY(idx)
                if (ltRect.contains(tx, ty)) { ltValue = 0f; ltPressed = false }
                if (rtRect.contains(tx, ty)) { rtValue = 0f; rtPressed = false }

                applyPresses()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun handleEditTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val hits = hitTest(event.x, event.y)
                draggedButton = hits.firstOrNull()
                return draggedButton != null
            }
            MotionEvent.ACTION_MOVE -> {
                draggedButton?.let { id ->
                    val btn = (buttons + dpadButtons).find { it.id == id }
                    if (btn != null) {
                        val dx = event.x - btn.rect.centerX()
                        val dy = event.y - btn.rect.centerY()
                        btn.rect.offset(dx, dy)
                    } else if (id == "lt_zone") {
                        val dx = event.x - ltRect.centerX()
                        val dy = event.y - ltRect.centerY()
                        ltRect.offset(dx, dy)
                    } else if (id == "rt_zone") {
                        val dx = event.x - rtRect.centerX()
                        val dy = event.y - rtRect.centerY()
                        rtRect.offset(dx, dy)
                    }
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                draggedButton?.let { id ->
                    val cx: Float
                    val cy: Float
                    val btn = (buttons + dpadButtons).find { it.id == id }
                    if (btn != null) {
                        cx = btn.rect.centerX()
                        cy = btn.rect.centerY()
                    } else if (id == "lt_zone") {
                        cx = ltRect.centerX()
                        cy = ltRect.centerY()
                    } else if (id == "rt_zone") {
                        cx = rtRect.centerX()
                        cy = rtRect.centerY()
                    } else {
                        return true
                    }
                    layoutOverrides[id] = floatArrayOf(cx / width, cy / height)
                    onLayoutChanged?.invoke(layoutOverrides)
                    draggedButton = null
                }
            }
        }
        return true
    }

    private fun hitTest(x: Float, y: Float): List<String> {
        val result = mutableListOf<String>()
        for (btn in buttons) {
            if (btn.isCircle) {
                val cx = btn.rect.centerX()
                val cy = btn.rect.centerY()
                val r = btn.rect.width() / 2f * 1.3f  // slightly generous
                if (hypot(x - cx, y - cy) <= r) result += btn.id
            } else {
                val expanded = RectF(btn.rect).apply { inset(-12f, -12f) }
                if (expanded.contains(x, y)) result += btn.id
            }
        }
        for (btn in dpadButtons) {
            val expanded = RectF(btn.rect).apply { inset(-8f, -8f) }
            if (expanded.contains(x, y)) result += btn.id
        }
        // Triggers
        if (ltRect.contains(x, y)) result += "lt_zone"
        if (rtRect.contains(x, y)) result += "rt_zone"
        return result
    }

    private fun applyPresses() {
        val allPressed = touchMap.values.flatten().toSet()

        var changed = false
        for (btn in buttons) {
            val nowPressed = btn.id in allPressed
            if (btn.pressed != nowPressed) { btn.pressed = nowPressed; changed = true }
        }
        for (btn in dpadButtons) {
            val nowPressed = btn.id in allPressed
            if (btn.pressed != nowPressed) { btn.pressed = nowPressed; changed = true }
        }

        // Haptic on new presses
        if (changed) {
            vibrate()
        }

        // D-pad decode
        val dpadStr = buildDpadString()

        // Build state and fire callback
        onStateChanged?.invoke(ButtonState(
            a = buttons.find { it.id == "a" }?.pressed == true,
            b = buttons.find { it.id == "b" }?.pressed == true,
            x = buttons.find { it.id == "x" }?.pressed == true,
            y = buttons.find { it.id == "y" }?.pressed == true,
            lb = buttons.find { it.id == "lb" }?.pressed == true,
            rb = buttons.find { it.id == "rb" }?.pressed == true,
            lt = ltValue,
            rt = rtValue,
            start = buttons.find { it.id == "start" }?.pressed == true,
            select = buttons.find { it.id == "select" }?.pressed == true,
            dpad = dpadStr
        ))

        invalidate()
    }

    private fun buildDpadString(): String {
        val up = dpadButtons.find { it.id == "dpad_up" }?.pressed == true
        val down = dpadButtons.find { it.id == "dpad_down" }?.pressed == true
        val left = dpadButtons.find { it.id == "dpad_left" }?.pressed == true
        val right = dpadButtons.find { it.id == "dpad_right" }?.pressed == true

        return when {
            up && left -> "up-left"
            up && right -> "up-right"
            down && left -> "down-left"
            down && right -> "down-right"
            up -> "up"
            down -> "down"
            left -> "left"
            right -> "right"
            else -> "none"
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator?.vibrate(20)
        }
    }
}
