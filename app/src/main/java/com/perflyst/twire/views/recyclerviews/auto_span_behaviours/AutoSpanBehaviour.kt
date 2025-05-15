package com.perflyst.twire.views.recyclerviews.auto_span_behaviours

import android.content.Context

/**
 * Created by Sebastian Rask on 09-05-2017.
 */
interface AutoSpanBehaviour {
    val elementSizeName: String

    fun getElementWidth(context: Context): Int
}
