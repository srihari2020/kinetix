package com.kinetix.controller.v2.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * Draws a subtle dotted grid background for the Editor screen.
 */
class EditorGridView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A1A2E")
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2A2A3E")
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        val spacing = 40f
        var x = 0f
        while (x < width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
            x += spacing
        }
        var y = 0f
        while (y < height) {
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
            y += spacing
        }
        // Dots at intersections
        x = 0f
        while (x < width) {
            y = 0f
            while (y < height) {
                canvas.drawCircle(x, y, 2f, dotPaint)
                y += spacing
            }
            x += spacing
        }
    }
}
