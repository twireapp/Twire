package com.perflyst.twire.views.recyclerviews.auto_span_behaviours

import android.content.Context
import com.perflyst.twire.R
import com.perflyst.twire.service.Settings.appearanceGameSize

/**
 * Created by Sebastian Rask on 09-05-2017.
 */
class GameAutoSpanBehaviour : AutoSpanBehaviour {
    override val elementSizeName: String get() = appearanceGameSize

    override fun getElementWidth(context: Context): Int {
        return context.getResources().getDimension(R.dimen.game_card_width)
            .toInt() + context.getResources().getDimension(R.dimen.game_card_margin).toInt()
    }
}
