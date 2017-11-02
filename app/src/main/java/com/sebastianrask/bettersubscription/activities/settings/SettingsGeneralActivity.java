package com.sebastianrask.bettersubscription.activities.settings;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.sebastianrask.bettersubscription.R;
import com.sebastianrask.bettersubscription.activities.ThemeActivity;
import com.sebastianrask.bettersubscription.activities.main.MyChannelsActivity;
import com.sebastianrask.bettersubscription.activities.main.MyGamesActivity;
import com.sebastianrask.bettersubscription.activities.main.MyStreamsActivity;
import com.sebastianrask.bettersubscription.activities.setup.LoginActivity;
import com.sebastianrask.bettersubscription.service.DialogService;
import com.sebastianrask.bettersubscription.service.Service;
import com.sebastianrask.bettersubscription.service.Settings;

public class SettingsGeneralActivity extends ThemeActivity {
	private String LOG_TAG = getClass().getSimpleName();
	private Settings settings;
	private TextView twitchNameView, startPageSubText, showDonationSubText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings_general);
		settings = new Settings(getBaseContext());

		final Toolbar toolbar = (Toolbar) findViewById(R.id.settings_general_toolbar);
		setSupportActionBar(toolbar);
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}

		//Service.isTranslucentActionbar(LOG_TAG, getBaseContext(), toolbar, this);
		twitchNameView = (TextView) findViewById(R.id.general_current_twitch_name);
		startPageSubText = (TextView) findViewById(R.id.start_page_sub_text);
		showDonationSubText = (TextView) findViewById(R.id.show_donation_sub_text);

		initTwitchDisplayName();
		initStartPageText();
		updateSummeries();
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		this.overridePendingTransition(R.anim.fade_in_semi_anim, R.anim.slide_out_right_anim);
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		onBackPressed();
		return super.onOptionsItemSelected(item);
	}

	private void initStartPageText() {
		String startUpPageTitle = settings.getStartPage();
		Class startUpPageClass = Service.getClassFromStartPageTitle(this, startUpPageTitle);
		if (!settings.isLoggedIn() && (startUpPageClass == MyStreamsActivity.class || startUpPageClass == MyGamesActivity.class || startUpPageClass == MyChannelsActivity.class)) {
			startUpPageTitle = settings.getDefaultNotLoggedInStartUpPageTitle();
		}
		startPageSubText.setText(startUpPageTitle);
	}

	private void initTwitchDisplayName() {
		if (settings.isLoggedIn()) {
			twitchNameView.setText(settings.getGeneralTwitchDisplayName());
		} else {
			twitchNameView.setText(getString(R.string.gen_not_logged_in));
		}
	}

	public void onClickTwitchName(View v) {
		if (settings.isLoggedIn()) {
			MaterialDialog dialog = DialogService.getSettingsLoginOrLogoutDialig(this, settings.getGeneralTwitchDisplayName());
			dialog.getBuilder().onPositive(new MaterialDialog.SingleButtonCallback() {
				@Override
				public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
					dialog.dismiss();
					Service.clearStreamerInfoDb(getBaseContext());
					navigateToLogin();
				}
			});

			dialog.getBuilder().onNegative(new MaterialDialog.SingleButtonCallback() {
				@Override
				public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
					settings.setLogin(false);
					initTwitchDisplayName();
					initStartPageText();
					dialog.dismiss();
				}
			});

			dialog.show();
		} else {
			navigateToLogin();
		}
	}

	public void onClickStartPage(View v) {
		MaterialDialog dialog = DialogService.getChooseStartUpPageDialog(this, startPageSubText.getText().toString(), new MaterialDialog.ListCallbackSingleChoice() {
			@Override
			public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
				settings.setStartPage(text.toString());
				startPageSubText.setText(text.toString());
				return true;
			}
		});

		dialog.show();
	}

	private void navigateToLogin() {
		Intent loginIntent = new Intent(this, LoginActivity.class);
		loginIntent.putExtra(getString(R.string.login_intent_part_of_setup), false);

		startActivity(loginIntent);
	}

	public void onClickResetTips(View v) {
		if (settings.isTipsShown()) {
			View topView = findViewById(R.id.container_settings_general);
			if (topView != null) {
				Snackbar.make(topView, getString(R.string.gen_tips_have_been_reset), Snackbar.LENGTH_LONG).show();
			}
		}
		settings.setTipsShown(false);
	}

	public void onClickShowDonation(View v) {
		settings.setShowDonationPrompt(!settings.getShowDonationPrompt());
		updateSummeries();
	}

	private void updateSummeries() {
		if (settings.getShowDonationPrompt()) {
			showDonationSubText.setText(getString(R.string.yes));
		} else {
			showDonationSubText.setText(getString(R.string.no));
		}
	}


}
