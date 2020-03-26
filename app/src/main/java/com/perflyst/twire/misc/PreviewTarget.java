package com.perflyst.twire.misc;

import android.graphics.Bitmap;

import com.bumptech.glide.request.target.CustomTarget;

/**
 * Created by Sebastian Rask on 09-05-2016.
 */
public abstract class PreviewTarget extends CustomTarget<Bitmap> {
    private Bitmap preview;

    public Bitmap getPreview() {
        return preview;
    }

    protected void setPreview(Bitmap preview) {
        this.preview = preview;
    }
}
