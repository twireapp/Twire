package com.perflyst.twire.misc;


import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

import com.squareup.picasso.Transformation;

/**
 * Created by SebastianRask on 20-03-2015.
 */
public class RoundedTopTransformation implements Transformation {
    private float cornerRadius;

    public RoundedTopTransformation(float cornerRadius) {
        this.cornerRadius = cornerRadius;

    }

    @Override
    public Bitmap transform(Bitmap source) {

        int w = source.getWidth();
        int h = source.getHeight();
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Shader shader = new BitmapShader(source, Shader.TileMode.MIRROR,
                Shader.TileMode.MIRROR);

        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        paint.setAntiAlias(true);
        paint.setShader(shader);
        RectF rec = new RectF(0, 0, w, h - (h / 3));
        c.drawRect(new RectF(0, (h / 3), w, h), paint);
        c.drawRoundRect(rec, cornerRadius, cornerRadius, paint);
        source.recycle();
        return bmp;

    }

    @Override
    public String key() {
        return "Rounded Top";
    }
}
