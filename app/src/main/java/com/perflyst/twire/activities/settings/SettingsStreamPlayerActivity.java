package com.perflyst.twire.activities.settings;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.perflyst.twire.R;
import com.perflyst.twire.activities.ThemeActivity;
import com.perflyst.twire.service.Settings;

public class SettingsStreamPlayerActivity extends ThemeActivity {

    private Settings settings;
    private TextView mShowViewCountSummary, mShowNavigationBarSummary, mAutoPlaybackSummary;
    private CheckedTextView mShowViewCountView, mShowNavigationBarView, mAutoPlaybackView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_stream_player);

        settings = new Settings(getBaseContext());
        mShowNavigationBarView = findViewById(R.id.player_show_navigation_title);
        mShowViewCountView = findViewById(R.id.player_show_viewercount_title);
        mAutoPlaybackView = findViewById(R.id.player_auto_continue_playback_title);

        mShowViewCountSummary = findViewById(R.id.player_show_viewercount_title_summary);
        mShowNavigationBarSummary = findViewById(R.id.player_show_navigation_summary);
        mAutoPlaybackSummary = findViewById(R.id.player_auto_continue_playback_summary);

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
        updateSummary(mShowViewCountView, mShowViewCountSummary, settings.getStreamPlayerShowViewerCount());
        updateSummary(mShowNavigationBarView, mShowNavigationBarSummary, settings.getStreamPlayerShowNavigationBar());
        updateSummary(mAutoPlaybackView, mAutoPlaybackSummary, settings.getStreamPlayerAutoContinuePlaybackOnReturn());
    }

    public void onClickShowNavigationBar(View v) {
        settings.setStreamPlayerShowNavigationBar(!settings.getStreamPlayerShowNavigationBar());
        updateSummaries();
    }

    public void onClickShowViewerCount(View v) {
        settings.setStreamPlayerShowViewerCount(!settings.getStreamPlayerShowViewerCount());
        updateSummaries();
    }

    public void onClickAutoPlayback(View v) {
        settings.setStreamPlayerAutoContinuePlaybackOnReturn(!settings.getStreamPlayerAutoContinuePlaybackOnReturn());
        updateSummaries();
    }
}
