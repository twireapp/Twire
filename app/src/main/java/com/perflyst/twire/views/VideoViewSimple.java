package com.perflyst.twire.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.VideoView;

/**
 * Created by SebastianRask on 29-02-2016.
 */
public class VideoViewSimple extends VideoView {

    public VideoViewSimple(Context context) {
        super(context);
    }

    public VideoViewSimple(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VideoViewSimple(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public VideoViewSimple(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        getHolder().setSizeFromLayout();
    }
}
