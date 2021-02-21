package com.perflyst.twire.fragments;


import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.afollestad.materialdialogs.MaterialDialog;
import com.perflyst.twire.R;
import com.perflyst.twire.service.DialogService;
import com.perflyst.twire.service.Settings;

public class AppearanceSettingsFragment extends Fragment {
    private TextView themeSummary, streamsStyleSummary, gameStyleSummary, followStyleSummary, streamSizeSummary, gameSizeSummary, streamerSizeSummary;
    private ImageView themeSummaryColor;
    private Settings settings;

    public AppearanceSettingsFragment() {
        // Required empty public constructor
    }

    public static AppearanceSettingsFragment newInstance() {

        return new AppearanceSettingsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_appearance_settings, container, false);
        settings = new Settings(getContext());
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
        themeSummary.setText(settings.getTheme());
        themeSummaryColor.setImageDrawable(getColorPreviewFromTheme(settings.getTheme()));

        // Style Summary
        streamsStyleSummary.setText(settings.getAppearanceStreamStyle());
        gameStyleSummary.setText(settings.getAppearanceGameStyle());
        followStyleSummary.setText(settings.getAppearanceChannelStyle());

        // Size Summary
        streamSizeSummary.setText(settings.getAppearanceStreamSize());
        gameSizeSummary.setText(settings.getAppearanceGameSize());
        streamerSizeSummary.setText(settings.getAppearanceChannelSize());

    }

    private Drawable getColorPreviewFromTheme(String themeTitle) {
        @DrawableRes int drawableRes = R.drawable.circle_theme_blue_chooser;

        if (themeTitle.equals(getString(R.string.purple_theme_name))) {
            drawableRes = R.drawable.circle_theme_purple_chooser;
        } else if (themeTitle.equals(getString(R.string.black_theme_name))) {
            drawableRes = R.drawable.circle_theme_black_chooser;
        } else if (themeTitle.equals(getString(R.string.night_theme_name)) || themeTitle.equals(getString(R.string.true_night_theme_name))) {
            drawableRes = R.drawable.circle_theme_night_chooser;
        }

        return ContextCompat.getDrawable(requireContext(), drawableRes);
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

            settings.setAppearanceStreamStyle(title);
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
            TextView gameViewers = previewView.findViewById(R.id.game_viewers);
            if (gameViewers.getText().equals("")) {
                gameViewers.setText(getString(R.string.preview_game_viewers));
            }

            if (title.equals(getString(R.string.card_style_expanded))) {
                gameTitle.setVisibility(View.VISIBLE);
                gameViewers.setVisibility(View.VISIBLE);
                sharedPadding.setVisibility(View.VISIBLE);
            } else if (title.equals(getString(R.string.card_style_normal))) {
                gameTitle.setVisibility(View.GONE);
                gameViewers.setVisibility(View.VISIBLE);
                sharedPadding.setVisibility(View.VISIBLE);
            } else if (title.equals(getString(R.string.card_style_minimal))) {
                gameTitle.setVisibility(View.GONE);
                gameViewers.setVisibility(View.GONE);
                sharedPadding.setVisibility(View.GONE);
            }

            settings.setAppearanceGameStyle(title);
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

            settings.setAppearanceChannelStyle(title);
            initSummaries();
        }).show();
    }

    private void onClickStreamSize() {
        DialogService.getChooseCardSizeDialog(
                requireActivity(),
                R.string.appearance_streams_size_title,
                settings.getAppearanceStreamSize(),
                (dialog, itemView, which, text) -> {
                    settings.setAppearanceStreamSize(text.toString());
                    initSummaries();
                    return true;
                }
        ).show();
    }

    private void onClickStreamerSize() {
        DialogService.getChooseCardSizeDialog(
                requireActivity(),
                R.string.appearance_streamer_size_title,
                settings.getAppearanceChannelSize(),
                (dialog, itemView, which, text) -> {
                    settings.setAppearanceChannelSize(text.toString());
                    initSummaries();
                    return true;
                }
        ).show();
    }

    private void onClickGameSize() {
        DialogService.getChooseCardSizeDialog(
                requireActivity(),
                R.string.appearance_game_size_title,
                settings.getAppearanceGameSize(),
                (dialog, itemView, which, text) -> {
                    settings.setAppearanceGameSize(text.toString());
                    initSummaries();
                    return true;
                }
        ).show();
    }

}
