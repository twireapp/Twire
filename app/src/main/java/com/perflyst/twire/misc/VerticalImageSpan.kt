package com.perflyst.twire.misc

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.FontMetricsInt
import android.graphics.drawable.Drawable
import android.text.style.ImageSpan
import androidx.core.graphics.withTranslation

open class VerticalImageSpan internal constructor(drawable: Drawable) : ImageSpan(drawable) {
    private var yOffset = 0

    override fun getSize(
        paint: Paint, text: CharSequence?,
        start: Int, end: Int,
        fm: FontMetricsInt?
    ): Int {
        val d = getDrawable()
        val rect = d.getBounds()

        if (fm != null) {
            val ascent = fm.ascent
            val middle = ascent + (fm.descent - ascent) / 2
            val halfHeight = rect.height() / 2

            yOffset = middle - halfHeight

            fm.ascent = middle - halfHeight
            fm.top = fm.ascent
            fm.bottom = middle + halfHeight
            fm.descent = fm.bottom
        }

        return rect.width()
    }

    override fun draw(
        canvas: Canvas, text: CharSequence?,
        start: Int, end: Int, x: Float,
        top: Int, y: Int, bottom: Int, paint: Paint
    ) {
        val drawable = getDrawable()
        canvas.withTranslation(x, (y + yOffset).toFloat()) {
            drawable.draw(this)
        }
    }
}
