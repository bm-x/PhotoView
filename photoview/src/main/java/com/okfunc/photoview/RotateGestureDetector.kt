package com.okfunc.photoview

import android.view.MotionEvent

/**
 *
 * Created by q2366 on 2015/10/12.
 */
class RotateGestureDetector(val listener: RotateListener) {

    private var mPrevSlope = 0f
    private var mCurrSlope = 0f

    private var x1 = 0f
    private var y1 = 0f
    private var x2 = 0f
    private var y2 = 0f

    fun onTouchEvent(event: MotionEvent) {

        val Action = event.actionMasked

        when (Action) {
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_POINTER_UP -> if (event.pointerCount == 2) mPrevSlope = caculateSlope(event)
            MotionEvent.ACTION_MOVE -> if (event.pointerCount > 1) {
                mCurrSlope = caculateSlope(event)

                val currDegrees = Math.toDegrees(Math.atan(mCurrSlope.toDouble()))
                val prevDegrees = Math.toDegrees(Math.atan(mPrevSlope.toDouble()))

                val deltaSlope = currDegrees - prevDegrees

                if (Math.abs(deltaSlope) <= MAX_DEGREES_STEP) {
                    listener.onRotate(deltaSlope.toFloat(), (x2 + x1) / 2, (y2 + y1) / 2)
                }

                mPrevSlope = mCurrSlope
            }
        }
    }

    private fun caculateSlope(event: MotionEvent): Float {
        x1 = event.getX(0)
        y1 = event.getY(0)
        x2 = event.getX(1)
        y2 = event.getY(1)
        return (y2 - y1) / (x2 - x1)
    }

    companion object {
        val MAX_DEGREES_STEP = 120
    }
}