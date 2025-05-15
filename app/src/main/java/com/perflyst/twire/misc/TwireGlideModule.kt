package com.perflyst.twire.misc

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoader.LoadData
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.load.resource.bitmap.ExifInterfaceImageHeaderParser
import com.bumptech.glide.module.AppGlideModule
import com.perflyst.twire.service.Settings.generalUseImageProxy
import com.perflyst.twire.service.Settings.imageProxyUrl
import java.io.InputStream
import java.lang.ref.WeakReference

@GlideModule
class TwireGlideModule : AppGlideModule() {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        val headers = registry.getImageHeaderParsers()

        for (header in headers) {
            if (header is ExifInterfaceImageHeaderParser) {
                headers.remove(header)
            }
        }

        registry.append(
            GlideUrl::class.java, InputStream::class.java,
            CustomUrlModelLoader.Factory(context)
        )
    }

    private class CustomUrlModelLoader(
        private val context: WeakReference<Context?>?,
        private val delegate: ModelLoader<GlideUrl?, InputStream?>
    ) : ModelLoader<GlideUrl?, InputStream?> {
        override fun buildLoadData(
            model: GlideUrl,
            width: Int,
            height: Int,
            options: Options
        ): LoadData<InputStream?>? {
            var model = model
            if (generalUseImageProxy) {
                val proxy = imageProxyUrl
                model = GlideUrl(proxy + model.toStringUrl())
            }

            return delegate.buildLoadData(model, width, height, options)
        }

        override fun handles(model: GlideUrl): Boolean {
            return true
        }

        class Factory(context: Context?) : ModelLoaderFactory<GlideUrl?, InputStream?> {
            private val context: WeakReference<Context?> = WeakReference<Context?>(context)

            override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<GlideUrl?, InputStream?> {
                return CustomUrlModelLoader(
                    context,
                    multiFactory.build(
                        GlideUrl::class.java,
                        InputStream::class.java
                    )
                )
            }

            override fun teardown() {
                // Do nothing
            }
        }
    }
}
