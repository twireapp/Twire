package com.perflyst.twire.views.recyclerviews.auto_span_behaviours;

import android.content.Context;

import com.perflyst.twire.service.Settings;

/**
 * Created by Sebastian Rask on 09-05-2017.
 */

public interface AutoSpanBehaviour {
    String getElementSizeName(Settings settings);

    int getElementWidth(Context context);
}
