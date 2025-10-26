package com.perflyst.twire.activities.settings

import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.Fade
import androidx.transition.Slide
import com.perflyst.twire.R
import com.perflyst.twire.adapters.SettingsCategoryAdapter
import com.perflyst.twire.adapters.SettingsCategoryAdapter.CategoryCallback
import com.perflyst.twire.databinding.ActivitySettingsBinding
import com.perflyst.twire.fragments.BindingFragment
import com.perflyst.twire.misc.navigate
import com.perflyst.twire.misc.popBackStack
import com.perflyst.twire.misc.setupToolbar
import com.perflyst.twire.model.SettingsCategory

class SettingsFragment : BindingFragment<ActivitySettingsBinding>(ActivitySettingsBinding::inflate),
    CategoryCallback {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupToolbar(binding.settingsToolbar, R.string.settings_activity_title)

        val mAdapter = SettingsCategoryAdapter(constructSettingsCategories(), this)

        binding.settingsCategoryList.setAdapter(mAdapter)
        binding.settingsCategoryList.setLayoutManager(LinearLayoutManager(requireContext()))
        binding.settingsCategoryList.setItemAnimator(DefaultItemAnimator())
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Up/back is the only option available :)
        popBackStack()

        return super.onOptionsItemSelected(item)
    }

    override fun onCategoryClicked(category: SettingsCategory) {
        navigate(
            category.fragment,
            enterAnim = Slide(Gravity.END),
            exitAnim = Fade()
        )
    }

    private fun constructSettingsCategories(): MutableList<SettingsCategory> {
        return ArrayList(
            listOf(
                SettingsCategory(
                    R.string.settings_general_name,
                    R.string.settings_general_name_summary,
                    R.drawable.ic_settings,
                    SettingsGeneralFragment::class.java
                ),
                SettingsCategory(
                    R.string.settings_chat_name,
                    R.string.settings_chat_name_summary,
                    R.drawable.ic_chat,
                    SettingsTwitchChatFragment::class.java
                ),
                SettingsCategory(
                    R.string.settings_stream_player_name,
                    R.string.settings_stream_player_summary,
                    R.drawable.ic_theaters,
                    SettingsStreamPlayerFragment::class.java
                ),
                SettingsCategory(
                    R.string.settings_appearance_name,
                    R.string.settings_appearance_summary,
                    R.drawable.ic_palette,
                    SettingsAppearanceFragment::class.java
                ) /*,
                new SettingsCategory(
                        R.string.settings_notifications_name,
                        R.string.settings_notifications_summary,
                        R.drawable.ic_notifications_active_black_48dp,
                        SettingsNotificationsActivity.class
                )*/
            )
        )
    }
}
