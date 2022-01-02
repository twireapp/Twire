package com.perflyst.twire.misc;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.style.ImageSpan;

import androidx.annotation.NonNull;

public class VerticalImageSpan extends ImageSpan {
    VerticalImageSpan(Drawable drawable) {
        super(drawable);
    }

    private int yOffset;

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text,
                       int start, int end,
                       Paint.FontMetricsInt fm) {
        Drawable d = getDrawable();
        Rect rect = d.getBounds();

        if (fm != null) {
            int ascent = fm.ascent;
            int middle = ascent + (fm.descent - ascent) / 2;
            int halfHeight = rect.height() / 2;

            yOffset = middle - halfHeight;

            fm.ascent = middle - halfHeight;
            fm.top = fm.ascent;
            fm.bottom = middle + halfHeight;
            fm.descent = fm.bottom;
        }

        return rect.width();
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text,
                     int start, int end, float x,
                     int top, int y, int bottom, @NonNull Paint paint) {
        Drawable drawable = getDrawable();
        canvas.save();
        canvas.translate(x, y + yOffset);
        drawable.draw(canvas);
        canvas.restore();
    }
}
