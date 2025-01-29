package com.perflyst.twire.fragments;


import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;

import com.afollestad.materialdialogs.MaterialDialog;
import com.perflyst.twire.R;
import com.perflyst.twire.service.DialogService;
import com.perflyst.twire.service.Settings;

public class AppearanceSettingsFragment extends Fragment {
    private TextView themeSummary, streamsStyleSummary, gameStyleSummary, followStyleSummary, streamSizeSummary, gameSizeSummary, streamerSizeSummary;
    private ImageView themeSummaryColor;

    public static AppearanceSettingsFragment newInstance() {
        return new AppearanceSettingsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_appearance_settings, container, false);
        themeSummary = rootView.findViewById(R.id.appearance_theme_color_summary);
        themeSummaryColor = rootView.findViewById(R.id.appearance_theme_color);

        streamsStyleSummary = rootView.findViewById(R.id.appearance_streams_style_summary);
        gameStyleSummary = rootView.findViewById(R.id.appearance_game_style_summary);
        followStyleSummary = rootView.findViewById(R.id.appearance_streamer_style_summary);

        streamSizeSummary = rootView.findViewById(R.id.appearance_streams_size_summary);
        gameSizeSummary = rootView.findViewById(R.id.appearance_game_size_summary);
        streamerSizeSummary = rootView.findViewById(R.id.appearance_streamer_size_summary);

        initSummaries();
        initOnClicks(rootView);

        return rootView;
    }

    private void initOnClicks(View rootView) {
        final View themeColorWrapper = rootView.findViewById(R.id.appearance_theme_color_wrapper);
        themeColorWrapper.setOnClickListener(v -> onClickThemeColor());

        final View streamStyleWrapper = rootView.findViewById(R.id.appearance_streams_style_wrapper);
        streamStyleWrapper.setOnClickListener(view -> onClickStreamStyle());

        final View streamSizeWrapper = rootView.findViewById(R.id.appearance_streams_size_wrapper);
        streamSizeWrapper.setOnClickListener(v -> onClickStreamSize());

        final View gameStyleWrapper = rootView.findViewById(R.id.appearance_game_style_wrapper);
        gameStyleWrapper.setOnClickListener(view -> onClickGameStyle());

        final View gameSizeWrapper = rootView.findViewById(R.id.appearance_game_size_wrapper);
        gameSizeWrapper.setOnClickListener(v1 -> onClickGameSize());

        final View streamerStyleWrapper = rootView.findViewById(R.id.appearance_streamer_style_wrapper);
        streamerStyleWrapper.setOnClickListener(view -> onClickStreamerStyle());

        final View streamerSizeWrapper = rootView.findViewById(R.id.appearance_streamer_size_wrapper);
        streamerSizeWrapper.setOnClickListener(v -> onClickStreamerSize());
    }

    private void initSummaries() {
        // Theme Summary
        themeSummary.setText(Settings.getTheme().getNameRes());
        themeSummaryColor.setImageDrawable(AppCompatResources.getDrawable(requireContext(), Settings.getTheme().chooser));

        // Style Summary
        streamsStyleSummary.setText(Settings.getAppearanceStreamStyle());
        gameStyleSummary.setText(Settings.getAppearanceGameStyle());
        followStyleSummary.setText(Settings.getAppearanceChannelStyle());

        // Size Summary
        streamSizeSummary.setText(Settings.getAppearanceStreamSize());
        gameSizeSummary.setText(Settings.getAppearanceGameSize());
        streamerSizeSummary.setText(Settings.getAppearanceChannelSize());
    }

    private void onClickThemeColor() {
        MaterialDialog themeChooserDialog = DialogService.getThemeDialog(getActivity());
        themeChooserDialog.show();
    }

    private void onClickStreamStyle() {
        DialogService.getChooseStreamCardStyleDialog(getActivity(), (title, index, previewView) -> {
            View sharedPadding = previewView.findViewById(R.id.shared_padding);
            ImageView view1 = previewView.findViewById(R.id.image_stream_preview);
            view1.setImageResource(R.drawable.preview_stream);
            View streamTitle = previewView.findViewById(R.id.stream_title);
            View viewersAndGame = previewView.findViewById(R.id.stream_game_and_viewers);

            if (title.equals(getString(R.string.card_style_expanded))) {
                streamTitle.setVisibility(View.VISIBLE);
                viewersAndGame.setVisibility(View.VISIBLE);
                sharedPadding.setVisibility(View.VISIBLE);
            } else if (title.equals(getString(R.string.card_style_normal))) {
                streamTitle.setVisibility(View.GONE);
                viewersAndGame.setVisibility(View.VISIBLE);
                sharedPadding.setVisibility(View.VISIBLE);
            } else if (title.equals(getString(R.string.card_style_minimal))) {
                streamTitle.setVisibility(View.GONE);
                viewersAndGame.setVisibility(View.GONE);
                sharedPadding.setVisibility(View.GONE);
            }

            Settings.setAppearanceStreamStyle(title);
            initSummaries();

        }).show();
    }

    private void onClickGameStyle() {
        DialogService.getChooseGameCardStyleDialog(getActivity(), (title, index, previewView) -> {
            View sharedPadding = previewView.findViewById(R.id.shared_padding);
            ImageView view1 = previewView.findViewById(R.id.image_game_preview);
            view1.setImageResource(R.drawable.preview_game);

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    (int) requireContext().getResources().getDimension(R.dimen.game_preview_width),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp.gravity = Gravity.CENTER;
            previewView.setLayoutParams(lp);

            View gameTitle = previewView.findViewById(R.id.game_card_title);
            /*
            TextView gameViewers = previewView.findViewById(R.id.game_viewers);
            if (gameViewers.getText().equals("")) {
                gameViewers.setText(R.string.preview_game_viewers);
            }
             */

            if (title.equals(getString(R.string.card_style_expanded))) {
                gameTitle.setVisibility(View.VISIBLE);
                //gameViewers.setVisibility(View.VISIBLE);
                sharedPadding.setVisibility(View.VISIBLE);
            } else if (title.equals(getString(R.string.card_style_normal))) {
                gameTitle.setVisibility(View.GONE);
                //gameViewers.setVisibility(View.VISIBLE);
                sharedPadding.setVisibility(View.VISIBLE);
            } else if (title.equals(getString(R.string.card_style_minimal))) {
                gameTitle.setVisibility(View.GONE);
                //gameViewers.setVisibility(View.GONE);
                sharedPadding.setVisibility(View.GONE);
            }

            Settings.setAppearanceGameStyle(title);
            initSummaries();
        }).show();
    }

    private void onClickStreamerStyle() {
        DialogService.getChooseStreamerCardStyleDialog(getActivity(), (title, index, previewView) -> {
            View nameView = previewView.findViewById(R.id.displayName);
            ImageView streamerLogo = previewView.findViewById(R.id.profileLogoImageView);
            streamerLogo.setImageResource(R.drawable.preview_streamer);

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    (int) requireContext().getResources().getDimension(R.dimen.subscription_card_preview_width),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp.gravity = Gravity.CENTER;
            previewView.setLayoutParams(lp);

            if (title.equals(getString(R.string.card_style_normal))) {
                nameView.setVisibility(View.VISIBLE);
            } else if (title.equals(getString(R.string.card_style_minimal))) {
                nameView.setVisibility(View.GONE);
            }

            Settings.setAppearanceChannelStyle(title);
            initSummaries();
        }).show();
    }

    private void onClickStreamSize() {
        DialogService.getChooseCardSizeDialog(
                requireActivity(),
                R.string.appearance_streams_size_title,
                Settings.getAppearanceStreamSize(),
                (dialog, itemView, which, text) -> {
                    Settings.setAppearanceStreamSize(text.toString());
                    initSummaries();
                    return true;
                }
        ).show();
    }

    private void onClickStreamerSize() {
        DialogService.getChooseCardSizeDialog(
                requireActivity(),
                R.string.appearance_streamer_size_title,
                Settings.getAppearanceChannelSize(),
                (dialog, itemView, which, text) -> {
                    Settings.setAppearanceChannelSize(text.toString());
                    initSummaries();
                    return true;
                }
        ).show();
    }

    private void onClickGameSize() {
        DialogService.getChooseCardSizeDialog(
                requireActivity(),
                R.string.appearance_game_size_title,
                Settings.getAppearanceGameSize(),
                (dialog, itemView, which, text) -> {
                    Settings.setAppearanceGameSize(text.toString());
                    initSummaries();
                    return true;
                }
        ).show();
    }
}
