package com.perflyst.twire.views.recyclerviews.auto_span_behaviours

import android.content.Context
import com.perflyst.twire.R

/**
 * Created by Sebastian Rask on 09-05-2017.
 */
class EmoteAutoSpanBehaviour : AutoSpanBehaviour {
    override val elementSizeName: String get() = ""

    override fun getElementWidth(context: Context): Int {
        return context.resources.getDimension(R.dimen.chat_grid_emote_size).toInt()
    }
}
