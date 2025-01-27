package com.perflyst.twire.views.recyclerviews.auto_span_behaviours;

import android.content.Context;

import com.perflyst.twire.R;

/**
 * Created by Sebastian Rask on 09-05-2017.
 */

public class EmoteAutoSpanBehaviour implements AutoSpanBehaviour {
    @Override
    public String getElementSizeName() {
        return "";
    }

    @Override
    public int getElementWidth(Context context) {
        return (int) context.getResources().getDimension(R.dimen.chat_grid_emote_size);
    }
}
