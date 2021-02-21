package com.perflyst.twire.service;

import android.app.Activity;
import android.view.View;

import androidx.annotation.ArrayRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.simplelist.MaterialSimpleListAdapter;
import com.afollestad.materialdialogs.simplelist.MaterialSimpleListItem;
import com.perflyst.twire.R;
import com.perflyst.twire.views.LayoutSelector;
import com.rey.material.widget.Slider;

import biz.kasual.materialnumberpicker.MaterialNumberPicker;

/**
 * Created by Sebastian Rask on 02-05-2016.
 */
public class DialogService {
    public static MaterialDialog getThemeDialog(final Activity activity) {
        final String CURRENT_THEME = new Settings(activity).getTheme();
        final MaterialSimpleListAdapter adapter = new MaterialSimpleListAdapter(activity);
        final MaterialSimpleListItem blueTheme = getThemeDialogAdapterItem(R.string.blue_theme_name, R.drawable.circle_theme_blue_chooser, activity);
        final MaterialSimpleListItem purpleTheme = getThemeDialogAdapterItem(R.string.purple_theme_name, R.drawable.circle_theme_purple_chooser, activity);
        final MaterialSimpleListItem blackTheme = getThemeDialogAdapterItem(R.string.black_theme_name, R.drawable.circle_theme_black_chooser, activity);
        final MaterialSimpleListItem nightTheme = getThemeDialogAdapterItem(R.string.night_theme_name, R.drawable.circle_theme_night_chooser, activity);
        final MaterialSimpleListItem trueNightTheme = getThemeDialogAdapterItem(R.string.true_night_theme_name, R.drawable.circle_theme_black_chooser, activity);
        adapter.addAll(blueTheme, purpleTheme, blackTheme, nightTheme, trueNightTheme);

        final MaterialDialog.Builder dialog = getBaseThemedDialog(activity)
                .title(R.string.theme_dialog_title)
                .adapter(adapter, (dialog1, itemView, which, text) -> {
                    String theme = adapter.getItem(which).getContent().toString();
                    dialog1.dismiss();

                    new Settings(activity).setTheme(theme);
                    if (!theme.equals(CURRENT_THEME)) {
                        activity.recreate();
                    }
                });

        return dialog.build();
    }

    private static MaterialSimpleListItem getThemeDialogAdapterItem(@StringRes int title, @DrawableRes int icon, Activity activity) {
        MaterialSimpleListItem.Builder builder = new MaterialSimpleListItem.Builder(activity)
                .content(title)
                .icon(icon);

        return builder.build();
    }

    public static MaterialDialog getSettingsLoginOrLogoutDialog(Activity activity, String username) {
        return getBaseThemedDialog(activity)
                .content(activity.getString(R.string.gen_dialog_login_or_out_content, username))
                .positiveText(R.string.gen_dialog_login_or_out_login_action)
                .negativeText(R.string.gen_dialog_login_or_out_logout_action).build();
    }

    public static MaterialDialog getChooseStartUpPageDialog(Activity activity, String currentlySelectedPageTitle, MaterialDialog.ListCallbackSingleChoice listCallbackSingleChoice) {
        final Settings settings = new Settings(activity);
        @ArrayRes int arrayResource = settings.isLoggedIn() ? R.array.StartupPages : R.array.StartupPagesNoLogin;

        int indexOfPage = 0;
        String[] androidStrings = activity.getResources().getStringArray(arrayResource);
        for (int i = 0; i < androidStrings.length; i++) {
            if (androidStrings[i].equals(currentlySelectedPageTitle)) {
                indexOfPage = i;
                break;
            }
        }

        return getBaseThemedDialog(activity)
                .title(R.string.gen_start_page)
                .items(arrayResource)
                .itemsCallbackSingleChoice(indexOfPage, listCallbackSingleChoice)
                .positiveText(R.string.ok)
                .negativeText(R.string.cancel)
                .onNegative((dialog, which) -> dialog.dismiss())
                .build();
    }

    public static MaterialDialog getChooseStreamCardStyleDialog(Activity activity, LayoutSelector.OnLayoutSelected onLayoutSelected) {
        LayoutSelector layoutSelector = new LayoutSelector(R.layout.cell_stream, R.array.StreamsCardStyles, onLayoutSelected, activity)
                .setSelectedLayoutTitle(new Settings(activity).getAppearanceStreamStyle())
                .setTextColorAttr(R.attr.navigationDrawerTextColor)
                .setPreviewMaxHeightRes(R.dimen.stream_preview_max_height);

        return getBaseThemedDialog(activity)
                .title(R.string.appearance_streams_style_title)
                .customView(layoutSelector.build(), true)
                .positiveText(R.string.done)
                .build();
    }

    public static MaterialDialog getChooseGameCardStyleDialog(Activity activity, LayoutSelector.OnLayoutSelected onLayoutSelected) {
        LayoutSelector layoutSelector = new LayoutSelector(R.layout.cell_game, R.array.GameCardStyles, onLayoutSelected, activity)
                .setSelectedLayoutTitle(new Settings(activity).getAppearanceGameStyle())
                .setTextColorAttr(R.attr.navigationDrawerTextColor)
                .setPreviewMaxHeightRes(R.dimen.game_preview_max_height);

        return getBaseThemedDialog(activity)
                .title(R.string.appearance_game_style_title)
                .customView(layoutSelector.build(), true)
                .positiveText(R.string.done)
                .build();
    }

    public static MaterialDialog getChooseStreamerCardStyleDialog(Activity activity, LayoutSelector.OnLayoutSelected onLayoutSelected) {
        LayoutSelector layoutSelector = new LayoutSelector(R.layout.cell_channel, R.array.FollowCardStyles, onLayoutSelected, activity)
                .setSelectedLayoutTitle(new Settings(activity).getAppearanceChannelStyle())
                .setTextColorAttr(R.attr.navigationDrawerTextColor)
                .setPreviewMaxHeightRes(R.dimen.subscription_card_preview_max_height);

        return getBaseThemedDialog(activity)
                .title(R.string.appearance_streamer_style_title)
                .customView(layoutSelector.build(), true)
                .positiveText(R.string.done)
                .build();
    }

    public static MaterialDialog getChooseCardSizeDialog(Activity activity, @StringRes int dialogTitle, String currentlySelected, MaterialDialog.ListCallbackSingleChoice callbackSingleChoice) {
        int indexOfPage = 0;
        String[] sizeTitles = activity.getResources().getStringArray(R.array.CardSizes);
        for (int i = 0; i < sizeTitles.length; i++) {
            if (sizeTitles[i].equals(currentlySelected)) {
                indexOfPage = i;
                break;
            }
        }

        return getBaseThemedDialog(activity)
                .title(dialogTitle)
                .itemsCallbackSingleChoice(indexOfPage, callbackSingleChoice)
                .items(sizeTitles)
                .positiveText(R.string.done)
                .build();
    }

    public static MaterialDialog getChooseChatSizeDialog(Activity activity, @StringRes int dialogTitle, @ArrayRes int array, int currentSize, MaterialDialog.ListCallbackSingleChoice callbackSingleChoice) {
        int indexOfPage = currentSize - 1;
        String[] sizeTitles = activity.getResources().getStringArray(array);

        return getBaseThemedDialog(activity)
                .title(dialogTitle)
                .itemsCallbackSingleChoice(indexOfPage, callbackSingleChoice)
                .items(sizeTitles)
                .positiveText(R.string.done)
                .build();
    }

    public static MaterialDialog getSleepTimerDialog(Activity activity, boolean isTimerRunning, MaterialDialog.SingleButtonCallback onStartCallback, MaterialDialog.SingleButtonCallback onStopCallBack, int hourValue, int minuteValue) {

        @StringRes int positiveText = isTimerRunning ? R.string.resume : R.string.start;
        @StringRes int negativeText = isTimerRunning ? R.string.stop : R.string.cancel;


        MaterialDialog dialog = getBaseThemedDialog(activity)
                .title(R.string.stream_sleep_timer_title)
                .customView(R.layout.dialog_sleep_timer, false)
                .positiveText(positiveText)
                .negativeText(negativeText)
                .onPositive(onStartCallback)
                .onNegative(onStopCallBack)
                .build();

        View customView = dialog.getCustomView();
        MaterialNumberPicker hourPicker = customView.findViewById(R.id.hourPicker);
        MaterialNumberPicker minPicker = customView.findViewById(R.id.minutePicker);

        hourPicker.setValue(hourValue);
        minPicker.setValue(minuteValue);

        return dialog;
    }

    public static MaterialDialog getSeekDialog(Activity activity, MaterialDialog.SingleButtonCallback buttonCallback, int currentProgress, int maxProgress) {
        MaterialDialog dialog = getBaseThemedDialog(activity)
                .title(R.string.stream_seek_dialog_title)
                .customView(R.layout.dialog_seek, false)
                .positiveText(R.string.done)
                .negativeText(R.string.cancel)
                .onPositive(buttonCallback)
                .onNegative(buttonCallback)
                .build();

        View customView = dialog.getCustomView();
        MaterialNumberPicker hourPicker = customView.findViewById(R.id.hour_picker);
        MaterialNumberPicker minutePicker = customView.findViewById(R.id.minute_picker);
        MaterialNumberPicker secondPicker = customView.findViewById(R.id.second_picker);

        hourPicker.setMaxValue(maxProgress / 3600);
        minutePicker.setMaxValue(Math.min(maxProgress / 60, 59));
        secondPicker.setMaxValue(Math.min(maxProgress, 59));

        hourPicker.setValue(currentProgress / 3600);
        minutePicker.setValue(currentProgress / 60 % 60);
        secondPicker.setValue(currentProgress % 60);

        return dialog;
    }

    public static MaterialDialog getSliderDialog(Activity activity, MaterialDialog.SingleButtonCallback onCancelCallback, Slider.OnPositionChangeListener sliderChangeListener, int startValue, int minValue, int maxValue, String title) {
        MaterialDialog dialog = getBaseThemedDialog(activity)
                .title(title)
                .customView(R.layout.dialog_slider, false)
                .positiveText(R.string.done)
                .negativeText(R.string.cancel)
                .autoDismiss(true)
                .onNegative(onCancelCallback)
                .build();

        View customView = dialog.getCustomView();
        if (customView != null) {
            Slider slider = customView.findViewById(R.id.slider);
            slider.setValueRange(minValue, maxValue, false);
            slider.setValue(startValue, false);
            slider.setOnPositionChangeListener(sliderChangeListener);
        }

        return dialog;
    }

    public static MaterialDialog getRouterErrorDialog(Activity activity, int errorMessage) {
        return getBaseThemedDialog(activity)
                .title(R.string.router_error_dialog_title)
                .content(errorMessage)
                .cancelListener(dialogInterface -> activity.finish())
                .build();
    }

    public static MaterialDialog.Builder getBaseThemedDialog(Activity activity) {
        return new MaterialDialog.Builder(activity)
                .titleColorAttr(R.attr.navigationDrawerTextColor)
                .backgroundColorAttr(R.attr.navigationDrawerBackground)
                .contentColorAttr(R.attr.navigationDrawerTextColor)
                .itemsColorAttr(R.attr.navigationDrawerTextColor);
    }
}
