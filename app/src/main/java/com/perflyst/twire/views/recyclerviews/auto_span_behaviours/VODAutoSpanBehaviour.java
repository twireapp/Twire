package com.perflyst.twire.views.recyclerviews.auto_span_behaviours;

import android.content.Context;

import com.perflyst.twire.R;
import com.perflyst.twire.service.Settings;

/**
 * Created by Sebastian Rask on 09-05-2017.
 */

public class VODAutoSpanBehaviour implements AutoSpanBehaviour {
    @Override
    public String getElementSizeName(Settings settings) {
        return settings.getContext().getString(R.string.card_size_large);
    }

    @Override
    public int getElementWidth(Context context) {
        return (int) context.getResources().getDimension(R.dimen.stream_card_min_width) + (int) context.getResources().getDimension(R.dimen.stream_card_left_margin);
    }
}
