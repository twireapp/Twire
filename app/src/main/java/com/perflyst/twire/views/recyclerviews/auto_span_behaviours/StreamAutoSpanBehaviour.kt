package com.perflyst.twire.views.recyclerviews.auto_span_behaviours

import android.content.Context
import com.perflyst.twire.R
import com.perflyst.twire.service.Settings.appearanceStreamSize

/**
 * Created by Sebastian Rask on 09-05-2017.
 */
class StreamAutoSpanBehaviour : AutoSpanBehaviour {
    override val elementSizeName: String get() = appearanceStreamSize

    override fun getElementWidth(context: Context): Int {
        return context.resources.getDimension(R.dimen.stream_card_min_width)
            .toInt() + context.resources.getDimension(R.dimen.stream_card_left_margin).toInt()
    }
}
