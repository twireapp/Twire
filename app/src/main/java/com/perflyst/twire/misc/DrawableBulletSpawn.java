package com.perflyst.twire.misc;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.Spanned;
import android.text.style.BulletSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DrawableBulletSpawn extends BulletSpan {
    final float scaleFactor = 0.6f;
    private final int mGap;
    private final Drawable mDrawable;

    public DrawableBulletSpawn(int gap, Drawable drawable) {
        super(gap);

        mGap = gap;
        mDrawable = drawable;
    }

    @Override
    public int getLeadingMargin(boolean first) {
        return (int) (mDrawable.getIntrinsicWidth() * scaleFactor + mGap * 2);
    }

    @Override
    public void drawLeadingMargin(@NonNull Canvas canvas, @NonNull Paint paint, int x, int dir,
                                  int top, int baseline, int bottom,
                                  @NonNull CharSequence text, int start, int end,
                                  boolean first, @Nullable Layout layout) {
        if (((Spanned) text).getSpanStart(this) == start) {
            Paint.Style style = paint.getStyle();

            paint.setStyle(Paint.Style.FILL);

            if (layout != null) {
                Paint.FontMetrics metrics = paint.getFontMetrics();
                bottom = (int) (baseline + metrics.bottom);
            }

            final float xPosition = x + mGap;
            final float yPosition = (top + bottom - mDrawable.getIntrinsicHeight() * scaleFactor) / 2f;

            canvas.save();
            canvas.translate(xPosition, yPosition);
            canvas.scale(scaleFactor, scaleFactor);
            mDrawable.draw(canvas);
            canvas.restore();

            paint.setStyle(style);
        }
    }
}
