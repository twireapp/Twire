package com.perflyst.twire.model

import android.content.Context

/**
 * Created by Sebastian Rask on 04-04-2016.
 */
interface MainElement {
    val previewUrl: String?
        get() = previewTemplate?.replace(Regex("%?\\{width\\}"), width)
            ?.replace(Regex("%?\\{height\\}"), height)

    val previewTemplate: String?

    val width: String
        get() = "320"

    val height: String
        get() = "180"

    fun getPlaceHolder(context: Context): Int

    fun refreshPreview(context: Context, callback: Runnable) {
    }
}
