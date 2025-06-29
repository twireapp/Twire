package com.perflyst.twire.misc

import android.graphics.Bitmap
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.ImageView
import androidx.core.graphics.drawable.RoundedBitmapDrawable
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory

/**
 * Created by Sebastian Rask on 24-06-2016.
 */
class RoundImageAnimation(
    private val fromRounded: Int,
    private val toRounded: Int,
    private val view: ImageView,
    imageBitmap: Bitmap?,
) : Animation() {
    var drawable: RoundedBitmapDrawable =
        RoundedBitmapDrawableFactory.create(view.resources, imageBitmap)

    init {
        view.setImageDrawable(drawable)
    }

    override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
        val rounded = (fromRounded + (toRounded - fromRounded) * interpolatedTime)
        drawable.setCornerRadius(rounded)
        view.requestLayout()
    }

    override fun willChangeBounds(): Boolean {
        return false
    }
}
