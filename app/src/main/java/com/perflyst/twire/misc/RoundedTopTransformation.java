package com.perflyst.twire.misc;


import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;

/**
 * Created by SebastianRask on 20-03-2015.
 */
public class RoundedTopTransformation extends BitmapTransformation {
    private static final String ID = "com.perflyst.twire.misc.RoundedTopTransformation";
    private static final byte[] ID_BYTES = ID.getBytes(Charset.forName("UTF-8"));

    private final float cornerRadius;

    public RoundedTopTransformation(float cornerRadius) {
        this.cornerRadius = cornerRadius;
    }

    @Override
    protected Bitmap transform(
            @NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {
        int w = toTransform.getWidth();
        int h = toTransform.getHeight();
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Shader shader = new BitmapShader(toTransform, Shader.TileMode.MIRROR,
                Shader.TileMode.MIRROR);

        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        paint.setAntiAlias(true);
        paint.setShader(shader);
        RectF rec = new RectF(0, 0, w, h - (h / 3.0f));
        c.drawRect(new RectF(0, (h / 3.0f), w, h), paint);
        c.drawRoundRect(rec, cornerRadius, cornerRadius, paint);
        return bmp;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof RoundedTopTransformation) {
            return cornerRadius == ((RoundedTopTransformation) o).cornerRadius;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (int) cornerRadius * 31 + ID.hashCode();
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
        messageDigest.update(ID_BYTES);

        byte[] radiusData = ByteBuffer.allocate(4).putFloat(cornerRadius).array();
        messageDigest.update(radiusData);
    }
}
