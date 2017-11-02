package com.sebastianrask.bettersubscription.views.recyclerviews.auto_span_behaviours;

import android.content.Context;

import com.sebastianrask.bettersubscription.service.Settings;

/**
 * Created by Sebastian Rask on 09-05-2017.
 */

public interface AutoSpanBehaviour {
	String getElementSizeName(Settings settings);
	int getElementWidth(Context context);
}
