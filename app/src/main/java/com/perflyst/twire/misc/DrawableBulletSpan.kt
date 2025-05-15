package com.perflyst.twire.misc

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.Layout
import android.text.Spanned
import android.text.style.BulletSpan
import androidx.core.graphics.withTranslation

class DrawableBulletSpan(private val mGap: Int, private val mDrawable: Drawable) :
    BulletSpan(mGap) {
    val scaleFactor: Float = 0.65f

    override fun getLeadingMargin(first: Boolean): Int {
        return (mDrawable.intrinsicWidth * scaleFactor + mGap * 2).toInt()
    }

    override fun drawLeadingMargin(
        canvas: Canvas, paint: Paint, x: Int, dir: Int,
        top: Int, baseline: Int, bottom: Int,
        text: CharSequence, start: Int, end: Int,
        first: Boolean, layout: Layout?
    ) {
        var bottom = bottom
        if ((text as Spanned).getSpanStart(this) == start) {
            val style = paint.style

            paint.style = Paint.Style.FILL

            if (layout != null) {
                val metrics = paint.getFontMetrics()
                bottom = (baseline + metrics.bottom).toInt()
            }

            val xPosition = (x + mGap).toFloat()
            val yPosition = (top + bottom - mDrawable.intrinsicHeight * scaleFactor) / 2f

            canvas.withTranslation(xPosition, yPosition) {
                scale(scaleFactor, scaleFactor)
                mDrawable.draw(this)
            }

            paint.style = style
        }
    }
}
