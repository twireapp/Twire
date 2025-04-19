package com.perflyst.twire.fragments;

import static android.provider.Settings.System.ACCELEROMETER_ROTATION;
import static android.provider.Settings.System.getInt;
import static androidx.media3.common.Player.EVENT_PLAYBACK_STATE_CHANGED;
import static androidx.media3.common.Player.EVENT_PLAY_WHEN_READY_CHANGED;
import static androidx.media3.common.Player.STATE_BUFFERING;
import static androidx.media3.common.Player.STATE_READY;
import static com.perflyst.twire.misc.Utils.appendSpan;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.PictureInPictureParams;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.media.MediaMetadataCompat;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.transition.Transition;
import android.util.DisplayMetrics;
import android.util.Rational;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.RoundedCorner;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.util.Util;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerControlView;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.Fade;
import androidx.transition.TransitionManager;

import com.balysv.materialripple.MaterialRippleLayout;
import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.perflyst.twire.PlaybackService;
import com.perflyst.twire.R;
import com.perflyst.twire.activities.ChannelActivity;
import com.perflyst.twire.activities.stream.StreamActivity;
import com.perflyst.twire.adapters.PanelAdapter;
import com.perflyst.twire.chat.ChatManager;
import com.perflyst.twire.misc.FollowHandler;
import com.perflyst.twire.misc.OnlineSince;
import com.perflyst.twire.misc.ResizeHeightAnimation;
import com.perflyst.twire.misc.ResizeWidthAnimation;
import com.perflyst.twire.misc.Utils;
import com.perflyst.twire.model.ChannelInfo;
import com.perflyst.twire.model.Quality;
import com.perflyst.twire.model.SleepTimer;
import com.perflyst.twire.model.UserInfo;
import com.perflyst.twire.service.DialogService;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.service.Settings;
import com.perflyst.twire.tasks.GetPanelsTask;
import com.perflyst.twire.tasks.GetStreamChattersTask;
import com.perflyst.twire.tasks.GetStreamURL;
import com.perflyst.twire.tasks.GetStreamViewersTask;
import com.perflyst.twire.utils.Constants;
import com.perflyst.twire.utils.Execute;
import com.rey.material.widget.ProgressView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import dev.chrisbanes.insetter.Insetter;
import timber.log.Timber;

@OptIn(markerClass = UnstableApi.class)
public class StreamFragment extends Fragment implements Player.Listener {
    private static final int SHOW_TIMEOUT = 3000;

    private static int rightInset = 0;
    private static boolean pipDisabling; // Tracks the PIP disabling animation.
    private final Handler fetchViewCountHandler = new Handler(),
            fetchChattersHandler = new Handler(),
            vodHandler = new Handler();
    private final HashMap<String, TextView> QualityOptions = new HashMap<>();
    private final int fetchViewCountDelay = 1000 * 60, // A minute
            fetchChattersDelay = 1000 * 60; // 30 seco... Nah just kidding. Also a minute.
    public StreamFragmentListener streamFragmentCallback;
    public boolean chatOnlyViewVisible = false;
    private boolean castingViewVisible = false,
            audioViewVisible = false,
            autoPlay = true,
            hasPaused = false,
            landscapeChatVisible = false;
    private UserInfo mUserInfo;
    private String vodId;
    private String title;
    private long startTime;
    private SleepTimer sleepTimer;
    private Map<String, Quality> qualityURLs;
    private boolean isLandscape = false;
    private Runnable fetchViewCountRunnable;
    private PlayerView mVideoView;
    private MediaController player;
    private ListenableFuture<MediaController> controllerFuture;
    private MediaItem currentMediaItem;
    private Toolbar mToolbar;
    private TextView mTitleText;
    private ConstraintLayout mVideoInterface;
    private RelativeLayout mControlToolbar;
    private ConstraintLayout mVideoWrapper;
    private ConstraintLayout mPlayPauseWrapper;
    private ImageView mPauseIcon,
            mPlayIcon,
            mQualityButton,
            mFullScreenButton,
            mPreview,
            mShowChatButton;
    private TextView castingTextView, mCurrentViewersView, mRuntime;
    private AppCompatActivity mActivity;
    private Snackbar snackbar;
    private ProgressView mBufferingView;
    private BottomSheetDialog mQualityBottomSheet, mProfileBottomSheet;
    private CheckedTextView mAudioOnlySelector, mMuteSelector, mChatOnlySelector;
    private ViewGroup rootView;
    private MenuItem optionsMenuItem;
    private LinearLayout mQualityWrapper;
    private View mOverlay;
    private PlayerControlView controlView;
    private OrientationEventListener orientationListener;

    private final Runnable vodRunnable = new Runnable() {
        @Override
        public void run() {
            if (player == null) return;

            ChatManager.instance.updateVodProgress(player.getCurrentPosition(), false);

            if (player.isPlaying()) vodHandler.postDelayed(this, 1000);
        }
    };

    private int originalCtrlToolbarPadding;
    private int originalMainToolbarPadding;
    private int videoHeightBeforeChatOnly;
    private Integer triesForNextBest = 0;
    private boolean pictureInPictureEnabled; // Tracks if PIP is enabled including the animation.

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
            int width, height;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity.isInMultiWindowMode() && !activity.isInPictureInPictureMode() && !pipDisabling) {
                display.getMetrics(metrics);
            } else {
                display.getRealMetrics(metrics);
            }

            width = metrics.widthPixels;
            height = metrics.heightPixels;

            return new Rect(0, 0, Math.min(width, height), Math.max(width, height));
        }

        return new Rect();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle args = getArguments();
        setHasOptionsMenu(true);

        if (args != null) {
            mUserInfo = args.getParcelable(getString(R.string.stream_fragment_streamerInfo));
            vodId = args.getString(getString(R.string.stream_fragment_vod_id));
            autoPlay = args.getBoolean(getString(R.string.stream_fragment_autoplay));
            title = args.getString(getString(R.string.stream_fragment_title));
        }

        final View mRootView = inflater.inflate(R.layout.fragment_stream, container, false);
        mRootView.requestLayout();

        // If the user has been in FULL SCREEN mode and presses the back button, we want to change the orientation to portrait.
        // As soon as the orientation has change we don't want to force the user to will be in portrait, so we "release" the request.
        if (requireActivity().getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            isLandscape = true;
        }

        //  If no streamer info is available we cant show the stream.
        if (mUserInfo == null) {
            if (getActivity() != null) {
                getActivity().finish();
            }
            return rootView;
        }

        rootView = (ViewGroup) mRootView;
        mVideoView = mRootView.findViewById(R.id.VideoView);
        mVideoInterface = mVideoView.findViewById(R.id.video_interface);
        mToolbar = mVideoView.findViewById(R.id.main_toolbar);
        mTitleText = mVideoView.findViewById(R.id.toolbar_title);
        mControlToolbar = mVideoView.findViewById(R.id.control_toolbar_wrapper);
        mVideoWrapper = mRootView.findViewById(R.id.video_wrapper);
        mPlayPauseWrapper = mVideoView.findViewById(R.id.play_pause_wrapper);
        mPlayIcon = mVideoView.findViewById(R.id.ic_play);
        mPauseIcon = mVideoView.findViewById(R.id.ic_pause);
        mPreview = mVideoView.findViewById(R.id.preview);
        mQualityButton = mVideoView.findViewById(R.id.settings_icon);
        mFullScreenButton = mVideoView.findViewById(R.id.fullscreen_icon);
        mShowChatButton = mVideoView.findViewById(R.id.show_chat_button);
        castingTextView = mVideoView.findViewById(R.id.chromecast_text);
        mBufferingView = mVideoView.findViewById(R.id.exo_buffering);
        mCurrentViewersView = mVideoView.findViewById(R.id.txtViewViewers);
        mRuntime = mVideoView.findViewById(R.id.txtViewRuntime);
        mActivity = (AppCompatActivity) getActivity();
        mOverlay = mVideoView.getOverlayFrameLayout();

        landscapeChatVisible = Settings.isChatInLandscapeEnabled();

        setupToolbar();
        setupSpinner();
        setupProfileBottomSheet();
        setupLandscapeChat();
        setupShowChatButton();

        if (savedInstanceState == null)
            setPreviewAndCheckForSharedTransition();

        mFullScreenButton.setOnClickListener(v -> toggleFullscreen());
        mPlayPauseWrapper.setOnClickListener(v -> player.setPlayWhenReady(!player.getPlayWhenReady()));

        initializePlayer();

        controlView = mVideoView.findViewById(androidx.media3.ui.R.id.exo_controller);

        controlView.setShowFastForwardButton(vodId != null);
        controlView.setShowRewindButton(vodId != null);

        controlView.setShowTimeoutMs(SHOW_TIMEOUT);
        controlView.setAnimationEnabled(false);

        mVideoView.setControllerVisibilityListener((PlayerView.ControllerVisibilityListener) visibility -> {
            if (visibility == View.VISIBLE) {
                showVideoInterface();
            } else {
                hideVideoInterface();
            }
        });

        // Use ExoPlayer's overlay frame to intercept click events and pass them along
        mOverlay.setOnClickListener(view -> mVideoView.performClick());

        if (vodId == null) {
            View mTimeController = mVideoView.findViewById(R.id.time_controller);
            mTimeController.setVisibility(View.INVISIBLE);

            if (!Settings.getStreamPlayerRuntime()) {
                mRuntime.setVisibility(View.GONE);
            } else {
                startTime = args.getLong(getString(R.string.stream_fragment_start_time));
                controlView.setProgressUpdateListener((position, bufferedPosition) -> mRuntime.setText(OnlineSince.getOnlineSince(startTime)));
            }


            if (args != null && args.containsKey(getString(R.string.stream_fragment_viewers)) && Settings.getStreamPlayerShowViewerCount()) {
                Utils.setNumber(mCurrentViewersView, args.getInt(getString(R.string.stream_fragment_viewers)));
                startFetchingViewers();
            } else {
                mCurrentViewersView.setVisibility(View.GONE);
            }
        } else {
            mCurrentViewersView.setVisibility(View.GONE);
            mRuntime.setVisibility(View.GONE);

            mVideoView.findViewById(R.id.exo_position).setOnClickListener(v -> showSeekDialog());
        }

        if (autoPlay || vodId != null) {
            startStreamWithQuality(Settings.getPrefStreamQuality());
        }

        // Enabled after the user toggles the fullscreen mode
        // Unlocks the orientation of the screen after the user rotates to the new orientation
        orientationListener = new OrientationEventListener(getActivity()) {
            @Override
            public void onOrientationChanged(int orientation) {
                int estimatedOrientation = close(orientation, 0, 180, 360) ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT :
                        close(orientation, 90, 270) ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE :
                                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
                int requestedOrientation = requireActivity().getRequestedOrientation();
                if (estimatedOrientation == requestedOrientation) {
                    requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                    orientationListener.disable();
                }
            }

            private boolean close(int current, int... targets) {
                for (int target : targets) {
                    if (Math.abs(current - target) < 15)
                        return true;
                }

                return false;
            }
        };

        // Apply the insets to the root view in portrait
        // But use the rounded corners for the controls in landscape
        Insetter.builder().setOnApplyInsetsListener((v, windowInsets, initialState) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
            boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

            if (isLandscape) {
                v.setPadding(0, 0, 0, 0);

                // set control padding
                var left = 0;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    left = calculatePadding(RoundedCorner.POSITION_TOP_LEFT);
                    rightInset = calculatePadding(RoundedCorner.POSITION_TOP_RIGHT);
                }
                mVideoInterface.setPadding(left, 0, rightInset, 0);
            } else {
                v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
                mVideoInterface.setPadding(0, 0, 0, 0);
            }
        }).applyToView(mRootView);

        return mRootView;
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    int calculatePadding(int position) {
        var rootInsets = rootView.getRootWindowInsets();
        var corner = rootInsets.getRoundedCorner(position);
        if (corner == null) return 0;
        var radius = corner.getRadius();
        return (int) (radius - radius / Math.sqrt(2) - getResources().getDimension(R.dimen.toolbar_icon_padding));
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("chatOnlyViewVisible", chatOnlyViewVisible);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState != null) {
            setChatOnlyView(savedInstanceState.getBoolean("chatOnlyViewVisible", false));
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private DisplayCutout getDisplayCutout() {
        Activity activity = getActivity();
        if (activity != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return activity.getWindowManager().getDefaultDisplay().getCutout();
            }

            WindowInsets windowInsets = activity.getWindow().getDecorView().getRootWindowInsets();
            if (windowInsets != null) {
                return windowInsets.getDisplayCutout();
            }
        }

        return null;
    }

    private void initializePlayer() {
        if (player == null) {
            SessionToken sessionToken = new SessionToken(getContext(), new ComponentName(getContext(), PlaybackService.class));
            controllerFuture = new MediaController.Builder(getContext(), sessionToken).buildAsync();
            StreamFragment streamFragment = this;
            Futures.addCallback(controllerFuture, new FutureCallback<>() {
                @Override
                public void onSuccess(MediaController result) {
                    player = result;
                    player.addListener(streamFragment);
                    mVideoView.setPlayer(player);

                    if (vodId != null) {
                        player.setPlaybackSpeed(Settings.getPlaybackSpeed());
                        PlaybackService.sendSkipSilenceUpdate(player);
                    }

                    if (currentMediaItem != null) {
                        player.setMediaItem(currentMediaItem, false);
                        player.prepare();
                    }
                }

                @Override
                public void onFailure(@NonNull Throwable t) {
                    Timber.e(t, "Failed to create MediaController");
                }
            }, ContextCompat.getMainExecutor(getContext()));
        }
    }

    private void releasePlayer() {
        if (player != null) {
            MediaController.releaseFuture(controllerFuture);
            player.removeListener(this);
            player = null;
        }
    }

    public boolean getPlayWhenReady() {
        return player.getPlayWhenReady();
    }

    /* Player.Listener implementation */
    @Override
    public void onEvents(@NonNull Player player, Player.Events events) {
        // Don't change the "keep screen on" state when chat only is enabled.
        if (!events.containsAny(EVENT_PLAY_WHEN_READY_CHANGED, EVENT_PLAYBACK_STATE_CHANGED) || chatOnlyViewVisible)
            return;

        int playbackState = player.getPlaybackState();
        View view = getView();
        if (view == null) return;
        view.setKeepScreenOn(player.getPlayWhenReady() && (playbackState == STATE_READY || playbackState == STATE_BUFFERING));
    }

    @Override
    public void onPlayerError(@NonNull PlaybackException exception) {
        Timber.e("Something went wrong playing the stream for " + mUserInfo.getDisplayName() + " - Exception: " + exception);

        playbackFailed();
    }

    @Override
    public void onPlayWhenReadyChanged(boolean isPlaying, int _ignored) {
        if (isPlaying) {
            showPauseIcon();

            if (!isAudioOnlyModeEnabled() && vodId == null) {
                player.seekToDefaultPosition(); // Go forward to live
            }
        } else {
            showPlayIcon();
        }

        updatePIPParameters();
    }

    @Override
    public void onSurfaceSizeChanged(int width, int height) {
        updatePIPParameters();
    }

    @Override
    public void onPositionDiscontinuity(@NonNull Player.PositionInfo oldPosition, @NonNull Player.PositionInfo newPosition, int reason) {
        if (vodId == null || reason != Player.DISCONTINUITY_REASON_SEEK) return;

        long oldMs = oldPosition.positionMs;
        long newMs = newPosition.positionMs;
        // A seek is when we've gone backwards or we go more than 10 seconds forward.
        boolean seek = oldMs > newMs || newMs - oldMs > 10000;
        if (seek) streamFragmentCallback.onSeek();
        ChatManager.instance.updateVodProgress(newMs, seek);
    }

    @Override
    public void onRenderedFirstFrame() {
        mPreview.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onIsPlayingChanged(boolean isPlaying) {
        if (vodId != null && isPlaying) vodRunnable.run();
    }

    public void updatePIPParameters() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || player == null) return;

        Rect videoRect = new Rect();
        mVideoView.getVideoSurfaceView().getGlobalVisibleRect(videoRect);

        PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder()
                .setAspectRatio(new Rational(16, 9))
                .setSourceRectHint(videoRect);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(player.getPlayWhenReady());
        }

        mActivity.setPictureInPictureParams(builder.build());
    }

    public void backPressed() {
        mVideoView.setVisibility(View.INVISIBLE);
        player.clearMediaItems();
        releasePlayer();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE;
        mFullScreenButton.setImageResource(isLandscape ? R.drawable.ic_fullscreen_exit : R.drawable.ic_fullscreen);

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

        pipDisabling = false;

        if (Util.SDK_INT <= 23 || player == null) {
            initializePlayer();
        }

        originalMainToolbarPadding = mToolbar.getPaddingRight();
        originalCtrlToolbarPadding = mControlToolbar.getPaddingRight();

        if (audioViewVisible && !isAudioOnlyModeEnabled()) {
            disableAudioOnlyView();
            startStreamWithQuality(Settings.getPrefStreamQuality());
        } else if (!castingViewVisible && !audioViewVisible && hasPaused && Settings.getStreamPlayerAutoContinuePlaybackOnReturn()) {
            startStreamWithQuality(Settings.getPrefStreamQuality());
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

        Timber.d("Stream Fragment paused");
        if (pictureInPictureEnabled)
            return;

        hasPaused = true;

        if (mQualityBottomSheet != null)
            mQualityBottomSheet.dismiss();

        if (mProfileBottomSheet != null)
            mProfileBottomSheet.dismiss();

        ChatManager.instance.setPreviousProgress();
    }

    @Override
    public void onStop() {
        Timber.d("Stream Fragment Stopped");
        super.onStop();

        if (!castingViewVisible && !audioViewVisible && player != null && !Settings.getStreamPlayerLockedPlayback()) {
            player.pause();
        }

        if (Util.SDK_INT > 23) {
            releasePlayer();
        }
    }

    @Override
    public void onDestroy() {
        Timber.d("Destroying");
        if (fetchViewCountRunnable != null) {
            fetchViewCountHandler.removeCallbacks(fetchViewCountRunnable);
        }

        releasePlayer();

        super.onDestroy();
    }

    private void startFetchingCurrentChatters() {
        Runnable fetchChattersRunnable = new Runnable() {
            @Override
            public void run() {
                GetStreamChattersTask task = new GetStreamChattersTask(mUserInfo.getLogin());

                Execute.background(task, chatters -> {
                });

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
                GetStreamViewersTask task = new GetStreamViewersTask(mUserInfo.getUserId());
                Execute.background(task, currentViewers -> {
                    try {
                        Timber.d("Fetching viewers");

                        if (currentViewers > -1)
                            Utils.setNumber(mCurrentViewersView, currentViewers);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

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
        mShowChatButton.setOnClickListener(view -> setLandscapeChat(!landscapeChatVisible));
    }

    /**
     * Sets the correct visibility of the show chat button.
     * If the screen is in landscape it is show, else it is shown
     */
    private void checkShowChatButtonVisibility() {
        if (isLandscape && Settings.isChatInLandscapeEnabled()) {
            mShowChatButton.setVisibility(View.VISIBLE);
        } else {
            mShowChatButton.setVisibility(View.GONE);
        }
    }

    private void shareButtonClicked() {
        // https://stackoverflow.com/questions/17167701/how-to-activate-share-button-in-android-app
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        String shareBody;

        if (vodId == null) {
            shareBody = "https://twitch.tv/" + mUserInfo.getLogin();
        } else {
            shareBody = "https://www.twitch.tv/" + mUserInfo.getLogin() + "/video/" + vodId;
        }

        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(sharingIntent, "Share via"));
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
                    player.pause();
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

    private void playbackButtonClicked() {
        DialogService.getPlaybackDialog(getActivity(), player).show();
    }

    private void showSeekDialog() {
        DialogService.getSeekDialog(getActivity(), player).show();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        optionsMenuItem = menu.findItem(R.id.menu_item_options);
        optionsMenuItem.setVisible(chatOnlyViewVisible);
        optionsMenuItem.setOnMenuItemClickListener(menuItem -> {
            if (mQualityButton != null) {
                mQualityButton.performClick();
            }
            return true;
        });

        menu.findItem(R.id.menu_item_playback).setVisible(vodId != null);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (!isVideoInterfaceShowing()) {
            mVideoWrapper.performClick();
            return true;
        }

        int itemId = item.getItemId();
        if (itemId == R.id.menu_item_sleep) {
            sleepButtonClicked();
            return true;
        } else if (itemId == R.id.menu_item_share) {
            shareButtonClicked();
            return true;
        } else if (itemId == R.id.menu_item_profile) {
            profileButtonClicked();
            return true;
        } else if (itemId == R.id.menu_item_external) {
            playWithExternalPlayer();
            return true;
        } else if (itemId == R.id.menu_item_playback) {
            playbackButtonClicked();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupLandscapeChat() {
        if (!Settings.isChatLandscapeSwipeable() || !Settings.isChatInLandscapeEnabled()) {
            return;
        }

        final int width = getScreenRect(getActivity()).height();

        View.OnTouchListener touchListener = new View.OnTouchListener() {
            private int downPosition = width;
            private int widthOnDown = width;

            public boolean onTouch(View view, MotionEvent event) {
                if (!isLandscape) {
                    return false;
                }

                final int X = (int) event.getRawX();
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        // If the user taps while the wrapper is in the resizing animation, cancel it.
                        mVideoWrapper.clearAnimation();

                        ConstraintLayout.LayoutParams lParams = (ConstraintLayout.LayoutParams) mVideoWrapper.getLayoutParams();
                        if (lParams.width > 0)
                            widthOnDown = lParams.width;

                        downPosition = (int) event.getRawX();
                        break;
                    case MotionEvent.ACTION_UP:
                        int upPosition = (int) event.getRawX();
                        int deltaPosition = upPosition - downPosition;

                        if (Math.abs(deltaPosition) < 20) {
                            setLandscapeChat(landscapeChatVisible);
                            return false;
                        }

                        setLandscapeChat(upPosition < downPosition);

                        break;
                    case MotionEvent.ACTION_MOVE:
                        ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) mVideoWrapper.getLayoutParams();
                        int newWidth;

                        if (X > downPosition) { // Swiping right
                            newWidth = widthOnDown + X - downPosition;
                        } else { // Swiping left
                            newWidth = widthOnDown - (downPosition - X);
                        }

                        layoutParams.width = Math.max(Math.min(newWidth, width), width - getLandscapeChatTargetWidth());

                        mVideoWrapper.setLayoutParams(layoutParams);
                        break;

                }
                rootView.invalidate();
                return false;
            }
        };

        mOverlay.setOnTouchListener(touchListener);
    }

    private void setLandscapeChat(boolean visible) {
        landscapeChatVisible = visible;

        int width = getScreenRect(getActivity()).height();
        ResizeWidthAnimation resizeWidthAnimation = new ResizeWidthAnimation(mVideoWrapper, visible ? width - getLandscapeChatTargetWidth() : width);
        resizeWidthAnimation.setDuration(250);
        mVideoWrapper.startAnimation(resizeWidthAnimation);
        mShowChatButton.animate().rotation(visible ? 180f : 0).start();

        var animator = ValueAnimator.ofInt(mVideoInterface.getPaddingRight(), visible ? 0 : rightInset).setDuration(250);
        animator.addUpdateListener((_animator) ->
                mVideoInterface.setPadding(mVideoInterface.getPaddingLeft(), 0, (Integer) _animator.getAnimatedValue(), 0)
        );
        animator.start();
    }

    private int getLandscapeChatTargetWidth() {
        return (int) (getScreenRect(getActivity()).height() * (Settings.getChatLandscapeWidth() / 100.0));
    }

    private void initCastingView() {
        castingViewVisible = true;
        //auto.setVisibility(View.GONE); // Auto does not work on chromecast
        mVideoView.setVisibility(View.INVISIBLE);
        mBufferingView.setVisibility(View.GONE);
        castingTextView.setVisibility(View.VISIBLE);
        //castingTextView.setText(R.string.stream_chromecast_connecting);
        showVideoInterface();
    }

    private void disableCastingView() {
        castingViewVisible = false;
        //auto.setVisibility(View.VISIBLE);
        mVideoView.setVisibility(View.VISIBLE);
        Service.bringToBack(mPreview);
        mBufferingView.setVisibility(View.VISIBLE);
        castingTextView.setVisibility(View.INVISIBLE);
        showVideoInterface();
    }

    /**
     * Checks if the activity was started with a shared view in high API levels.
     */
    private void setPreviewAndCheckForSharedTransition() {
        final Intent intent = requireActivity().getIntent();
        if (intent.hasExtra(getString(R.string.stream_preview_url))) {
            String imageUrl = intent.getStringExtra(getString(R.string.stream_preview_url));

            if (imageUrl == null || imageUrl.isEmpty()) {
                return;
            }

            Glide.with(requireContext())
                    .asBitmap()
                    .load(imageUrl)
                    .signature(new ObjectKey(System.currentTimeMillis() / TimeUnit.MINUTES.toMillis(5))) // Refresh preview images every 5 minutes
                    .into(mPreview);
        }

        if (intent.getBooleanExtra(getString(R.string.stream_shared_transition), false)) {
            mPreview.setTransitionName(getString(R.string.stream_preview_transition));

            final View[] viewsToHide = {mToolbar, mControlToolbar};
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
     * Call to make sure the UI is shown correctly
     */
    private void updateUI() {
        setAndroidUiMode();
        keepControlIconsInView();
        setVideoViewLayout();
    }

    /**
     * Sets the System UI visibility so that the status- and navigation bar automatically hides if the app is current in landscape.
     * But they will automatically show when the user swipes the screen.
     */
    private void setAndroidUiMode() {
        if (getActivity() == null) {
            return;
        }

        Window window = getActivity().getWindow();
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(window, rootView);

        WindowCompat.setDecorFitsSystemWindows(window, !isLandscape);

        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        if (isLandscape) windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
        else windowInsetsController.show(WindowInsetsCompat.Type.systemBars());
    }

    private void setVideoViewLayout() {
        ViewGroup.LayoutParams layoutParams = rootView.getLayoutParams();
        layoutParams.height = isLandscape ? ViewGroup.LayoutParams.MATCH_PARENT : ViewGroup.LayoutParams.WRAP_CONTENT;

        ConstraintLayout.LayoutParams layoutWrapper = (ConstraintLayout.LayoutParams) mVideoWrapper.getLayoutParams();
        boolean landscapeLayout = isLandscape && !pictureInPictureEnabled;
        if (landscapeLayout) {
            layoutWrapper.width = !landscapeChatVisible ? ConstraintLayout.LayoutParams.MATCH_CONSTRAINT : getScreenRect(getActivity()).height() - getLandscapeChatTargetWidth();
            layoutWrapper.height = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT;
        } else {
            layoutWrapper.width = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT;
            layoutWrapper.height = ConstraintLayout.LayoutParams.WRAP_CONTENT;
        }
        mVideoWrapper.setLayoutParams(layoutWrapper);

        mVideoView.setResizeMode(isLandscape ? AspectRatioFrameLayout.RESIZE_MODE_FIT : AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH);

        ((ConstraintLayout.LayoutParams) mVideoView.getLayoutParams()).dimensionRatio = landscapeLayout ? null : "16:9";
    }

    /**
     * Checks if the video interface is fully showing
     */
    public boolean isVideoInterfaceShowing() {
        return mVideoInterface.getAlpha() == 1f;
    }

    /**
     * Hides the video control interface with animations
     */
    private void hideVideoInterface() {
        if (mToolbar != null && !audioViewVisible && !chatOnlyViewVisible) {
            mVideoInterface.animate().alpha(0f).setInterpolator(new AccelerateDecelerateInterpolator()).setListener(new AnimatorListenerAdapter() {
                // ExoPlayer will hide the UI immediately
                // Set it visible again for the animation
                @Override
                public void onAnimationStart(Animator animation) {
                    controlView.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    controlView.setVisibility(View.INVISIBLE);
                }
            }).start();
        }
    }

    /**
     * Shows the video control interface with animations
     */
    private void showVideoInterface() {
        mVideoInterface.animate().alpha(1f).setInterpolator(new AccelerateDecelerateInterpolator()).setListener(null).start();
    }

    /**
     * Keeps the rightmost icons on the toolbars in view when the device is in landscape.
     * Otherwise the icons would be covered my the navigationbar
     */
    private void keepControlIconsInView() {
        if (Settings.getStreamPlayerShowNavigationBar()) {
            int ctrlPadding = originalCtrlToolbarPadding;
            int mainPadding = originalMainToolbarPadding;
            int delta = getNavigationBarHeight();

            if (isLandscape && !Service.isTablet(getContext())) {
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
     * If the device doesn't have a navigation bar (Such as Samsung Galaxy devices) the height is 0
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
     * Otherwise if the device is in fullscreen then is releases the lock by requesting for portrait
     */
    public void toggleFullscreen() {
        requireActivity().setRequestedOrientation(isLandscape ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // If the user is using auto-rotation, we'll need to unlock the orientation once the device is rotated.
        if (getInt(getContext().getContentResolver(), ACCELEROMETER_ROTATION, 0) == 1) {
            orientationListener.enable();
        }
    }

    /**
     * Tries playing stream with a quality.
     * If the given quality doesn't exist for the stream the try the next best quality option.
     * If no Quality URLS have yet been created then try to start stream with an aync task.
     */
    private void startStreamWithQuality(String quality) {
        if (qualityURLs == null) {
            startStreamWithTask();
        } else {
            if (qualityURLs.containsKey(quality)) {
                if (chatOnlyViewVisible) {
                    return;
                }

                playUrl(qualityURLs.get(quality).URL);
                showQualities();
                updateSelectedQuality(quality);
                showPauseIcon();
                Timber.d("Starting Stream With a quality on " + quality + " for " + mUserInfo.getDisplayName());
                Timber.d("URLS: %s", qualityURLs.keySet());
            } else if (!qualityURLs.isEmpty()) {
                Timber.d("Quality unavailable for this stream -  " + quality + ". Trying next best");
                tryNextBestQuality(quality);
            }
        }
    }

    /**
     * Starts an Async task that fetches all available Stream URLs for a stream,
     * then tries to start stream with the latest user defined quality setting.
     * If no URLs are available for the stream, the user is notified.
     */
    private void startStreamWithTask() {
        Consumer<Map<String, Quality>> callback = url -> {
            try {
                if (!url.isEmpty()) {
                    updateQualitySelections(url);
                    qualityURLs = url;

                    if (!isAudioOnlyModeEnabled()) {
                        startStreamWithQuality(Settings.getPrefStreamQuality());
                    }
                } else {
                    playbackFailed();
                }
            } catch (IllegalStateException | NullPointerException e) {
                e.printStackTrace();
            }
        };

        String[] types = getResources().getStringArray(R.array.PlayerType);

        var isClip = requireArguments().containsKey(Constants.KEY_CLIP);
        GetStreamURL task = new GetStreamURL(mUserInfo.getLogin(), vodId, types[Settings.getStreamPlayerType()], Settings.getStreamPlayerProxy(), isClip);
        Execute.background(task, callback);
    }

    /**
     * Stops the buffering and notifies the user that the stream could not be played
     */
    private void playbackFailed() {
        if (getContext() == null) return;

        showSnackbar(getString(vodId == null ? R.string.stream_playback_failed : R.string.vod_playback_failed), getString(R.string.retry), v -> startStreamWithTask());
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
                startStreamWithQuality(GetStreamURL.QUALITY_SOURCE);
            } else {
                startStreamWithQuality(qualityList.get(next));
            }
        } else {
            playbackFailed();
        }
    }

    /**
     * Sets the URL to the VideoView and ChromeCast and starts playback.
     */
    private void playUrl(String url) {
        Bundle extras = new Bundle();
        if (vodId == null) {
            extras.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, -1);
        }

        // Grab the preview image from the intent and give it to the media metadata
        Intent intent = requireActivity().getIntent();
        String uriString = intent.getStringExtra(getString(R.string.stream_preview_url));

        MediaItem mediaItem = new MediaItem.Builder()
                .setLiveConfiguration(new MediaItem.LiveConfiguration.Builder().setTargetOffsetMs(1000).build())
                .setUri(url)
                .setMediaId(vodId == null ? "" : vodId)
                .setMediaMetadata(new MediaMetadata.Builder()
                        .setTitle(title)
                        .setArtist(mUserInfo.getDisplayName())
                        .setExtras(extras)
                        .setArtworkUri(uriString != null ? Uri.parse(uriString) : null)
                        .build())
                .build();

        if (vodId != null) {
            long startPosition = Settings.getVodProgress(vodId) * 1000L;
            // Don't lose the position if we're playing the same VOD
            if (currentMediaItem != null && currentMediaItem.mediaId.equals(vodId)) {
                startPosition = player.getCurrentPosition();
            }

            player.setMediaItem(mediaItem, startPosition);
        } else {
            player.setMediaItem(mediaItem, false);
        }
        currentMediaItem = mediaItem;
        player.prepare();

        player.play();
    }

    private void playWithExternalPlayer() {
        if (currentMediaItem.localConfiguration == null) {
            Toast.makeText(getContext(), R.string.error_external_playback_failed, Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(currentMediaItem.localConfiguration.uri, "video/*");
        startActivity(Intent.createChooser(intent, getString(R.string.stream_external_play_using)));
    }

    private void registerAudioOnlyDelegate() {
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
                v.setBackgroundColor(ContextCompat.getColor(getContext(), com.balysv.materialripple.R.color.transparent));
            }
        }
    }

    /**
     * Adds the available qualities for a stream to the spinner menu
     */
    private void updateQualitySelections(Map<String, Quality> availableQualities) {
        for (TextView view : QualityOptions.values()) {
            mQualityWrapper.removeView((MaterialRippleLayout) view.getParent());
        }

        for (Map.Entry<String, Quality> entry : availableQualities.entrySet()) {
            Quality quality = entry.getValue();
            String qualityKey = entry.getKey();
            if (qualityKey.equals("audio_only"))
                continue;

            MaterialRippleLayout layout = (MaterialRippleLayout) LayoutInflater.from(getContext()).inflate(R.layout.quality_item, null);
            TextView textView = (TextView) layout.getChildAt(0);
            textView.setText(quality.Name.equals("Auto") ? getString(R.string.quality_auto) : quality.Name);

            setQualityOnClick(textView, qualityKey);
            QualityOptions.put(qualityKey, textView);
            mQualityWrapper.addView(layout);
        }
    }

    /**
     * Sets an OnClickListener on a select quality view (From bottom dialog).
     * The Listener starts the stream with a new quality setting and updates the background for the select quality views in the bottom dialog
     */
    private void setQualityOnClick(final TextView qualityView, String quality) {
        qualityView.setOnClickListener(v -> {
            // don´t set audio only mode as default
            if (!quality.equals("audio_only")) {
                Settings.setPrefStreamQuality(quality);
            }
            // don`t allow to change the Quality when using audio only Mode
            if (!isAudioOnlyModeEnabled()) {
                startStreamWithQuality(quality);
                resetQualityViewBackground(qualityView);
                mQualityBottomSheet.dismiss();
            }
        });
    }

    private BottomSheetBehavior<View> getDefaultBottomSheetBehaviour(View bottomSheetView) {
        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from((View) bottomSheetView.getParent());
        behavior.setPeekHeight(requireActivity().getResources().getDisplayMetrics().heightPixels / 3);
        return behavior;
    }

    private void setupProfileBottomSheet() {
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.stream_profile_preview, null);
        mProfileBottomSheet = new BottomSheetDialog(requireContext());
        mProfileBottomSheet.setContentView(v);
        final BottomSheetBehavior<View> behavior = getDefaultBottomSheetBehaviour(v);

        mProfileBottomSheet.setOnDismissListener(dialogInterface -> behavior.setState(BottomSheetBehavior.STATE_COLLAPSED));

        TextView mNameView = mProfileBottomSheet.findViewById(R.id.twitch_name);
        TextView mFollowers = mProfileBottomSheet.findViewById(R.id.txt_followers);
        ImageView mFollowButton = mProfileBottomSheet.findViewById(R.id.follow_unfollow_icon);
        ImageView mFullProfileButton = mProfileBottomSheet.findViewById(R.id.full_profile_icon);
        RecyclerView mPanelsRecyclerView = mProfileBottomSheet.findViewById(R.id.panel_recyclerview);
        if (mNameView == null || mFollowers == null || mFullProfileButton == null || mPanelsRecyclerView == null)
            return;

        mNameView.setText(mUserInfo.getDisplayName());

        Execute.background(() -> Service.getStreamerInfoFromUserId(mUserInfo.getUserId()), channelInfo -> {
            channelInfo.getFollowers(followers -> Utils.setNumber(mFollowers, followers), 0);

            setupFollowButton(mFollowButton, channelInfo);
        });

        mFullProfileButton.setOnClickListener(view -> {
            mProfileBottomSheet.dismiss();

            final Intent intent = new Intent(getContext(), ChannelActivity.class);
            intent.putExtra(getString(R.string.channel_info_intent_object), mUserInfo);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            startActivity(intent);
        });

        setupPanels(mPanelsRecyclerView);
    }

    private void setupFollowButton(final ImageView imageView, ChannelInfo channelInfo) {
        final FollowHandler mFollowHandler = new FollowHandler(
                channelInfo,
                getContext(),
                () -> imageView.setVisibility(View.GONE)
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
                : R.drawable.ic_favorite;
        imageView.setImageResource(imageRes);
    }

    private void setupPanels(RecyclerView recyclerView) {
        final PanelAdapter mPanelAdapter = new PanelAdapter(getActivity());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        recyclerView.setAdapter(mPanelAdapter);

        GetPanelsTask mTask = new GetPanelsTask(mUserInfo.getLogin());
        Execute.background(mTask, mPanelAdapter::addPanels);
    }

    /**
     * Setups the Quality Select spinner.
     * Automatically hides the text of the selected Quality
     */
    private void setupSpinner() {
        mQualityButton.setOnClickListener(v -> mQualityBottomSheet.show());

        View v = LayoutInflater.from(getContext()).inflate(R.layout.stream_settings, null);
        mQualityBottomSheet = new BottomSheetDialog(requireContext());
        mQualityBottomSheet.setContentView(v);

        final BottomSheetBehavior behavior = getDefaultBottomSheetBehaviour(v);

        mQualityBottomSheet.setOnDismissListener(dialogInterface -> behavior.setState(BottomSheetBehavior.STATE_COLLAPSED));

        mQualityWrapper = mQualityBottomSheet.findViewById(R.id.quality_wrapper);
        mAudioOnlySelector = mQualityBottomSheet.findViewById(R.id.audio_only_selector);
        mMuteSelector = mQualityBottomSheet.findViewById(R.id.mute_selector);
        mChatOnlySelector = mQualityBottomSheet.findViewById(R.id.chat_only_selector);
        TextView optionsTitle = mQualityBottomSheet.findViewById(R.id.options_text);

        if (optionsTitle != null) {
            optionsTitle.setVisibility(View.VISIBLE);
        }

        if (vodId == null) {
            mChatOnlySelector.setVisibility(View.VISIBLE);
        }

        mAudioOnlySelector.setVisibility(View.VISIBLE);
        mAudioOnlySelector.setOnClickListener(view -> {
            mQualityBottomSheet.dismiss();
            audioOnlyClicked();
        });

        mMuteSelector.setVisibility(View.VISIBLE);
        mMuteSelector.setOnClickListener(view -> {
            mQualityBottomSheet.dismiss();
            muteClicked();
        });

        mChatOnlySelector.setOnClickListener(view -> {
            mQualityBottomSheet.dismiss();
            setChatOnlyView(!chatOnlyViewVisible);
        });
    }

    private void initAudioOnlyView() {
        if (!audioViewVisible) {
            audioViewVisible = true;
            mVideoView.setVisibility(View.INVISIBLE);
            mBufferingView.start();
            //mBufferingView.setVisibility(View.GONE);
            castingTextView.setVisibility(View.VISIBLE);
            castingTextView.setText(R.string.stream_audio_only_active);

            showVideoInterface();
            updateSelectedQuality(null);
            startStreamWithQuality("audio_only");
            hideQualities();
        }
    }

    private void disableAudioOnlyView() {
        if (audioViewVisible) {
            audioViewVisible = false;
            mAudioOnlySelector.setChecked(false);
            mVideoView.setVisibility(View.VISIBLE);
            mBufferingView.setVisibility(View.VISIBLE);
            Service.bringToBack(mPreview);
            castingTextView.setVisibility(View.INVISIBLE);

            showQualities();
            showVideoInterface();
        }
    }

    private boolean isAudioOnlyModeEnabled() {
        // just use audioViewVisible as boolean
        return audioViewVisible;
    }

    private void audioOnlyClicked() {
        mAudioOnlySelector.setChecked(!mAudioOnlySelector.isChecked());
        if (mAudioOnlySelector.isChecked()) {
            initAudioOnlyView();
        } else {
            stopAudioOnly();
        }
    }

    private void muteClicked() {
        mMuteSelector.setChecked(!mMuteSelector.isChecked());
        if (mMuteSelector.isChecked()) {
            player.setVolume(0);
        } else {
            player.setVolume(1);
        }
    }

    private void stopAudioOnly() {
        disableAudioOnlyView();

        // start stream with last quality
        releasePlayer();
        initializePlayer();
        updateSelectedQuality(Settings.getPrefStreamQuality());
        startStreamWithQuality(Settings.getPrefStreamQuality());

        // resume the stream
        player.play();
    }

    private void stopAudioOnlyNoServiceCall() {
        disableAudioOnlyView();
    }

    private void setChatOnlyView(boolean enabled) {
        if (chatOnlyViewVisible == enabled) return;

        chatOnlyViewVisible = enabled;

        mChatOnlySelector.setChecked(chatOnlyViewVisible);
        if (chatOnlyViewVisible) initChatOnlyView();
        else disableChatOnlyView();

        controlView.setShowTimeoutMs(chatOnlyViewVisible ? -1 : SHOW_TIMEOUT);
        controlView.show();

        requireView().setKeepScreenOn(chatOnlyViewVisible);
    }

    private void initChatOnlyView() {
        if (isLandscape) {
            toggleFullscreen();
        }

        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        videoHeightBeforeChatOnly = mVideoWrapper.getHeight();
        ResizeHeightAnimation heightAnimation = new ResizeHeightAnimation(mVideoWrapper, (int) getResources().getDimension(R.dimen.main_toolbar_height));
        heightAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        heightAnimation.setDuration(240);
        mVideoWrapper.startAnimation(heightAnimation);

        mPlayPauseWrapper.setVisibility(View.GONE);
        mControlToolbar.setVisibility(View.GONE);
        mToolbar.setBackgroundColor(Service.getColorAttribute(androidx.appcompat.R.attr.colorPrimary, R.color.primary, requireContext()));

        releasePlayer();
        if (optionsMenuItem != null) optionsMenuItem.setVisible(true);

        updateSelectedQuality(null);
        hideQualities();
    }

    private void disableChatOnlyView() {
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        ResizeHeightAnimation heightAnimation = new ResizeHeightAnimation(mVideoWrapper, videoHeightBeforeChatOnly);
        heightAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        heightAnimation.setDuration(240);
        heightAnimation.setFillAfter(false);
        mVideoWrapper.startAnimation(heightAnimation);

        mControlToolbar.setVisibility(View.VISIBLE);
        mPlayPauseWrapper.setVisibility(View.VISIBLE);
        mToolbar.setBackgroundColor(Service.getColorAttribute(R.attr.streamToolbarColor, R.color.black_transparent, requireActivity()));

        if (!castingViewVisible) {
            initializePlayer();
            startStreamWithQuality(Settings.getPrefStreamQuality());
        }

        optionsMenuItem.setVisible(false);
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
        mToolbar.setPadding(0, 0, Service.dpToPixels(requireActivity(), 5), 0);
        setHasOptionsMenu(true);
        mActivity.setSupportActionBar(mToolbar);
        mToolbar.bringToFront();

        ActionBar actionBar = mActivity.getSupportActionBar();
        if (actionBar == null) {
            return;
        }

        final SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(mUserInfo.getDisplayName());
        if (title != null) {
            int secondaryColor = ContextCompat.getColor(requireContext(), R.color.white_text_secondary);
            appendSpan(builder, "\n" + title, new ForegroundColorSpan(secondaryColor), new RelativeSizeSpan(0.75f));
        }
        mTitleText.setText(builder);
        actionBar.setTitle("");
        actionBar.setDisplayHomeAsUpEnabled(true);
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
}
