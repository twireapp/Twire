package com.perflyst.twire.activities.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.snackbar.Snackbar;
import com.perflyst.twire.R;
import com.perflyst.twire.activities.ThemeActivity;
import com.perflyst.twire.activities.main.MyChannelsActivity;
import com.perflyst.twire.activities.main.MyGamesActivity;
import com.perflyst.twire.activities.main.MyStreamsActivity;
import com.perflyst.twire.activities.setup.LoginActivity;
import com.perflyst.twire.service.DialogService;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.service.Settings;

public class SettingsGeneralActivity extends ThemeActivity {
    private String LOG_TAG = getClass().getSimpleName();
    private Settings settings;
    private TextView twitchNameView, startPageSubText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_general);
        settings = new Settings(getBaseContext());

        final Toolbar toolbar = findViewById(R.id.settings_general_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        //Service.isTranslucentActionbar(LOG_TAG, getBaseContext(), toolbar, this);
        twitchNameView = findViewById(R.id.general_current_twitch_name);
        startPageSubText = findViewById(R.id.start_page_sub_text);

        initTwitchDisplayName();
        initStartPageText();
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
            dialog.getBuilder().onPositive((dialog1, which) -> {
                dialog1.dismiss();
                Service.clearStreamerInfoDb(getBaseContext());
                navigateToLogin();
            });

            dialog.getBuilder().onNegative((dialog12, which) -> {
                settings.setLogin(false);
                initTwitchDisplayName();
                initStartPageText();
                dialog12.dismiss();
            });

            dialog.show();
        } else {
            navigateToLogin();
        }
    }

    public void onClickStartPage(View v) {
        MaterialDialog dialog = DialogService.getChooseStartUpPageDialog
                (this, startPageSubText.getText().toString(), (dialog1, view, which, text) -> {
                    settings.setStartPage(text.toString());
                    startPageSubText.setText(text.toString());
                    return true;
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

}
