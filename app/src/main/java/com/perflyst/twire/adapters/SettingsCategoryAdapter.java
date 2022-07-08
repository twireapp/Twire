package com.perflyst.twire.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.perflyst.twire.R;
import com.perflyst.twire.model.SettingsCategory;

import java.util.List;

/**
 * Created by Sebastian Rask on 16-05-2017.
 */

public class SettingsCategoryAdapter extends RecyclerView.Adapter<SettingsCategoryAdapter.SettingsCategoryViewHolder> {

    protected List<SettingsCategory> mCategories;
    protected CategoryCallback mCategoryCallback;

    public SettingsCategoryAdapter(List<SettingsCategory> mCategories, CategoryCallback mCategoryCallback) {
        this.mCategories = mCategories;
        this.mCategoryCallback = mCategoryCallback;
    }
    @NonNull
    @Override
    public SettingsCategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.cell_settings_category, parent, false);
        return new SettingsCategoryViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull SettingsCategoryViewHolder holder, int position) {
        SettingsCategory category = mCategories.get(position);
        holder.mTitleView.setText(category.getTitleRes());
        holder.mSummaryView.setText(category.getSummaryRes());
        holder.mCategoryIconView.setImageResource(category.getIconRes());

        if (mCategoryCallback != null) {
            holder.itemView.setOnClickListener(view -> mCategoryCallback.onCategoryClicked(category));
        }
    }

    @Override
    public int getItemCount() {
        return mCategories.size();
    }

    public interface CategoryCallback {
        void onCategoryClicked(SettingsCategory category);
    }

    public static class SettingsCategoryViewHolder extends RecyclerView.ViewHolder {
        protected TextView mTitleView;

        protected TextView mSummaryView;

        protected ImageView mCategoryIconView;

        SettingsCategoryViewHolder(View itemView) {
            super(itemView);

            mTitleView = itemView.findViewById(R.id.txt_category_title);
            mSummaryView = itemView.findViewById(R.id.txt_category_summary);
            mCategoryIconView = itemView.findViewById(R.id.img_category_icon);
        }
    }
}
