package com.perflyst.twire.misc;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.rey.material.drawable.BlankDrawable;

public class GlideImageSpan extends VerticalImageSpan implements Drawable.Callback {
    private TextView textView;

    private Drawable mDrawable;
    private Animatable animatable;

    public GlideImageSpan(Context context, String url, TextView textView, SpannableStringBuilder builder, int assumedSize) {
        super(new BlankDrawable());

        this.textView = textView;
        this.textView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        final GlideImageSpan instance = this;

        final Drawable placeHolderDrawable = new ColorDrawable(Color.LTGRAY);
        placeHolderDrawable.setBounds(0, 0, assumedSize, assumedSize);

        final Drawable errorDrawable = new ColorDrawable(0xFFFFCCCC); // Reddish light gray
        errorDrawable.setBounds(0, 0, assumedSize, assumedSize);

        Glide
                .with(context)
                .load(url)
                .error(errorDrawable)
                .placeholder(placeHolderDrawable)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onLoadStarted(@NonNull Drawable resource) {
                        mDrawable = resource;
                        textView.invalidate();
                    }

                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        resource.setBounds(0, 0, resource.getIntrinsicWidth(), resource.getIntrinsicHeight());

                        if (resource instanceof Animatable) {
                            animatable = (Animatable) resource;
                            resource.setCallback(instance);

                            animatable.start();
                        }

                        mDrawable = resource;
                        if (resource.getIntrinsicWidth() != assumedSize) {
                            textView.setText(builder);
                        } else {
                            textView.invalidate();
                        }
                    }


                    @Override
                    public void onLoadFailed(@NonNull Drawable resource) {
                        mDrawable = resource;
                        textView.invalidate();
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        if (animatable != null) {
                            animatable.stop();
                        }

                        mDrawable = placeholder;
                        textView.invalidate();
                    }
                });
    }

    @Override
    public void invalidateDrawable(Drawable drawable) {
        textView.invalidate();
    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        textView.postDelayed(what, when);
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {
        textView.removeCallbacks(what);
    }

    @Override
    public Drawable getDrawable() {
        return this.mDrawable != null ? this.mDrawable : super.getDrawable();
    }

/*
    @Override
    public void chooseHeight(CharSequence text, int start, int end, int spanstartv, int lineHeight, Paint.FontMetricsInt fm) {
        if (end == ((Spanned) text).getSpanEnd(this)) {
            int ht = getDrawable().getIntrinsicHeight();

            int need = ht - (lineHeight + fm.descent - fm.ascent - spanstartv);
            if (need > 0) {
                fm.descent += need;
            }

            need = ht - (lineHeight + fm.bottom - fm.top - spanstartv);
            if (need > 0) {
                fm.bottom += need;
            }
        }
    }*/
}
