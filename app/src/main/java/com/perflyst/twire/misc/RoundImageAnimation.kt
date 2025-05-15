package com.perflyst.twire.misc

import android.graphics.Bitmap
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.ImageView
import com.perflyst.twire.service.Service

/**
 * Created by Sebastian Rask on 24-06-2016.
 */
class RoundImageAnimation(
    private val fromRounded: Int,
    private val toRounded: Int,
    private val view: ImageView,
    private val imageBitmap: Bitmap?
) : Animation() {
    override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
        val rounded = (fromRounded + (toRounded - fromRounded) * interpolatedTime).toInt()
        view.setImageBitmap(Service.getRoundedCornerBitmap(imageBitmap, rounded))
        view.requestLayout()
    }

    override fun willChangeBounds(): Boolean {
        return false
    }
}
