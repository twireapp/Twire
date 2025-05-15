package com.perflyst.twire.misc

import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation

/**
 * Created by SebastianRask on 24-01-2016.
 */
class ResizeWidthAnimation(private val view: View, private val targetWidth: Int) : Animation() {
    private val startWidth: Int = view.width

    override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
        view.layoutParams.width =
            (startWidth + (targetWidth - startWidth) * interpolatedTime).toInt()
        view.requestLayout()
    }

    override fun willChangeBounds(): Boolean {
        return true
    }
}
