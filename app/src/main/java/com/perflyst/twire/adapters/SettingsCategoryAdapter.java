package com.perflyst.twire.adapters;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.perflyst.twire.R;
import com.perflyst.twire.model.SettingsCategory;

import net.nrask.srjneeds.SRJAdapter;
import net.nrask.srjneeds.SRJViewHolder;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Sebastian Rask on 16-05-2017.
 */

public class SettingsCategoryAdapter extends SRJAdapter<SettingsCategory, SettingsCategoryAdapter.SettingsCategoryViewHolder> {

    @Override
    protected int getLayoutResource(int viewType) {
        return R.layout.cell_settings_category;
    }

    @Override
    protected ViewHolderFactory<SettingsCategoryViewHolder> getViewHolderCreator(int i) {
        return SettingsCategoryViewHolder::new;
    }

    public static class SettingsCategoryViewHolder extends SRJViewHolder<SettingsCategory> {
        @BindView(R.id.txt_category_title)
        protected TextView mTitleView;

        @BindView(R.id.txt_category_summary)
        protected TextView mSummaryView;

        @BindView(R.id.img_category_icon)
        protected ImageView mCategoryIconView;

        SettingsCategoryViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @Override
        protected void onDataBinded(SettingsCategory settingsCategory) {
            mTitleView.setText(settingsCategory.getTitleRes());
            mSummaryView.setText(settingsCategory.getSummaryRes());
            mCategoryIconView.setImageResource(settingsCategory.getIconRes());
        }
    }
}
