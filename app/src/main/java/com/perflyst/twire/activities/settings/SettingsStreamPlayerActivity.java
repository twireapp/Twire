package com.perflyst.twire.activities.settings;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.afollestad.materialdialogs.MaterialDialog;
import com.perflyst.twire.R;
import com.perflyst.twire.activities.ThemeActivity;
import com.perflyst.twire.service.DialogService;
import com.perflyst.twire.service.Settings;

import java.util.Arrays;
import java.util.List;

public class SettingsStreamPlayerActivity extends ThemeActivity {

    private Settings settings;
    private TextView mShowViewCountSummary, mShowNavigationBarSummary, mAutoPlaybackSummary, mLockedPlaybackSummary, mShowRuntimeSummary, mPlayerTypeSummary, mPlayerProxySummary;
    private CheckedTextView mShowViewCountView, mShowNavigationBarView, mAutoPlaybackView, mLockedPlaybackView, mShowRuntimeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_stream_player);

        settings = new Settings(getBaseContext());
        mShowNavigationBarView = findViewById(R.id.player_show_navigation_title);
        mShowViewCountView = findViewById(R.id.player_show_viewercount_title);
        mShowRuntimeView = findViewById(R.id.player_show_runtime);
        mAutoPlaybackView = findViewById(R.id.player_auto_continue_playback_title);
        mLockedPlaybackView = findViewById(R.id.player_locked_playback_title);
        mPlayerTypeSummary = findViewById(R.id.player_type_summary);
        mPlayerProxySummary = findViewById(R.id.player_proxy_summary);

        mShowViewCountSummary = findViewById(R.id.player_show_viewercount_title_summary);
        mShowRuntimeSummary = findViewById(R.id.player_show_runtime_summary);
        mShowNavigationBarSummary = findViewById(R.id.player_show_navigation_summary);
        mAutoPlaybackSummary = findViewById(R.id.player_auto_continue_playback_summary);
        mLockedPlaybackSummary = findViewById(R.id.player_locked_playback_summary);

        final Toolbar toolbar = findViewById(R.id.settings_player_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.settings_stream_player_name));
        }

        updateSummaries();
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
        String[] types = getResources().getStringArray(R.array.PlayerType);
        mPlayerTypeSummary.setText(types[settings.getStreamPlayerType()]);
        mPlayerProxySummary.setText(convertProxyOption(settings.getStreamPlayerProxy()));
        updateSummary(mShowViewCountView, mShowViewCountSummary, settings.getStreamPlayerShowViewerCount());
        updateSummary(mShowRuntimeView, mShowRuntimeSummary, settings.getStreamPlayerRuntime());
        updateSummary(mShowNavigationBarView, mShowNavigationBarSummary, settings.getStreamPlayerShowNavigationBar());
        updateSummary(mAutoPlaybackView, mAutoPlaybackSummary, settings.getStreamPlayerAutoContinuePlaybackOnReturn());
        updateSummary(mLockedPlaybackView, mLockedPlaybackSummary, settings.getStreamPlayerLockedPlayback());
    }

    public void onClickShowNavigationBar(View v) {
        settings.setStreamPlayerShowNavigationBar(!settings.getStreamPlayerShowNavigationBar());
        updateSummaries();
    }

    public void onClickShowViewerCount(View v) {
        settings.setStreamPlayerShowViewerCount(!settings.getStreamPlayerShowViewerCount());
        updateSummaries();
    }

    public void onClickShowRuntime(View v) {
        settings.setStreamPlayerRuntime(!settings.getStreamPlayerRuntime());
        updateSummaries();
    }

    public void onClickAutoPlayback(View v) {
        settings.setStreamPlayerAutoContinuePlaybackOnReturn(!settings.getStreamPlayerAutoContinuePlaybackOnReturn());
        updateSummaries();
    }

    public void onClickLockedPlayback(View v) {
        settings.setStreamPlayerLockedPlayback(!settings.getStreamPlayerLockedPlayback());
        updateSummaries();
    }

    public void onClickPlayerType(View _view) {
        MaterialDialog dialog = DialogService.getChoosePlayerTypeDialog
                (this, R.string.player_type, R.array.PlayerType, settings.getStreamPlayerType(), (dialog1, itemView, which, text) -> {
                    settings.setStreamPlayerType(which);
                    updateSummaries();
                    return true;
                });
        dialog.show();
    }

    private String convertProxyOption(String option) {
        if (option.equals("custom")) return getString(R.string.player_proxy_custom);
        else if (option.isEmpty()) return getString(R.string.disabled);
        else return option;
    }

    public void onClickPlayerProxy(View _view) {
        List<String> proxies = Arrays.asList(getResources().getStringArray(R.array.PlayerProxies));
        int selectedIndex = proxies.indexOf(settings.getStreamPlayerProxy());
        // Since the custom proxy is not in the presets, we need to select the custom option
        if (selectedIndex == -1) {
            selectedIndex = proxies.size() - 1;
        }

        DialogService.getBaseThemedDialog(this)
                .title(R.string.player_proxy)
                .items(proxies.stream().map(this::convertProxyOption).toArray(String[]::new))
                .itemsCallbackSingleChoice(selectedIndex, (dialog, itemView, which, text) -> {
                    if (which == proxies.size() - 1) {
                        DialogService.getBaseThemedDialog(this)
                                .title(R.string.player_proxy_custom)
                                .input("https://example.com", settings.getStreamPlayerProxy(), false, (dialog1, input) -> {
                                    settings.setStreamPlayerProxy(input.toString());
                                    updateSummaries();
                                })
                                .show();
                    } else {
                        settings.setStreamPlayerProxy(proxies.get(which));
                        updateSummaries();
                    }

                    return true;
                })
                .show();
    }
}
