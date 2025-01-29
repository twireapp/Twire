package com.perflyst.twire.activities.settings;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.perflyst.twire.R;
import com.perflyst.twire.activities.ThemeActivity;
import com.perflyst.twire.adapters.SettingsCategoryAdapter;
import com.perflyst.twire.databinding.ActivitySettingsBinding;
import com.perflyst.twire.model.SettingsCategory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SettingsActivity extends ThemeActivity implements SettingsCategoryAdapter.CategoryCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivitySettingsBinding binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.settingsToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        SettingsCategoryAdapter mAdapter = new SettingsCategoryAdapter(constructSettingsCategories(), this);

        binding.settingsCategoryList.setAdapter(mAdapter);
        binding.settingsCategoryList.setLayoutManager(new LinearLayoutManager(getBaseContext()));
        binding.settingsCategoryList.setItemAnimator(new DefaultItemAnimator());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Up/back is the only option available :)
        onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.overridePendingTransition(R.anim.fade_in_semi_anim, R.anim.slide_out_right_anim);
    }

    @Override
    public void onCategoryClicked(SettingsCategory category) {
        ActivityOptions settingsAnim = ActivityOptions.makeCustomAnimation(this, R.anim.slide_in_right_anim, R.anim.fade_out_semi_anim); // First animation is how the new activity enters - Second is how the current activity exits
        startActivity(category.intent, settingsAnim.toBundle());
    }

    private List<SettingsCategory> constructSettingsCategories() {
        return new ArrayList<>(Arrays.asList(
                new SettingsCategory(
                        R.string.settings_general_name,
                        R.string.settings_general_name_summary,
                        R.drawable.ic_settings,
                        constructCategoryIntent(SettingsGeneralActivity.class)
                ),
                new SettingsCategory(
                        R.string.settings_chat_name,
                        R.string.settings_chat_name_summary,
                        R.drawable.ic_chat,
                        constructCategoryIntent(SettingsTwitchChatActivity.class)
                ),
                new SettingsCategory(
                        R.string.settings_stream_player_name,
                        R.string.settings_stream_player_summary,
                        R.drawable.ic_theaters,
                        constructCategoryIntent(SettingsStreamPlayerActivity.class)
                ),
                new SettingsCategory(
                        R.string.settings_appearance_name,
                        R.string.settings_appearance_summary,
                        R.drawable.ic_palette,
                        constructCategoryIntent(SettingsAppearanceActivity.class)
                )/*,
                new SettingsCategory(
                        R.string.settings_notifications_name,
                        R.string.settings_notifications_summary,
                        R.drawable.ic_notifications_active_black_48dp,
                        constructCategoryIntent(SettingsNotificationsActivity.class)
                )*/));
    }

    private Intent constructCategoryIntent(final Class toActivity) {
        Intent intent = new Intent(this, toActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        return intent;
    }

}
