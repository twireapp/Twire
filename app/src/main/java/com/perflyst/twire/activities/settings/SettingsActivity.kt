package com.perflyst.twire.activities.settings

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.perflyst.twire.R
import com.perflyst.twire.activities.ThemeActivity
import com.perflyst.twire.adapters.SettingsCategoryAdapter
import com.perflyst.twire.adapters.SettingsCategoryAdapter.CategoryCallback
import com.perflyst.twire.databinding.ActivitySettingsBinding
import com.perflyst.twire.model.SettingsCategory

class SettingsActivity : ThemeActivity(), CategoryCallback {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.getRoot())

        setSupportActionBar(binding.settingsToolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
        val mAdapter = SettingsCategoryAdapter(constructSettingsCategories(), this)

        binding.settingsCategoryList.setAdapter(mAdapter)
        binding.settingsCategoryList.setLayoutManager(LinearLayoutManager(baseContext))
        binding.settingsCategoryList.setItemAnimator(DefaultItemAnimator())
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Up/back is the only option available :)
        onBackPressed()
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        this.overridePendingTransition(R.anim.fade_in_semi_anim, R.anim.slide_out_right_anim)
    }

    override fun onCategoryClicked(category: SettingsCategory) {
        val settingsAnim = ActivityOptions.makeCustomAnimation(
            this,
            R.anim.slide_in_right_anim,
            R.anim.fade_out_semi_anim
        ) // First animation is how the new activity enters - Second is how the current activity exits
        startActivity(category.intent, settingsAnim.toBundle())
    }

    private fun constructSettingsCategories(): MutableList<SettingsCategory> {
        return ArrayList(
            listOf(
                SettingsCategory(
                    R.string.settings_general_name,
                    R.string.settings_general_name_summary,
                    R.drawable.ic_settings,
                    constructCategoryIntent(SettingsGeneralActivity::class.java)
                ),
                SettingsCategory(
                    R.string.settings_chat_name,
                    R.string.settings_chat_name_summary,
                    R.drawable.ic_chat,
                    constructCategoryIntent(SettingsTwitchChatActivity::class.java)
                ),
                SettingsCategory(
                    R.string.settings_stream_player_name,
                    R.string.settings_stream_player_summary,
                    R.drawable.ic_theaters,
                    constructCategoryIntent(SettingsStreamPlayerActivity::class.java)
                ),
                SettingsCategory(
                    R.string.settings_appearance_name,
                    R.string.settings_appearance_summary,
                    R.drawable.ic_palette,
                    constructCategoryIntent(SettingsAppearanceActivity::class.java)
                ) /*,
                new SettingsCategory(
                        R.string.settings_notifications_name,
                        R.string.settings_notifications_summary,
                        R.drawable.ic_notifications_active_black_48dp,
                        constructCategoryIntent(SettingsNotificationsActivity.class)
                )*/
            )
        )
    }

    private fun constructCategoryIntent(toActivity: Class<*>?): Intent {
        val intent = Intent(this, toActivity)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return intent
    }
}
