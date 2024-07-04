package com.perflyst.twire.activities.settings;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.snackbar.Snackbar;
import com.perflyst.twire.R;
import com.perflyst.twire.activities.ThemeActivity;
import com.perflyst.twire.activities.setup.LoginActivity;
import com.perflyst.twire.databinding.ActivitySettingsGeneralBinding;
import com.perflyst.twire.fragments.ChangelogDialogFragment;
import com.perflyst.twire.service.DialogService;
import com.perflyst.twire.service.ReportErrors;
import com.perflyst.twire.service.Settings;
import com.perflyst.twire.service.SubscriptionsDbHelper;

import java.util.regex.Matcher;

import timber.log.Timber;

public class SettingsGeneralActivity extends ThemeActivity {
    private TextView twitchNameView, startPageSubText, general_image_proxy_summary, errorReportSubText;
    private CheckedTextView filterTopStreamsByLanguageView, general_image_proxy;
    private EditText mImageProxyUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        var binding = ActivitySettingsGeneralBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        final Toolbar toolbar = findViewById(R.id.settings_general_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        twitchNameView = findViewById(R.id.general_current_twitch_name);
        startPageSubText = findViewById(R.id.start_page_sub_text);
        filterTopStreamsByLanguageView = findViewById(R.id.language_filter_title);

        general_image_proxy_summary = findViewById(R.id.general_image_proxy_summary);
        general_image_proxy = findViewById(R.id.general_image_proxy);
        mImageProxyUrl = findViewById(R.id.image_proxy_url_input);
        errorReportSubText = binding.errorReportSubText;

        updateSummaries();

        initTwitchDisplayName();
        initStartPageText();
        initFilterTopsStreamsByLanguage();

        binding.twitchNameButton.setOnClickListener(this::onClickTwitchName);
        binding.startPageButton.setOnClickListener(this::onClickStartPage);
        binding.resetTipsButton.setOnClickListener(this::onClickResetTips);
        binding.languageFilterButton.setOnClickListener(this::onClickFiltersStreamsByLanguageEnable);
        binding.changelogButton.setOnClickListener(this::onClickOpenChangelog);
        binding.imageProxyButton.setOnClickListener(this::onClickImageProxy);
        binding.imageProxyUrlButton.setOnClickListener(this::onClickImageProxyUrl);
        binding.wipeFollowsButton.setOnClickListener(this::onClickWipeFollows);
        binding.exportFollowsButton.setOnClickListener(this::onExport);
        binding.importFollowsButton.setOnClickListener(this::onImport);
        binding.errorReportButton.setOnClickListener((_view) -> {
            DialogService.getChooseDialog(this, (R.string.report_error_title), R.array.ErrorReportOptions, Settings.getReportErrors().ordinal(), (dialog, view, which, text) -> {
                Settings.setReportErrors(ReportErrors.getEntries().get(which));
                updateSummaries();
                return false;
            }).show();
        });
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

    private void updateSummary(CheckedTextView checkView, TextView summary, boolean isEnabled) {
        checkView.setChecked(isEnabled);
        summary.setText(isEnabled ? R.string.enabled : R.string.disabled);
    }

    private void updateSummaries() {
        updateSummary(general_image_proxy, general_image_proxy_summary, Settings.getGeneralUseImageProxy());
        mImageProxyUrl.setText(Settings.getImageProxyUrl());
        errorReportSubText.setText(Settings.getReportErrors().getStringRes());
    }

    private void initStartPageText() {
        startPageSubText.setText(Settings.getStartPage());
    }

    private void initTwitchDisplayName() {
        if (Settings.isLoggedIn()) {
            twitchNameView.setText(Settings.getGeneralTwitchDisplayName());
        } else {
            twitchNameView.setText(R.string.gen_not_logged_in);
        }
    }

    private void initFilterTopsStreamsByLanguage() {
        filterTopStreamsByLanguageView.setChecked(Settings.getGeneralFilterTopStreamsByLanguage());
    }

    public void onClickTwitchName(View v) {
        if (Settings.isLoggedIn()) {
            MaterialDialog dialog = DialogService.getSettingsLoginOrLogoutDialog(this, Settings.getGeneralTwitchDisplayName());
            dialog.getBuilder().onPositive((dialog1, which) -> navigateToLogin());

            dialog.getBuilder().onNegative((dialog12, which) -> {
                Settings.setLoggedIn(false);
                initTwitchDisplayName();
            });

            dialog.show();
        } else {
            navigateToLogin();
        }
    }

    public void onClickStartPage(View v) {
        MaterialDialog dialog = DialogService.getChooseStartUpPageDialog
                (this, startPageSubText.getText().toString(), (dialog1, view, which, text) -> {
                    Settings.setStartPage(text.toString());
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
        if (Settings.isTipsShown()) {
            View topView = findViewById(R.id.container_settings_general);
            if (topView != null) {
                Snackbar.make(topView, getString(R.string.gen_tips_have_been_reset), Snackbar.LENGTH_LONG).show();
            }
        }
        Settings.setTipsShown(false);
    }

    public void onClickFiltersStreamsByLanguageEnable(View v) {
        Settings.setGeneralFilterTopStreamsByLanguage(!Settings.getGeneralFilterTopStreamsByLanguage());
        initFilterTopsStreamsByLanguage();
    }

    public void onClickOpenChangelog(View v) {
        new ChangelogDialogFragment().show(getSupportFragmentManager(), "ChangelogDialog");
    }

    // Database Stuff below

    public void onClickWipeFollows(View v) {
        MaterialDialog dialog = DialogService.getSettingsWipeFollowsDialog(this);
        dialog.getBuilder().onPositive((dialog1, which) -> {
            SubscriptionsDbHelper helper = new SubscriptionsDbHelper(getBaseContext());
            helper.onWipe(helper.getWritableDatabase(), Settings.isLoggedIn());
            Toast infoToast = Toast.makeText(getBaseContext(), getString(R.string.gen_toast_wipe_database), Toast.LENGTH_SHORT);
            infoToast.show();
        }).show();
    }

    // Export/Import for Follows

    public void onExport(View v) {
        MaterialDialog dialog = DialogService.getSettingsExportFollowsDialog(this);
        dialog.getBuilder().onPositive((dialog1, which) -> {
            SubscriptionsDbHelper helper = new SubscriptionsDbHelper(getBaseContext());
            int exported = helper.onExport(helper.getWritableDatabase());
            Toast infoToast = Toast.makeText(getBaseContext(), String.format(getString(R.string.gen_toast_export_database), exported), Toast.LENGTH_SHORT);
            infoToast.show();
        }).show();
    }

    public void onImport(View v) {
        MaterialDialog dialog = DialogService.getSettingsImportFollowsDialog(this);
        dialog.getBuilder().onPositive((dialog1, which) -> {
            SubscriptionsDbHelper helper = new SubscriptionsDbHelper(getBaseContext());
            int imported = helper.onImport(helper.getWritableDatabase());
            Toast infoToast = Toast.makeText(getBaseContext(), String.format(getString(R.string.gen_toast_import_database), imported), Toast.LENGTH_SHORT);
            infoToast.show();
        }).show();
    }

    public void onClickImageProxy(View v) {
        Settings.setGeneralUseImageProxy(!Settings.getGeneralUseImageProxy());
        updateSummaries();
    }

    public void onClickImageProxyUrl(View v) {
        String proxy_url = mImageProxyUrl.getText().toString();

        // app/src/main/java/com/perflyst/twire/activities/DeepLinkActivity.java 115
        Matcher matcher = Patterns.WEB_URL.matcher(proxy_url);
        if (matcher.find()) {
            Settings.setImageProxyUrl(proxy_url);
            Timber.d("Setting as Image Proxy: %s", proxy_url);
            updateSummaries();
        } else {
            Timber.d("Url looks wrong%s", proxy_url);
        }
    }

}
