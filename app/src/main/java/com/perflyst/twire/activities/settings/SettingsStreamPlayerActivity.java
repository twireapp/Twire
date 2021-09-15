package com.perflyst.twire.activities.settings;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.afollestad.materialdialogs.MaterialDialog;
import com.perflyst.twire.R;
import com.perflyst.twire.activities.ThemeActivity;
import com.perflyst.twire.service.DialogService;
import com.perflyst.twire.service.Settings;

public class SettingsStreamPlayerActivity extends ThemeActivity {

    private Settings settings;
    private TextView mShowViewCountSummary, mShowNavigationBarSummary, mAutoPlaybackSummary, mShowRuntimeSummary, mPlayerTypeSummary, mUseProxySummary;
    private CheckedTextView mShowViewCountView, mShowNavigationBarView, mAutoPlaybackView, mShowRuntimeView, mUseProxy;
    private EditText mProxyUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_stream_player);

        settings = new Settings(getBaseContext());
        mShowNavigationBarView = findViewById(R.id.player_show_navigation_title);
        mShowViewCountView = findViewById(R.id.player_show_viewercount_title);
        mShowRuntimeView = findViewById(R.id.player_show_runtime);
        mAutoPlaybackView = findViewById(R.id.player_auto_continue_playback_title);
        mPlayerTypeSummary = findViewById(R.id.player_type_summary);
        mUseProxy = findViewById(R.id.player_use_proxy);
        mProxyUrl = findViewById(R.id.player_proxy_url_input);

        // set current API Url as Text
        mProxyUrl.setText(settings.getStreamPlayerProxyUrl());

        mShowViewCountSummary = findViewById(R.id.player_show_viewercount_title_summary);
        mShowRuntimeSummary = findViewById(R.id.player_show_runtime_summary);
        mShowNavigationBarSummary = findViewById(R.id.player_show_navigation_summary);
        mAutoPlaybackSummary = findViewById(R.id.player_auto_continue_playback_summary);
        mUseProxySummary = findViewById(R.id.player_use_proxy_summary);

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
        if (isEnabled) {
            summary.setText(getString(R.string.enabled));
        } else {
            summary.setText(getString(R.string.disabled));
        }
    }

    private void updateSummaries() {
        String[] types = getResources().getStringArray(R.array.PlayerType);
        mPlayerTypeSummary.setText(types[settings.getStreamPlayerType()]);
        updateSummary(mShowViewCountView, mShowViewCountSummary, settings.getStreamPlayerShowViewerCount());
        updateSummary(mShowRuntimeView, mShowRuntimeSummary, settings.getStreamPlayerRuntime());
        updateSummary(mShowNavigationBarView, mShowNavigationBarSummary, settings.getStreamPlayerShowNavigationBar());
        updateSummary(mAutoPlaybackView, mAutoPlaybackSummary, settings.getStreamPlayerAutoContinuePlaybackOnReturn());
        updateSummary(mUseProxy, mUseProxySummary, settings.getStreamPlayerUseProxy());
        mProxyUrl.setText(settings.getStreamPlayerProxyUrl());
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

    public void onClickVideoProxy(View v) {
        settings.setStreamPlayerVideoProxy(!settings.getStreamPlayerUseProxy());
        updateSummaries();
    }

    public void onClickProxyUrl(View v) {
        if (mProxyUrl.getText().toString().contains("http://") | mProxyUrl.getText().toString().contains("https://")) {
            settings.setStreamPlayerVideoProxyUrl(mProxyUrl.getText().toString());
            Log.d("Setting as API", mProxyUrl.getText().toString());
            updateSummaries();
        } else {
            Log.d("Url looks wrong", mProxyUrl.getText().toString());
        }
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

}
