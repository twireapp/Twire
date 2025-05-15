package com.perflyst.twire.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.core.widget.NestedScrollView
import kotlin.math.abs

/**
 * Created by Sebastian Rask on 24-02-2017.
 */
class FlingingNestedScrollView : NestedScrollView {
    @Suppress("unused")
    private var slop = 0

    @Suppress("unused")
    private val mInitialMotionX = 0f

    @Suppress("unused")
    private val mInitialMotionY = 0f
    private var xDistance = 0f
    private var yDistance = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var mOnScrollChangedListener: OnScrollChangedListener? = null

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(
        context: Context, attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    private fun init(context: Context) {
        val config = ViewConfiguration.get(context)
        slop = config.scaledEdgeSlop
    }

    @Suppress("unused")
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        ev.x
        ev.y
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                run {
                    yDistance = 0f
                    xDistance = yDistance
                }
                lastX = ev.x
                lastY = ev.y
                // This is very important line that fixes
                computeScroll()
            }

            MotionEvent.ACTION_MOVE -> {
                val curX = ev.x
                val curY = ev.y
                xDistance += abs(curX - lastX)
                yDistance += abs(curY - lastY)
                lastX = curX
                lastY = curY
                if (xDistance > yDistance) {
                    return false
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    fun setOnScrollChangedListener(listener: OnScrollChangedListener?) {
        mOnScrollChangedListener = listener
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        if (mOnScrollChangedListener != null) {
            mOnScrollChangedListener!!.onScrollChanged(this, l, t, oldl, oldt)
        }
    }

    interface OnScrollChangedListener {
        fun onScrollChanged(
            who: NestedScrollView?, l: Int, t: Int, oldl: Int,
            oldt: Int
        )
    }
}
