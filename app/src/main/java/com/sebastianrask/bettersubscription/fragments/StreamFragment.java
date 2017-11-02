package com.sebastianrask.bettersubscription.fragments;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.os.ResultReceiver;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.app.MediaRouteChooserDialog;
import android.support.v7.app.MediaRouteChooserDialogFragment;
import android.support.v7.app.MediaRouteDialogFactory;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.transition.Transition;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.CheckedTextView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.common.images.WebImage;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.android.libraries.cast.companionlibrary.cast.dialog.video.VideoMediaRouteDialogFactory;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.Gson;
import com.rey.material.widget.ProgressView;
import com.sebastianrask.bettersubscription.R;
import com.sebastianrask.bettersubscription.activities.stream.LiveStreamActivity;
import com.sebastianrask.bettersubscription.activities.stream.StreamActivity;
import com.sebastianrask.bettersubscription.activities.ChannelActivity;
import com.sebastianrask.bettersubscription.activities.UsageTrackingAppCompatActivity;
import com.sebastianrask.bettersubscription.activities.stream.VODActivity;
import com.sebastianrask.bettersubscription.adapters.PanelAdapter;
import com.sebastianrask.bettersubscription.broadcasts_and_services.PlayerService;
import com.sebastianrask.bettersubscription.misc.FollowHandler;
import com.sebastianrask.bettersubscription.misc.ResizeHeightAnimation;
import com.sebastianrask.bettersubscription.misc.ResizeWidthAnimation;
import com.sebastianrask.bettersubscription.model.Panel;
import com.sebastianrask.bettersubscription.model.SleepTimer;
import com.sebastianrask.bettersubscription.model.ChannelInfo;
import com.sebastianrask.bettersubscription.service.Service;
import com.sebastianrask.bettersubscription.service.Settings;
import com.sebastianrask.bettersubscription.tasks.GetLiveStreamURL;
import com.sebastianrask.bettersubscription.tasks.GetPanelsTask;
import com.sebastianrask.bettersubscription.tasks.GetStreamChattersTask;
import com.sebastianrask.bettersubscription.tasks.GetStreamViewersTask;
import com.sebastianrask.bettersubscription.tasks.GetVODStreamURL;
import com.sebastianrask.bettersubscription.views.VideoViewSimple;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;
import com.transitionseverywhere.TransitionManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StreamFragment extends Fragment {
	private final int 	PLAY_PAUSE_ANIMATION_DURATION 	= 500,
			HIDE_ANIMATION_DELAY 			= 3000,
			SNACKBAR_SHOW_DURATION 			= 4000;

	private final String LOG_TAG = getClass().getSimpleName();
	private final Handler 	delayAnimationHandler 	= new Handler(),
			progressHandler 		= new Handler(),
			fetchViewCountHandler 	= new Handler(),
			fetchChattersHandler 	= new Handler();

	public boolean isFullscreen = false, embedded = false;
	public ChannelInfo mChannelInfo;
	public String vodId;
	public boolean 	castingViewVisible 	= false,
			audioViewVisible 	= false,
			chatOnlyViewVisible = false,
			autoPlay 			= true,
			hasPaused			= false;
	private HeadsetPlugIntentReceiver headsetIntentReceiver;
	private Settings settings;
	private SleepTimer sleepTimer;
	private HashMap<String, String> qualityURLs;
	private BiMap<String, String> supportedQualities;
	private boolean isLandscape = false, previewInbackGround = false;
	private Runnable fetchViewCountRunnable;
	private Runnable fetchChattersRunnable;
	// This is the media router window for the chromecast dialog
	private MediaRouteDialogFactory mMediaRouteDialogFactory = new VideoMediaRouteDialogFactory() {
		@NonNull
		@Override
		public MediaRouteChooserDialogFragment onCreateChooserDialogFragment() {
			return new CustomMediaRouteChooserDialogFragment();
		}
	};
	private View mVideoBackground;
	private VideoViewSimple 	mVideoView;
	private Toolbar 			mToolbar;
	private RelativeLayout 		mControlToolbar,
			mVideoWrapper;
	private FrameLayout 		mPlayPauseWrapper;
	private ImageView 			mPauseIcon,
			mPlayIcon,
			mQualityButton,
			mFullScreenButton,
			mPreview,
			mShowChatButton;
	private SeekBar 			mProgressBar;
	private TextView			mCurrentProgressView, source, high, medium, low, mobile, auto, castingTextView, mCurrentViewersView;
	private AppCompatActivity 	mActivity;
	private Snackbar 			snackbar;
	private ProgressView		mBufferingView;

	private final Runnable progressRunnable = new Runnable() {
		@Override
		public void run() {
			if (mVideoView.isPlaying() || (audioViewVisible && isAudioOnlyModeEnabled() && PlayerService.getInstance().getMediaPlayer().isPlaying())) {
				mProgressBar.setProgress(currentProgress + 1);

				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
					mBufferingView.stop();
					delayHiding();
					if (!previewInbackGround) {
						hidePreview();
					}
				}
			}
			progressHandler.postDelayed(this, 1000);
		}
	};
	private BottomSheetDialog 	mQualityBottomSheet, mProfileBottomSheet;
	private CheckedTextView 	mAudioOnlySelector, mChatOnlySelector;
	private ViewGroup 			rootView;
	private MenuItem 			optionsMenuItem;
	private LinearLayout		mQualityWrapper;
	private View 				mClickIntercepter;
	private VideoCastManager mCastManager;
	private final Runnable 	hideAnimationRunnable = new Runnable() {
		@Override
		public void run() {
			if(getActivity() != null)
				hideVideoInterface();
		}
	};
	private int originalCtrlToolbarPadding,
			originalMainToolbarPadding,
			vodLength = 0,
			currentProgress = 0,
			videoHeightBeforeChatOnly,
			fetchViewCountDelay = 1000 * 60, // A minute
			fetchChattersDelay = 1000 * 60; // 30 seco... Nah just kidding. Also a minute.

	private Integer triesForNextBest = 0;
	private final VideoCastConsumerImpl mCastConsumer = new VideoCastConsumerImpl() {
		private final String LOG_TAG = "VideoCastConsumer";
		@Override
		public void onVolumeChanged(double value, boolean isMute) {
			super.onVolumeChanged(value, isMute);
			Log.d(LOG_TAG, "Volume Changed " + value + " - Is mute " + isMute);
		}

		@Override
		public void onRemoteMediaPlayerStatusUpdated() {
			super.onRemoteMediaPlayerStatusUpdated();
			Log.d(LOG_TAG, "MediaPlayer status updated");
			if (mCastManager.getMediaStatus().getPlayerState() == MediaStatus.PLAYER_STATE_PLAYING) {
				castingTextView.setText(getString(R.string.stream_chromecast_playing, mCastManager.getDeviceName()));
			}
		}

		@Override
		public void onApplicationStatusChanged(String appStatus) {
			super.onApplicationStatusChanged(appStatus);
			Log.d(LOG_TAG, "Application Status changed to " + appStatus);
		}

		@Override
		public void onApplicationConnected(ApplicationMetadata appMetadata, String sessionId, boolean wasLaunched) {
			super.onApplicationConnected(appMetadata, sessionId, wasLaunched);
			Log.d(LOG_TAG, "Application connected. Was launched: " + wasLaunched);

			if (qualityURLs != null && qualityURLs.containsKey(GetLiveStreamURL.QUALITY_MEDIUM)) {
				onCastMediaPlaying();
				playOnCast();
			}
		}

		@Override
		public void onApplicationConnectionFailed(int errorCode) {
			super.onApplicationConnectionFailed(errorCode);
			Log.d(LOG_TAG, "Application connection failed " + errorCode);
			showSnackbar(getString(R.string.stream_chromecast_connection_failed, mCastManager.getDeviceName()), Snackbar.LENGTH_LONG);
		}

		@Override
		public void onDataMessageSendFailed(int errorCode) {
			super.onDataMessageSendFailed(errorCode);
			Log.d(LOG_TAG, "Data message send failed " + errorCode);
		}

		@Override
		public void onDataMessageReceived(String message) {
			super.onDataMessageReceived(message);
			Log.d(LOG_TAG, "Data message received " + message);
		}

		@Override
		public void onConnected() {
			super.onConnected();
			Log.d(LOG_TAG, "Connected");
			initCastingView();
		}

		@Override
		public void onDisconnected() {
			super.onDisconnected();
			Log.d(LOG_TAG, "Disconnected");
			disableCastingView();
			startStreamWithQuality(settings.getPrefStreamQuality());
		}

		@Override
		public void onFailed(int resourceId, int statusCode) {
			super.onFailed(resourceId, statusCode);
			if (mCastManager.getMediaStatus() != null && mCastManager.getMediaStatus().getPlayerState() != MediaStatus.PLAYER_STATE_BUFFERING) {
				showSnackbar(getString(R.string.stream_chromecast_failed, mCastManager.getDeviceName()), Snackbar.LENGTH_LONG);
			}
			Log.d(LOG_TAG, "Failed " + statusCode);
		}
	};

	public static StreamFragment newInstance(Bundle args) {
		StreamFragment fragment = new StreamFragment();
		fragment.setArguments(args);
		return fragment;
	}

	/**
	 * Finds and returns the TRUE width of the screen
	 * @return
	 */
	public static int getScreenWidth(Activity activity) {
		if (activity != null) {
			Display display = activity.getWindowManager().getDefaultDisplay();
			DisplayMetrics metrics = new DisplayMetrics();
			Point size = new Point();
			int width, height;

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity.isInMultiWindowMode()) {
					display.getMetrics(metrics);
				} else {
					display.getRealMetrics(metrics);
				}

				width = metrics.widthPixels;
				height = metrics.heightPixels;
			} else {
				display.getSize(size);
				width = size.x;
				height = size.y;
			}

			return Math.max(width, height);
		}

		return 0;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		VideoCastManager.checkGooglePlayServices(getActivity());
		mCastManager = VideoCastManager.getInstance();
		mCastManager.reconnectSessionIfPossible(10);

		Bundle args = getArguments();
		setHasOptionsMenu(true);
		settings = new Settings(getActivity());

		if (args != null) {
			mChannelInfo = args.getParcelable(getString(R.string.stream_fragment_streamerInfo));
			vodId = args.getString(getString(R.string.stream_fragment_vod_id));
			vodLength = args.getInt(getString(R.string.stream_fragment_vod_length));
			autoPlay = args.getBoolean(getString(R.string.stream_fragment_autoplay));

			settings.setVodLength(vodId, vodLength);
		}

		final View mRootView = inflater.inflate(R.layout.fragment_stream, container, false);
		mRootView.requestLayout();

		// If the user has been in FULL SCREEN mode and presses the back button, we want to change the orientation to portrait.
		// As soon as the orientation has change we don't want to force the user to will be in portrait, so we "release" the request.
		if(getActivity().getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
			getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
		}

		if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			isLandscape = true;
		}

		//  If no streamer info is available we cant show the stream.
		if (mChannelInfo == null) {
			if (getActivity() != null) {
				getActivity().finish();
			}
			return rootView;
		}

		rootView			= (ViewGroup) mRootView;
		mToolbar 			= (Toolbar) mRootView.findViewById(R.id.main_toolbar);
		mControlToolbar 	= (RelativeLayout) mRootView.findViewById(R.id.control_toolbar_wrapper);
		mVideoWrapper		= (RelativeLayout) mRootView.findViewById(R.id.video_wrapper);
		mVideoView 			= (VideoViewSimple) mRootView.findViewById(R.id.VideoView);
		mVideoBackground 	= mRootView.findViewById(R.id.video_background);
		mPlayPauseWrapper 	= (FrameLayout) mRootView.findViewById(R.id.play_pause_wrapper);
		mPlayIcon 			= (ImageView) mRootView.findViewById(R.id.ic_play);
		mPauseIcon 			= (ImageView) mRootView.findViewById(R.id.ic_pause);
		mPreview 			= (ImageView) mRootView.findViewById(R.id.preview);
		mQualityButton 		= (ImageView) mRootView.findViewById(R.id.settings_icon);
		mFullScreenButton 	= (ImageView) mRootView.findViewById(R.id.fullscreen_icon);
		mShowChatButton		= (ImageView) mRootView.findViewById(R.id.show_chat_button);
		mCurrentProgressView= (TextView) mRootView.findViewById(R.id.currentProgess);
		castingTextView		= (TextView) mRootView.findViewById(R.id.chromecast_text);
		mProgressBar 		= (SeekBar) mRootView.findViewById(R.id.progressBar);
		mBufferingView		= (ProgressView) mRootView.findViewById(R.id.circle_progress);
		mCurrentViewersView = (TextView) mRootView.findViewById(R.id.txtViewViewers);
		mActivity 			= ((AppCompatActivity) getActivity());
		mClickIntercepter	= mRootView.findViewById(R.id.click_intercepter);
		View mCurrentViewersWrapper = mRootView.findViewById(R.id.viewers_wrapper);

		setPreviewAndCheckForSharedTransition();
		setupToolbar();
		setupSpinner();
		setupProfileBottomSheet();
		setupLandscapeChat();
		setupShowChatButton();


		mFullScreenButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleFullscreen();
			}
		});
		mPlayPauseWrapper.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(mPlayPauseWrapper.getAlpha() < 0.5f) {
					return;
				}

				try {
					if(mVideoView.isPlaying() || (isAudioOnlyModeEnabled() && PlayerService.getInstance().getMediaPlayer().isPlaying()) || (mCastManager.isConnected() && mCastManager.isRemoteMediaPlaying()) ) {
						pauseStream();
					} else if (!mVideoView.isPlaying()){
						resumeStream();
					}
				} catch (TransientNetworkDisconnectionException | NoConnectionException e) {
					e.printStackTrace();
					startStreamWithQuality(settings.getPrefStreamQuality());
				}
			}
		});

		mVideoWrapper.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				delayAnimationHandler.removeCallbacks(hideAnimationRunnable);
				if (isVideoInterfaceShowing()) {
					hideVideoInterface();
					if (isDeviceBelowKitkat())
						setAndroidUiMode();
				} else {
					showVideoInterface();

					// Show the navigation bar
					if (isLandscape && settings.getStreamPlayerShowNavigationBar() && Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
						View decorView = getActivity().getWindow().getDecorView();
						decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
								| View.SYSTEM_UI_FLAG_FULLSCREEN // Hide Status bar
								| View.SYSTEM_UI_FLAG_IMMERSIVE);
					}

					if (mVideoView.isPlaying()) {
						delayHiding();
					}

					Handler h = new Handler();
					h.postDelayed(new Runnable() {
						@Override
						public void run() {
							setAndroidUiMode();
						}
					}, HIDE_ANIMATION_DELAY);
				}
			}
		});

		mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				Log.e(LOG_TAG, "Something went wrong playing the stream for " + mChannelInfo.getDisplayName() + " - What: " + what + " - Extra: " + extra);
				if (getActivity() != null && getActivity() instanceof UsageTrackingAppCompatActivity) {
					UsageTrackingAppCompatActivity mUsageTrackingActivity = (UsageTrackingAppCompatActivity) getActivity();
					mUsageTrackingActivity.trackEvent("Error", "StreamPlayback error " + mChannelInfo.getDisplayName() + " - What: " + what + " - Extra: " + extra);
				}

				playbackFailed();
				return true;
			}
		});

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			mVideoView.setOnInfoListener(new MediaPlayer.OnInfoListener() {
				@Override
				public boolean onInfo(MediaPlayer mp, int what, int extra) {
					Log.d(LOG_TAG, "" + what);
					if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START || what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
						mBufferingView.stop();
						hideVideoInterface();
						delayHiding();

						Log.d(LOG_TAG, "Render Start");
						if (!previewInbackGround) {
							hidePreview();
						}
					}

					if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
						mBufferingView.start();
						delayAnimationHandler.removeCallbacks(hideAnimationRunnable);
						showVideoInterface();

						Log.d(LOG_TAG, "Render stop. Buffering start");
					}

					return true;
				}
			});
		} else {
			// ToDo: Find a way to see buffering on API level 16
		}

		mRootView.setOnSystemUiVisibilityChangeListener(
				new View.OnSystemUiVisibilityChangeListener() {
					@Override
					public void onSystemUiVisibilityChange(int visibility) {
						if (visibility == 0) {
							showVideoInterface();
							delayHiding();
							Handler h = new Handler();
							h.postDelayed(new Runnable() {
								@Override
								public void run() {
									setAndroidUiMode();
								}
							}, HIDE_ANIMATION_DELAY);
						}
					}
				}
		);


		if (vodId == null) {
			View mTimeController = mRootView.findViewById(R.id.time_controller);
			mTimeController.setVisibility(View.INVISIBLE);

			if (args != null && args.containsKey(getString(R.string.stream_fragment_viewers)) && settings.getStreamPlayerShowViewerCount() ) {
				mCurrentViewersView.setText("" + args.getInt(getString(R.string.stream_fragment_viewers)));
				startFetchingViewers();
			} else {
				mCurrentViewersWrapper.setVisibility(View.GONE);
			}
		} else {
			mCurrentViewersWrapper.setVisibility(View.GONE);

			TextView maxProgress = (TextView) mRootView.findViewById(R.id.maxProgress);
			maxProgress.setText(Service.calculateTwitchVideoLength(vodLength));

			mProgressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					if (progress == vodLength) {
						pauseStream();
					}

					if (progress != currentProgress + 1) {
						if (audioViewVisible) {
							Intent seekToPlayer = new Intent(getContext(), PlayerService.class);
							seekToPlayer.setAction(PlayerService.ACTION_SEEK);
							seekToPlayer.putExtra(PlayerService.VOD_PROGRESS, progress * 1000);
							getContext().startService(seekToPlayer);
						} else {
							mVideoView.seekTo(progress * 1000);
							showVideoInterface();

							if (progress > 0) {
								settings.setVodProgress(vodId, progress);
							}
						}
					}
					currentProgress = progress;
					mCurrentProgressView.setText(Service.calculateTwitchVideoLength(currentProgress));
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {}

				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
					delayHiding();
				}
			});
			mProgressBar.setMax(vodLength);
		}
		progressHandler.postDelayed(progressRunnable, 1000);

		keepScreenOn();
		checkChromecastConnection();

		if (autoPlay || vodId != null) {
			startStreamWithQuality(settings.getPrefStreamQuality());
		}

		headsetIntentReceiver = new HeadsetPlugIntentReceiver();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			getActivity().registerReceiver(headsetIntentReceiver, new IntentFilter(AudioManager.ACTION_HEADSET_PLUG));
		}

		return mRootView;
	}

	/**
	 * Hides the preview image and updates the state
	 */
	private void hidePreview() {
		Service.bringToBack(mPreview);
		previewInbackGround = true;
	}

	public void backPressed() {
		mVideoView.setVisibility(View.INVISIBLE);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		// Checks the orientation of the screen
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			isLandscape = true;
		} else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
			isLandscape = false;
		}

		checkShowChatButtonVisibility();
		updateUI();
	}

	@Override
	public void onResume() {
		super.onResume();

		mCastManager.addVideoCastConsumer(mCastConsumer);
		mCastManager.incrementUiCounter();

		originalMainToolbarPadding = mToolbar.getPaddingRight();
		originalCtrlToolbarPadding = mControlToolbar.getPaddingRight();

		if (castingViewVisible) {
			if ((!mCastManager.isConnected() && !mCastManager.isConnecting())) {
				disableCastingView();
				startStreamWithQuality(settings.getPrefStreamQuality());
			} else {
				// Connected to chrome cast.
				updateQualitySelectorsWithTask();
			}

		} else if (audioViewVisible && !isAudioOnlyModeEnabled()) {
			disableAudioOnlyView();
			startStreamWithQuality(settings.getPrefStreamQuality());
		} else if (!castingViewVisible && !audioViewVisible && hasPaused && settings.getStreamPlayerAutoContinuePlaybackOnReturn()) {
			startStreamWithQuality(settings.getPrefStreamQuality());
		}

		registerAudioOnlyDelegate();

		if (!chatOnlyViewVisible) {
			showVideoInterface();
			updateUI();
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		Log.d(LOG_TAG, "Stream Fragment paused");
		mCastManager.removeVideoCastConsumer(mCastConsumer);
		mCastManager.decrementUiCounter();
		hasPaused = true;
		if (PlayerService.getInstance() != null) {
			PlayerService.getInstance().unregisterDelegate();
		}
	}

	@Override
	public void onStop() {
		Log.d(LOG_TAG, "Stream Fragment Stopped");
		super.onStop();

		mBufferingView.stop();

		if (!castingViewVisible && !audioViewVisible) {
			pauseStream();
		}

		if (vodId != null) {
			settings.setVodProgress(vodId, currentProgress);
			settings.setVodLength(vodId, vodLength);
			Log.d(LOG_TAG, "Saving Current progress: " + currentProgress);
		}

	}

	@Override
	public void onDestroy() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && getActivity() != null) {
			getActivity().unregisterReceiver(headsetIntentReceiver);
		}
		Log.d(LOG_TAG, "Destroying");
		if (fetchViewCountRunnable != null) {
			fetchViewCountHandler.removeCallbacks(fetchViewCountRunnable);
		}

		progressHandler.removeCallbacks(progressRunnable);
		super.onDestroy();
	}

	private void startFetchingCurrentChatters() {
		fetchChattersRunnable = new Runnable() {
			@Override
			public void run() {
				GetStreamChattersTask task = new GetStreamChattersTask(
						new GetStreamChattersTask.GetStreamChattersTaskDelegate() {
							@Override
							public void onChattersFetched(ArrayList<String> chatters) {

							}

							@Override
							public void onChattersFetchFailed() {

							}
						}, mChannelInfo.getStreamerName()
				);

				task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

				if (!StreamFragment.this.isDetached()) {
					fetchChattersHandler.postDelayed(this, fetchChattersDelay);
				}
			}
		};

		fetchChattersHandler.post(fetchChattersRunnable);
	}

	/**
	 * Starts fetching current viewers for the current stream
	 */
	private void startFetchingViewers() {
		fetchViewCountRunnable = new Runnable() {
			@Override
			public void run() {
				GetStreamViewersTask task = new GetStreamViewersTask(
						new GetStreamViewersTask.GetStreamViewersTaskDelegate() {
							@Override
							public void onViewersFetched(Integer currentViewers) {
								try {
									Log.d(LOG_TAG, "Fetching viewers");

									mCurrentViewersView.setText("" + currentViewers);
								} catch (Exception e) {
									e.printStackTrace();
								}
							}

							@Override
							public void onViewersFetchFailed() {
								// WELP
							}
						}, mChannelInfo.getUserId()
				);

				task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

				if (!StreamFragment.this.isDetached()) {
					fetchViewCountHandler.postDelayed(this, fetchViewCountDelay);
				}
			}
		};


		fetchViewCountHandler.post(fetchViewCountRunnable);
	}

	/**
	 * Sets up the show chat button.
	 * Sets the correct visibility and the onclicklistener
	 */
	private void setupShowChatButton() {

		checkShowChatButtonVisibility();
		mShowChatButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (!isVideoInterfaceShowing()) {
					showVideoInterface();
					delayHiding();
				}

				if (view.getRotation() == 0f) {
					showLandscapeChat();
				} else {
					hideLandscapeChat();
				}
			}
		});
	}

	/**
	 * Sets the correct visibility of the show chat button.
	 * If the screen is in landscape it is show, else it is shown
	 */
	private void checkShowChatButtonVisibility()  {
		if (isLandscape && vodId == null && settings.isChatInLandscapeEnabled()) {
			mShowChatButton.setRotation(0f);
			mShowChatButton.setVisibility(View.VISIBLE);
		} else {
			mShowChatButton.setVisibility(View.GONE);
		}
	}

	private void profileButtonClicked() {
		mProfileBottomSheet.show();
	}

	private void sleepButtonClicked() {
		if (sleepTimer == null) {
			sleepTimer = new SleepTimer(new SleepTimer.SleepTimerDelegate() {
				@Override
				public void onTimesUp() {
					stopAudioOnly();
					pauseStream();
				}

				@Override
				public void onStart(String message) {
					showSnackbar(message, SNACKBAR_SHOW_DURATION);
				}

				@Override
				public void onStop(String message) {
					showSnackbar(message, SNACKBAR_SHOW_DURATION);
				}
			}, getContext());
		}

		sleepTimer.show(getActivity());
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		if (!embedded) {
			// Only add chromecast btn for live streams
			if (vodId == null) {
				mCastManager.addMediaRouterButton(menu, R.id.media_route_menu_item);

				MenuItem routeItem = menu.findItem(R.id.media_route_menu_item);
				MediaRouteActionProvider mediaRouteButton = (MediaRouteActionProvider) MenuItemCompat.getActionProvider(routeItem);
				mediaRouteButton.setDialogFactory(mMediaRouteDialogFactory);
			}

			optionsMenuItem = menu.findItem(R.id.menu_item_options);
			optionsMenuItem.setVisible(false);
			optionsMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem menuItem) {
					if (mQualityButton != null) {
						mQualityButton.performClick();
					}
					return true;
				}
			});
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (!isVideoInterfaceShowing()) {
			mVideoWrapper.performClick();
			return true;
		}

		switch (item.getItemId()) {
			case R.id.menu_item_sleep:
				sleepButtonClicked();
				return true;
			case R.id.menu_item_profile:
				profileButtonClicked();
				return true;
			case R.id.menu_item_external:
				playWithExternalPlayer();
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private void setupLandscapeChat() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && vodId == null && settings.isChatLandscapeSwipable() && settings.isChatInLandscapeEnabled()) {
			final int width = getScreenWidth(getActivity());

			View.OnTouchListener touchListener = new View.OnTouchListener() {
				private int downPosition = width;
				private int widthOnDown = width;

				public boolean onTouch(View view, MotionEvent event) {
					if (isLandscape) {
						final int X = (int) event.getRawX();
						switch (event.getAction() & MotionEvent.ACTION_MASK) {
							case MotionEvent.ACTION_DOWN:
								RelativeLayout.LayoutParams lParams = (RelativeLayout.LayoutParams) mVideoWrapper.getLayoutParams();
								if (lParams.width > 0)
									widthOnDown = lParams.width;

								downPosition = (int) event.getRawX();
								break;
							case MotionEvent.ACTION_UP:
								int upPosition = (int) event.getRawX();
								int deltaPostion = upPosition - downPosition;

								if (deltaPostion < 20 && deltaPostion > -20) {
									return false;
								}

								if (upPosition < downPosition) {
									showLandscapeChat();
								} else {
									hideLandscapeChat();
								}

								break;
							case MotionEvent.ACTION_MOVE:
								RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mVideoWrapper.getLayoutParams();
								int newWidth = 0;

								if (X > downPosition) { // Swiping right
									newWidth = widthOnDown + (X - downPosition);
								} else { // Swiping left
									newWidth = widthOnDown - (downPosition - X);
								}

								if (newWidth > width - getLandscapeChatTargetWidth()) {
									layoutParams.width = newWidth;
								}

								mVideoWrapper.setLayoutParams(layoutParams);
								break;

						}
						rootView.invalidate();
					}
					return false;
				}
			};

			mVideoWrapper.setOnTouchListener(touchListener);
			mClickIntercepter.setOnTouchListener(touchListener);
		}
	}

	/**
	 * Show the landscape chat with an animation that changes the width of the videoview wrapper.
	 * The ShowChatButton is also rotated
	 */
	private void showLandscapeChat() {
		int width = getScreenWidth(getActivity());
		ResizeWidthAnimation resizeWidthAnimation = new ResizeWidthAnimation(mVideoWrapper, (width - getLandscapeChatTargetWidth()));
		resizeWidthAnimation.setDuration(250);
		mVideoWrapper.startAnimation(resizeWidthAnimation);
		mShowChatButton.animate().rotation(180f).start();
	}

	/**
	 * hides the landscape chat with an animation that changes the width of the videoview wrapper to the width of the screen.
	 * The ShowChatButton is also rotated
	 */
	private void hideLandscapeChat() {
		int width = getScreenWidth(getActivity());
		ResizeWidthAnimation resizeWidthAnimation = new ResizeWidthAnimation(mVideoWrapper, width);
		resizeWidthAnimation.setDuration(250);
		mVideoWrapper.startAnimation(resizeWidthAnimation);
		mShowChatButton.animate().rotation(0f).start();
	}

	public int getLandscapeChatTargetWidth() {
		return (int) (getScreenWidth(getActivity()) * (settings.getChatLandscapeWidth() / 100.0));
	}

	private void checkChromecastConnection() {
		if (mCastManager.isConnected() || mCastManager.isConnecting()) {
			if (getActivity() instanceof VODActivity) {
				showSnackbar(getString(R.string.stream_chromecast_no_vod), SNACKBAR_SHOW_DURATION);
				mCastManager.disconnect();
			} else {
				initCastingView();
				try {
					if (mCastManager.isRemoteMediaLoaded()) {
						showPauseIcon();
						onCastMediaPlaying();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void onCastMediaPlaying() {
		castingTextView.setText(getString(R.string.stream_chromecast_playing, mCastManager.getDeviceName()));
	}

	private void initCastingView() {
		castingViewVisible = true;
		auto.setVisibility(View.GONE); // Auto does not work on chromecast
		mVideoView.setVisibility(View.INVISIBLE);
		mBufferingView.setVisibility(View.GONE);
		previewInbackGround = false;
		castingTextView.setVisibility(View.VISIBLE);
		castingTextView.setText(getString(R.string.stream_chromecast_connecting));
		showVideoInterface();
	}

	private void disableCastingView() {
		castingViewVisible = false;
		auto.setVisibility(View.VISIBLE);
		mVideoView.setVisibility(View.VISIBLE);
		Service.bringToBack(mPreview);
		mBufferingView.setVisibility(View.VISIBLE);
		previewInbackGround = true;
		castingTextView.setVisibility(View.INVISIBLE);
		showVideoInterface();
	}

	private String getBestCastQuality(Map<String, String> castQualities, String quality, Integer numberOfTries) {
		if (numberOfTries > GetLiveStreamURL.CAST_QUALITIES.length - 1) {
			return null;
		}

		if (quality.equals(GetLiveStreamURL.QUALITY_AUTO) || quality.equals(GetLiveStreamURL.QUALITY_AUDIO_ONLY)) {
			quality = GetLiveStreamURL.QUALITY_MEDIUM;
		}

		if (castQualities.containsKey(quality)) {
			return quality;
		} else {
			numberOfTries++;
			List<String> qualityList = Arrays.asList(GetLiveStreamURL.CAST_QUALITIES);
			int next = qualityList.indexOf(quality) - 1;
			if (next < 0) {
				quality = GetLiveStreamURL.QUALITY_SOURCE;
			} else {
				quality = qualityList.get(next);
			}
			return getBestCastQuality(castQualities, quality, numberOfTries);
		}
	}

	private void playOnCast() {
		Map<String, String> availableCastQualities = new HashMap<>(qualityURLs);
		// Cant play auto or audio only on chrome cast
		if (availableCastQualities.containsKey(GetLiveStreamURL.QUALITY_AUTO)) {
			availableCastQualities.remove(GetLiveStreamURL.QUALITY_AUTO);
		}
		if (availableCastQualities.containsKey(GetLiveStreamURL.QUALITY_AUDIO_ONLY)) {
			availableCastQualities.remove(GetLiveStreamURL.QUALITY_AUDIO_ONLY);
		}

		String castQuality = getBestCastQuality(availableCastQualities, settings.getPrefStreamQuality(), 0);
		if (castQuality != null) {
			updateSelectedQuality(castQuality);
			playOnCast(qualityURLs.get(castQuality));
			Log.d(LOG_TAG, "CASTING URL: " + qualityURLs.get(castQuality));
		} else {
			//No Available qualities for casting
			showSnackbar(getString(R.string.stream_no_chromecast_quality_supported), SNACKBAR_SHOW_DURATION);
		}
	}

	/**
	 * Sends the URL to a chromecast (Or android TV) if it is connected.
	 * @param videoURL
	 */
	private void playOnCast(String videoURL) {
		//ToDo: We probably need to make a custom Cast receiver to play a link from Twitch. As Twitch doesnt allow third party receivers to play link from another origin
		// Use https://www.udacity.com/course/progress#!/c-ud875B and
		// https://github.com/googlecast/CastReferencePlayer
		try {
			String logoImageUrl = mChannelInfo.getLogoURLString();
			String streamerName = mChannelInfo.getDisplayName();

			if(mCastManager.isConnected()) {
				MediaMetadata mediaMetadata = new MediaMetadata();
				mediaMetadata.putString(getString(R.string.stream_fragment_vod_id), vodId);
				mediaMetadata.putInt(getString(R.string.stream_fragment_vod_length), vodLength);
				mediaMetadata.putString(getString(R.string.stream_fragment_streamerInfo), new Gson().toJson(mChannelInfo));
				mediaMetadata.putString(MediaMetadata.KEY_TITLE, getString(R.string.app_name) + " " + streamerName);
				if (logoImageUrl != null) {
					mediaMetadata.addImage(new WebImage(Uri.parse(logoImageUrl)));
				}

				MediaInfo.Builder mediaBuilder = new MediaInfo.Builder(videoURL)
						.setMetadata(mediaMetadata)
						.setContentType("application/x-mpegURL");


				if (vodId == null) {
					mediaBuilder
							.setStreamType(MediaInfo.STREAM_TYPE_LIVE)
							.setStreamDuration(MediaInfo.UNKNOWN_DURATION);
				} else {
					mediaBuilder
							.setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
							.setStreamDuration(vodLength/1000);
				}


				try {
					mCastManager.loadMedia(mediaBuilder.build(), true, 0);


				} catch (TransientNetworkDisconnectionException | NoConnectionException e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	/**
	 * Checks if the activity was started with a shared view in high API levels.
	 */
	private void setPreviewAndCheckForSharedTransition() {
		final Intent intent = getActivity().getIntent();
		if (intent.hasExtra(getString(R.string.stream_preview_url))) {
			String imageUrl = intent.getStringExtra(getString(R.string.stream_preview_url));

			if (imageUrl == null || imageUrl.isEmpty()) {
				return;
			}

			RequestCreator creator = Picasso.with(getContext()).load(imageUrl);
			Target target = new Target() {
				@Override
				public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
					mPreview.setImageBitmap(bitmap);
				}

				public void onBitmapFailed(Drawable errorDrawable) {}
				public void onPrepareLoad(Drawable placeHolderDrawable) {}
			};
			creator.into(target);
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && intent.getBooleanExtra(getString(R.string.stream_shared_transition), false)) {
			mPreview.setTransitionName(getString(R.string.stream_preview_transition));

			final View[] viewsToHide = {mVideoView, mToolbar, mControlToolbar, mVideoBackground};
			for (View view : viewsToHide) {
				view.setVisibility(View.INVISIBLE);
			}

			getActivity().getWindow().getEnterTransition().addListener(new Transition.TransitionListener() {

				@Override
				public void onTransitionEnd(Transition transition) {
					TransitionManager.beginDelayedTransition(
							mVideoWrapper,
							new com.transitionseverywhere.Fade()
									.setDuration(340)
									.excludeTarget(mVideoView, true)
									.excludeTarget(mPreview, true)
					);

					for (View view : viewsToHide) {
						view.setVisibility(View.VISIBLE);
					}
				}

				@Override
				public void onTransitionCancel(Transition transition) {
					onTransitionEnd(transition);
				}

				public void onTransitionStart(Transition transition) {}
				public void onTransitionPause(Transition transition) {}
				public void onTransitionResume(Transition transition) {}
			});
		}

	}

	/**
	 * Checks if the user is currently in progress of watching a VOD. If so seek forward to where the user left off.
	 */
	private void checkVodProgress() {
		if (vodId != null) {
			if (currentProgress == 0) {
				currentProgress = settings.getVodProgress(vodId);
				mVideoView.seekTo(currentProgress * 1000);
				Log.d(LOG_TAG, "Current progress: " + currentProgress);
			} else {
				mVideoView.seekTo(currentProgress * 1000);
				Log.d(LOG_TAG, "Seeking to " + currentProgress * 1000);
			}
		}
	}

	/**
	 * Call to make sure the UI is shown correctly
	 */
	private void updateUI() {
		setAndroidUiMode();
		keepControlIconsInView();
		setVideoViewLayout();
	}

	/**
	 * This makes sure that the System UI automatically hides when the user changes focus by opening the navigation drawer.
	 * @param hasFocus
	 */
	public void onWindowFocusChanged(boolean hasFocus) {
		Log.d(LOG_TAG, "WindowFocusChanged to " + hasFocus + " - isLandscape " + isLandscape);
		setAndroidUiMode();
	}

	/**
	 * Sets the System UI visibility so that the status- and navigation bar automatically hides if the app is current in fullscreen or in landscape.
	 * But they will automatically show when the user touches the screen.
	 */
	private void setAndroidUiMode() {
		if (getActivity() == null) {
			return;
		}

		View decorView = getActivity().getWindow().getDecorView();
		if(isLandscape || isFullscreen) {
			Log.d(LOG_TAG, "Hiding navigation");

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
				decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
						| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // Hide navigation bar
						| View.SYSTEM_UI_FLAG_FULLSCREEN // Hide Status bar
						| View.SYSTEM_UI_FLAG_IMMERSIVE);
			} else {
				decorView.setSystemUiVisibility(
						View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
								| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // Hide navigation bar
								| View.SYSTEM_UI_FLAG_FULLSCREEN // Hide Status bar

				);
			}

		} else {
			Log.d(LOG_TAG, "Showing navigation");
			decorView.setSystemUiVisibility(0); // Remove all flags.
		}
	}

	private void setVideoViewLayout() {
		DisplayMetrics displaymetrics = new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		int width = displaymetrics.widthPixels;
		RelativeLayout.LayoutParams layoutWrapper = (RelativeLayout.LayoutParams) mVideoWrapper.getLayoutParams();
		if(isLandscape) {
			layoutWrapper.height = FrameLayout.LayoutParams.MATCH_PARENT;
		} else {
			layoutWrapper.height = (int) Math.ceil(1.0*width/(16.0/9.0));
		}
		layoutWrapper.width = RelativeLayout.LayoutParams.MATCH_PARENT;
		mVideoWrapper.setLayoutParams(layoutWrapper);
	}

	/**
	 * Delays the hiding for the Video control interface.
	 */
	private void delayHiding() {
		delayAnimationHandler.postDelayed(hideAnimationRunnable, HIDE_ANIMATION_DELAY);
	}

	/**
	 * Checks if the video interface is fully showing
	 * @return
	 */
	public boolean isVideoInterfaceShowing() {
		return mControlToolbar.getAlpha() == 1f;
	}

	/**
	 * Hides the video control interface with animations
	 */
	private void hideVideoInterface() {
		if(mToolbar != null && !mCastManager.isConnected() && !mCastManager.isConnecting() && !audioViewVisible && !chatOnlyViewVisible) {
			mToolbar .animate().alpha(0f).setInterpolator(new AccelerateDecelerateInterpolator()).start();
			mControlToolbar .animate().alpha(0f).setInterpolator(new AccelerateDecelerateInterpolator()).start();
			mPlayPauseWrapper.animate().alpha(0f).setInterpolator(new AccelerateDecelerateInterpolator()).start();
			mShowChatButton.animate().alpha(0f).setInterpolator(new AccelerateDecelerateInterpolator()).start();
			changeVideoControlClickablity(false);
		}
	}

	/**
	 * Shows the video control interface with animations
	 */
	protected void showVideoInterface() {
		int MaintoolbarY = 0, CtrlToolbarY = 0;
		if ((isFullscreen || isLandscape) && isDeviceBelowKitkat()) {
			MaintoolbarY = getStatusBarHeight();
		}
		if((isFullscreen || isLandscape) && Service.isTablet(getContext()) && isDeviceBelowKitkat()) {
			CtrlToolbarY = getNavigationBarHeight();
		}

		mControlToolbar.setTranslationY(-CtrlToolbarY);
		mControlToolbar.animate().alpha(1f).start();
		mToolbar.setTranslationY(MaintoolbarY);
		mToolbar.animate().alpha(1f).start();
		mPlayPauseWrapper.animate().alpha(1f).setInterpolator(new AccelerateDecelerateInterpolator()).start();
		mShowChatButton.animate().alpha(1f).setInterpolator(new AccelerateDecelerateInterpolator()).start();
		changeVideoControlClickablity(true);
	}

	private void changeVideoControlClickablity(boolean clickable) {
		mClickIntercepter.setVisibility(clickable ? View.GONE : View.VISIBLE);
		mClickIntercepter.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				mVideoWrapper.performClick();
			}
		});
	}

	/**
	 * Keeps the rightmost icons on the toolbars in view when the device is in landscape.
	 * Otherwise the icons would be covered my the navigationbar
	 */
	private void keepControlIconsInView() {
		if (isDeviceBelowKitkat() || settings.getStreamPlayerShowNavigationBar()) {
			int ctrlPadding = originalCtrlToolbarPadding;
			int mainPadding = originalMainToolbarPadding;
			int delta = getNavigationBarHeight();

			if((isFullscreen || isLandscape) && !Service.isTablet(getContext())) {
				ctrlPadding += delta;
				mainPadding += delta;
			}

			mShowChatButton.setPadding(0, 0, ctrlPadding, 0);
			mToolbar.setPadding(0, 0, mainPadding, 0);
			mControlToolbar.setPadding(0, 0, ctrlPadding, 0);
		}
	}

	/**
	 * Returns the height of the statusbar
	 */
	private int getStatusBarHeight() {
		int result = 0;
		int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = getResources().getDimensionPixelSize(resourceId);
		}
		return result;
	}

	/**
	 * Returns the height of the navigation bar.
	 * If the device doesn't have a navigaion bar (Such as Samsung Galaxy devices) the height is 0
	 */
	private int getNavigationBarHeight() {
		Resources resources = getResources();
		int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
		if (resourceId > 0) {
			return resources.getDimensionPixelSize(resourceId);
		}
		return 0;
	}

	/**
	 * If the device isn't currently in fullscreen a request is sent to turn the device into landscape.
	 * Otherwise if the device is in fullscreen then is releases the lock by requesting for an unspecified orientation
	 * After and update to the VideoView layout is requested.
	 */
	public void toggleFullscreen() {
		isFullscreen = !isFullscreen;
		if(isFullscreen) {
			getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
		} else {
			getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
		updateFullscreenButtonState();
		setVideoViewLayout();
	}

	/**
	 * Sets the icon drawable of the fullscreen button depending on the current state.
	 * If the app is currently in full screen an "exit fullscreen" icon will appear,
	 * else and "enter fullscreen" icon will.
	 */
	private void updateFullscreenButtonState() {
		if (isFullscreen) {
			mFullScreenButton.setImageResource(R.drawable.ic_fullscreen_exit_24dp);
		} else {
			mFullScreenButton.setImageResource(R.drawable.ic_fullscreen_24dp);
		}
	}

	/**
	 * Pauses and stops the playback of the Video Stream
	 */
	private void pauseStream() {
		Log.d(LOG_TAG, "Chat, pausing stream");
		showPlayIcon();

		delayAnimationHandler.removeCallbacks(hideAnimationRunnable);

		if (mCastManager.isConnected()) {
			try {
				mCastManager.pause();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if(isAudioOnlyModeEnabled()) {
			Log.d(LOG_TAG, "Pausing audio");
			Intent pausePlayerService = new Intent(getContext(), PlayerService.class);
			pausePlayerService.setAction(PlayerService.ACTION_PAUSE);
			getContext().startService(pausePlayerService);
		} else {
			mVideoView.pause();
		}
		releaseScreenOn();
	}

	/**
	 * Goes forward to live and starts plackback of the VideoView
	 */
	private void resumeStream() {
		showPauseIcon();
		mBufferingView.start();

		if (mCastManager.isConnected() || mCastManager.isConnecting()) {
			try {
				mCastManager.play();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if(isAudioOnlyModeEnabled()) {
			Log.d(LOG_TAG, "Resuming Audio");
			Intent playServiceIntent = new Intent(getContext(), PlayerService.class);
			playServiceIntent.setAction(PlayerService.ACTION_PLAY);
			getContext().startService(playServiceIntent);
		} else {
			if (vodId == null) {
				mVideoView.resume(); // Go forward to  live
			}

			mVideoView.start();
		}

		checkVodProgress();
		keepScreenOn();
	}

	/**
	 * Tries playing stream with a quality.
	 * If the given quality doesn't exist for the stream the try the next best quality option.
	 * If no Quality URLS have yet been created then try to start stream with an aync task.
	 * @param quality
	 */
	protected void startStreamWithQuality(String quality) {
		if(qualityURLs == null) {
			startStreamWithTask();
		} else {
			if(qualityURLs.containsKey(quality)) {
				if (chatOnlyViewVisible || audioViewVisible) {
					return;
				}

				playUrl(qualityURLs.get(quality));
				showQualities();
				updateSelectedQuality(quality);
				showPauseIcon();
				mBufferingView.start();
				Log.d(LOG_TAG, "Starting Stream With a quality on " + quality  + " for " + mChannelInfo.getDisplayName());
				Log.d(LOG_TAG, "URLS: " + qualityURLs.keySet().toString());
			} else if(!qualityURLs.isEmpty()) {
				Log.d(LOG_TAG, "Quality unavailable for this stream -  " + quality + ". Trying next best");
				tryNextBestQuality(quality);
			}
		}
	}

	/**
	 * Starts and Aync task that fetches all available Stream URLs for a stream,
	 * then tries to start stream with the latest user defined quality setting.
	 * If no URLs are available for the stream, the user is notified.
	 */
	private void startStreamWithTask() {
		GetLiveStreamURL.AsyncResponse delegate = new GetLiveStreamURL.AsyncResponse() {
			@Override
			public void finished(HashMap<String, String> url) {
				try {
					if (!url.isEmpty()) {
						updateQualitySelections(url.keySet());
						qualityURLs = url;

						if (!checkForAudioOnlyMode()) {
							startStreamWithQuality(new Settings(getContext()).getPrefStreamQuality());
						}
					} else {
						playbackFailed();
						return;
					}
				} catch (IllegalStateException | NullPointerException e) {
					e.printStackTrace();
				}
			}
		};

		if (vodId == null) {
			GetLiveStreamURL task = new GetLiveStreamURL(delegate);
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mChannelInfo.getStreamerName());
		} else {
			GetLiveStreamURL task = new GetVODStreamURL(delegate);
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, vodId.substring(1));
		}
	}

	/**
	 * Connects to the Twitch API to fetch the live stream url and quality selection urls.
	 * If the task is successful the quality selector views' click listeners will be updated.
	 */
	private void updateQualitySelectorsWithTask() {
		GetLiveStreamURL.AsyncResponse delegate = new GetLiveStreamURL.AsyncResponse() {
			@Override
			public void finished(HashMap<String, String> url) {
				try {
					if (!url.isEmpty()) {
						updateQualitySelections(url.keySet());
						qualityURLs = url;
					}
				} catch (IllegalStateException | NullPointerException e) {
					e.printStackTrace();
				}
			}
		};

		if (vodId == null) {
			GetLiveStreamURL task = new GetLiveStreamURL(delegate);
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mChannelInfo.getStreamerName());
		} else {
			GetLiveStreamURL task = new GetVODStreamURL(delegate);
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, vodId.substring(1));
		}
	}

	/**
	 * Stops the buffering and notifies the user that the stream could not be played
	 */
	private void playbackFailed() {
		mBufferingView.stop();
		if (vodId == null) {
			showSnackbar(getString(R.string.stream_playback_failed), SNACKBAR_SHOW_DURATION);
		} else {
			showSnackbar(getString(R.string.vod_playback_failed), SNACKBAR_SHOW_DURATION);
		}
	}

	private void showSnackbar(String message, int duration) {
		if (getActivity() != null && !isDetached()) {
			View mainView = ((StreamActivity) getActivity()).getMainContentLayout();

			if ((snackbar == null || !snackbar.isShown()) && mainView != null) {
				snackbar = Snackbar.make(mainView, message, duration);
				snackbar.show();
			}
		}

	}

	private void tryNextBestQuality(String quality) {
		if (triesForNextBest < GetLiveStreamURL.QUALITIES.length - 1) { // Subtract 1 as we don't count AUDIO ONLY as a quality
			triesForNextBest++;
			List<String> qualityList = Arrays.asList(GetLiveStreamURL.QUALITIES);
			int next = qualityList.indexOf(quality) - 1;
			if(next < 0) {
				startStreamWithQuality(GetLiveStreamURL.QUALITY_SOURCE);
			} else {
				startStreamWithQuality(qualityList.get(next));
			}
		} else {
			playbackFailed();
		}
	}

	/**
	 * Sets the URL to the VideoView and ChromeCast and starts playback.
	 * @param url
	 */
	private void playUrl(String url) {
		if (mCastManager.isConnected() || mCastManager.isConnecting()) {
			playOnCast();
		}

		mVideoView.setVideoPath(url);
		resumeStream();
	}

	private void playWithExternalPlayer() {
		if (qualityURLs == null) {
			Toast.makeText(getContext(), R.string.error_external_playback_failed, Toast.LENGTH_LONG).show();
		}

		String castQuality = getBestCastQuality(qualityURLs, settings.getPrefStreamQuality(), 0);
		if (castQuality == null) {
			Toast.makeText(getContext(), R.string.error_external_playback_failed, Toast.LENGTH_LONG).show();
			return;
		}

		updateSelectedQuality(castQuality);
		String url = qualityURLs.get(castQuality);

		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.parse(url), "video/*");
		startActivity(Intent.createChooser(intent, getString(R.string.stream_external_play_using)));
	}

	/**
	 * Sets up audio mode and starts playback of audio, while pausing any playing video
	 */
	private void playAudioOnly() {
		String urlToPlay = getLowestQualityUrl();
		if (urlToPlay != null) {
			try {
				if (castingViewVisible) {
					mCastManager.disconnect();
				}

				Intent playerService = PlayerService.createPlayServiceIntent(
						getContext(),
						urlToPlay,
						mChannelInfo,
						vodId != null,
						vodLength,
						vodId,
						currentProgress,
						getActivity().getIntent(),
						getAudioOnlyDelegateReceiver()
				);

				getContext().startService(playerService);
				Log.d(LOG_TAG, "Audio, service started");
				initAudioOnlyView();
				showPauseIcon();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			showSnackbar(getString(R.string.stream_no_audio_only), SNACKBAR_SHOW_DURATION);
			startStreamWithQuality(settings.getPrefStreamQuality());
		}
	}

	private ResultReceiver getAudioOnlyDelegateReceiver() {
		return new ResultReceiver(null) {
			@Override
			protected void onReceiveResult(int resultCode, Bundle resultData) {
				Log.d(LOG_TAG, "Received delegate: " + resultCode);
				try {
					switch (resultCode) {
						case PlayerService.DELEGATE_PLAY:
							mBufferingView.stop();
							showPauseIcon();
							break;
						case PlayerService.DELEGATE_PAUSE:
							showPlayIcon();
							break;
						case PlayerService.DELEGATE_STOP:
							stopAudioOnlyNoServiceCall();
							startStreamWithQuality(settings.getPrefStreamQuality());
							break;
						case PlayerService.DELEGATE_SEEK_TO:
							int seekToPosition = resultData.getInt(PlayerService.DELEGATE_SEEK_TO_POSITION);

							break;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
	}

	private void registerAudioOnlyDelegate() {
		if (PlayerService.getInstance() != null) {
			PlayerService.getInstance().registerDelegate(getAudioOnlyDelegateReceiver());
		}
	}

	private String  getLowestQualityUrl() {
		if (qualityURLs == null) {
			return null;
		}

		if (qualityURLs.containsKey(GetLiveStreamURL.QUALITY_MOBILE)) {
			return qualityURLs.get(GetLiveStreamURL.QUALITY_MOBILE);
		} else if (qualityURLs.containsKey(GetLiveStreamURL.QUALITY_LOW)) {
			return qualityURLs.get(GetLiveStreamURL.QUALITY_LOW);
		} else if (qualityURLs.containsKey(GetLiveStreamURL.QUALITY_MEDIUM)) {
			return qualityURLs.get(GetLiveStreamURL.QUALITY_MEDIUM);
		} else if (qualityURLs.containsKey(GetLiveStreamURL.QUALITY_HIGH)) {
			return qualityURLs.get(GetLiveStreamURL.QUALITY_HIGH);
		} else if (qualityURLs.containsKey(GetLiveStreamURL.QUALITY_SOURCE)) {
			return qualityURLs.get(GetLiveStreamURL.QUALITY_SOURCE);
		} else {
			return null;
		}
	}

	/**
	 * Notifies the system that the screen though not timeout and fade to black.
	 */
	private void keepScreenOn() {
		getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	/**
	 * Notifies the system that the screen can now time out.
	 */
	private void releaseScreenOn() {
		getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	private void updateSelectedQuality(String quality) {
		//TODO: Bad design
		if (quality == null) {
			resetQualityViewBackground(null);
		} else if (quality.equals(GetLiveStreamURL.QUALITY_AUTO)) {
			resetQualityViewBackground(auto);
		} else if (quality.equals(GetLiveStreamURL.QUALITY_SOURCE)) {
			resetQualityViewBackground(source);
		} else if (quality.equals(GetLiveStreamURL.QUALITY_HIGH)) {
			resetQualityViewBackground(high);
		} else if (quality.equals(GetLiveStreamURL.QUALITY_MEDIUM)) {
			resetQualityViewBackground(medium);
		} else if (quality.equals(GetLiveStreamURL.QUALITY_LOW)) {
			resetQualityViewBackground(low);
		} else if (quality.equals(GetLiveStreamURL.QUALITY_MOBILE)) {
			resetQualityViewBackground(mobile);
		}
	}

	/**
	 * Resets the background color of all the select quality views in the bottom dialog
	 */
	private void resetQualityViewBackground(TextView selected) {
		TextView[] textViews = {auto, source, high, medium, low, mobile};
		for (TextView v : textViews) {
			if (v.equals(selected)) {
				v.setBackgroundColor(Service.getColorAttribute(R.attr.navigationDrawerHighlighted, R.color.grey_300, getContext()));
			} else {
				v.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.transparent));
			}
		}
	}

	/**
	 * Adds the available qualities for a stream to the spinner menu
	 * @param availableQualities
	 */
	private void updateQualitySelections(Set<String> availableQualities) {
		setupViewQuality(auto, GetLiveStreamURL.QUALITY_AUTO, availableQualities);
		setupViewQuality(source, GetLiveStreamURL.QUALITY_SOURCE, availableQualities);
		setupViewQuality(high, GetLiveStreamURL.QUALITY_HIGH, availableQualities);
		setupViewQuality(medium, GetLiveStreamURL.QUALITY_MEDIUM, availableQualities);
		setupViewQuality(low, GetLiveStreamURL.QUALITY_LOW, availableQualities);
		setupViewQuality(mobile, GetLiveStreamURL.QUALITY_MOBILE, availableQualities);
	}

	/**
	 * Sets up a select quality view in the bottom dialog. If the quality doesn't exist in the availableQualities the view is hidden.
	 * If it exists an onClick listener is set
	 * @param view The select quality view
	 * @param quality The quality name corresponding to the view
	 * @param availableQualities Set of available qualities
	 */
	private void setupViewQuality(TextView view, String quality, Set<String> availableQualities) {
		if(!availableQualities.contains(quality)) {
			view.setVisibility(View.GONE);
		} else {
			setQualityOnClick(view);
		}
	}

	/**
	 * Sets an OnClickListener on a select quality view (From bottom dialog).
	 * The Listener starts the stream with a new quality setting and updates the background for the select quality views in the bottom dialog
	 * @param qualityView
	 */
	private void setQualityOnClick(final TextView qualityView) {
		qualityView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String quality = supportedQualities.get(qualityView.getText());
				settings.setPrefStreamQuality(quality);
				startStreamWithQuality(quality);
				resetQualityViewBackground(qualityView);
				mQualityBottomSheet.dismiss();
			}
		});
	}

	private BottomSheetBehavior getDefaultBottomSheetBehaviour(View bottomSheetView) {
		BottomSheetBehavior behavior = BottomSheetBehavior.from((View) bottomSheetView.getParent());
		behavior.setPeekHeight(getContext().getResources().getDisplayMetrics().heightPixels/3);
		return behavior;
	}

	private void setupProfileBottomSheet() {
		View v = LayoutInflater.from(getContext()).inflate(R.layout.stream_profile_preview, null);
		mProfileBottomSheet = new BottomSheetDialog(getContext());
		mProfileBottomSheet.setContentView(v);
		final BottomSheetBehavior behavior = getDefaultBottomSheetBehaviour(v);

		mProfileBottomSheet.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialogInterface) {
				behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
			}
		});

		TextView mNameView = (TextView) mProfileBottomSheet.findViewById(R.id.twitch_name);
		TextView mFollowers = (TextView) mProfileBottomSheet.findViewById(R.id.txt_followers);
		TextView mViewers = (TextView) mProfileBottomSheet.findViewById(R.id.txt_viewers);
		ImageView mFollowButton = (ImageView) mProfileBottomSheet.findViewById(R.id.follow_unfollow_icon);
		ImageView mFullProfileButton = (ImageView) mProfileBottomSheet.findViewById(R.id.full_profile_icon);
		RecyclerView mPanelsRecyclerView = (RecyclerView) mProfileBottomSheet.findViewById(R.id.panel_recyclerview);

		mNameView.setText(mChannelInfo.getDisplayName());
		mFollowers.setText(mChannelInfo.getFollowers() + "");
		mViewers.setText(mChannelInfo.getViews() + "");

		mFullProfileButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				mProfileBottomSheet.dismiss();

				final Intent intent = new Intent(getContext(), ChannelActivity.class);
				intent.putExtra(getContext().getResources().getString(R.string.channel_info_intent_object), mChannelInfo);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

				getContext().startActivity(intent);
			}
		});

		setupFollowButton(mFollowButton);
		setupPanels(mPanelsRecyclerView);
	}

	private void setupFollowButton(final ImageView imageView) {
		final FollowHandler mFollowHandler = new FollowHandler(
				mChannelInfo,
				getContext(),
				new FollowHandler.Delegate() {
					@Override
					public void streamerIsFollowed() {}

					@Override
					public void streamerIsNotFollowed() {}

					@Override
					public void userIsNotLoggedIn() {
						imageView.setVisibility(View.GONE);
					}

					@Override
					public void followSuccess() {}

					@Override
					public void followFailure() {}

					@Override
					public void unfollowSuccess() {}

					@Override
					public void unfollowFailure() {}
				}
		);
		updateFollowIcon(imageView, mFollowHandler.isStreamerFollowed());

		imageView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final boolean isFollowed = mFollowHandler.isStreamerFollowed();
				if (isFollowed) {
					mFollowHandler.unfollowStreamer();
				} else {
					mFollowHandler.followStreamer();
				}

				final int ANIMATION_DURATION = 240;

				imageView.animate()
						.setDuration(ANIMATION_DURATION)
						.alpha(0f)
						.start();

				new Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						updateFollowIcon(imageView, !isFollowed);
						imageView.animate().alpha(1f).setDuration(ANIMATION_DURATION).start();
					}
				}, ANIMATION_DURATION);
			}
		});
	}

	private void updateFollowIcon(ImageView imageView, boolean isFollowing) {
		@DrawableRes int imageRes = isFollowing
				? R.drawable.ic_heart_broken_24dp
				: R.drawable.ic_heart_24dp;
		imageView.setImageResource(imageRes);
	}

	private void setupPanels(RecyclerView recyclerView) {
		final PanelAdapter mPanelAdapter = new PanelAdapter(getActivity());
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
		recyclerView.setAdapter(mPanelAdapter);

		GetPanelsTask mTask = new GetPanelsTask(mChannelInfo.getStreamerName(), new GetPanelsTask.Delegate() {
			@Override
			public void onPanelsFetched(List<Panel> result) {
				mPanelAdapter.addPanels(result);
			}
		});
		mTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	/**
	 * Setups the Quality Select spinner.
	 * Automatically hides the text of the selected Quality
	 */
	private void setupSpinner() {
		supportedQualities = HashBiMap.create();
		supportedQualities.put(getResources().getString(R.string.quality_auto), 	GetLiveStreamURL.QUALITY_AUTO);
		supportedQualities.put(getResources().getString(R.string.quality_source), GetLiveStreamURL.QUALITY_SOURCE);
		supportedQualities.put(getResources().getString(R.string.quality_high), 	GetLiveStreamURL.QUALITY_HIGH);
		supportedQualities.put(getResources().getString(R.string.quality_medium), GetLiveStreamURL.QUALITY_MEDIUM);
		supportedQualities.put(getResources().getString(R.string.quality_low), GetLiveStreamURL.QUALITY_LOW);
		supportedQualities.put(getResources().getString(R.string.quality_mobile), GetLiveStreamURL.QUALITY_MOBILE);

		mQualityButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mQualityBottomSheet.show();
			}
		});

		View v = LayoutInflater.from(getContext()).inflate(R.layout.stream_settings, null);
		mQualityBottomSheet = new BottomSheetDialog(getContext());
		mQualityBottomSheet.setContentView(v);

		final BottomSheetBehavior behavior = getDefaultBottomSheetBehaviour(v);

		mQualityBottomSheet.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialogInterface) {
				behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
			}
		});

		auto 	= (TextView) mQualityBottomSheet.findViewById(R.id.auto);
		source 	= (TextView) mQualityBottomSheet.findViewById(R.id.source);
		high 	= (TextView) mQualityBottomSheet.findViewById(R.id.high);
		medium 	= (TextView) mQualityBottomSheet.findViewById(R.id.medium);
		low 	= (TextView) mQualityBottomSheet.findViewById(R.id.low);
		mobile 	= (TextView) mQualityBottomSheet.findViewById(R.id.mobile);

		mQualityWrapper = (LinearLayout) mQualityBottomSheet.findViewById(R.id.quality_wrapper);
		mAudioOnlySelector = (CheckedTextView) mQualityBottomSheet.findViewById(R.id.audio_only_selector);
		mChatOnlySelector = (CheckedTextView) mQualityBottomSheet.findViewById(R.id.chat_only_selector);
		TextView optionsTitle = (TextView) mQualityBottomSheet.findViewById(R.id.options_text);

		if (optionsTitle != null) {
			optionsTitle.setVisibility(View.VISIBLE);
		}

		if (vodId == null) {
			mChatOnlySelector.setVisibility(View.VISIBLE);
		}

		mAudioOnlySelector.setVisibility(View.VISIBLE);
		mAudioOnlySelector.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				mQualityBottomSheet.dismiss();
				audioOnlyClicked();
			}
		});
		mChatOnlySelector.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				mQualityBottomSheet.dismiss();
				chatOnlyClicked();
			}
		});
	}

	private void initAudioOnlyView() {
		if (!audioViewVisible) {
			audioViewVisible = true;
			mVideoView.setVisibility(View.INVISIBLE);
			mBufferingView.start();
			//mBufferingView.setVisibility(View.GONE);
			previewInbackGround = false;
			castingTextView.setVisibility(View.VISIBLE);
			castingTextView.setText(getString(R.string.stream_audio_only_active));

			showVideoInterface();
			updateSelectedQuality(null);
			hideQualities();
		}
	}

	private void disableAudioOnlyView() {
		if (audioViewVisible) {
			mAudioOnlySelector.setChecked(false);
			audioViewVisible = false;
			mVideoView.setVisibility(View.VISIBLE);
			mBufferingView.setVisibility(View.VISIBLE);
			Service.bringToBack(mPreview);
			previewInbackGround = true;
			castingTextView.setVisibility(View.INVISIBLE);

			showVideoInterface();
			startStreamWithQuality(settings.getPrefStreamQuality());
		}
	}

	private boolean checkForAudioOnlyMode() {
		boolean isAudioOnly = isAudioOnlyModeEnabled();
		if (isAudioOnly) {
			mAudioOnlySelector.setChecked(isAudioOnlyModeEnabled());

			if (vodId != null && PlayerService.getInstance().isPlayingVod() && PlayerService.getInstance().getVodId() != null && PlayerService.getInstance().getVodId().equals(vodId)) {
				int position = PlayerService.getInstance().getMediaPlayer().getCurrentPosition();

				initAudioOnlyView();
				mBufferingView.stop();
				currentProgress = position/1000;
				if (PlayerService.getInstance().getMediaPlayer().isPlaying()) {
					showPauseIcon();
				} else {
					showPlayIcon();
				}
			} else if (PlayerService.getInstance().getStreamerInfo().equals(mChannelInfo) && vodId == null) {
				initAudioOnlyView();
				mBufferingView.stop();
				showPauseIcon();
			} else {
				playAudioOnly();
			}
		}

		return isAudioOnly;
	}

	private boolean isAudioOnlyModeEnabled() {
		PlayerService instance = PlayerService.getInstance();
		return instance != null
				&& instance.getMediaSession() != null
				&& instance.getMediaSession().isActive()
				&& instance.getMediaPlayer() != null;
	}

	private void audioOnlyClicked() {
		mAudioOnlySelector.setChecked(!mAudioOnlySelector.isChecked());
		if (mAudioOnlySelector.isChecked()) {
			playAudioOnly();
		} else {
			stopAudioOnly();
		}
	}

	private void stopAudioOnly() {
		Intent playerServiceIntent = new Intent(getContext(), PlayerService.class);
		playerServiceIntent.setAction(PlayerService.ACTION_STOP);
		getContext().startService(playerServiceIntent);
		disableAudioOnlyView();
		showPlayIcon();
		//startStreamWithQuality(settings.getPrefStreamQuality());
	}

	private void stopAudioOnlyNoServiceCall() {
		disableAudioOnlyView();
	}

	private void chatOnlyClicked() {
		mChatOnlySelector.setChecked(!mChatOnlySelector.isChecked());
		if (mChatOnlySelector.isChecked()) {
			initChatOnlyView();
		} else {
			disableChatOnlyView();
		}
	}

	private void initChatOnlyView() {
		if (!chatOnlyViewVisible) {
			chatOnlyViewVisible = true;
			if (isFullscreen) {
				toggleFullscreen();
			}

			getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

			videoHeightBeforeChatOnly = mVideoWrapper.getHeight();
			ResizeHeightAnimation heightAnimation = new ResizeHeightAnimation(mVideoWrapper, (int) getResources().getDimension(R.dimen.main_toolbar_height));
			heightAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
			heightAnimation.setDuration(240);
			mVideoWrapper.startAnimation(heightAnimation);

			mPlayPauseWrapper.setVisibility(View.GONE);
			mControlToolbar.setVisibility(View.GONE);
			mToolbar.setBackgroundColor(Service.getColorAttribute(R.attr.colorPrimary, R.color.primary, getContext()));

			mVideoView.stopPlayback();
			optionsMenuItem.setVisible(true);

			showVideoInterface();
			updateSelectedQuality(null);
			hideQualities();
		}
	}

	private void disableChatOnlyView() {
		if (chatOnlyViewVisible) {
			chatOnlyViewVisible = false;
			getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

			ResizeHeightAnimation heightAnimation = new ResizeHeightAnimation(mVideoWrapper, videoHeightBeforeChatOnly);
			heightAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
			heightAnimation.setDuration(240);
			heightAnimation.setFillAfter(false);
			mVideoWrapper.startAnimation(heightAnimation);

			mControlToolbar.setVisibility(View.VISIBLE);
			mPlayPauseWrapper.setVisibility(View.VISIBLE);
			mToolbar.setBackgroundColor(Service.getColorAttribute(R.attr.streamToolbarColor, R.color.black_transparent, getContext()));

			if (!castingViewVisible) {
				startStreamWithQuality(settings.getPrefStreamQuality());
			}

			optionsMenuItem.setVisible(false);

			showVideoInterface();
		}
	}

	/**
	 * Setups the toolbar by giving it a bit of extra right padding (To make sure the icons are 16dp from right)
	 * Also adds the main toolbar as the support actionbar
	 */
	private void setupToolbar() {
		mToolbar.setPadding(0, 0, Service.dpToPixels(getActivity(), 5), 0);
		setHasOptionsMenu(true);
		mActivity.setSupportActionBar(mToolbar);
		mActivity.getSupportActionBar().setTitle(mChannelInfo.getDisplayName());
		mActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		//mActivity.getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);
		mToolbar.bringToFront();
	}

	/**
	 * Rotates the Play Pause wrapper with an Rotation Animation.
	 */
	private void rotatePlayPauseWrapper() {
		RotateAnimation rotate = new RotateAnimation(mPlayPauseWrapper.getRotation(), 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		rotate.setDuration(PLAY_PAUSE_ANIMATION_DURATION);
		rotate.setInterpolator(new AccelerateDecelerateInterpolator());
		mPlayPauseWrapper.startAnimation(rotate);
	}

	/**
	 * Checks if the device is below SDK API 19 (Kitkat)
	 * @return the result
	 */
	private boolean isDeviceBelowKitkat() {
		return Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT;
	}

	private void showPauseIcon() {
		if (mPauseIcon.getAlpha() == 0f) {
			rotatePlayPauseWrapper();
			mPauseIcon.animate().alpha(1f).start();
			mPlayIcon.animate().alpha(0f).start();
		}
	}

	private void showPlayIcon() {
		if (mPauseIcon.getAlpha() != 0f) {
			rotatePlayPauseWrapper();
			mPauseIcon.animate().alpha(0f).start();
			mPlayIcon.animate().alpha(1f).start();
		}
	}

	private void showQualities() {
		mQualityWrapper.setVisibility(View.VISIBLE);
	}

	private void hideQualities() {
		mQualityWrapper.setVisibility(View.GONE);
	}

	public static class CustomMediaRouteChooserDialogFragment extends MediaRouteChooserDialogFragment {
		@Override
		public MediaRouteChooserDialog onCreateChooserDialog(Context context, Bundle savedInstanceState) {
			return new MediaRouteChooserDialog(context);
		}
	}

	/**
	 * Broadcast class for detecting when the user plugs or unplug a headset.
	 */
	private class HeadsetPlugIntentReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
				int state = intent.getIntExtra("state", -1);
				switch (state) {
					case 0:
						if(mVideoView.isPlaying()) {
							Log.d(LOG_TAG, "Chat, pausing from headsetPlug");
							showVideoInterface();
							pauseStream();
						}
						break;
					case 1:
						showVideoInterface();
						break;
					default:

				}
			}
		}
	}
}
