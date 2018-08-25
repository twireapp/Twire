package com.sebastianrask.bettersubscription.service;

import android.app.Activity;
import androidx.annotation.ArrayRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import android.view.View;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.simplelist.MaterialSimpleListAdapter;
import com.afollestad.materialdialogs.simplelist.MaterialSimpleListItem;
import com.rey.material.widget.Slider;
import com.sebastianrask.bettersubscription.R;
import com.sebastianrask.bettersubscription.activities.UsageTrackingAppCompatActivity;
import com.sebastianrask.bettersubscription.views.LayoutSelector;

import biz.kasual.materialnumberpicker.MaterialNumberPicker;

/**
 * Created by Sebastian Rask on 02-05-2016.
 */
public class DialogService {
	public static MaterialDialog getThemeDialog(final Activity activity) {
		final String CURRENT_THEME = new Settings(activity).getTheme();
		final MaterialSimpleListAdapter adapter = new MaterialSimpleListAdapter(activity);
		final MaterialSimpleListItem blueTheme = getThemeDialogAdapterItem(R.string.blue_theme_name, R.drawable.circle_theme_blue_chooser, CURRENT_THEME, activity);
		final MaterialSimpleListItem purpleTheme = getThemeDialogAdapterItem(R.string.purple_theme_name, R.drawable.circle_theme_purple_chooser, CURRENT_THEME, activity);
		final MaterialSimpleListItem blackTheme = getThemeDialogAdapterItem(R.string.black_theme_name, R.drawable.circle_theme_black_chooser, CURRENT_THEME, activity);
		final MaterialSimpleListItem nightTheme = getThemeDialogAdapterItem(R.string.night_theme_name, R.drawable.circle_theme_night_chooser, CURRENT_THEME, activity);
		final MaterialSimpleListItem trueNightTheme = getThemeDialogAdapterItem(R.string.true_night_theme_name, R.drawable.circle_theme_black_chooser, CURRENT_THEME, activity);
		adapter.addAll(blueTheme, purpleTheme, blackTheme, nightTheme, trueNightTheme);

		final MaterialDialog.Builder dialog = getBaseThemedDialog(activity)
				.title(R.string.theme_dialog_title)
				.adapter(adapter, new MaterialDialog.ListCallback() {
					@Override
					public void onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
						String theme = adapter.getItem(which).getContent().toString();
						dialog.dismiss();

						new Settings(activity).setTheme(theme);
						if (!theme.equals(CURRENT_THEME)) {
							if (activity instanceof UsageTrackingAppCompatActivity) {
								UsageTrackingAppCompatActivity trackingAppCompatActivity = ((UsageTrackingAppCompatActivity) activity);
								trackingAppCompatActivity.trackEvent(R.string.category_click, R.string.action_theme, theme);
							}
							activity.recreate();
						}
					}
				});

		return dialog.build();
	}

	private static MaterialSimpleListItem getThemeDialogAdapterItem(@StringRes int title, @DrawableRes int icon, String currentTheme, Activity activity) {
		MaterialSimpleListItem.Builder builder = new MaterialSimpleListItem.Builder(activity)
				.content(title)
				.icon(icon);

		return builder.build();
	}

	public static MaterialDialog getSettingsLoginOrLogoutDialig(Activity activity, String username) {
		return getBaseThemedDialog(activity)
				.content(activity.getString(R.string.gen_dialog_login_or_out_content, username))
				.positiveText(R.string.gen_dialog_login_or_out_login_action)
				.negativeText(R.string.gen_dialog_login_or_out_logout_action).build();
	}

	public static MaterialDialog getChooseStartUpPageDialog(Activity activity, String currentlySelectedPageTitle, MaterialDialog.ListCallbackSingleChoice listCallbackSingleChoice) {
		final Settings settings = new Settings(activity);
		@ArrayRes int arrayRessource = settings.isLoggedIn() ? R.array.StartupPages : R.array.StartupPagesNoLogin;

		int indexOfPage = 0;
		String[] androidStrings = activity.getResources().getStringArray(arrayRessource);
		for (int i = 0; i < androidStrings.length; i++) {
			if (androidStrings[i].equals(currentlySelectedPageTitle)) {
				indexOfPage = i;
				break;
			}
		}

		return getBaseThemedDialog(activity)
				.title(R.string.gen_start_page)
				.items(arrayRessource)
				.itemsCallbackSingleChoice(indexOfPage, listCallbackSingleChoice)
				.positiveText(R.string.ok)
				.negativeText(R.string.cancel)
				.onNegative(new MaterialDialog.SingleButtonCallback() {
					@Override
					public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
						dialog.dismiss();
					}
				})
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
		MaterialNumberPicker hourPicker = (MaterialNumberPicker) customView.findViewById(R.id.hourPicker);
		MaterialNumberPicker minPicker = (MaterialNumberPicker) customView.findViewById(R.id.minutePicker);

		hourPicker.setValue(hourValue);
		minPicker.setValue(minuteValue);

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
			Slider slider = (Slider) customView.findViewById(R.id.slider);
			slider.setValueRange(minValue, maxValue, false);
			slider.setValue(startValue, false);
			slider.setOnPositionChangeListener(sliderChangeListener);
		}

		return dialog;
	}

	private static MaterialDialog.Builder getBaseThemedDialog(Activity activity) {
		return new MaterialDialog.Builder(activity)
				.titleColorAttr(R.attr.navigationDrawerTextColor)
				.backgroundColorAttr(R.attr.navigationDrawerBackground)
				.contentColorAttr(R.attr.navigationDrawerTextColor)
				.itemsColorAttr(R.attr.navigationDrawerTextColor);
	}
}
