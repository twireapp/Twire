package com.perflyst.twire.misc

import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation

/**
 * Created by SebastianRask on 24-01-2016.
 */
class ResizeHeightAnimation(private val view: View, private val targetHeight: Int) : Animation() {
    private val startHeight: Int = view.height

    override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
        view.layoutParams.height =
            (startHeight + (targetHeight - startHeight) * interpolatedTime).toInt()
        view.requestLayout()
    }

    override fun willChangeBounds(): Boolean {
        return true
    }
}
