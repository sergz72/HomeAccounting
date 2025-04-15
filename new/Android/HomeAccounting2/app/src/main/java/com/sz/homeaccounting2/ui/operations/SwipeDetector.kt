package com.sz.homeaccounting2.ui.operations

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class SwipeDetector(private val minDistance: Int, private val listener: OnSwipeListener): View.OnTouchListener {
    interface OnSwipeListener {
        fun onSwipe(v: View?, a: Action)
    }

    enum class Action {
        LR, // Left to Right
        RL, // Right to Left
        TB, // Top to bottom
        BT, // Bottom to Top
    }

    private var mDownX: Float = 0f
    private var mDownY: Float = 0f

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        when (event!!.action) {
            MotionEvent.ACTION_DOWN -> {
                mDownX = event.x
                mDownY = event.y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = mDownX - event.x
                val deltaY = mDownY - event.y
                if (abs(deltaX) > minDistance) {
                    if (deltaX < 0) {
                        listener.onSwipe(v, Action.LR)
                    }
                    if (deltaX > 0) {
                        listener.onSwipe(v, Action.RL)
                    }
                } else if (abs(deltaY) > minDistance) {
                    if (deltaY < 0) {
                        listener.onSwipe(v, Action.TB)
                    }
                    if (deltaY > 0) {
                        listener.onSwipe(v, Action.BT)
                    }
                }
                return true
            }
        }
        return false
    }
}