package com.perflyst.twire.model;

import android.content.Intent;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

/**
 * Created by Sebastian Rask on 16-05-2017.
 */

public class SettingsCategory {
    @StringRes
    private final int mTitleRes;
    @StringRes
    private final int mSummaryRes;

    @DrawableRes
    private final int iconRes;

    private final Intent intent;

    public SettingsCategory(int title, int summary, int icon, Intent clickIntent) {
        mTitleRes = title;
        mSummaryRes = summary;
        iconRes = icon;
        intent = clickIntent;
    }

    public int getTitleRes() {
        return mTitleRes;
    }

    public int getSummaryRes() {
        return mSummaryRes;
    }

    public int getIconRes() {
        return iconRes;
    }

    public Intent getIntent() {
        return intent;
    }
}
