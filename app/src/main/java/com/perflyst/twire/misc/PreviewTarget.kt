package com.perflyst.twire.misc

import android.graphics.Bitmap
import com.bumptech.glide.request.target.CustomTarget

/**
 * Created by Sebastian Rask on 09-05-2016.
 */
abstract class PreviewTarget : CustomTarget<Bitmap?>() {
    var preview: Bitmap? = null
        protected set
}
