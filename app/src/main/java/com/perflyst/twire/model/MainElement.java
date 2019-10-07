package com.perflyst.twire.model;

import android.content.Context;

/**
 * Created by Sebastian Rask on 04-04-2016.
 */
public interface MainElement {
    String getHighPreview();

    String getMediumPreview();

    String getLowPreview();

    int getPlaceHolder(Context context);
}
