package com.sebastianrask.bettersubscription.fragments;


import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.sebastianrask.bettersubscription.R;
import com.sebastianrask.bettersubscription.activities.UsageTrackingAppCompatActivity;
import com.sebastianrask.bettersubscription.service.DialogService;
import com.sebastianrask.bettersubscription.service.Settings;
import com.sebastianrask.bettersubscription.views.LayoutSelector;

public class AppearanceSettingsFragment extends Fragment {
	private String LOG_TAG = getClass().getSimpleName();
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
		View rootView =  inflater.inflate(R.layout.fragment_appearance_settings, container, false);
		settings = new Settings(getContext());
		themeSummary = (TextView) rootView.findViewById(R.id.appearance_theme_color_summary);
		themeSummaryColor = (ImageView) rootView.findViewById(R.id.appearance_theme_color);

		streamsStyleSummary = (TextView) rootView.findViewById(R.id.appearance_streams_style_summary);
		gameStyleSummary = (TextView) rootView.findViewById(R.id.appearance_game_style_summary);
		followStyleSummary = (TextView) rootView.findViewById(R.id.appearance_streamer_style_summary);

		streamSizeSummary = (TextView) rootView.findViewById(R.id.appearance_streams_size_summary);
		gameSizeSummary = (TextView) rootView.findViewById(R.id.appearance_game_size_summary);
		streamerSizeSummary = (TextView) rootView.findViewById(R.id.appearance_streamer_size_summary);

		initSummaries();
		initOnClicks(rootView);

		return rootView;
	}

	private void initOnClicks(View rootView) {
		final View themeColorWrapper = rootView.findViewById(R.id.appearance_theme_color_wrapper);
		themeColorWrapper.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onClickThemeColor(themeColorWrapper);
			}
		});

		final View streamStyleWrapper = rootView.findViewById(R.id.appearance_streams_style_wrapper);
		streamStyleWrapper.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onClickStreamStyle(v);
			}
		});

		final View streamSizeWrapper = rootView.findViewById(R.id.appearance_streams_size_wrapper);
		streamSizeWrapper.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onClickStreamSize(v);
			}
		});

		final View gameStyleWrapper = rootView.findViewById(R.id.appearance_game_style_wrapper);
		gameStyleWrapper.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onClickGameStyle(v);
			}
		});

		final View gameSizeWrapper = rootView.findViewById(R.id.appearance_game_size_wrapper);
		gameSizeWrapper.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onClickGameSize(v);
			}
		});

		final View streamerStyleWrapper = rootView.findViewById(R.id.appearance_streamer_style_wrapper);
		streamerStyleWrapper.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onClickStreamerStyle(v);
			}
		});

		final View streamerSizeWrapper = rootView.findViewById(R.id.appearance_streamer_size_wrapper);
		streamerSizeWrapper.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onClickStreamerSize(v);
			}
		});
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
		} else if (themeTitle.equals(getString(R.string.night_theme_name))) {
			drawableRes = R.drawable.circle_theme_night_chooser;
		}

		return ContextCompat.getDrawable(getContext(), drawableRes);
	}

	public void onClickThemeColor(View view) {
		MaterialDialog themeChooserDialog = DialogService.getThemeDialog(getActivity());
		themeChooserDialog.show();
	}

	public void onClickStreamStyle(View view) {
		DialogService.getChooseStreamCardStyleDialog(getActivity(), new LayoutSelector.OnLayoutSelected() {
			@Override
			public void onSelected(String title, int index, View previewView) {
				track(R.string.action_stream_style, title);

				View sharedPadding = previewView.findViewById(R.id.shared_padding);
				ImageView view = (ImageView) previewView.findViewById(R.id.image_stream_preview);
				view.setImageResource(R.drawable.preview_stream);
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

			}
		}).show();
	}

	public void onClickGameStyle(View view) {
		DialogService.getChooseGameCardStyleDialog(getActivity(), new LayoutSelector.OnLayoutSelected() {
			@Override
			public void onSelected(String title, int index, View previewView) {
				track(R.string.action_game_style, title);

				View sharedPadding = previewView.findViewById(R.id.shared_padding);
				ImageView view = (ImageView) previewView.findViewById(R.id.image_game_preview);
				view.setImageResource(R.drawable.preview_game);

				FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
						(int) getContext().getResources().getDimension(R.dimen.game_preview_width),
						ViewGroup.LayoutParams.WRAP_CONTENT
				);
				lp.gravity = Gravity.CENTER;
				previewView.setLayoutParams(lp);

				View gameTitle = previewView.findViewById(R.id.game_card_title);
				TextView gameViewers = (TextView) previewView.findViewById(R.id.game_viewers);
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
			}
		}).show();
	}

	public void onClickStreamerStyle(View view) {
		DialogService.getChooseStreamerCardStyleDialog(getActivity(), new LayoutSelector.OnLayoutSelected() {
			@Override
			public void onSelected(String title, int index, View previewView) {
				track(R.string.action_streamer_style, title);

				View nameView = previewView.findViewById(R.id.displayName);
				ImageView streamerLogo = (ImageView) previewView.findViewById(R.id.profileLogoImageView);
				streamerLogo.setImageResource(R.drawable.preview_streamer);

				FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
						(int) getContext().getResources().getDimension(R.dimen.subscription_card_preview_width),
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
			}
		}).show();
	}

	private void track(@StringRes int type, String title) {
		if (getActivity() instanceof UsageTrackingAppCompatActivity) {
			UsageTrackingAppCompatActivity activity = ((UsageTrackingAppCompatActivity) getActivity());
			activity.trackEvent(R.string.category_click, type, title);
		}
	}

	public void onClickStreamSize(View v) {
		DialogService.getChooseCardSizeDialog(
				getActivity(),
				R.string.appearance_streams_size_title,
				settings.getAppearanceStreamSize(),
				new MaterialDialog.ListCallbackSingleChoice() {
					@Override
					public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
						track(R.string.action_stream_size, text.toString());
						settings.setAppearanceStreamSize(text.toString());
						initSummaries();
						return true;
					}
				}
		).show();
	}

	public void onClickStreamerSize(View v) {
		DialogService.getChooseCardSizeDialog(
				getActivity(),
				R.string.appearance_streamer_size_title,
				settings.getAppearanceChannelSize(),
				new MaterialDialog.ListCallbackSingleChoice() {
					@Override
					public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
						track(R.string.action_streamer_size, text.toString());

						settings.setAppearanceChannelSize(text.toString());
						initSummaries();
						return true;
					}
				}
		).show();
	}

	public void onClickGameSize(View v) {
		DialogService.getChooseCardSizeDialog(
				getActivity(),
				R.string.appearance_game_size_title,
				settings.getAppearanceGameSize(),
				new MaterialDialog.ListCallbackSingleChoice() {
					@Override
					public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
						track(R.string.action_game_size, text.toString());

						settings.setAppearanceGameSize(text.toString());
						initSummaries();
						return true;
					}
				}
		).show();
	}

}
