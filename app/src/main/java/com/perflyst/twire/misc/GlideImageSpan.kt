package com.perflyst.twire.misc

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.rey.material.drawable.BlankDrawable
import timber.log.Timber
import kotlin.math.roundToInt

class GlideImageSpan(
    context: Context,
    url: String?,
    private val textView: TextView,
    assumedSize: Int,
    scale: Float
) : VerticalImageSpan(BlankDrawable()), Drawable.Callback {
    private var layerDrawable: LayerDrawable? = null

    private var mDrawable: Drawable? = null
    private var animatable: Animatable? = null

    constructor(
        context: Context,
        url: String?,
        textView: TextView,
        assumedSize: Int,
        scale: Float,
        backgroundColor: String?
    ) : this(context, url, textView, assumedSize, scale) {
        if (backgroundColor == null) return

        val backgroundDrawable = backgroundColor.toColorInt().toDrawable()

        this.layerDrawable = LayerDrawable(arrayOf<Drawable>(backgroundDrawable, BlankDrawable()))
        layerDrawable!!.setId(0, 0)
        layerDrawable!!.setId(1, 1)
    }

    init {
        val instance = this

        val scaledAssumedSize = (assumedSize / scale).roundToInt()

        val placeHolderDrawable: Drawable = Color.LTGRAY.toDrawable()
        placeHolderDrawable.setBounds(0, 0, scaledAssumedSize, scaledAssumedSize)

        val errorDrawable: Drawable = (-0x3334).toDrawable() // Reddish light gray
        errorDrawable.setBounds(0, 0, scaledAssumedSize, scaledAssumedSize)

        Glide
            .with(context)
            .load(url)
            .error(errorDrawable)
            .placeholder(placeHolderDrawable)
            .into(object : CustomTarget<Drawable?>() {
                override fun onLoadStarted(resource: Drawable?) {
                    mDrawable = resource
                }

                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable?>?
                ) {
                    resource.setBounds(
                        0,
                        0,
                        (resource.intrinsicWidth / scale).roundToInt(),
                        (resource.intrinsicHeight / scale).roundToInt()
                    )

                    if (resource is Animatable) {
                        animatable = resource as Animatable
                        resource.callback = instance

                        animatable!!.start()
                    }

                    mDrawable = resource

                    if (resource.intrinsicWidth != assumedSize) {
                        textView.text = textView.getText()
                        Timber.tag("EmoteShift")
                            .d("Got $resource.intrinsicWidth but assumed $assumedSize ($url)")
                    } else {
                        textView.invalidate()
                    }
                }


                override fun onLoadFailed(resource: Drawable?) {
                    mDrawable = resource
                    textView.invalidate()
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    if (animatable != null) {
                        animatable!!.stop()
                    }

                    mDrawable = placeholder
                    textView.invalidate()
                }
            })
    }

    override fun invalidateDrawable(drawable: Drawable) {
        textView.invalidate()
    }

    override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
        textView.postDelayed(what, `when`)
    }

    override fun unscheduleDrawable(who: Drawable, what: Runnable) {
        textView.removeCallbacks(what)
    }

    override fun getDrawable(): Drawable? {
        val drawable = (if (this.mDrawable != null) this.mDrawable else super.getDrawable())!!

        if (layerDrawable != null) {
            layerDrawable!!.getDrawable(0).bounds = drawable.getBounds()
            layerDrawable!!.bounds = drawable.getBounds()
            layerDrawable!!.setDrawableByLayerId(1, drawable)

            return layerDrawable
        }

        return drawable
    }
}
