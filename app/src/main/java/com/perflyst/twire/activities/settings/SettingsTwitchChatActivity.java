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

public class SettingsTwitchChatActivity extends ThemeActivity {
    private Settings settings;
    private TextView emoteSizeSummary, messageSizeSummary, chatLandscapeWidthSummary, chatLandscapeToggleSummary, chatLandscapeSwipeToShowSummary, chat_enable_ssl_summary;
    private CheckedTextView chatLandscapeToggle, chatSwipeToShowToggle, chat_enable_ssl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_twitch_chat);
        settings = new Settings(getBaseContext());

        final Toolbar toolbar = findViewById(R.id.settings_player_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        emoteSizeSummary = findViewById(R.id.chat_emote_size_summary);
        messageSizeSummary = findViewById(R.id.message_size_summary);
        chatLandscapeWidthSummary = findViewById(R.id.chat_landscape_summary);
        chatLandscapeToggleSummary = findViewById(R.id.chat_landscape_enable_summary);
        chatLandscapeSwipeToShowSummary = findViewById(R.id.chat_landscape_swipe_summary);
        chat_enable_ssl_summary = findViewById(R.id.chat_enable_ssl_summary);

        chatLandscapeToggle = findViewById(R.id.chat_landscape_enable_title);
        chatSwipeToShowToggle = findViewById(R.id.chat_landscape_swipe_title);
        chat_enable_ssl = findViewById(R.id.chat_enable_ssl);
        updateSummaries();
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
        String[] sizes = getResources().getStringArray(R.array.ChatSize);
        emoteSizeSummary.setText(sizes[settings.getEmoteSize() - 1]);
        messageSizeSummary.setText(sizes[settings.getMessageSize() - 1]);
        chatLandscapeWidthSummary.setText(String.format(getString(R.string.percent), settings.getChatLandscapeWidth()));

        // Chat enabled in landscape
        updateSummary(chatLandscapeToggle, chatLandscapeToggleSummary, settings.isChatInLandscapeEnabled());
        // Chat showable by swiping
        updateSummary(chatSwipeToShowToggle, chatLandscapeSwipeToShowSummary, settings.isChatLandscapeSwipeable());
        // Chat SSL Enabled
        updateSummary(chat_enable_ssl, chat_enable_ssl_summary, settings.getChatEnableSSL());
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

    public void onClickEmoteSize(View _view) {
        MaterialDialog dialog = DialogService.getChooseChatSizeDialog
                (this, R.string.chat_emote_size, R.array.ChatSize, settings.getEmoteSize(), (dialog1, itemView, which, text) -> {
                    settings.setEmoteSize(which + 1);
                    updateSummaries();
                    return true;
                });
        dialog.show();
    }

    public void onClickMessageSize(View _view) {
        MaterialDialog dialog = DialogService.getChooseChatSizeDialog
                (this, R.string.chat_message_size, R.array.ChatSize, settings.getMessageSize(), (dialog1, itemView, which, text) -> {
                    settings.setMessageSize(which + 1);
                    updateSummaries();
                    return true;
                });
        dialog.show();
    }

    public void onClickChatLandscapeEnable(View _view) {
        settings.setShowChatInLandscape(!settings.isChatInLandscapeEnabled());
        updateSummaries();
    }

    public void onClickChatLandscapeSwipeable(View _view) {
        settings.setChatLandscapeSwipeable(!settings.isChatLandscapeSwipeable());
        updateSummaries();
    }

    public void onClickChatEnableSSL(View _view) {
        settings.setChatEnableSSL(!settings.getChatEnableSSL());
        updateSummaries();
    }

    public void onClickChatLandScapeWidth(View _view) {
        final int landscapeWidth = settings.getChatLandscapeWidth();

        DialogService.getSliderDialog(
                this,
                (dialog, which) -> {
                    settings.setChatLandscapeWidth(landscapeWidth);
                    updateSummaries();
                },
                (view, fromUser, oldPos, newPos, oldValue, newValue) -> {
                    settings.setChatLandscapeWidth(newValue);
                    updateSummaries();
                },
                landscapeWidth,
                25,
                60,
                getString(R.string.chat_landscape_width_dialog)
        ).show();
    }
}
