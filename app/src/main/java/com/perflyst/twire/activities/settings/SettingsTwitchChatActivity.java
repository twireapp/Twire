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
import com.perflyst.twire.databinding.ActivitySettingsTwitchChatBinding;
import com.perflyst.twire.misc.Utils;
import com.perflyst.twire.service.DialogService;
import com.perflyst.twire.service.Settings;

public class SettingsTwitchChatActivity extends ThemeActivity {
    private TextView emoteSizeSummary, messageSizeSummary, chatLandscapeWidthSummary, chatLandscapeToggleSummary, chatLandscapeSwipeToShowSummary, chat_enable_ssl_summary, chat_enable_account_connect_summary, chat_enable_emote_bbtv_summary, chat_enable_emote_ffz_summary, chat_enable_emote_seventv_summary;
    private CheckedTextView chatLandscapeToggle, chatSwipeToShowToggle, chat_enable_ssl, chat_enable_account_connect, chat_enable_emote_bbtv, chat_enable_emote_ffz, chat_enable_emote_seventv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        var binding = ActivitySettingsTwitchChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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
        chat_enable_emote_bbtv_summary = findViewById(R.id.chat_enable_emote_bttv_summary);
        chat_enable_emote_ffz_summary = findViewById(R.id.chat_enable_emote_ffz_summary);
        chat_enable_emote_seventv_summary = findViewById(R.id.chat_enable_emote_seventv_summary);
        chat_enable_account_connect_summary = findViewById(R.id.chat_enable_account_connect_summary);


        chatLandscapeToggle = findViewById(R.id.chat_landscape_enable_title);
        chatSwipeToShowToggle = findViewById(R.id.chat_landscape_swipe_title);
        chat_enable_ssl = findViewById(R.id.chat_enable_ssl);
        chat_enable_emote_bbtv = findViewById(R.id.chat_enable_emote_bttv);
        chat_enable_emote_ffz = findViewById(R.id.chat_enable_emote_ffz);
        chat_enable_emote_seventv = findViewById(R.id.chat_enable_emote_seventv);
        chat_enable_account_connect = findViewById(R.id.chat_enable_account_connect);

        updateSummaries();

        binding.emoteSizeButton.setOnClickListener(this::onClickEmoteSize);
        binding.messageSizeButton.setOnClickListener(this::onClickMessageSize);
        binding.landscapeEnableButton.setOnClickListener(this::onClickChatLandscapeEnable);
        binding.landscapeSwipeButton.setOnClickListener(this::onClickChatLandscapeSwipeable);
        binding.landscapeWidthButton.setOnClickListener(this::onClickChatLandScapeWidth);
        binding.enableSslButton.setOnClickListener(this::onClickChatEnableSSL);
        binding.accountConnectButton.setOnClickListener(this::onClickChatAccountConnect);
        binding.emoteBttvButton.setOnClickListener(this::onClickChatEmoteBTTV);
        binding.emoteFfzButton.setOnClickListener(this::onClickChatEmoteFFZ);
        binding.emoteSeventvButton.setOnClickListener(this::onClickChatEmoteSEVENTV);
    }

    private void updateSummary(CheckedTextView checkView, TextView summary, boolean isEnabled) {
        checkView.setChecked(isEnabled);
        summary.setText(isEnabled ? R.string.enabled : R.string.disabled);
    }

    private void updateSummaries() {
        String[] sizes = getResources().getStringArray(R.array.ChatSize);
        emoteSizeSummary.setText(sizes[Settings.getEmoteSize() - 1]);
        messageSizeSummary.setText(sizes[Settings.getMessageSize() - 1]);
        Utils.setPercent(chatLandscapeWidthSummary, Settings.getChatLandscapeWidth() / 100f);

        // Chat enabled in landscape
        updateSummary(chatLandscapeToggle, chatLandscapeToggleSummary, Settings.isChatInLandscapeEnabled());
        // Chat showable by swiping
        updateSummary(chatSwipeToShowToggle, chatLandscapeSwipeToShowSummary, Settings.isChatLandscapeSwipeable());
        // Chat SSL Enabled
        updateSummary(chat_enable_ssl, chat_enable_ssl_summary, Settings.getChatEnableSSL());
        // Update Chat Emote Stuff
        updateSummary(chat_enable_emote_bbtv, chat_enable_emote_bbtv_summary, Settings.getChatEmoteBTTV());
        updateSummary(chat_enable_emote_ffz, chat_enable_emote_ffz_summary, Settings.getChatEmoteFFZ());
        updateSummary(chat_enable_emote_seventv, chat_enable_emote_seventv_summary, Settings.getChatEmoteSEVENTV());
        // Chat enable Login with Account
        updateSummary(chat_enable_account_connect, chat_enable_account_connect_summary, Settings.getChatAccountConnect());
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
                (this, R.string.chat_emote_size, R.array.ChatSize, Settings.getEmoteSize(), (dialog1, itemView, which, text) -> {
                    Settings.setEmoteSize(which + 1);
                    updateSummaries();
                    return true;
                });
        dialog.show();
    }

    public void onClickMessageSize(View _view) {
        MaterialDialog dialog = DialogService.getChooseChatSizeDialog
                (this, R.string.chat_message_size, R.array.ChatSize, Settings.getMessageSize(), (dialog1, itemView, which, text) -> {
                    Settings.setMessageSize(which + 1);
                    updateSummaries();
                    return true;
                });
        dialog.show();
    }

    public void onClickChatLandscapeEnable(View _view) {
        Settings.setChatInLandscapeEnabled(!Settings.isChatInLandscapeEnabled());
        updateSummaries();
    }

    public void onClickChatLandscapeSwipeable(View _view) {
        Settings.setChatLandscapeSwipeable(!Settings.isChatLandscapeSwipeable());
        updateSummaries();
    }

    public void onClickChatEnableSSL(View _view) {
        Settings.setChatEnableSSL(!Settings.getChatEnableSSL());
        updateSummaries();
    }


    public void onClickChatEmoteBTTV(View _view) {
        Settings.setChatEmoteBTTV(!Settings.getChatEmoteBTTV());
        updateSummaries();
    }

    public void onClickChatEmoteFFZ(View _view) {
        Settings.setChatEmoteFFZ(!Settings.getChatEmoteFFZ());
        updateSummaries();
    }

    public void onClickChatEmoteSEVENTV(View _view) {
        Settings.setChatEmoteSEVENTV(!Settings.getChatEmoteSEVENTV());
        updateSummaries();
    }

    public void onClickChatAccountConnect(View _view) {
        Settings.setChatAccountConnect(!Settings.getChatAccountConnect());
        updateSummaries();
    }

    public void onClickChatLandScapeWidth(View _view) {
        final int landscapeWidth = Settings.getChatLandscapeWidth();

        DialogService.getSliderDialog(
                this,
                (dialog, which) -> {
                    Settings.setChatLandscapeWidth(landscapeWidth);
                    updateSummaries();
                },
                (view, fromUser, oldPos, newPos, oldValue, newValue) -> {
                    Settings.setChatLandscapeWidth(newValue);
                    updateSummaries();
                },
                landscapeWidth,
                10,
                60,
                getString(R.string.chat_landscape_width_dialog)
        ).show();
    }
}
