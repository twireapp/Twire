package com.perflyst.twire.misc;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
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
    private final TextView textView;
    private LayerDrawable layerDrawable;

    private Drawable mDrawable;
    private Animatable animatable;

    public GlideImageSpan(Context context, String url, TextView textView, SpannableStringBuilder builder, int assumedSize, float scale, String backgroundColor) {
        this(context, url, textView, builder, assumedSize, scale);

        if (backgroundColor == null)
            return;

        ColorDrawable backgroundDrawable = new ColorDrawable(Color.parseColor(backgroundColor));

        this.layerDrawable = new LayerDrawable(new Drawable[]{backgroundDrawable, new BlankDrawable()});
        layerDrawable.setId(0, 0);
        layerDrawable.setId(1, 1);
    }

    public GlideImageSpan(Context context, String url, TextView textView, SpannableStringBuilder builder, int assumedSize, float scale) {
        super(new BlankDrawable());

        this.textView = textView;
        this.textView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        final GlideImageSpan instance = this;

        int scaledAssumedSize = Math.round(assumedSize / scale);

        final Drawable placeHolderDrawable = new ColorDrawable(Color.LTGRAY);
        placeHolderDrawable.setBounds(0, 0, scaledAssumedSize, scaledAssumedSize);

        final Drawable errorDrawable = new ColorDrawable(0xFFFFCCCC); // Reddish light gray
        errorDrawable.setBounds(0, 0, scaledAssumedSize, scaledAssumedSize);

        Glide
                .with(context)
                .load(url)
                .error(errorDrawable)
                .placeholder(placeHolderDrawable)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onLoadStarted(Drawable resource) {
                        mDrawable = resource;
                        textView.invalidate();
                    }

                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        resource.setBounds(0, 0, Math.round(resource.getIntrinsicWidth() / scale), Math.round(resource.getIntrinsicHeight() / scale));

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
                    public void onLoadFailed(Drawable resource) {
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
    public void invalidateDrawable(@NonNull Drawable drawable) {
        textView.invalidate();
    }

    @Override
    public void scheduleDrawable(@NonNull Drawable who, @NonNull Runnable what, long when) {
        textView.postDelayed(what, when);
    }

    @Override
    public void unscheduleDrawable(@NonNull Drawable who, @NonNull Runnable what) {
        textView.removeCallbacks(what);
    }

    @Override
    public Drawable getDrawable() {
        Drawable drawable = this.mDrawable != null ? this.mDrawable : super.getDrawable();

        if (layerDrawable != null) {
            layerDrawable.getDrawable(0).setBounds(drawable.getBounds());
            layerDrawable.setBounds(drawable.getBounds());
            layerDrawable.setDrawableByLayerId(1, drawable);

            return layerDrawable;
        }

        return drawable;
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
