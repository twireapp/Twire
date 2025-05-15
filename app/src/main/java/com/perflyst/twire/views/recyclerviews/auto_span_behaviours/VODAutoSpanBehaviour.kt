package com.perflyst.twire.views.recyclerviews.auto_span_behaviours

import android.content.Context
import com.perflyst.twire.R
import com.perflyst.twire.service.Settings.context

/**
 * Created by Sebastian Rask on 09-05-2017.
 */
class VODAutoSpanBehaviour : AutoSpanBehaviour {
    override val elementSizeName: String get() = context.getString(R.string.card_size_large)

    override fun getElementWidth(context: Context): Int {
        return context.resources.getDimension(R.dimen.stream_card_min_width)
            .toInt() + context.resources.getDimension(R.dimen.stream_card_left_margin).toInt()
    }
}
