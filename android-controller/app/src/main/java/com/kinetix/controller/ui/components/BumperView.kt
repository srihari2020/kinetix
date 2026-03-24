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

class BumperView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var onStateChanged: ((pressed: Boolean) -> Unit)? = null
    var haptics: HapticsManager? = null

    private var isPressed = false
    private var groupScale = 1.0f

    var label = "BUMP"

    // Teal gradient accent
    private val activeColorStart = Color.parseColor("#00D2FF")
    private val activeColorEnd = Color.parseColor("#0078A0")

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#34344A"); style = Paint.Style.STROKE; strokeWidth = 4f }
    private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 40f; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
    
    private val rect = RectF()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        rect.set(5f, 5f, w - 5f, h - 5f)
        textPaint.textSize = h * 0.38f
    }

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        canvas.scale(groupScale, groupScale, width/2f, height/2f)

        val corner = height / 2f

        // Base gradient
        basePaint.shader = LinearGradient(0f, 0f, width.toFloat(), 0f,
            Color.parseColor("#1A1A2E"), Color.parseColor("#151525"), Shader.TileMode.CLAMP)
        basePaint.setShadowLayer(15f, 0f, 5f, Color.parseColor("#05050A"))
        canvas.drawRoundRect(rect, corner, corner, basePaint)
        basePaint.clearShadowLayer()
        basePaint.shader = null
        
        if (isPressed) {
            // Teal gradient fill with glow
            activePaint.shader = LinearGradient(rect.left, rect.top, rect.right, rect.bottom,
                activeColorStart, activeColorEnd, Shader.TileMode.CLAMP)
            activePaint.setShadowLayer(25f, 0f, 0f, activeColorStart)
            canvas.drawRoundRect(rect, corner, corner, activePaint)
            activePaint.clearShadowLayer()
            activePaint.shader = null

            // Colored stroke when pressed
            strokePaint.color = Color.parseColor("#40FFFFFF")
        } else {
            strokePaint.color = Color.parseColor("#34344A")
        }
        
        canvas.drawRoundRect(rect, corner, corner, strokePaint)
        canvas.drawText(label, width/2f, height/2f - (textPaint.descent() + textPaint.ascent()) / 2, textPaint)

        canvas.restore()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (tag == "editor_mode") return false
        
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                if (!isPressed) {
                    isPressed = true
                    haptics?.vibrate(HapticsManager.Type.RUBBER_PRESS)
                    onStateChanged?.invoke(true)
                    animateScale(0.88f)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isPressed) {
                    isPressed = false
                    haptics?.vibrate(HapticsManager.Type.RUBBER_RELEASE)
                    onStateChanged?.invoke(false)
                    animateScale(1.0f)
                }
            }
        }
        return true
    }

    private fun animateScale(target: Float) {
        ValueAnimator.ofFloat(groupScale, target).apply {
            duration = 110
            interpolator = OvershootInterpolator(3f)
            addUpdateListener { 
                groupScale = it.animatedValue as Float
                invalidate() 
            }
            start()
        }
    }
}
