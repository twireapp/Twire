package com.sebastianrask.bettersubscription.activities.settings;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.sebastianrask.bettersubscription.R;
import com.sebastianrask.bettersubscription.activities.DonationActivity;
import com.sebastianrask.bettersubscription.activities.ThemeActivity;
import com.sebastianrask.bettersubscription.adapters.SettingsCategoryAdapter;
import com.sebastianrask.bettersubscription.model.SettingsCategory;
import com.sebastianrask.bettersubscription.service.Service;

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

	private String LOG_TAG = "SettingsActivity";

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
	public boolean onOptionsItemSelected(MenuItem item) {
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

		ActivityOptionsCompat settingsAnim = ActivityOptionsCompat.makeCustomAnimation(this, R.anim.slide_in_right_anim, R.anim.fade_out_semi_anim); // First animation is how the new activity enters - Second is how the current activity exits
		ActivityCompat.startActivity(this, category.getIntent(), settingsAnim.toBundle());
	}

	private List<SettingsCategory> constructSettingsCategories() {
		return new ArrayList<>(Arrays.asList(new SettingsCategory[] {
				new SettingsCategory(
						R.string.settings_general_name,
						R.string.settings_general_name_summary,
						R.drawable.ic_settings_black_24dp,
						constructCategoryIntent(SettingsGeneralActivity.class)
				),
				new SettingsCategory(
						R.string.settings_chat_name,
						R.string.settings_chat_name_summary,
						R.drawable.ic_chat_black_48dp,
						constructCategoryIntent(SettingsTwitchChatActivity.class)
				),
				new SettingsCategory(
						R.string.settings_stream_player_name,
						R.string.settings_stream_player_summary,
						R.drawable.ic_filmstrip_black_48dp,
						constructCategoryIntent(SettingsStreamPlayerActivity.class)
				),
				new SettingsCategory(
						R.string.settings_appearance_name,
						R.string.settings_appearance_summary,
						R.drawable.ic_color_lens_black_48dp,
						constructCategoryIntent(SettingsAppearanceActivity.class)
				),
				new SettingsCategory(
						R.string.settings_notifications_name,
						R.string.settings_notifications_summary,
						R.drawable.ic_notifications_active_black_48dp,
						constructCategoryIntent(SettingsNotificationsActivity.class)
				),
				new SettingsCategory(
						R.string.settings_rate_name,
						R.string.settings_rate_summary,
						R.drawable.ic_thumb_up_black_48dp,
						Service.getPlayStoreIntent()
				),
				new SettingsCategory(
						R.string.settings_donate_name,
						R.string.settings_donate_summary,
						R.drawable.ic_heart_24dp,
						constructDonationIntent()
				)
		}));
	}

	private Intent constructCategoryIntent(final Class toActivity) {
		Intent intent = new Intent(this, toActivity);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		return intent;
	}

	private Intent constructDonationIntent() {
		final Intent intent = new Intent(this, DonationActivity.class);
		intent.putExtra(getString(R.string.donation_flow_is_user_started), true);

		return intent;
	}
}