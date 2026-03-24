package com.kinetix.controller.v2.ui.components

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.kinetix.controller.v2.system.HapticsManager

class TriggerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var onValueChanged: ((Float) -> Unit)? = null
    var haptics: HapticsManager? = null
    
    // Simulate PS5 adaptive trigger resistance thresholds
    var resistanceThreshold = 0.5f
    
    private var value = 0f // 0.0 to 1.0
    private var pressed = false

    // Purple gradient accent
    private val fillColorStart = Color.parseColor("#B24BF3")
    private val fillColorEnd = Color.parseColor("#7B2FBE")

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#34344A"); style = Paint.Style.STROKE; strokeWidth = 5f }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    
    private val rect = RectF()
    private val fillRect = RectF()
    
    private var hapticTriggered = false

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        rect.set(5f, 5f, w - 5f, h - 5f)
    }

    override fun onDraw(canvas: Canvas) {
        val corner = width / 4f
        
        // Background with gradient
        basePaint.shader = LinearGradient(0f, 0f, 0f, height.toFloat(),
            Color.parseColor("#1A1A2E"), Color.parseColor("#0D0D1A"), Shader.TileMode.CLAMP)
        basePaint.setShadowLayer(15f, 0f, 5f, Color.parseColor("#05050A"))
        canvas.drawRoundRect(rect, corner, corner, basePaint)
        basePaint.clearShadowLayer()
        basePaint.shader = null

        canvas.drawRoundRect(rect, corner, corner, strokePaint)

        // Fill based on value — purple gradient
        if (value > 0.01f) {
            val fillTop = rect.bottom - (rect.height() * value)
            fillRect.set(rect.left, fillTop, rect.right, rect.bottom)

            fillPaint.shader = LinearGradient(rect.left, fillTop, rect.left, rect.bottom,
                fillColorStart, fillColorEnd, Shader.TileMode.CLAMP)
            
            // Glow when past threshold
            if (value >= resistanceThreshold) {
                fillPaint.setShadowLayer(20f, 0f, 0f, fillColorStart)
            }
            
            canvas.drawRoundRect(fillRect, corner, corner, fillPaint)
            fillPaint.clearShadowLayer()
            fillPaint.shader = null
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (tag == "editor_mode") return false
        
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                pressed = true
                val newValue = 1f - ((event.y - rect.top) / rect.height()).coerceIn(0f, 1f)
                
                if (newValue >= resistanceThreshold && value < resistanceThreshold) {
                    haptics?.vibrate(HapticsManager.Type.RUBBER_PRESS)
                    hapticTriggered = true
                }
                
                value = newValue
                onValueChanged?.invoke(value)
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                pressed = false
                hapticTriggered = false
                haptics?.vibrate(HapticsManager.Type.RUBBER_RELEASE)
                animateReturn()
            }
        }
        return true
    }
    
    private fun animateReturn() {
        ValueAnimator.ofFloat(value, 0f).apply {
            duration = 150
            addUpdateListener { 
                value = it.animatedValue as Float
                onValueChanged?.invoke(value)
                invalidate() 
            }
            start()
        }
    }
}
