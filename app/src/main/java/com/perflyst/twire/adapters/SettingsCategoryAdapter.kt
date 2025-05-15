package com.perflyst.twire.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.perflyst.twire.R
import com.perflyst.twire.adapters.SettingsCategoryAdapter.SettingsCategoryViewHolder
import com.perflyst.twire.model.SettingsCategory

/**
 * Created by Sebastian Rask on 16-05-2017.
 */
class SettingsCategoryAdapter(
    private var mCategories: MutableList<SettingsCategory>,
    private var mCategoryCallback: CategoryCallback?
) : RecyclerView.Adapter<SettingsCategoryViewHolder?>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingsCategoryViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.cell_settings_category, parent, false)
        return SettingsCategoryViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: SettingsCategoryViewHolder, position: Int) {
        val category = mCategories[position]
        holder.mTitleView.setText(category.titleRes)
        holder.mSummaryView.setText(category.summaryRes)
        holder.mCategoryIconView.setImageResource(category.iconRes)

        if (mCategoryCallback != null) {
            holder.itemView.setOnClickListener { view: View? ->
                mCategoryCallback!!.onCategoryClicked(
                    category
                )
            }
        }
    }

    override fun getItemCount(): Int {
        return mCategories.size
    }

    interface CategoryCallback {
        fun onCategoryClicked(category: SettingsCategory)
    }

    class SettingsCategoryViewHolder internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var mTitleView: TextView = itemView.findViewById(R.id.txt_category_title)

        var mSummaryView: TextView = itemView.findViewById(R.id.txt_category_summary)

        var mCategoryIconView: ImageView = itemView.findViewById(R.id.img_category_icon)
    }
}
