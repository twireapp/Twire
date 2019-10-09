package com.perflyst.twire.views.recyclerviews.auto_span_behaviours;

import android.content.Context;

import com.perflyst.twire.R;
import com.perflyst.twire.service.Settings;

/**
 * Created by Sebastian Rask on 09-05-2017.
 */

public class GameAutoSpanBehaviour implements AutoSpanBehaviour {
    @Override
    public String getElementSizeName(Settings settings) {
        return settings.getAppearanceGameSize();
    }

    @Override
    public int getElementWidth(Context context) {
        return (int) context.getResources().getDimension(R.dimen.game_card_width) + (int) context.getResources().getDimension(R.dimen.game_card_margin);
    }
}
