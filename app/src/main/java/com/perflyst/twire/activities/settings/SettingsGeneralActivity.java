package com.perflyst.twire.activities.settings;

import static java.security.AccessController.getContext;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.snackbar.Snackbar;
import com.perflyst.twire.R;
import com.perflyst.twire.activities.ThemeActivity;
import com.perflyst.twire.activities.main.MyChannelsActivity;
import com.perflyst.twire.activities.main.MyStreamsActivity;
import com.perflyst.twire.activities.setup.LoginActivity;
import com.perflyst.twire.fragments.ChangelogDialogFragment;
import com.perflyst.twire.service.DialogService;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.service.Settings;
import com.perflyst.twire.service.SubscriptionsDbHelper;

public class SettingsGeneralActivity extends ThemeActivity {
    private Settings settings;
    private TextView twitchNameView, startPageSubText;
    private CheckedTextView filterTopStreamsByLanguageView;

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
        filterTopStreamsByLanguageView = findViewById(R.id.language_filter_title);

        initTwitchDisplayName();
        initStartPageText();
        initFilterTopsStreamsByLanguage();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.overridePendingTransition(R.anim.fade_in_semi_anim, R.anim.slide_out_right_anim);
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    private void initStartPageText() {
        String startUpPageTitle = settings.getStartPage();
        Class startUpPageClass = Service.getClassFromStartPageTitle(this, startUpPageTitle);
        if (!settings.isLoggedIn() && (startUpPageClass == MyStreamsActivity.class || startUpPageClass == MyChannelsActivity.class)) {
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

    private void initFilterTopsStreamsByLanguage() {
        filterTopStreamsByLanguageView.setChecked(settings.getGeneralFilterTopStreamsByLanguage());
    }

    public void onClickTwitchName(View v) {
        if (settings.isLoggedIn()) {
            MaterialDialog dialog = DialogService.getSettingsLoginOrLogoutDialog(this, settings.getGeneralTwitchDisplayName());
            dialog.getBuilder().onPositive((dialog1, which) -> {
                dialog1.dismiss();
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

    public void onClickFiltersStreamsByLanguageEnable(View v) {
        settings.setGeneralFilterTopStreamsByLanguage(!settings.getGeneralFilterTopStreamsByLanguage());
        initFilterTopsStreamsByLanguage();
    }

    public void onClickOpenChangelog(View v) {
        new ChangelogDialogFragment().show(getSupportFragmentManager(), "ChangelogDialog");
    }

    // Database Stuff below

    public void onClickWipeFollows(View v) {
        MaterialDialog dialog = DialogService.getSettingsWipeFollowsDialog(this);
        dialog.getBuilder().onPositive((dialog1, which) -> {
            dialog1.dismiss();
            SubscriptionsDbHelper helper = new SubscriptionsDbHelper(getBaseContext());
            if (settings.isLoggedIn()) {
                helper.onWipe(helper.getWritableDatabase(), true);
            } else {
                helper.onWipe(helper.getWritableDatabase(), false);
            }
            Toast infoToast = Toast.makeText(getBaseContext(),"Wiped Follows", Toast.LENGTH_SHORT);
            infoToast.show();
        });

        dialog.getBuilder().onNegative((dialog2, which) -> {
            dialog2.dismiss();
        });

        dialog.show();
    }

    // Export/Import for Follows

    public void onExport(View v) {
        MaterialDialog dialog = DialogService.getSettingsExportFollowsDialog(this);
        dialog.getBuilder().onPositive((dialog1, which) -> {
            dialog1.dismiss();
            SubscriptionsDbHelper helper = new SubscriptionsDbHelper(getBaseContext());
            int exported = helper.onExport(helper.getWritableDatabase());
            Toast infoToast = Toast.makeText(getBaseContext(),"Exported " + exported + " Follows", Toast.LENGTH_SHORT);
            infoToast.show();
        });

        dialog.getBuilder().onNegative((dialog2, which) -> {
            dialog2.dismiss();
        });

        dialog.show();
    }

    public void onImport(View v) {
        MaterialDialog dialog = DialogService.getSettingsImportFollowsDialog(this);
        dialog.getBuilder().onPositive((dialog1, which) -> {
            dialog1.dismiss();
            SubscriptionsDbHelper helper = new SubscriptionsDbHelper(getBaseContext());
            int imported = helper.onImport(helper.getWritableDatabase());
            Toast infoToast = Toast.makeText(getBaseContext(),"Imported " + imported + " Follows", Toast.LENGTH_SHORT);
            infoToast.show();
        });

        dialog.getBuilder().onNegative((dialog2, which) -> {
            dialog2.dismiss();
        });

        dialog.show();
    }

}
