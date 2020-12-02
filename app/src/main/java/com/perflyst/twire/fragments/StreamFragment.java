package com.perflyst.twire.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.media.session.MediaSessionCompat;
import android.transition.Transition;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.media.session.MediaButtonReceiver;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.Fade;
import androidx.transition.TransitionManager;

import com.afollestad.materialdialogs.DialogAction;
import com.balysv.materialripple.MaterialRippleLayout;
import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackPreparer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.perflyst.twire.R;
import com.perflyst.twire.activities.ChannelActivity;
import com.perflyst.twire.activities.stream.StreamActivity;
import com.perflyst.twire.adapters.PanelAdapter;
import com.perflyst.twire.chat.ChatManager;
import com.perflyst.twire.misc.FollowHandler;
import com.perflyst.twire.misc.ResizeHeightAnimation;
import com.perflyst.twire.misc.ResizeWidthAnimation;
import com.perflyst.twire.model.ChannelInfo;
import com.perflyst.twire.model.Quality;
import com.perflyst.twire.model.SleepTimer;
import com.perflyst.twire.service.DialogService;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.service.Settings;
import com.perflyst.twire.tasks.GetLiveStreamURL;
import com.perflyst.twire.tasks.GetPanelsTask;
import com.perflyst.twire.tasks.GetStreamChattersTask;
import com.perflyst.twire.tasks.GetStreamViewersTask;
import com.perflyst.twire.tasks.GetVODStreamURL;
import com.rey.material.widget.ProgressView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import biz.kasual.materialnumberpicker.MaterialNumberPicker;

public class StreamFragment extends Fragment implements Player.EventListener, PlaybackPreparer {
    private final int HIDE_ANIMATION_DELAY = 3000;
    private final int SNACKBAR_SHOW_DURATION = 4000;

    private final String LOG_TAG = getClass().getSimpleName();
    private final Handler delayAnimationHandler = new Handler(),
            progressHandler = new Handler(),
            fetchViewCountHandler = new Handler(),
            fetchChattersHandler = new Handler();
    public StreamFragmentListener streamFragmentCallback;
    public boolean chatOnlyViewVisible = false;
    public boolean isFullscreen = false;
    private boolean castingViewVisible = false,
            audioViewVisible = false,
            autoPlay = true,
            hasPaused = false,
            seeking = false;
    private ChannelInfo mChannelInfo;
    private String vodId;
    private HeadsetPlugIntentReceiver headsetIntentReceiver;
    private Settings settings;
    private SleepTimer sleepTimer;
    private LinkedHashMap<String, Quality> qualityURLs;
    private boolean isLandscape = false, previewInbackGround = false;
    private Runnable fetchViewCountRunnable;
    private PlayerView mVideoView;
    private ExoPlayer player;
    private MediaSource currentMediaSource;
    private Toolbar mToolbar;
    private ConstraintLayout mVideoInterface;
    private RelativeLayout mControlToolbar;
    private ConstraintLayout mVideoWrapper;
    private ConstraintLayout mPlayPauseWrapper;
    private ImageView mPauseIcon,
            mPlayIcon,
            mQualityButton,
            mFullScreenButton,
            mPreview,
            mShowChatButton,
            mForward,
            mBackward;
    private SeekBar mProgressBar;
    private TextView mCurrentProgressView, castingTextView, mCurrentViewersView;
    private HashMap<String, TextView> QualityOptions = new HashMap<>();
    private AppCompatActivity mActivity;
    private Snackbar snackbar;
    private ProgressView mBufferingView;
    private BottomSheetDialog mQualityBottomSheet, mProfileBottomSheet;
    private CheckedTextView mAudioOnlySelector, mChatOnlySelector;
    private ViewGroup rootView;
    private MenuItem optionsMenuItem;
    private LinearLayout mQualityWrapper;
    private View mClickIntercepter;
    private final Runnable hideAnimationRunnable = () -> {
        if (getActivity() != null)
            hideVideoInterface();
    };
    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (player == null)
                return;

            if (player.isPlaying()) {
                if (currentProgress != player.getCurrentPosition())
                    mProgressBar.setProgress((int) player.getCurrentPosition());

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
    private int originalCtrlToolbarPadding,
            originalMainToolbarPadding,
            vodLength = 0,
            currentProgress = 0,
            videoHeightBeforeChatOnly,
            fetchViewCountDelay = 1000 * 60, // A minute
            fetchChattersDelay = 1000 * 60; // 30 seco... Nah just kidding. Also a minute.
    private Integer triesForNextBest = 0;

    private static int totalVerticalInset;
    private boolean pictureInPictureEnabled; // Tracks if PIP is enabled including the animation.
    private static boolean pipDisabling; // Tracks the PIP disabling animation.
    private MediaSessionCompat mediaSession;

    public static StreamFragment newInstance(Bundle args) {
        StreamFragment fragment = new StreamFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Gets a Rect representing the usable area of the screen
     *
     * @return A Rect representing the usable area of the screen
     */
    public static Rect getScreenRect(Activity activity) {
        if (activity != null) {
            Display display = activity.getWindowManager().getDefaultDisplay();
            DisplayMetrics metrics = new DisplayMetrics();
            Point size = new Point();
            int width, height;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity.isInMultiWindowMode() && !activity.isInPictureInPictureMode() && !pipDisabling) {
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

            return new Rect(0, 0, Math.min(width, height), Math.max(width, height) - totalVerticalInset);
        }

        return new Rect();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

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
        if (getActivity().getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            isLandscape = true;
        }

        //  If no streamer info is available we cant show the stream.
        if (mChannelInfo == null) {
            if (getActivity() != null) {
                getActivity().finish();
            }
            return rootView;
        }

        rootView = (ViewGroup) mRootView;
        mVideoInterface = mRootView.findViewById(R.id.video_interface);
        mToolbar = mRootView.findViewById(R.id.main_toolbar);
        mControlToolbar = mRootView.findViewById(R.id.control_toolbar_wrapper);
        mVideoWrapper = mRootView.findViewById(R.id.video_wrapper);
        mVideoView = mRootView.findViewById(R.id.VideoView);
        mPlayPauseWrapper = mRootView.findViewById(R.id.play_pause_wrapper);
        mPlayIcon = mRootView.findViewById(R.id.ic_play);
        mPauseIcon = mRootView.findViewById(R.id.ic_pause);
        mPreview = mRootView.findViewById(R.id.preview);
        mQualityButton = mRootView.findViewById(R.id.settings_icon);
        mFullScreenButton = mRootView.findViewById(R.id.fullscreen_icon);
        mShowChatButton = mRootView.findViewById(R.id.show_chat_button);
        mForward = mRootView.findViewById(R.id.forward);
        mBackward = mRootView.findViewById(R.id.backward);
        mCurrentProgressView = mRootView.findViewById(R.id.currentProgess);
        castingTextView = mRootView.findViewById(R.id.chromecast_text);
        mProgressBar = mRootView.findViewById(R.id.progressBar);
        mBufferingView = mRootView.findViewById(R.id.circle_progress);
        mCurrentViewersView = mRootView.findViewById(R.id.txtViewViewers);
        mActivity = ((AppCompatActivity) getActivity());
        mClickIntercepter = mRootView.findViewById(R.id.click_intercepter);
        View mCurrentViewersWrapper = mRootView.findViewById(R.id.viewers_wrapper);

        setupToolbar();
        setupSpinner();
        setupProfileBottomSheet();
        setupLandscapeChat();
        setupShowChatButton();

        if (savedInstanceState == null)
            setPreviewAndCheckForSharedTransition();

        mFullScreenButton.setOnClickListener(v -> toggleFullscreen());
        mPlayPauseWrapper.setOnClickListener(v -> {
            if (mPlayPauseWrapper.getAlpha() < 0.5f) {
                return;
            }

            try {
                if (player.isPlaying()) {
                    pauseStream();
                } else if (!player.isPlaying()) {
                    resumeStream();
                }
            } catch (Exception e) {
                e.printStackTrace();
                startStreamWithQuality(settings.getPrefStreamQuality());
            }
        });

        mVideoWrapper.setOnClickListener(v -> {
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

                if (player.isPlaying()) {
                    delayHiding();
                }

                Handler h = new Handler();
                h.postDelayed(this::setAndroidUiMode, HIDE_ANIMATION_DELAY);
            }
        });

        initializePlayer();

        mRootView.setOnSystemUiVisibilityChangeListener(
                visibility -> {
                    if (visibility == 0) {
                        showVideoInterface();
                        delayHiding();
                        Handler h = new Handler();
                        h.postDelayed(this::setAndroidUiMode, HIDE_ANIMATION_DELAY);
                    }
                }
        );


        int seekButtonVisibility = vodId == null ? View.INVISIBLE : View.VISIBLE;
        mForward.setVisibility(seekButtonVisibility);
        mBackward.setVisibility(seekButtonVisibility);

        if (vodId == null) {
            View mTimeController = mRootView.findViewById(R.id.time_controller);
            mTimeController.setVisibility(View.INVISIBLE);

            if (args != null && args.containsKey(getString(R.string.stream_fragment_viewers)) && settings.getStreamPlayerShowViewerCount()) {
                mCurrentViewersView.setText("" + args.getInt(getString(R.string.stream_fragment_viewers)));
                startFetchingViewers();
            } else {
                mCurrentViewersWrapper.setVisibility(View.GONE);
            }
        } else {
            mCurrentViewersWrapper.setVisibility(View.GONE);

            mForward.setOnClickListener(v -> {
                seeking = true;
                mProgressBar.setProgress(currentProgress + 10000);
                seeking = false;
                ChatManager.updateVodProgress(currentProgress, false);
            });

            mBackward.setOnClickListener(v -> {
                seeking = true;
                mProgressBar.setProgress(currentProgress - 10000);
                seeking = false;
                streamFragmentCallback.onSeek();
                ChatManager.updateVodProgress(currentProgress, true);
            });

            mCurrentProgressView.setOnClickListener(v -> showSeekDialog());

            ChatManager.updateVodProgress(ChatManager.VOD_LOADING, true);

            TextView maxProgress = mRootView.findViewById(R.id.maxProgress);
            maxProgress.setText(Service.calculateTwitchVideoLength(vodLength));

            mProgressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (progress == vodLength) {
                        pauseStream();
                    }

                    if (vodId != null && !seeking && !fromUser) {
                        ChatManager.updateVodProgress(progress, false);
                    }

                    if ((fromUser || seeking) && !audioViewVisible) {
                        player.seekTo(progress);
                        showVideoInterface();

                        if (progress > 0) {
                            settings.setVodProgress(vodId, progress / 1000);
                        }
                    }
                    currentProgress = progress;
                    mCurrentProgressView.setText(Service.calculateTwitchVideoLength(currentProgress / 1000));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    seeking = true;
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    seeking = false;
                    delayHiding();

                    if (vodId != null) {
                        ChatManager.updateVodProgress(currentProgress, true);
                        streamFragmentCallback.onSeek();
                    }
                }
            });
            seeking = true;
            mProgressBar.setMax(vodLength * 1000);
            seeking = false;

            checkVodProgress();
        }

        keepScreenOn();

        if (autoPlay || vodId != null) {
            startStreamWithQuality(settings.getPrefStreamQuality());
        }

        headsetIntentReceiver = new HeadsetPlugIntentReceiver();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getActivity().registerReceiver(headsetIntentReceiver, new IntentFilter(AudioManager.ACTION_HEADSET_PLUG));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            mRootView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                    DisplayCutout displayCutout = getDisplayCutout();
                    if (displayCutout != null) {
                        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            totalVerticalInset = displayCutout.getSafeInsetLeft() + displayCutout.getSafeInsetRight();
                        } else {
                            totalVerticalInset = displayCutout.getSafeInsetTop() + displayCutout.getSafeInsetBottom();
                        }

                        setVideoViewLayout();
                        setupLandscapeChat();
                        streamFragmentCallback.refreshLayout();
                    }
                }

                @Override
                public void onViewDetachedFromWindow(View v) {
                }
            });
        }

        return mRootView;
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private DisplayCutout getDisplayCutout() {
        Activity activity = getActivity();
        if (activity != null) {
            WindowInsets windowInsets = activity.getWindow().getDecorView().getRootWindowInsets();
            if (windowInsets != null) {
                return windowInsets.getDisplayCutout();
            }
        }

        return null;
    }

    private void initializePlayer() {
        if (player == null) {
            player = new SimpleExoPlayer.Builder(getContext()).build();
            player.addListener(this);
            mVideoView.setPlayer(player);
            mVideoView.setPlaybackPreparer(this);

            if (currentMediaSource != null) {
                player.setMediaSource(currentMediaSource);
                player.prepare();
            }

            ComponentName mediaButtonReceiver = new ComponentName(
                    getContext(), MediaButtonReceiver.class);
            mediaSession = new MediaSessionCompat(
                    getContext(),
                    getContext().getPackageName(),
                    mediaButtonReceiver,
                    null);
            MediaSessionConnector mediaSessionConnector = new MediaSessionConnector(mediaSession);
            mediaSessionConnector.setPlayer(player);
            mediaSession.setActive(true);

            progressHandler.postDelayed(progressRunnable, 1000);
        }
    }

    private void releasePlayer() {
        if (player != null) {
            mediaSession.release();

            player.release();
            player = null;
        }
    }

    @Override
    public void preparePlayback() {
        player.prepare();
    }

    @Override
    public void onPlaybackStateChanged(@Player.State int playbackState) {
        if (playbackState == Player.STATE_READY) {
            mBufferingView.stop();
            hideVideoInterface();
            delayHiding();

            Log.d(LOG_TAG, "Render Start");
            if (!previewInbackGround) {
                hidePreview();
            }
        } else if (playbackState == Player.STATE_BUFFERING) {
            mBufferingView.start();
            delayAnimationHandler.removeCallbacks(hideAnimationRunnable);
            showVideoInterface();

            Log.d(LOG_TAG, "Render stop. Buffering start");
        }
    }

    @Override
    public void onPlayerError(ExoPlaybackException exception) {
        Log.e(LOG_TAG, "Something went wrong playing the stream for " + mChannelInfo.getDisplayName() + " - Exception: " + exception);

        playbackFailed();
    }

    /**
     * Hides the preview image and updates the state
     */
    private void hidePreview() {
        mPreview.setVisibility(View.INVISIBLE);
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
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            isLandscape = false;
        }

        checkShowChatButtonVisibility();
        updateUI();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) {
            initializePlayer();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // If the app was closed in the background we need to seek to currentProgress when resuming.
        // Android also triggers onResume when coming out of PIP but we don't need to do it then.
        if (!pipDisabling)
            player.seekTo(currentProgress);

        pipDisabling = false;

        if (Util.SDK_INT <= 23 || player == null) {
            initializePlayer();
        }

        originalMainToolbarPadding = mToolbar.getPaddingRight();
        originalCtrlToolbarPadding = mControlToolbar.getPaddingRight();

        if (audioViewVisible && !isAudioOnlyModeEnabled()) {
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
        if (pictureInPictureEnabled)
            return;

        hasPaused = true;

        if (mQualityBottomSheet != null)
            mQualityBottomSheet.dismiss();

        if (mProfileBottomSheet != null)
            mProfileBottomSheet.dismiss();

        if (Util.SDK_INT <= 23) {
            releasePlayer();
        }

        ChatManager.setPreviousProgress();
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
            settings.setVodProgress(vodId, currentProgress / 1000);
            settings.setVodLength(vodId, vodLength);
            Log.d(LOG_TAG, "Saving Current progress: " + currentProgress);
        }

        if (Util.SDK_INT > 23) {
            releasePlayer();
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
        Runnable fetchChattersRunnable = new Runnable() {
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
        mShowChatButton.setOnClickListener(view -> {
            if (!isVideoInterfaceShowing()) {
                showVideoInterface();
                delayHiding();
            }

            if (view.getRotation() == 0f) {
                showLandscapeChat();
            } else {
                hideLandscapeChat();
            }
        });
    }

    /**
     * Sets the correct visibility of the show chat button.
     * If the screen is in landscape it is show, else it is shown
     */
    private void checkShowChatButtonVisibility() {
        if (isLandscape && settings.isChatInLandscapeEnabled() && !pictureInPictureEnabled) {
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
                    showSnackbar(message);
                }

                @Override
                public void onStop(String message) {
                    showSnackbar(message);
                }
            }, getContext());
        }

        sleepTimer.show(getActivity());
    }

    private void showSeekDialog() {
        DialogService.getSeekDialog(getActivity(), (dialog, which) -> {
                    if (which == DialogAction.NEGATIVE)
                        return;

                    View customView = dialog.getCustomView();
                    MaterialNumberPicker hourPicker = customView.findViewById(R.id.hour_picker);
                    MaterialNumberPicker minutePicker = customView.findViewById(R.id.minute_picker);
                    MaterialNumberPicker secondPicker = customView.findViewById(R.id.second_picker);

                    seeking = true;
                    mProgressBar.setProgress((hourPicker.getValue() * 3600 + minutePicker.getValue() * 60 + secondPicker.getValue()) * 1000);
                    seeking = false;
                    streamFragmentCallback.onSeek();
                    ChatManager.updateVodProgress(currentProgress, true);
                },
                currentProgress / 1000,
                vodLength)
                .show();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        optionsMenuItem = menu.findItem(R.id.menu_item_options);
        optionsMenuItem.setVisible(false);
        optionsMenuItem.setOnMenuItemClickListener(menuItem -> {
            if (mQualityButton != null) {
                mQualityButton.performClick();
            }
            return true;
        });
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && settings.isChatLandscapeSwipable() && settings.isChatInLandscapeEnabled()) {
            final int width = getScreenRect(getActivity()).height();

            View.OnTouchListener touchListener = new View.OnTouchListener() {
                private int downPosition = width;
                private int widthOnDown = width;

                public boolean onTouch(View view, MotionEvent event) {
                    if (isLandscape) {
                        final int X = (int) event.getRawX();
                        switch (event.getAction() & MotionEvent.ACTION_MASK) {
                            case MotionEvent.ACTION_DOWN:
                                ConstraintLayout.LayoutParams lParams = (ConstraintLayout.LayoutParams) mVideoWrapper.getLayoutParams();
                                if (lParams.width > 0)
                                    widthOnDown = lParams.width;

                                downPosition = (int) event.getRawX();
                                break;
                            case MotionEvent.ACTION_UP:
                                int upPosition = (int) event.getRawX();
                                int deltaPosition = upPosition - downPosition;

                                if (deltaPosition < 20 && deltaPosition > -20) {
                                    return false;
                                }

                                if (upPosition < downPosition) {
                                    showLandscapeChat();
                                } else {
                                    hideLandscapeChat();
                                }

                                break;
                            case MotionEvent.ACTION_MOVE:
                                ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) mVideoWrapper.getLayoutParams();
                                int newWidth;

                                if (X > downPosition) { // Swiping right
                                    newWidth = widthOnDown + (X - downPosition);
                                } else { // Swiping left
                                    newWidth = widthOnDown - (downPosition - X);
                                }

                                layoutParams.width = Math.max(Math.min(newWidth, width), width - getLandscapeChatTargetWidth());

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
        int width = getScreenRect(getActivity()).height();
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
        int width = getScreenRect(getActivity()).height();
        ResizeWidthAnimation resizeWidthAnimation = new ResizeWidthAnimation(mVideoWrapper, width);
        resizeWidthAnimation.setDuration(250);
        mVideoWrapper.startAnimation(resizeWidthAnimation);
        mShowChatButton.animate().rotation(0f).start();
    }

    private int getLandscapeChatTargetWidth() {
        return (int) (getScreenRect(getActivity()).height() * (settings.getChatLandscapeWidth() / 100.0));
    }

    private void initCastingView() {
        castingViewVisible = true;
        //auto.setVisibility(View.GONE); // Auto does not work on chromecast
        mVideoView.setVisibility(View.INVISIBLE);
        mBufferingView.setVisibility(View.GONE);
        previewInbackGround = false;
        castingTextView.setVisibility(View.VISIBLE);
        //castingTextView.setText(getString(R.string.stream_chromecast_connecting));
        showVideoInterface();
    }

    private void disableCastingView() {
        castingViewVisible = false;
        //auto.setVisibility(View.VISIBLE);
        mVideoView.setVisibility(View.VISIBLE);
        Service.bringToBack(mPreview);
        mBufferingView.setVisibility(View.VISIBLE);
        previewInbackGround = true;
        castingTextView.setVisibility(View.INVISIBLE);
        showVideoInterface();
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

            Glide.with(getContext())
                    .asBitmap()
                    .load(imageUrl)
                    .signature(new ObjectKey(System.currentTimeMillis() / TimeUnit.MINUTES.toMillis(5))) // Refresh preview images every 5 minutes
                    .into(mPreview);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && intent.getBooleanExtra(getString(R.string.stream_shared_transition), false)) {
            mPreview.setTransitionName(getString(R.string.stream_preview_transition));

            final View[] viewsToHide = {mVideoView, mToolbar, mControlToolbar};
            for (View view : viewsToHide) {
                view.setVisibility(View.INVISIBLE);
            }

            getActivity().getWindow().getEnterTransition().addListener(new Transition.TransitionListener() {

                @Override
                public void onTransitionEnd(Transition transition) {
                    TransitionManager.beginDelayedTransition(
                            mVideoWrapper,
                            new Fade()
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

                public void onTransitionStart(Transition transition) {
                }

                public void onTransitionPause(Transition transition) {
                }

                public void onTransitionResume(Transition transition) {
                }
            });
        }

    }

    /**
     * Checks if the user is currently in progress of watching a VOD. If so seek forward to where the user left off.
     */
    private void checkVodProgress() {
        if (vodId != null) {
            if (currentProgress == 0) {
                currentProgress = settings.getVodProgress(vodId) * 1000;
                ChatManager.updateVodProgress(currentProgress, true);
                player.seekTo(currentProgress);
                Log.d(LOG_TAG, "Current progress: " + currentProgress);
            } else {
                ChatManager.updateVodProgress(currentProgress, false);
                player.seekTo(currentProgress);
                Log.d(LOG_TAG, "Seeking to " + currentProgress);
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
     *
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
        if (isLandscape || isFullscreen) {
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
            decorView.setSystemUiVisibility(0); // Remove all flags.
        }
    }

    private void setVideoViewLayout() {
        ViewGroup.LayoutParams layoutParams = rootView.getLayoutParams();
        layoutParams.height = isLandscape ? ViewGroup.LayoutParams.MATCH_PARENT : ViewGroup.LayoutParams.WRAP_CONTENT;

        ConstraintLayout.LayoutParams layoutWrapper = (ConstraintLayout.LayoutParams) mVideoWrapper.getLayoutParams();
        if (isLandscape && !pictureInPictureEnabled) {
            layoutWrapper.width = mShowChatButton.getRotation() == 0 ? ConstraintLayout.LayoutParams.MATCH_PARENT : getScreenRect(getActivity()).height() - getLandscapeChatTargetWidth();
        } else {
            layoutWrapper.width = ConstraintLayout.LayoutParams.MATCH_PARENT;
        }
        mVideoWrapper.setLayoutParams(layoutWrapper);

        AspectRatioFrameLayout contentFrame = mVideoWrapper.findViewById(R.id.exo_content_frame);
        if (isLandscape) {
            contentFrame.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        } else {
            contentFrame.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH);
        }
    }

    /**
     * Delays the hiding for the Video control interface.
     */
    private void delayHiding() {
        delayAnimationHandler.postDelayed(hideAnimationRunnable, HIDE_ANIMATION_DELAY);
    }

    /**
     * Checks if the video interface is fully showing
     *
     * @return
     */
    public boolean isVideoInterfaceShowing() {
        return mVideoInterface.getAlpha() == 1f;
    }

    /**
     * Hides the video control interface with animations
     */
    private void hideVideoInterface() {
        if (mToolbar != null && !audioViewVisible && !chatOnlyViewVisible) {
            mVideoInterface.animate().alpha(0f).setInterpolator(new AccelerateDecelerateInterpolator()).start();
            changeVideoControlClickablity(false);
        }
    }

    /**
     * Shows the video control interface with animations
     */
    private void showVideoInterface() {
        int MaintoolbarY = 0, CtrlToolbarY = 0;
        if ((isFullscreen || isLandscape) && isDeviceBelowKitkat()) {
            MaintoolbarY = getStatusBarHeight();
        }
        if ((isFullscreen || isLandscape) && Service.isTablet(getContext()) && isDeviceBelowKitkat()) {
            CtrlToolbarY = getNavigationBarHeight();
        }

        mControlToolbar.setTranslationY(-CtrlToolbarY);
        mToolbar.setTranslationY(MaintoolbarY);
        mVideoInterface.animate().alpha(1f).setInterpolator(new AccelerateDecelerateInterpolator()).start();
        changeVideoControlClickablity(true);
    }

    private void changeVideoControlClickablity(boolean clickable) {
        mClickIntercepter.setVisibility(clickable ? View.GONE : View.VISIBLE);
        mClickIntercepter.setOnClickListener(view -> mVideoWrapper.performClick());
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

            if ((isFullscreen || isLandscape) && !Service.isTablet(getContext())) {
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
        if (isFullscreen) {
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
            mFullScreenButton.setImageResource(R.drawable.ic_fullscreen_exit);
        } else {
            mFullScreenButton.setImageResource(R.drawable.ic_fullscreen);
        }
    }

    /**
     * Pauses and stops the playback of the Video Stream
     */
    private void pauseStream() {
        Log.d(LOG_TAG, "Chat, pausing stream");
        showPlayIcon();

        delayAnimationHandler.removeCallbacks(hideAnimationRunnable);

        if (isAudioOnlyModeEnabled()) {
            Log.d(LOG_TAG, "Pausing audio");
        } else if (player != null) {
            player.setPlayWhenReady(false);
        }
        releaseScreenOn();
    }

    /**
     * Goes forward to live and starts playback of the VideoView
     */
    private void resumeStream() {
        showPauseIcon();

        if (isAudioOnlyModeEnabled()) {
        } else {
            if (vodId == null) {
                player.seekToDefaultPosition(); // Go forward to live
            }

            player.setPlayWhenReady(true);
        }

        keepScreenOn();
    }

    /**
     * Tries playing stream with a quality.
     * If the given quality doesn't exist for the stream the try the next best quality option.
     * If no Quality URLS have yet been created then try to start stream with an aync task.
     *
     * @param quality
     */
    private void startStreamWithQuality(String quality) {
        if (qualityURLs == null) {
            startStreamWithTask();
        } else {
            if (qualityURLs.containsKey(quality)) {
                if (chatOnlyViewVisible || audioViewVisible) {
                    return;
                }

                playUrl(qualityURLs.get(quality).URL);
                showQualities();
                updateSelectedQuality(quality);
                showPauseIcon();
                Log.d(LOG_TAG, "Starting Stream With a quality on " + quality + " for " + mChannelInfo.getDisplayName());
                Log.d(LOG_TAG, "URLS: " + qualityURLs.keySet().toString());
            } else if (!qualityURLs.isEmpty()) {
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
        GetLiveStreamURL.AsyncResponse callback = url -> {
            try {
                if (!url.isEmpty()) {
                    updateQualitySelections(url);
                    qualityURLs = url;

                    if (!checkForAudioOnlyMode()) {
                        startStreamWithQuality(new Settings(getContext()).getPrefStreamQuality());
                    }
                } else {
                    playbackFailed();
                }
            } catch (IllegalStateException | NullPointerException e) {
                e.printStackTrace();
            }
        };

        if (vodId == null) {
            GetLiveStreamURL task = new GetLiveStreamURL(callback);
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mChannelInfo.getStreamerName());
        } else {
            GetLiveStreamURL task = new GetVODStreamURL(callback);
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, vodId.substring(1));
        }
    }

    /**
     * Connects to the Twitch API to fetch the live stream url and quality selection urls.
     * If the task is successful the quality selector views' click listeners will be updated.
     */
    private void updateQualitySelectorsWithTask() {
        GetLiveStreamURL.AsyncResponse delegate = url -> {
            try {
                if (!url.isEmpty()) {
                    updateQualitySelections(url);
                    qualityURLs = url;
                }
            } catch (IllegalStateException | NullPointerException e) {
                e.printStackTrace();
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
            showSnackbar(getString(R.string.stream_playback_failed), "Retry", v -> startStreamWithTask());
        } else {
            showSnackbar(getString(R.string.vod_playback_failed), "Retry", v -> startStreamWithTask());
        }
    }

    private void showSnackbar(String message) {
        showSnackbar(message, null, null);
    }

    private void showSnackbar(String message, String actionText, View.OnClickListener action) {
        if (getActivity() != null && !isDetached()) {
            View mainView = ((StreamActivity) getActivity()).getMainContentLayout();

            if ((snackbar == null || !snackbar.isShown()) && mainView != null) {
                snackbar = Snackbar.make(mainView, message, 4000);
                if (actionText != null)
                    snackbar.setAction(actionText, action);
                snackbar.show();
            }
        }

    }

    private void tryNextBestQuality(String quality) {
        if (triesForNextBest < qualityURLs.size() - 1) { // Subtract 1 as we don't count AUDIO ONLY as a quality
            triesForNextBest++;
            List<String> qualityList = new ArrayList<>(qualityURLs.keySet());
            int next = qualityList.indexOf(quality) + 1;
            if (next >= qualityList.size() - 1) {
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
     *
     * @param url
     */
    private void playUrl(String url) {
        DefaultHttpDataSourceFactory dataSourceFactory = new DefaultHttpDataSourceFactory(getString(R.string.app_name));

        HttpDataSource.RequestProperties properties = dataSourceFactory.getDefaultRequestProperties();
        properties.set("Referer", "https://player.twitch.tv");
        properties.set("Origin", "https://player.twitch.tv");

        MediaSource mediaSource = new HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(
                        new MediaItem.Builder()
                                .setUri(Uri.parse(url))
                                .build());
        currentMediaSource = mediaSource;
        player.setMediaSource(mediaSource);
        player.prepare();

        checkVodProgress();
        resumeStream();
    }

    private void playWithExternalPlayer() {
        Toast errorToast = Toast.makeText(getContext(), R.string.error_external_playback_failed, Toast.LENGTH_LONG);
        if (qualityURLs == null) {
            errorToast.show();
            return;
        }

        String castQuality = GetLiveStreamURL.QUALITY_AUTO;
        updateSelectedQuality(castQuality);
        String url = qualityURLs.get(castQuality).URL;

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(url), "video/*");
        startActivity(Intent.createChooser(intent, getString(R.string.stream_external_play_using)));
    }

    /**
     * Sets up audio mode and starts playback of audio, while pausing any playing video
     */
    private void playAudioOnly() {
    }

    private void registerAudioOnlyDelegate() {
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
        } else {
            resetQualityViewBackground(QualityOptions.get(quality));
        }
    }

    /**
     * Resets the background color of all the select quality views in the bottom dialog
     */
    private void resetQualityViewBackground(TextView selected) {
        for (TextView v : QualityOptions.values()) {
            if (v.equals(selected)) {
                v.setBackgroundColor(Service.getColorAttribute(R.attr.navigationDrawerHighlighted, R.color.grey_300, getContext()));
            } else {
                v.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.transparent));
            }
        }
    }

    /**
     * Adds the available qualities for a stream to the spinner menu
     *
     * @param availableQualities
     */
    private void updateQualitySelections(LinkedHashMap<String, Quality> availableQualities) {
        for (TextView view : QualityOptions.values()) {
            mQualityWrapper.removeView((MaterialRippleLayout) view.getParent());
        }

        for (Map.Entry<String, Quality> entry : availableQualities.entrySet()) {
            Quality quality = entry.getValue();
            String qualityKey = entry.getKey();
            if (qualityKey.equals("audio_only"))
                continue;

            MaterialRippleLayout layout = (MaterialRippleLayout) LayoutInflater.from(getContext()).inflate(R.layout.quality_item, null);
            TextView textView = ((TextView) layout.getChildAt(0));
            textView.setText(quality.Name);

            setQualityOnClick(textView, qualityKey);
            QualityOptions.put(qualityKey, textView);
            mQualityWrapper.addView(layout);
        }
    }

    /**
     * Sets an OnClickListener on a select quality view (From bottom dialog).
     * The Listener starts the stream with a new quality setting and updates the background for the select quality views in the bottom dialog
     *
     * @param qualityView
     */
    private void setQualityOnClick(final TextView qualityView, String quality) {
        qualityView.setOnClickListener(v -> {
            settings.setPrefStreamQuality(quality);
            startStreamWithQuality(quality);
            resetQualityViewBackground(qualityView);
            mQualityBottomSheet.dismiss();
        });
    }

    private BottomSheetBehavior getDefaultBottomSheetBehaviour(View bottomSheetView) {
        BottomSheetBehavior behavior = BottomSheetBehavior.from((View) bottomSheetView.getParent());
        behavior.setPeekHeight(getContext().getResources().getDisplayMetrics().heightPixels / 3);
        return behavior;
    }

    private void setupProfileBottomSheet() {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.stream_profile_preview, null);
        mProfileBottomSheet = new BottomSheetDialog(getContext());
        mProfileBottomSheet.setContentView(v);
        final BottomSheetBehavior behavior = getDefaultBottomSheetBehaviour(v);

        mProfileBottomSheet.setOnDismissListener(dialogInterface -> behavior.setState(BottomSheetBehavior.STATE_COLLAPSED));

        TextView mNameView = mProfileBottomSheet.findViewById(R.id.twitch_name);
        TextView mFollowers = mProfileBottomSheet.findViewById(R.id.txt_followers);
        TextView mViewers = mProfileBottomSheet.findViewById(R.id.txt_viewers);
        ImageView mFollowButton = mProfileBottomSheet.findViewById(R.id.follow_unfollow_icon);
        ImageView mFullProfileButton = mProfileBottomSheet.findViewById(R.id.full_profile_icon);
        RecyclerView mPanelsRecyclerView = mProfileBottomSheet.findViewById(R.id.panel_recyclerview);

        mNameView.setText(mChannelInfo.getDisplayName());
        mFollowers.setText(mChannelInfo.getFollowers() + "");
        mViewers.setText(mChannelInfo.getViews() + "");

        mFullProfileButton.setOnClickListener(view -> {
            mProfileBottomSheet.dismiss();

            final Intent intent = new Intent(getContext(), ChannelActivity.class);
            intent.putExtra(getContext().getResources().getString(R.string.channel_info_intent_object), mChannelInfo);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            getContext().startActivity(intent);
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
                    public void streamerIsFollowed() {
                    }

                    @Override
                    public void streamerIsNotFollowed() {
                    }

                    @Override
                    public void userIsNotLoggedIn() {
                        imageView.setVisibility(View.GONE);
                    }

                    @Override
                    public void followSuccess() {
                    }

                    @Override
                    public void followFailure() {
                    }

                    @Override
                    public void unfollowSuccess() {
                    }

                    @Override
                    public void unfollowFailure() {
                    }
                }
        );
        updateFollowIcon(imageView, mFollowHandler.isStreamerFollowed());

        imageView.setOnClickListener(view -> {
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

            new Handler().postDelayed(() -> {
                updateFollowIcon(imageView, !isFollowed);
                imageView.animate().alpha(1f).setDuration(ANIMATION_DURATION).start();
            }, ANIMATION_DURATION);
        });
    }

    private void updateFollowIcon(ImageView imageView, boolean isFollowing) {
        @DrawableRes int imageRes = isFollowing
                ? R.drawable.ic_heart_broken
                : R.drawable.ic_heart;
        imageView.setImageResource(imageRes);
    }

    private void setupPanels(RecyclerView recyclerView) {
        final PanelAdapter mPanelAdapter = new PanelAdapter(getActivity());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        recyclerView.setAdapter(mPanelAdapter);

        GetPanelsTask mTask = new GetPanelsTask(mChannelInfo.getStreamerName(), mPanelAdapter::addPanels);
        mTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Setups the Quality Select spinner.
     * Automatically hides the text of the selected Quality
     */
    private void setupSpinner() {
        mQualityButton.setOnClickListener(v -> mQualityBottomSheet.show());

        View v = LayoutInflater.from(getContext()).inflate(R.layout.stream_settings, null);
        mQualityBottomSheet = new BottomSheetDialog(getContext());
        mQualityBottomSheet.setContentView(v);

        final BottomSheetBehavior behavior = getDefaultBottomSheetBehaviour(v);

        mQualityBottomSheet.setOnDismissListener(dialogInterface -> behavior.setState(BottomSheetBehavior.STATE_COLLAPSED));

        mQualityWrapper = mQualityBottomSheet.findViewById(R.id.quality_wrapper);
        mAudioOnlySelector = mQualityBottomSheet.findViewById(R.id.audio_only_selector);
        mChatOnlySelector = mQualityBottomSheet.findViewById(R.id.chat_only_selector);
        TextView optionsTitle = mQualityBottomSheet.findViewById(R.id.options_text);

        if (optionsTitle != null) {
            optionsTitle.setVisibility(View.VISIBLE);
        }

        if (vodId == null) {
            mChatOnlySelector.setVisibility(View.VISIBLE);
        }

        // Audio Only is currently broken, so let's not show it
        mAudioOnlySelector.setVisibility(View.GONE);
        /*
        mAudioOnlySelector.setVisibility(View.VISIBLE);
        mAudioOnlySelector.setOnClickListener(view -> {
            mQualityBottomSheet.dismiss();
            audioOnlyClicked();
        });
        */

        mChatOnlySelector.setOnClickListener(view -> {
            mQualityBottomSheet.dismiss();
            chatOnlyClicked();
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
            mAudioOnlySelector.setChecked(true);
            playAudioOnly();
        }

        return isAudioOnly;
    }

    private boolean isAudioOnlyModeEnabled() {
        return false;
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

            releasePlayer();
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
                initializePlayer();
                startStreamWithQuality(settings.getPrefStreamQuality());
            }

            optionsMenuItem.setVisible(false);

            showVideoInterface();
        }
    }

    public void prePictureInPicture() {
        pictureInPictureEnabled = true;

        int width = getScreenRect(getActivity()).height();
        ResizeWidthAnimation resizeWidthAnimation = new ResizeWidthAnimation(mVideoWrapper, width);
        resizeWidthAnimation.setDuration(250);
        mVideoWrapper.startAnimation(resizeWidthAnimation);
    }

    @Override
    public void onPictureInPictureModeChanged(boolean enabled) {
        mVideoInterface.setVisibility(enabled ? View.INVISIBLE : View.VISIBLE);
        pictureInPictureEnabled = enabled;

        if (!enabled)
            pipDisabling = true;
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
        RotateAnimation rotate = new RotateAnimation(mPlayPauseWrapper.getRotation(),
                360, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        int PLAY_PAUSE_ANIMATION_DURATION = 500;
        rotate.setDuration(PLAY_PAUSE_ANIMATION_DURATION);
        rotate.setInterpolator(new AccelerateDecelerateInterpolator());
        mPlayPauseWrapper.startAnimation(rotate);
    }

    /**
     * Checks if the device is below SDK API 19 (Kitkat)
     *
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

    public interface StreamFragmentListener {
        void onSeek();
        void refreshLayout();
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
                        if (player.isPlaying()) {
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
