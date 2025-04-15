package com.sz.homeaccounting2.ui.operations

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class ExpandIndicator(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    private var isExpanded: Boolean? = false
    private val mIndicatorPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        style = Paint.Style.FILL
        strokeWidth = 10.0F
    }

    fun setExpanded(expanded: Boolean?) {
        isExpanded = expanded
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isExpanded != null) {
            val w2 = (width / 2).toFloat()
            val h = height.toFloat()
            val start = w2 - h / 2
            val end = w2 + h / 2
            if (isExpanded!!) {
                canvas.drawLine(start, h, w2, 0f, mIndicatorPaint)
                canvas.drawLine(w2, 0f, end, h, mIndicatorPaint)
            } else {
                canvas.drawLine(start, 0f, w2, h, mIndicatorPaint)
                canvas.drawLine(w2, h, end, 0f, mIndicatorPaint)
            }
        }
    }
}