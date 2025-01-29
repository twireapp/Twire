package com.perflyst.twire.model

import android.content.Context

/**
 * Created by Sebastian Rask on 04-04-2016.
 */
interface MainElement {
    val highPreview: String?

    val mediumPreview: String?

    val lowPreview: String?

    fun getPlaceHolder(context: Context): Int

    fun refreshPreview(context: Context, callback: Runnable) {
    }
}
