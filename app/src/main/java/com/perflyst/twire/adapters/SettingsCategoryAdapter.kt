package com.perflyst.twire.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.perflyst.twire.adapters.SettingsCategoryAdapter.SettingsCategoryViewHolder
import com.perflyst.twire.databinding.CellSettingsCategoryBinding
import com.perflyst.twire.model.SettingsCategory

/**
 * Created by Sebastian Rask on 16-05-2017.
 */
class SettingsCategoryAdapter(
    private var mCategories: MutableList<SettingsCategory>,
    private var mCategoryCallback: CategoryCallback?
) : RecyclerView.Adapter<SettingsCategoryViewHolder?>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingsCategoryViewHolder {
        val binding = CellSettingsCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SettingsCategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SettingsCategoryViewHolder, position: Int) {
        val category = mCategories[position]
        holder.binding.txtCategoryTitle.setText(category.titleRes)
        holder.binding.txtCategorySummary.setText(category.summaryRes)
        holder.binding.imgCategoryIcon.setImageResource(category.iconRes)

        if (mCategoryCallback != null) {
            holder.itemView.setOnClickListener {
                mCategoryCallback!!.onCategoryClicked(category)
            }
        }
    }

    override fun getItemCount(): Int {
        return mCategories.size
    }

    interface CategoryCallback {
        fun onCategoryClicked(category: SettingsCategory)
    }

    class SettingsCategoryViewHolder(binding: CellSettingsCategoryBinding) :
        BindingViewHolder<CellSettingsCategoryBinding>(binding)
}
