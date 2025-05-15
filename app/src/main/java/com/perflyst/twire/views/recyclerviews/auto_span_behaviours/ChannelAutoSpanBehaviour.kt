package com.perflyst.twire.views.recyclerviews.auto_span_behaviours

import android.content.Context
import com.perflyst.twire.R
import com.perflyst.twire.service.Settings.appearanceChannelSize

/**
 * Created by Sebastian Rask on 09-05-2017.
 */
class ChannelAutoSpanBehaviour : AutoSpanBehaviour {
    override val elementSizeName: String get() = appearanceChannelSize

    override fun getElementWidth(context: Context): Int {
        return context.resources.getDimension(R.dimen.subscription_card_width)
            .toInt() + context.resources.getDimension(R.dimen.subscription_card_margin).toInt()
    }
}
