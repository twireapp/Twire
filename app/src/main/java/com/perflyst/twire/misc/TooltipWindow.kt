package com.perflyst.twire.misc

import android.app.ActionBar
import android.content.Context
import android.graphics.Rect
import android.os.Handler
import android.os.Message
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import com.perflyst.twire.R
import io.codetail.animation.SupportAnimator
import io.codetail.animation.ViewAnimationUtils

/*
* Created by Sebastian Rask on 10-05-2016.
*/

class TooltipWindow(ctx: Context?, private val position: Int) {
    private val REVEAL_DURATION = 500
    private val tipWindow: PopupWindow? = PopupWindow(ctx)
    private val contentView: View = LayoutInflater.from(ctx).inflate(R.layout.tooltip_layout, null)
    private val mTipText: TextView = contentView.findViewById(R.id.tooltip_text)
    private val mNavLeftArrow: ImageView = contentView.findViewById(R.id.tooltip_nav_left)
    private val mNavUpArrow: ImageView = contentView.findViewById(R.id.tooltip_nav_up)
    private val mainLayout: LinearLayout = contentView.findViewById(R.id.main_layout)
    private var revealTransition: SupportAnimator? = null
    private val handler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            if (msg.what == MSG_DISMISS_TOOLTIP) {
                if (tipWindow != null && tipWindow.isShowing) {
                    if (revealTransition != null) {
                        val hideAnim = revealTransition!!.reverse()
                        if (hideAnim != null) {
                            hideAnim.interpolator = AccelerateDecelerateInterpolator()
                            hideAnim.duration = REVEAL_DURATION.toLong()
                            hideAnim.addListener(object : SupportAnimator.AnimatorListener {
                                override fun onAnimationStart() {
                                }

                                override fun onAnimationEnd() {
                                    if (tipWindow != null) {
                                        tipWindow.dismiss()
                                    }
                                }

                                override fun onAnimationCancel() {
                                }

                                override fun onAnimationRepeat() {
                                }
                            })
                            hideAnim.start()
                        } else {
                            if (tipWindow != null) {
                                tipWindow.dismiss()
                            }
                        }
                    } else {
                        tipWindow.dismiss()
                    }
                }
            }
        }
    }

    fun showToolTip(anchor: View, tipText: String?) {
        mTipText.text = tipText

        tipWindow!!.height = ActionBar.LayoutParams.WRAP_CONTENT
        tipWindow.width = ActionBar.LayoutParams.WRAP_CONTENT

        tipWindow.isOutsideTouchable = true
        tipWindow.isTouchable = true

        tipWindow.setBackgroundDrawable(null)

        tipWindow.setContentView(contentView)

        tipWindow.elevation = 1f

        val screen_pos = IntArray(2)
        anchor.getLocationOnScreen(screen_pos)

        // Get rect for anchor view
        val anchor_rect = Rect(
            screen_pos[0], screen_pos[1], screen_pos[0]
                    + anchor.width, screen_pos[1] + anchor.height
        )

        // Call view measure to calculate how big your view should be.
        contentView.measure(
            ActionBar.LayoutParams.WRAP_CONTENT,
            ActionBar.LayoutParams.WRAP_CONTENT
        )

        var position_x = 0
        var position_y = 0
        if (position == POSITION_TO_RIGHT) {
            position_x = anchor_rect.right
            position_y = anchor.y.toInt() + anchor.height / 2
        } else if (position == POSITION_BOTTOM) {
            mNavUpArrow.setVisibility(View.VISIBLE)
            mNavLeftArrow.setVisibility(View.GONE)
            mTipText.forceLayout()

            position_x =
                anchor_rect.centerX() - contentView.measuredWidth / 2 //anchor_rect.centerX() - (contentView.getWidth() / 2);
            position_y = anchor_rect.bottom - anchor_rect.height() / 2
        }

        tipWindow.showAtLocation(
            anchor, Gravity.NO_GRAVITY, position_x,
            position_y
        )


        contentView.addOnLayoutChangeListener { v: View?, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int ->
            revealTransition = ViewAnimationUtils.createCircularReveal(
                mainLayout,
                mainLayout.x.toInt(),
                (mainLayout.y + mainLayout.height / 2).toInt(),
                0f,
                mainLayout.width.toFloat()
            )
            revealTransition!!.interpolator = AccelerateDecelerateInterpolator()
            revealTransition!!.duration = REVEAL_DURATION.toLong()
            revealTransition!!.addListener(object : SupportAnimator.AnimatorListener {
                override fun onAnimationStart() {
                    mainLayout.visibility = View.VISIBLE
                }

                override fun onAnimationEnd() {
                }

                override fun onAnimationCancel() {
                }

                override fun onAnimationRepeat() {
                }
            })
            revealTransition!!.start()
        }

        val hideDelay = 1000 * 5
        handler.sendEmptyMessageDelayed(MSG_DISMISS_TOOLTIP, hideDelay.toLong())
    }

    val isTooltipShown: Boolean
        get() = tipWindow != null && tipWindow.isShowing

    fun dismissTooltip() {
        if (tipWindow != null && tipWindow.isShowing) tipWindow.dismiss()
    }

    companion object {
        const val POSITION_TO_RIGHT: Int = 0
        const val POSITION_BOTTOM: Int = 1
        private const val MSG_DISMISS_TOOLTIP = 100
    }
}
