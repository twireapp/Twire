package com.perflyst.twire.misc;

import android.content.Context;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.resource.bitmap.ExifInterfaceImageHeaderParser;
import com.bumptech.glide.module.AppGlideModule;

import java.util.List;

@GlideModule
public class ExifGlideModule extends AppGlideModule {
    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, Registry registry) {
        List<ImageHeaderParser> headers = registry.getImageHeaderParsers();

        for (ImageHeaderParser header : headers) {
            if (header instanceof ExifInterfaceImageHeaderParser) {
                headers.remove(header);
            }
        }
    }
}
