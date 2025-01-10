package com.perflyst.twire.misc;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.load.resource.bitmap.ExifInterfaceImageHeaderParser;
import com.bumptech.glide.module.AppGlideModule;
import com.perflyst.twire.service.Settings;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.List;

@GlideModule
public class TwireGlideModule extends AppGlideModule {
    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, Registry registry) {
        List<ImageHeaderParser> headers = registry.getImageHeaderParsers();

        for (ImageHeaderParser header : headers) {
            if (header instanceof ExifInterfaceImageHeaderParser) {
                headers.remove(header);
            }
        }

        registry.append(GlideUrl.class, InputStream.class,
                new TwireGlideModule.CustomUrlModelLoader.Factory(context));
    }

    private static class CustomUrlModelLoader implements ModelLoader<GlideUrl, InputStream> {
        private final WeakReference<Context> context;
        private final ModelLoader<GlideUrl, InputStream> delegate;

        public CustomUrlModelLoader(WeakReference<Context> context, ModelLoader<GlideUrl, InputStream> delegate) {
            this.context = context;
            this.delegate = delegate;
        }

        @Nullable
        @Override
        public LoadData<InputStream> buildLoadData(@NonNull GlideUrl model,
                                                   int width,
                                                   int height,
                                                   @NonNull Options options) {
            Settings settings = new Settings(context.get());
            if (settings.getGeneralUseImageProxy()) {
                String proxy = settings.getImageProxyUrl();
                model = new GlideUrl(proxy + model.toStringUrl());
            }

            return delegate.buildLoadData(model, width, height, options);
        }

        @Override
        public boolean handles(@NonNull GlideUrl model) {
            return true;
        }

        private static class Factory implements ModelLoaderFactory<GlideUrl, InputStream> {
            private final WeakReference<Context> context;

            public Factory(Context context) {
                this.context = new WeakReference<>(context);
            }

            @NonNull
            @Override
            public ModelLoader<GlideUrl, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
                return new CustomUrlModelLoader(context, multiFactory.build(GlideUrl.class, InputStream.class));
            }

            @Override
            public void teardown() {
                // Do nothing
            }
        }
    }
}
