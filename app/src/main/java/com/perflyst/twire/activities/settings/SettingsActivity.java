package com.perflyst.twire.activities.settings;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.perflyst.twire.R;
import com.perflyst.twire.activities.ThemeActivity;
import com.perflyst.twire.adapters.SettingsCategoryAdapter;
import com.perflyst.twire.model.SettingsCategory;

import net.nrask.srjneeds.SRJAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SettingsActivity extends ThemeActivity implements SRJAdapter.ItemCallback<SettingsCategoryAdapter.SettingsCategoryViewHolder> {

    @BindView(R.id.settings_category_list)
    protected RecyclerView mCategoryList;

    @BindView(R.id.settings_toolbar)
    protected Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        SettingsCategoryAdapter mAdapter = new SettingsCategoryAdapter();
        mAdapter.setItemCallback(this);
        mAdapter.addItems(constructSettingsCategories());

        mCategoryList.setAdapter(mAdapter);
        mCategoryList.setLayoutManager(new LinearLayoutManager(getBaseContext()));
        mCategoryList.setItemAnimator(new DefaultItemAnimator());
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
    public void onItemClicked(View view, SettingsCategoryAdapter.SettingsCategoryViewHolder settingsCategoryViewHolder) {
        SettingsCategory category = settingsCategoryViewHolder.getData();

        ActivityOptions settingsAnim = ActivityOptions.makeCustomAnimation(this, R.anim.slide_in_right_anim, R.anim.fade_out_semi_anim); // First animation is how the new activity enters - Second is how the current activity exits
        startActivity(category.getIntent(), settingsAnim.toBundle());
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
                        R.drawable.ic_filmstrip,
                        constructCategoryIntent(SettingsStreamPlayerActivity.class)
                ),
                new SettingsCategory(
                        R.string.settings_appearance_name,
                        R.string.settings_appearance_summary,
                        R.drawable.ic_color_lens,
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
