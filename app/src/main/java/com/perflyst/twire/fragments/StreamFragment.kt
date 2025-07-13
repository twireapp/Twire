package com.perflyst.twire.fragments

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.app.PictureInPictureParams
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.support.v4.media.MediaMetadataCompat
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.transition.Transition
import android.util.DisplayMetrics
import android.util.Rational
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.OrientationEventListener
import android.view.RoundedCorner
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.CheckedTextView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.LiveConfiguration
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.PositionInfo
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerControlView
import androidx.media3.ui.PlayerView
import androidx.media3.ui.PlayerView.ControllerVisibilityListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.balysv.materialripple.MaterialRippleLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.perflyst.twire.PlaybackService
import com.perflyst.twire.R
import com.perflyst.twire.activities.ChannelActivity
import com.perflyst.twire.activities.stream.StreamActivity
import com.perflyst.twire.adapters.PanelAdapter
import com.perflyst.twire.chat.ChatManager
import com.perflyst.twire.misc.FollowHandler
import com.perflyst.twire.misc.OnlineSince
import com.perflyst.twire.misc.ResizeHeightAnimation
import com.perflyst.twire.misc.ResizeWidthAnimation
import com.perflyst.twire.misc.Utils
import com.perflyst.twire.model.ChannelInfo
import com.perflyst.twire.model.Quality
import com.perflyst.twire.model.SleepTimer
import com.perflyst.twire.model.SleepTimer.SleepTimerDelegate
import com.perflyst.twire.model.UserInfo
import com.perflyst.twire.service.DialogService
import com.perflyst.twire.service.Service
import com.perflyst.twire.service.Settings.chatLandscapeWidth
import com.perflyst.twire.service.Settings.isChatInLandscapeEnabled
import com.perflyst.twire.service.Settings.isChatLandscapeSwipeable
import com.perflyst.twire.service.Settings.playbackSpeed
import com.perflyst.twire.service.Settings.prefStreamQuality
import com.perflyst.twire.service.Settings.streamPlayerAutoContinuePlaybackOnReturn
import com.perflyst.twire.service.Settings.streamPlayerLockedPlayback
import com.perflyst.twire.service.Settings.streamPlayerProxy
import com.perflyst.twire.service.Settings.streamPlayerRuntime
import com.perflyst.twire.service.Settings.streamPlayerShowNavigationBar
import com.perflyst.twire.service.Settings.streamPlayerShowViewerCount
import com.perflyst.twire.service.Settings.streamPlayerType
import com.perflyst.twire.tasks.GetPanelsTask
import com.perflyst.twire.tasks.GetStreamChattersTask
import com.perflyst.twire.tasks.GetStreamURL
import com.perflyst.twire.tasks.GetStreamViewersTask
import com.perflyst.twire.utils.Constants
import com.perflyst.twire.utils.Execute
import com.rey.material.widget.ProgressView
import dev.chrisbanes.insetter.Insetter
import dev.chrisbanes.insetter.ViewState
import timber.log.Timber
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

@OptIn(UnstableApi::class)
class StreamFragment : Fragment(), Player.Listener {
    private val fetchViewCountHandler = Handler()
    private val fetchChattersHandler = Handler()
    private val vodHandler = Handler()
    private val qualityOptions = HashMap<String, TextView>()
    private val fetchViewCountDelay = 1000 * 60 // A minute
    private val fetchChattersDelay = 1000 * 60 // 30 seco... Nah just kidding. Also a minute.

    @JvmField
    var streamFragmentCallback: StreamFragmentListener? = null

    @JvmField
    var chatOnlyViewVisible: Boolean = false
    private var castingViewVisible = false

    // just use audioViewVisible as boolean
    private var isAudioOnlyModeEnabled = false
    private var autoPlay = true
    private var hasPaused = false
    private var landscapeChatVisible = false
    private var mUserInfo: UserInfo? = null
    private var vodId: String? = null
    private var title: String? = null
    private var startTime: Long = 0
    private var sleepTimer: SleepTimer? = null
    private var qualityURLs: MutableMap<String, Quality>? = null
    private var isLandscape = false
    private var fetchViewCountRunnable: Runnable? = null
    private lateinit var mVideoView: PlayerView
    private var player: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var currentMediaItem: MediaItem? = null
    private lateinit var mToolbar: Toolbar
    private lateinit var mTitleText: TextView
    private lateinit var mVideoInterface: ConstraintLayout
    private lateinit var mControlToolbar: RelativeLayout
    private lateinit var mVideoWrapper: ConstraintLayout
    private lateinit var mPlayPauseWrapper: ConstraintLayout
    private lateinit var mPauseIcon: ImageView
    private lateinit var mPlayIcon: ImageView
    private lateinit var mQualityButton: ImageView
    private lateinit var mFullScreenButton: ImageView
    private lateinit var mPreview: ImageView
    private lateinit var mShowChatButton: ImageView
    private lateinit var castingTextView: TextView
    private lateinit var mCurrentViewersView: TextView
    private lateinit var mRuntime: TextView
    private var mActivity: AppCompatActivity? = null
    private var snackbar: Snackbar? = null
    private lateinit var mBufferingView: ProgressView
    private var mQualityBottomSheet: BottomSheetDialog? = null
    private var mProfileBottomSheet: BottomSheetDialog? = null
    private var mAudioOnlySelector: CheckedTextView? = null
    private var mMuteSelector: CheckedTextView? = null
    private var mChatOnlySelector: CheckedTextView? = null
    private lateinit var rootView: ViewGroup
    private var optionsMenuItem: MenuItem? = null
    private lateinit var mQualityWrapper: LinearLayout
    private lateinit var mOverlay: View
    private lateinit var controlView: PlayerControlView
    private var orientationListener: OrientationEventListener? = null

    private val vodRunnable: Runnable = object : Runnable {
        override fun run() {
            if (player == null) return

            ChatManager.instance?.updateVodProgress(player!!.getCurrentPosition(), false)

            if (player!!.isPlaying()) vodHandler.postDelayed(this, 1000)
        }
    }

    private var originalCtrlToolbarPadding = 0
    private var originalMainToolbarPadding = 0
    private var videoHeightBeforeChatOnly = 0
    private var triesForNextBest = 0
    private var pictureInPictureEnabled = false // Tracks if PIP is enabled including the animation.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val args = arguments
        setHasOptionsMenu(true)

        if (args != null) {
            mUserInfo =
                args.getParcelable(getString(R.string.stream_fragment_streamerInfo))
            vodId = args.getString(getString(R.string.stream_fragment_vod_id))
            autoPlay = args.getBoolean(getString(R.string.stream_fragment_autoplay))
            title = args.getString(getString(R.string.stream_fragment_title))
        }

        val mRootView = inflater.inflate(R.layout.fragment_stream, container, false)
        mRootView.requestLayout()

        // If the user has been in FULL SCREEN mode and presses the back button, we want to change the orientation to portrait.
        // As soon as the orientation has change we don't want to force the user to will be in portrait, so we "release" the request.
        if (requireActivity().getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
        }

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            isLandscape = true
        }

        //  If no streamer info is available we cant show the stream.
        if (mUserInfo == null) {
            if (activity != null) {
                requireActivity().finish()
            }
            return rootView
        }

        rootView = mRootView as ViewGroup
        mVideoView = mRootView.findViewById(R.id.VideoView)
        mVideoInterface = mVideoView.findViewById(R.id.video_interface)
        mToolbar = mVideoView.findViewById(R.id.main_toolbar)
        mTitleText = mVideoView.findViewById(R.id.toolbar_title)
        mControlToolbar = mVideoView.findViewById(R.id.control_toolbar_wrapper)
        mVideoWrapper = mRootView.findViewById(R.id.video_wrapper)
        mPlayPauseWrapper = mVideoView.findViewById(R.id.play_pause_wrapper)
        mPlayIcon = mVideoView.findViewById(R.id.ic_play)
        mPauseIcon = mVideoView.findViewById(R.id.ic_pause)
        mPreview = mVideoView.findViewById(R.id.preview)
        mQualityButton = mVideoView.findViewById(R.id.settings_icon)
        mFullScreenButton = mVideoView.findViewById(R.id.fullscreen_icon)
        mShowChatButton = mVideoView.findViewById(R.id.show_chat_button)
        castingTextView = mVideoView.findViewById(R.id.chromecast_text)
        mBufferingView = mVideoView.findViewById(R.id.exo_buffering)
        mCurrentViewersView = mVideoView.findViewById(R.id.txtViewViewers)
        mRuntime = mVideoView.findViewById(R.id.txtViewRuntime)
        mActivity = activity as AppCompatActivity?
        mOverlay = mVideoView.overlayFrameLayout!!

        landscapeChatVisible = isChatInLandscapeEnabled

        setupToolbar()
        setupSpinner()
        setupProfileBottomSheet()
        setupLandscapeChat()
        setupShowChatButton()

        if (savedInstanceState == null) setPreviewAndCheckForSharedTransition()

        mFullScreenButton.setOnClickListener { v: View? -> toggleFullscreen() }
        mPlayPauseWrapper.setOnClickListener { v: View? ->
            player!!.setPlayWhenReady(
                !player!!.getPlayWhenReady()
            )
        }

        initializePlayer()

        controlView =
            mVideoView.findViewById(androidx.media3.ui.R.id.exo_controller)

        controlView.setShowFastForwardButton(vodId != null)
        controlView.setShowRewindButton(vodId != null)

        controlView.setShowTimeoutMs(SHOW_TIMEOUT)
        controlView.isAnimationEnabled = false

        mVideoView.setControllerVisibilityListener(ControllerVisibilityListener { visibility: Int ->
            if (visibility == View.VISIBLE) {
                showVideoInterface()
            } else {
                hideVideoInterface()
            }
        })

        // Use ExoPlayer's overlay frame to intercept click events and pass them along
        mOverlay.setOnClickListener { view: View? -> mVideoView.performClick() }

        if (vodId == null) {
            val mTimeController = mVideoView.findViewById<View>(R.id.time_controller)
            mTimeController.visibility = View.INVISIBLE

            if (!streamPlayerRuntime) {
                mRuntime.visibility = View.GONE
            } else {
                startTime = args!!.getLong(getString(R.string.stream_fragment_start_time))
                controlView.setProgressUpdateListener { position: Long, bufferedPosition: Long ->
                    mRuntime.text = OnlineSince.getOnlineSince(startTime)
                }
            }


            if (args != null && args.containsKey(getString(R.string.stream_fragment_viewers)) && streamPlayerShowViewerCount) {
                Utils.setNumber(
                    mCurrentViewersView,
                    args.getInt(getString(R.string.stream_fragment_viewers)).toLong()
                )
                startFetchingViewers()
            } else {
                mCurrentViewersView.visibility = View.GONE
            }
        } else {
            mCurrentViewersView.visibility = View.GONE
            mRuntime.visibility = View.GONE

            mVideoView.findViewById<View?>(R.id.exo_position)
                .setOnClickListener { v: View? -> showSeekDialog() }
        }

        if (autoPlay || vodId != null) {
            startStreamWithQuality(prefStreamQuality)
        }

        // Enabled after the user toggles the fullscreen mode
        // Unlocks the orientation of the screen after the user rotates to the new orientation
        orientationListener = object : OrientationEventListener(activity) {
            override fun onOrientationChanged(orientation: Int) {
                val estimatedOrientation = if (close(
                        orientation,
                        0,
                        180,
                        360
                    )
                ) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else if (close(
                        orientation,
                        90,
                        270
                    )
                ) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                val requestedOrientation = requireActivity().getRequestedOrientation()
                if (estimatedOrientation == requestedOrientation) {
                    requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                    orientationListener!!.disable()
                }
            }

            fun close(current: Int, vararg targets: Int): Boolean {
                for (target in targets) {
                    if (abs((current - target).toDouble()) < 15) return true
                }

                return false
            }
        }

        // Apply the insets to the root view in portrait
        // But use the rounded corners for the controls in landscape
        Insetter.builder()
            .setOnApplyInsetsListener { v: View?, windowInsets: WindowInsetsCompat?, initialState: ViewState? ->
                val insets = windowInsets!!.getInsets(WindowInsetsCompat.Type.statusBars())
                val isLandscape =
                    resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                if (isLandscape) {
                    v!!.setPadding(0, 0, 0, 0)

                    // set control padding
                    var left = 0
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        left = calculatePadding(RoundedCorner.POSITION_TOP_LEFT)
                        rightInset = calculatePadding(RoundedCorner.POSITION_TOP_RIGHT)
                    }
                    mVideoInterface.setPadding(left, 0, rightInset, 0)
                } else {
                    v!!.setPadding(insets.left, insets.top, insets.right, insets.bottom)
                    mVideoInterface.setPadding(0, 0, 0, 0)
                }
            }.applyToView(mRootView)

        return mRootView
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    fun calculatePadding(position: Int): Int {
        val rootInsets = rootView.getRootWindowInsets()
        val corner = rootInsets.getRoundedCorner(position)
        if (corner == null) return 0
        val radius = corner.radius
        return (radius - radius / sqrt(2.0) - resources.getDimension(R.dimen.toolbar_icon_padding)).toInt()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean("chatOnlyViewVisible", chatOnlyViewVisible)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        if (savedInstanceState != null) {
            setChatOnlyView(savedInstanceState.getBoolean("chatOnlyViewVisible", false))
        }
    }

    private fun initializePlayer() {
        if (player == null) {
            val sessionToken = SessionToken(
                requireContext(),
                ComponentName(requireContext(), PlaybackService::class.java)
            )
            controllerFuture = MediaController.Builder(requireContext(), sessionToken).buildAsync()
            val streamFragment = this
            Futures.addCallback<MediaController?>(
                controllerFuture,
                object : FutureCallback<MediaController?> {
                    override fun onSuccess(result: MediaController?) {
                        player = result
                        player?.let { player ->
                            player.addListener(streamFragment)
                            mVideoView.setPlayer(player)

                            if (vodId != null) {
                                player.setPlaybackSpeed(playbackSpeed)
                                PlaybackService.sendSkipSilenceUpdate(player)
                            }

                            currentMediaItem?.let { currentMediaItem ->
                                player.setMediaItem(currentMediaItem, false)
                                player.prepare()
                            }
                        }
                    }

                    override fun onFailure(t: Throwable) {
                        Timber.e(t, "Failed to create MediaController")
                    }
                },
                ContextCompat.getMainExecutor(requireContext())
            )
        }
    }

    private fun releasePlayer() {
        if (player == null) return
        MediaController.releaseFuture(controllerFuture!!)
        player!!.removeListener(this)
        player = null
    }

    val playWhenReady: Boolean
        get() = player!!.getPlayWhenReady()

    /* Player.Listener implementation */
    override fun onEvents(player: Player, events: Player.Events) {
        // Don't change the "keep screen on" state when chat only is enabled.
        if (!events.containsAny(
                Player.EVENT_PLAY_WHEN_READY_CHANGED,
                Player.EVENT_PLAYBACK_STATE_CHANGED
            ) || chatOnlyViewVisible
        ) return

        val playbackState = player.playbackState
        val view = getView()
        if (view == null) return
        view.keepScreenOn =
            player.playWhenReady && (playbackState == Player.STATE_READY || playbackState == Player.STATE_BUFFERING)
    }

    override fun onPlayerError(exception: PlaybackException) {
        Timber.e(exception, "Something went wrong playing the stream")

        playbackFailed()
    }

    override fun onPlayWhenReadyChanged(isPlaying: Boolean, ignored: Int) {
        if (isPlaying) {
            showPauseIcon()

            if (!this.isAudioOnlyModeEnabled && vodId == null && player != null) {
                player!!.seekToDefaultPosition() // Go forward to live
            }
        } else {
            showPlayIcon()
        }

        updatePIPParameters()
    }

    override fun onSurfaceSizeChanged(width: Int, height: Int) {
        updatePIPParameters()
    }

    override fun onPositionDiscontinuity(
        oldPosition: PositionInfo,
        newPosition: PositionInfo,
        reason: Int
    ) {
        if (vodId == null || reason != Player.DISCONTINUITY_REASON_SEEK) return

        val oldMs = oldPosition.positionMs
        val newMs = newPosition.positionMs
        // A seek is when we've gone backwards or we go more than 10 seconds forward.
        val seek = oldMs > newMs || newMs - oldMs > 10000
        if (seek) streamFragmentCallback!!.onSeek()
        ChatManager.instance?.updateVodProgress(newMs, seek)
    }

    override fun onRenderedFirstFrame() {
        mPreview.setVisibility(View.INVISIBLE)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (vodId != null && isPlaying) vodRunnable.run()
    }

    fun updatePIPParameters() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || player == null) return

        val videoRect = Rect()
        mVideoView.videoSurfaceView!!.getGlobalVisibleRect(videoRect)

        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .setSourceRectHint(videoRect)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(player!!.getPlayWhenReady())
        }

        mActivity!!.setPictureInPictureParams(builder.build())
    }

    fun backPressed() {
        mVideoView.setVisibility(View.INVISIBLE)
        player!!.clearMediaItems()
        releasePlayer()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
        mFullScreenButton.setImageResource(if (isLandscape) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen)

        checkShowChatButtonVisibility()
        updateUI()
    }

    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            initializePlayer()
        }
    }

    override fun onResume() {
        super.onResume()

        pipDisabling = false

        if (Util.SDK_INT <= 23 || player == null) {
            initializePlayer()
        }

        originalMainToolbarPadding = mToolbar.getPaddingRight()
        originalCtrlToolbarPadding = mControlToolbar.getPaddingRight()

        if (this.isAudioOnlyModeEnabled && !this.isAudioOnlyModeEnabled) {
            disableAudioOnlyView()
            startStreamWithQuality(prefStreamQuality)
        } else if (!castingViewVisible && !this.isAudioOnlyModeEnabled && hasPaused && streamPlayerAutoContinuePlaybackOnReturn) {
            startStreamWithQuality(prefStreamQuality)
        }

        registerAudioOnlyDelegate()

        if (!chatOnlyViewVisible) {
            showVideoInterface()
            updateUI()
        }
    }

    override fun onPause() {
        super.onPause()

        Timber.d("Stream Fragment paused")
        if (pictureInPictureEnabled) return

        hasPaused = true

        mQualityBottomSheet?.dismiss()

        mProfileBottomSheet?.dismiss()

        ChatManager.instance?.setPreviousProgress()
    }

    override fun onStop() {
        Timber.d("Stream Fragment Stopped")
        super.onStop()

        if (!castingViewVisible && !this.isAudioOnlyModeEnabled && player != null && !streamPlayerLockedPlayback) {
            player!!.pause()
        }

        if (Util.SDK_INT > 23) {
            releasePlayer()
        }
    }

    override fun onDestroy() {
        Timber.d("Destroying")
        if (fetchViewCountRunnable != null) {
            fetchViewCountHandler.removeCallbacks(fetchViewCountRunnable!!)
        }

        releasePlayer()

        super.onDestroy()
    }

    private fun startFetchingCurrentChatters() {
        val fetchChattersRunnable: Runnable = object : Runnable {
            override fun run() {
                val task = GetStreamChattersTask(mUserInfo!!.login)

                Execute.background(
                    task
                ) { chatters: ArrayList<String>? -> }

                if (!this@StreamFragment.isDetached) {
                    fetchChattersHandler.postDelayed(this, fetchChattersDelay.toLong())
                }
            }
        }

        fetchChattersHandler.post(fetchChattersRunnable)
    }

    /**
     * Starts fetching current viewers for the current stream
     */
    private fun startFetchingViewers() {
        fetchViewCountRunnable = object : Runnable {
            override fun run() {
                val task = GetStreamViewersTask(mUserInfo!!.userId)
                Execute.background(task) { currentViewers: Int? ->
                    try {
                        Timber.d("Fetching viewers")

                        if (currentViewers!! > -1) Utils.setNumber(
                            mCurrentViewersView,
                            currentViewers.toLong()
                        )
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                }

                if (!this@StreamFragment.isDetached) {
                    fetchViewCountHandler.postDelayed(this, fetchViewCountDelay.toLong())
                }
            }
        }


        fetchViewCountHandler.post(fetchViewCountRunnable!!)
    }

    /**
     * Sets up the show chat button.
     * Sets the correct visibility and the onclicklistener
     */
    private fun setupShowChatButton() {
        checkShowChatButtonVisibility()
        mShowChatButton.setOnClickListener { view: View? ->
            setLandscapeChat(
                !landscapeChatVisible
            )
        }
    }

    /**
     * Sets the correct visibility of the show chat button.
     * If the screen is in landscape it is show, else it is shown
     */
    private fun checkShowChatButtonVisibility() {
        if (isLandscape && isChatInLandscapeEnabled) {
            mShowChatButton.setVisibility(View.VISIBLE)
        } else {
            mShowChatButton.setVisibility(View.GONE)
        }
    }

    private fun shareButtonClicked() {
        // https://stackoverflow.com/questions/17167701/how-to-activate-share-button-in-android-app
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.setType("text/plain")

        val shareBody = if (vodId == null) {
            "https://twitch.tv/${mUserInfo!!.login}"
        } else {
            "https://www.twitch.tv/${mUserInfo!!.login}/video/$vodId"
        }

        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
        startActivity(Intent.createChooser(sharingIntent, "Share via"))
    }

    private fun profileButtonClicked() {
        mProfileBottomSheet!!.show()
    }

    private fun sleepButtonClicked() {
        if (sleepTimer == null) {
            sleepTimer = SleepTimer(object : SleepTimerDelegate {
                override fun onTimesUp() {
                    stopAudioOnly()
                    player!!.pause()
                }

                override fun onStart(message: String) {
                    showSnackbar(message)
                }

                override fun onStop(message: String) {
                    showSnackbar(message)
                }
            }, requireContext())
        }

        sleepTimer!!.show(requireActivity())
    }

    private fun playbackButtonClicked() {
        DialogService.getPlaybackDialog(requireActivity(), player!!).show()
    }

    private fun showSeekDialog() {
        DialogService.getSeekDialog(requireActivity(), player!!).show()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        optionsMenuItem = menu.findItem(R.id.menu_item_options)
        optionsMenuItem!!.isVisible = chatOnlyViewVisible
        optionsMenuItem!!.setOnMenuItemClickListener { menuItem: MenuItem? ->
            mQualityButton.performClick()
            true
        }

        menu.findItem(R.id.menu_item_playback).isVisible = vodId != null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (!this.isVideoInterfaceShowing) {
            mVideoWrapper.performClick()
            return true
        }

        val itemId = item.itemId
        when (itemId) {
            R.id.menu_item_sleep -> {
                sleepButtonClicked()
                return true
            }

            R.id.menu_item_share -> {
                shareButtonClicked()
                return true
            }

            R.id.menu_item_profile -> {
                profileButtonClicked()
                return true
            }

            R.id.menu_item_external -> {
                playWithExternalPlayer()
                return true
            }

            R.id.menu_item_playback -> {
                playbackButtonClicked()
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun setupLandscapeChat() {
        if (!isChatLandscapeSwipeable || !isChatInLandscapeEnabled) {
            return
        }

        val width: Int = getScreenRect(activity).height()
        val streamFragment = this
        val touchListener: OnTouchListener = object : OnTouchListener {
            private var downPosition = width
            private var widthOnDown = width

            override fun onTouch(view: View?, event: MotionEvent): Boolean {
                if (!isLandscape) {
                    return false
                }

                val x = event.rawX.toInt()
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        // If the user taps while the wrapper is in the resizing animation, cancel it.
                        mVideoWrapper.clearAnimation()

                        val lParams =
                            mVideoWrapper.layoutParams as ConstraintLayout.LayoutParams
                        if (lParams.width > 0) widthOnDown = lParams.width

                        downPosition = event.rawX.toInt()
                    }

                    MotionEvent.ACTION_UP -> {
                        val upPosition = event.rawX.toInt()
                        val deltaPosition = upPosition - downPosition

                        if (abs(deltaPosition.toDouble()) < 20) {
                            setLandscapeChat(landscapeChatVisible)
                            return false
                        }

                        setLandscapeChat(upPosition < downPosition)
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val layoutParams =
                            mVideoWrapper.layoutParams as ConstraintLayout.LayoutParams
                        val newWidth: Int = if (x > downPosition) { // Swiping right
                            widthOnDown + x - downPosition
                        } else { // Swiping left
                            widthOnDown - (downPosition - x)
                        }

                        layoutParams.width = max(
                            min(newWidth.toDouble(), width.toDouble()),
                            (width - streamFragment.landscapeChatTargetWidth).toDouble()
                        ).toInt()

                        mVideoWrapper.setLayoutParams(layoutParams)
                    }
                }
                rootView.invalidate()
                return false
            }
        }

        mOverlay.setOnTouchListener(touchListener)
    }

    private fun setLandscapeChat(visible: Boolean) {
        landscapeChatVisible = visible

        val width: Int = getScreenRect(activity).height()
        val resizeWidthAnimation = ResizeWidthAnimation(
            mVideoWrapper,
            if (visible) width - this.landscapeChatTargetWidth else width
        )
        resizeWidthAnimation.setDuration(250)
        mVideoWrapper.startAnimation(resizeWidthAnimation)
        mShowChatButton.animate().rotation(if (visible) 180f else 0f).start()

        val animator =
            ValueAnimator.ofInt(mVideoInterface.getPaddingRight(), if (visible) 0 else rightInset)
                .setDuration(250)
        animator.addUpdateListener { animator: ValueAnimator ->
            mVideoInterface.setPadding(
                mVideoInterface.getPaddingLeft(), 0, (animator.getAnimatedValue() as Int?)!!, 0
            )
        }
        animator.start()
    }

    private val landscapeChatTargetWidth: Int
        get() = (getScreenRect(activity)
            .height() * (chatLandscapeWidth / 100.0)).toInt()

    private fun initCastingView() {
        castingViewVisible = true
        //auto.setVisibility(View.GONE); // Auto does not work on chromecast
        mVideoView.setVisibility(View.INVISIBLE)
        mBufferingView.visibility = View.GONE
        castingTextView.visibility = View.VISIBLE
        //castingTextView.setText(R.string.stream_chromecast_connecting);
        showVideoInterface()
    }

    private fun disableCastingView() {
        castingViewVisible = false
        //auto.setVisibility(View.VISIBLE);
        mVideoView.setVisibility(View.VISIBLE)
        Service.bringToBack(mPreview)
        mBufferingView.visibility = View.VISIBLE
        castingTextView.visibility = View.INVISIBLE
        showVideoInterface()
    }

    /**
     * Checks if the activity was started with a shared view in high API levels.
     */
    private fun setPreviewAndCheckForSharedTransition() {
        val intent = requireActivity().intent
        if (intent.hasExtra(getString(R.string.stream_preview_url))) {
            val imageUrl = intent.getStringExtra(getString(R.string.stream_preview_url))

            if (imageUrl == null || imageUrl.isEmpty()) {
                return
            }

            Glide.with(requireContext())
                .asBitmap()
                .load(imageUrl)
                .signature(ObjectKey(System.currentTimeMillis() / TimeUnit.MINUTES.toMillis(5))) // Refresh preview images every 5 minutes
                .into(mPreview)
        }

        if (intent.getBooleanExtra(getString(R.string.stream_shared_transition), false)) {
            mPreview.transitionName = getString(R.string.stream_preview_transition)

            val viewsToHide = arrayOf<View>(mToolbar, mControlToolbar)
            for (view in viewsToHide) {
                view.visibility = View.INVISIBLE
            }

            requireActivity().window.enterTransition
                .addListener(object : Transition.TransitionListener {
                    override fun onTransitionEnd(transition: Transition?) {
                        TransitionManager.beginDelayedTransition(
                            mVideoWrapper,
                            Fade()
                                .setDuration(340)
                                .excludeTarget(mVideoView, true)
                                .excludeTarget(mPreview, true)
                        )

                        for (view in viewsToHide) {
                            view.visibility = View.VISIBLE
                        }
                    }

                    override fun onTransitionCancel(transition: Transition?) {
                        onTransitionEnd(transition)
                    }

                    override fun onTransitionStart(transition: Transition?) {
                    }

                    override fun onTransitionPause(transition: Transition?) {
                    }

                    override fun onTransitionResume(transition: Transition?) {
                    }
                })
        }
    }

    /**
     * Call to make sure the UI is shown correctly
     */
    private fun updateUI() {
        setAndroidUiMode()
        keepControlIconsInView()
        setVideoViewLayout()
    }

    /**
     * Sets the System UI visibility so that the status- and navigation bar automatically hides if the app is current in landscape.
     * But they will automatically show when the user swipes the screen.
     */
    private fun setAndroidUiMode() {
        if (activity == null) {
            return
        }

        val window = requireActivity().window
        val windowInsetsController = WindowCompat.getInsetsController(window, rootView)

        WindowCompat.setDecorFitsSystemWindows(window, !isLandscape)

        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (isLandscape) windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        else windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
    }

    private fun setVideoViewLayout() {
        val layoutParams = rootView.layoutParams
        layoutParams.height =
            if (isLandscape) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT

        val layoutWrapper = mVideoWrapper.layoutParams as ConstraintLayout.LayoutParams
        val landscapeLayout = isLandscape && !pictureInPictureEnabled
        if (landscapeLayout) {
            layoutWrapper.width =
                if (!landscapeChatVisible) ConstraintLayout.LayoutParams.MATCH_CONSTRAINT else getScreenRect(
                    activity
                ).height() - this.landscapeChatTargetWidth
            layoutWrapper.height = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
        } else {
            layoutWrapper.width = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
            layoutWrapper.height = ConstraintLayout.LayoutParams.WRAP_CONTENT
        }
        mVideoWrapper.setLayoutParams(layoutWrapper)

        mVideoView.setResizeMode(if (isLandscape) AspectRatioFrameLayout.RESIZE_MODE_FIT else AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH)

        (mVideoView.layoutParams as ConstraintLayout.LayoutParams).dimensionRatio =
            if (landscapeLayout) null else "16:9"
    }

    val isVideoInterfaceShowing: Boolean
        /**
         * Checks if the video interface is fully showing
         */
        get() = mVideoInterface.alpha == 1f

    /**
     * Hides the video control interface with animations
     */
    private fun hideVideoInterface() {
        if (!this.isAudioOnlyModeEnabled && !chatOnlyViewVisible) {
            mVideoInterface.animate().alpha(0f)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .setListener(object : AnimatorListenerAdapter() {
                    // ExoPlayer will hide the UI immediately
                    // Set it visible again for the animation
                    override fun onAnimationStart(animation: Animator) {
                        controlView.visibility = View.VISIBLE
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        controlView.visibility = View.INVISIBLE
                    }
                }).start()
        }
    }

    /**
     * Shows the video control interface with animations
     */
    private fun showVideoInterface() {
        mVideoInterface.animate().alpha(1f).setInterpolator(AccelerateDecelerateInterpolator())
            .setListener(null).start()
    }

    /**
     * Keeps the rightmost icons on the toolbars in view when the device is in landscape.
     * Otherwise the icons would be covered my the navigationbar
     */
    private fun keepControlIconsInView() {
        if (streamPlayerShowNavigationBar) {
            var ctrlPadding = originalCtrlToolbarPadding
            var mainPadding = originalMainToolbarPadding
            val delta = this.navigationBarHeight

            if (isLandscape && !resources.getBoolean(R.bool.isTablet)) {
                ctrlPadding += delta
                mainPadding += delta
            }

            mShowChatButton.setPadding(0, 0, ctrlPadding, 0)
            mToolbar.setPadding(0, 0, mainPadding, 0)
            mControlToolbar.setPadding(0, 0, ctrlPadding, 0)
        }
    }

    private val navigationBarHeight: Int
        /**
         * Returns the height of the navigation bar.
         * If the device doesn't have a navigation bar (Such as Samsung Galaxy devices) the height is 0
         */
        get() {
            val resources = getResources()
            val resourceId =
                resources.getIdentifier("navigation_bar_height", "dimen", "android")
            if (resourceId > 0) {
                return resources.getDimensionPixelSize(resourceId)
            }
            return 0
        }

    /**
     * If the device isn't currently in fullscreen a request is sent to turn the device into landscape.
     * Otherwise if the device is in fullscreen then is releases the lock by requesting for portrait
     */
    fun toggleFullscreen() {
        requireActivity().setRequestedOrientation(if (isLandscape) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)

        // If the user is using auto-rotation, we'll need to unlock the orientation once the device is rotated.
        if (Settings.System.getInt(
                requireContext().contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                0
            ) == 1
        ) {
            orientationListener!!.enable()
        }
    }

    /**
     * Tries playing stream with a quality.
     * If the given quality doesn't exist for the stream the try the next best quality option.
     * If no Quality URLS have yet been created then try to start stream with an aync task.
     */
    private fun startStreamWithQuality(quality: String?) {
        if (qualityURLs == null) {
            startStreamWithTask()
        } else {
            if (qualityURLs!!.containsKey(quality)) {
                if (chatOnlyViewVisible) {
                    return
                }

                playUrl(qualityURLs!![quality]!!.url)
                showQualities()
                updateSelectedQuality(quality)
                showPauseIcon()
                Timber.d("Starting Stream With a quality on $quality for ${mUserInfo!!.displayName}")
                Timber.d("URLS: %s", qualityURLs!!.keys)
            } else if (!qualityURLs!!.isEmpty()) {
                Timber.d("Quality unavailable for this stream -  $quality. Trying next best")
                tryNextBestQuality(quality)
            }
        }
    }

    /**
     * Starts an Async task that fetches all available Stream URLs for a stream,
     * then tries to start stream with the latest user defined quality setting.
     * If no URLs are available for the stream, the user is notified.
     */
    private fun startStreamWithTask() {
        val callback = Consumer { url: MutableMap<String, Quality>? ->
            try {
                if (!url!!.isEmpty()) {
                    updateQualitySelections(url)
                    qualityURLs = url

                    if (!this.isAudioOnlyModeEnabled) {
                        startStreamWithQuality(prefStreamQuality)
                    }
                } else {
                    playbackFailed()
                }
            } catch (e: IllegalStateException) {
                Timber.e(e)
            } catch (e: NullPointerException) {
                Timber.e(e)
            }
        }

        val types = resources.getStringArray(R.array.PlayerType)

        val isClip = requireArguments().containsKey(Constants.KEY_CLIP)
        val task = GetStreamURL(
            mUserInfo!!.login,
            vodId,
            types[streamPlayerType],
            streamPlayerProxy,
            isClip
        )
        Execute.background(task, callback)
    }

    /**
     * Stops the buffering and notifies the user that the stream could not be played
     */
    private fun playbackFailed() {
        if (context == null) return

        showSnackbar(
            getString(if (vodId == null) R.string.stream_playback_failed else R.string.vod_playback_failed),
            getString(R.string.retry)
        ) { v: View? -> startStreamWithTask() }
    }

    private fun showSnackbar(
        message: String,
        actionText: String? = null,
        action: View.OnClickListener? = null
    ) {
        if (activity != null && !isDetached) {
            val mainView = (activity as StreamActivity).mainContentLayout

            if ((snackbar == null || !snackbar!!.isShown) && mainView != null) {
                snackbar = Snackbar.make(mainView, message, 4000)
                if (actionText != null) snackbar!!.setAction(actionText, action)
                snackbar!!.show()
            }
        }
    }

    private fun tryNextBestQuality(quality: String?) {
        if (triesForNextBest < qualityURLs!!.size - 1) { // Subtract 1 as we don't count AUDIO ONLY as a quality
            triesForNextBest++
            val qualityList: MutableList<String> = ArrayList(qualityURLs!!.keys)
            val next = qualityList.indexOf(quality) + 1
            if (next >= qualityList.size - 1) {
                startStreamWithQuality(GetStreamURL.QUALITY_SOURCE)
            } else {
                startStreamWithQuality(qualityList[next])
            }
        } else {
            playbackFailed()
        }
    }

    /**
     * Sets the URL to the VideoView and ChromeCast and starts playback.
     */
    private fun playUrl(url: String?) {
        val extras = Bundle()
        if (vodId == null) {
            extras.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, -1)
        }

        // Grab the preview image from the intent and give it to the media metadata
        val intent = requireActivity().intent
        val uriString = intent.getStringExtra(getString(R.string.stream_preview_url))

        val mediaItem = MediaItem.Builder()
            .setLiveConfiguration(LiveConfiguration.Builder().setTargetOffsetMs(1000).build())
            .setUri(url)
            .setMediaId((if (vodId == null) "" else vodId)!!)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(mUserInfo!!.displayName)
                    .setExtras(extras)
                    .setArtworkUri(uriString?.toUri())
                    .build()
            )
            .build()

        if (vodId != null) {
            var startPosition = com.perflyst.twire.service.Settings.getVodProgress(vodId!!) * 1000L
            // Don't lose the position if we're playing the same VOD
            if (currentMediaItem != null && currentMediaItem!!.mediaId == vodId) {
                startPosition = player!!.getCurrentPosition()
            }

            player!!.setMediaItem(mediaItem, startPosition)
        } else {
            player!!.setMediaItem(mediaItem, false)
        }
        currentMediaItem = mediaItem
        player!!.prepare()

        player!!.play()
    }

    private fun playWithExternalPlayer() {
        if (currentMediaItem!!.localConfiguration == null) {
            Toast.makeText(context, R.string.error_external_playback_failed, Toast.LENGTH_LONG)
                .show()
            return
        }

        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(currentMediaItem!!.localConfiguration!!.uri, "video/*")
        startActivity(Intent.createChooser(intent, getString(R.string.stream_external_play_using)))
    }

    private fun registerAudioOnlyDelegate() {
    }

    private fun updateSelectedQuality(quality: String?) {
        //TODO: Bad design
        if (quality == null) {
            resetQualityViewBackground(null)
        } else {
            resetQualityViewBackground(qualityOptions[quality])
        }
    }

    /**
     * Resets the background color of all the select quality views in the bottom dialog
     */
    private fun resetQualityViewBackground(selected: TextView?) {
        for (v in qualityOptions.values) {
            if (v == selected) {
                v.setBackgroundColor(
                    Service.getColorAttribute(
                        R.attr.navigationDrawerHighlighted,
                        R.color.grey_300,
                        requireContext()
                    )
                )
            } else {
                v.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        com.balysv.materialripple.R.color.transparent
                    )
                )
            }
        }
    }

    /**
     * Adds the available qualities for a stream to the spinner menu
     */
    private fun updateQualitySelections(availableQualities: MutableMap<String, Quality>) {
        for (view in qualityOptions.values) {
            mQualityWrapper.removeView(view.parent as MaterialRippleLayout?)
        }

        for (entry in availableQualities.entries) {
            val quality: Quality = entry.value
            val qualityKey: String = entry.key
            if (qualityKey == "audio_only") continue

            val layout = LayoutInflater.from(context)
                .inflate(R.layout.quality_item, null) as MaterialRippleLayout
            val textView = layout.getChildAt(0) as TextView
            textView.text =
                if (quality.name == "Auto") getString(R.string.quality_auto) else quality.name

            setQualityOnClick(textView, qualityKey)
            qualityOptions.put(qualityKey, textView)
            mQualityWrapper.addView(layout)
        }
    }

    /**
     * Sets an OnClickListener on a select quality view (From bottom dialog).
     * The Listener starts the stream with a new quality setting and updates the background for the select quality views in the bottom dialog
     */
    private fun setQualityOnClick(qualityView: TextView, quality: String) {
        qualityView.setOnClickListener { v: View? ->
            // don´t set audio only mode as default
            if (quality != "audio_only") {
                prefStreamQuality = quality
            }
            // don`t allow to change the Quality when using audio only Mode
            if (!this.isAudioOnlyModeEnabled) {
                startStreamWithQuality(quality)
                resetQualityViewBackground(qualityView)
                mQualityBottomSheet!!.dismiss()
            }
        }
    }

    private fun getDefaultBottomSheetBehaviour(bottomSheetView: View): BottomSheetBehavior<View?> {
        val behavior = BottomSheetBehavior.from(bottomSheetView.parent as View)
        behavior.peekHeight = requireActivity().resources.displayMetrics.heightPixels / 3
        return behavior
    }

    private fun setupProfileBottomSheet() {
        val v = LayoutInflater.from(requireContext()).inflate(R.layout.stream_profile_preview, null)
        mProfileBottomSheet = BottomSheetDialog(requireContext())
        mProfileBottomSheet!!.setContentView(v)
        val behavior = getDefaultBottomSheetBehaviour(v)

        mProfileBottomSheet!!.setOnDismissListener { dialogInterface: DialogInterface? ->
            behavior.setState(
                BottomSheetBehavior.STATE_COLLAPSED
            )
        }

        val mNameView = mProfileBottomSheet!!.findViewById<TextView?>(R.id.twitch_name)
        val mFollowers = mProfileBottomSheet!!.findViewById<TextView?>(R.id.txt_followers)
        val mFollowButton =
            mProfileBottomSheet!!.findViewById<ImageView?>(R.id.follow_unfollow_icon)
        val mFullProfileButton =
            mProfileBottomSheet!!.findViewById<ImageView?>(R.id.full_profile_icon)
        val mPanelsRecyclerView =
            mProfileBottomSheet!!.findViewById<RecyclerView?>(R.id.panel_recyclerview)
        if (mNameView == null || mFollowers == null || mFullProfileButton == null || mPanelsRecyclerView == null) return

        mNameView.text = mUserInfo!!.displayName

        Execute.background(
            { Service.getStreamerInfoFromUserId(mUserInfo!!.userId) },
            { channelInfo: ChannelInfo? ->
                if (channelInfo == null) return@background
                channelInfo.getFollowers({ followers: Int? ->
                    Utils.setNumber(
                        mFollowers,
                        followers!!.toLong()
                    )
                }, 0)
                setupFollowButton(mFollowButton!!, channelInfo)
            })

        mFullProfileButton.setOnClickListener { view: View? ->
            mProfileBottomSheet!!.dismiss()
            val intent = Intent(context, ChannelActivity::class.java)
            intent.putExtra(getString(R.string.channel_info_intent_object), mUserInfo)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        setupPanels(mPanelsRecyclerView)
    }

    private fun setupFollowButton(imageView: ImageView, channelInfo: ChannelInfo) {
        val mFollowHandler = FollowHandler(
            channelInfo,
            context
        ) { imageView.setVisibility(View.GONE) }
        updateFollowIcon(imageView, mFollowHandler.isStreamerFollowed())

        imageView.setOnClickListener { view: View? ->
            val isFollowed = mFollowHandler.isStreamerFollowed()
            if (isFollowed) {
                mFollowHandler.unfollowStreamer()
            } else {
                mFollowHandler.followStreamer()
            }

            val animationDuration = 240

            imageView.animate()
                .setDuration(animationDuration.toLong())
                .alpha(0f)
                .start()
            Handler().postDelayed({
                updateFollowIcon(imageView, !isFollowed)
                imageView.animate().alpha(1f).setDuration(animationDuration.toLong()).start()
            }, animationDuration.toLong())
        }
    }

    private fun updateFollowIcon(imageView: ImageView, isFollowing: Boolean) {
        @DrawableRes val imageRes = if (isFollowing)
            R.drawable.ic_heart_broken
        else
            R.drawable.ic_favorite
        imageView.setImageResource(imageRes)
    }

    private fun setupPanels(recyclerView: RecyclerView) {
        val mPanelAdapter = PanelAdapter(requireActivity())
        recyclerView.setLayoutManager(
            LinearLayoutManager(
                context,
                RecyclerView.VERTICAL,
                false
            )
        )
        recyclerView.setAdapter(mPanelAdapter)

        val mTask = GetPanelsTask(mUserInfo!!.login)
        Execute.background(
            mTask
        ) { panels -> mPanelAdapter.addPanels(panels) }
    }

    /**
     * Setups the Quality Select spinner.
     * Automatically hides the text of the selected Quality
     */
    private fun setupSpinner() {
        mQualityButton.setOnClickListener { v: View? -> mQualityBottomSheet!!.show() }

        val v = LayoutInflater.from(context).inflate(R.layout.stream_settings, null)
        mQualityBottomSheet = BottomSheetDialog(requireContext())
        mQualityBottomSheet!!.setContentView(v)

        val behavior: BottomSheetBehavior<*> = getDefaultBottomSheetBehaviour(v)

        mQualityBottomSheet!!.setOnDismissListener { dialogInterface: DialogInterface? ->
            behavior.setState(
                BottomSheetBehavior.STATE_COLLAPSED
            )
        }

        mQualityWrapper = mQualityBottomSheet!!.findViewById(R.id.quality_wrapper)!!
        mAudioOnlySelector =
            mQualityBottomSheet!!.findViewById(R.id.audio_only_selector)
        mMuteSelector = mQualityBottomSheet!!.findViewById(R.id.mute_selector)
        mChatOnlySelector =
            mQualityBottomSheet!!.findViewById(R.id.chat_only_selector)
        val optionsTitle = mQualityBottomSheet!!.findViewById<TextView?>(R.id.options_text)

        if (optionsTitle != null) {
            optionsTitle.visibility = View.VISIBLE
        }

        if (vodId == null) {
            mChatOnlySelector!!.setVisibility(View.VISIBLE)
        }

        mAudioOnlySelector!!.setVisibility(View.VISIBLE)
        mAudioOnlySelector!!.setOnClickListener { view: View? ->
            mQualityBottomSheet!!.dismiss()
            audioOnlyClicked()
        }

        mMuteSelector!!.setVisibility(View.VISIBLE)
        mMuteSelector!!.setOnClickListener { view: View? ->
            mQualityBottomSheet!!.dismiss()
            muteClicked()
        }

        mChatOnlySelector!!.setOnClickListener { view: View? ->
            mQualityBottomSheet!!.dismiss()
            setChatOnlyView(!chatOnlyViewVisible)
        }
    }

    private fun initAudioOnlyView() {
        if (!this.isAudioOnlyModeEnabled) {
            this.isAudioOnlyModeEnabled = true
            mVideoView.setVisibility(View.INVISIBLE)
            mBufferingView.start()
            //mBufferingView.setVisibility(View.GONE);
            castingTextView.visibility = View.VISIBLE
            castingTextView.setText(R.string.stream_audio_only_active)

            showVideoInterface()
            updateSelectedQuality(null)
            startStreamWithQuality("audio_only")
            hideQualities()
        }
    }

    private fun disableAudioOnlyView() {
        if (this.isAudioOnlyModeEnabled) {
            this.isAudioOnlyModeEnabled = false
            mAudioOnlySelector!!.isChecked = false
            mVideoView.setVisibility(View.VISIBLE)
            mBufferingView.visibility = View.VISIBLE
            Service.bringToBack(mPreview)
            castingTextView.visibility = View.INVISIBLE

            showQualities()
            showVideoInterface()
        }
    }

    private fun audioOnlyClicked() {
        mAudioOnlySelector!!.isChecked = !mAudioOnlySelector!!.isChecked
        if (mAudioOnlySelector!!.isChecked) {
            initAudioOnlyView()
        } else {
            stopAudioOnly()
        }
    }

    private fun muteClicked() {
        mMuteSelector!!.isChecked = !mMuteSelector!!.isChecked
        if (mMuteSelector!!.isChecked) {
            player!!.setVolume(0f)
        } else {
            player!!.setVolume(1f)
        }
    }

    private fun stopAudioOnly() {
        disableAudioOnlyView()

        // start stream with last quality
        releasePlayer()
        initializePlayer()
        updateSelectedQuality(prefStreamQuality)
        startStreamWithQuality(prefStreamQuality)

        // resume the stream
        player!!.play()
    }

    private fun stopAudioOnlyNoServiceCall() {
        disableAudioOnlyView()
    }

    private fun setChatOnlyView(enabled: Boolean) {
        if (chatOnlyViewVisible == enabled) return

        chatOnlyViewVisible = enabled

        mChatOnlySelector!!.isChecked = chatOnlyViewVisible
        if (chatOnlyViewVisible) initChatOnlyView()
        else disableChatOnlyView()

        controlView.setShowTimeoutMs(if (chatOnlyViewVisible) -1 else SHOW_TIMEOUT)
        controlView.show()

        requireView().keepScreenOn = chatOnlyViewVisible
    }

    private fun initChatOnlyView() {
        if (isLandscape) {
            toggleFullscreen()
        }

        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

        videoHeightBeforeChatOnly = mVideoWrapper.height
        val heightAnimation = ResizeHeightAnimation(
            mVideoWrapper,
            resources.getDimension(R.dimen.main_toolbar_height).toInt()
        )
        heightAnimation.interpolator = AccelerateDecelerateInterpolator()
        heightAnimation.setDuration(240)
        mVideoWrapper.startAnimation(heightAnimation)

        mPlayPauseWrapper.visibility = View.GONE
        mControlToolbar.visibility = View.GONE
        mToolbar.setBackgroundColor(
            Service.getColorAttribute(
                androidx.appcompat.R.attr.colorPrimary,
                R.color.primary,
                requireContext()
            )
        )

        releasePlayer()
        if (optionsMenuItem != null) optionsMenuItem!!.isVisible = true

        updateSelectedQuality(null)
        hideQualities()
    }

    private fun disableChatOnlyView() {
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)

        val heightAnimation = ResizeHeightAnimation(mVideoWrapper, videoHeightBeforeChatOnly)
        heightAnimation.interpolator = AccelerateDecelerateInterpolator()
        heightAnimation.setDuration(240)
        heightAnimation.fillAfter = false
        mVideoWrapper.startAnimation(heightAnimation)

        mControlToolbar.visibility = View.VISIBLE
        mPlayPauseWrapper.visibility = View.VISIBLE
        mToolbar.setBackgroundColor(
            Service.getColorAttribute(
                R.attr.streamToolbarColor,
                R.color.black_transparent,
                requireActivity()
            )
        )

        if (!castingViewVisible) {
            initializePlayer()
            startStreamWithQuality(prefStreamQuality)
        }

        optionsMenuItem!!.isVisible = false
    }

    override fun onPictureInPictureModeChanged(enabled: Boolean) {
        mVideoInterface.visibility = if (enabled) View.INVISIBLE else View.VISIBLE
        pictureInPictureEnabled = enabled

        if (!enabled) pipDisabling = true
    }

    /**
     * Setups the toolbar by giving it a bit of extra right padding (To make sure the icons are 16dp from right)
     * Also adds the main toolbar as the support actionbar
     */
    private fun setupToolbar() {
        mToolbar.setPadding(0, 0, Service.dpToPixels(requireActivity(), 5f), 0)
        setHasOptionsMenu(true)
        mActivity!!.setSupportActionBar(mToolbar)
        mToolbar.bringToFront()

        val actionBar = mActivity!!.supportActionBar
        if (actionBar == null) {
            return
        }

        val builder = SpannableStringBuilder()
        builder.append(mUserInfo!!.displayName)
        if (title != null) {
            val secondaryColor =
                ContextCompat.getColor(requireContext(), R.color.white_text_secondary)
            Utils.appendSpan(
                builder,
                "\n$title",
                ForegroundColorSpan(secondaryColor),
                RelativeSizeSpan(0.75f)
            )
        }
        mTitleText.text = builder
        actionBar.title = ""
        actionBar.setDisplayHomeAsUpEnabled(true)
    }

    /**
     * Rotates the Play Pause wrapper with an Rotation Animation.
     */
    private fun rotatePlayPauseWrapper() {
        val rotate = RotateAnimation(
            mPlayPauseWrapper.rotation,
            360f, Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
        val playPauseAnimationDuration = 500
        rotate.setDuration(playPauseAnimationDuration.toLong())
        rotate.interpolator = AccelerateDecelerateInterpolator()
        mPlayPauseWrapper.startAnimation(rotate)
    }

    private fun showPauseIcon() {
        if (mPauseIcon.alpha == 0f) {
            rotatePlayPauseWrapper()
            mPauseIcon.animate().alpha(1f).start()
            mPlayIcon.animate().alpha(0f).start()
        }
    }

    private fun showPlayIcon() {
        if (mPauseIcon.alpha != 0f) {
            rotatePlayPauseWrapper()
            mPauseIcon.animate().alpha(0f).start()
            mPlayIcon.animate().alpha(1f).start()
        }
    }

    private fun showQualities() {
        mQualityWrapper.visibility = View.VISIBLE
    }

    private fun hideQualities() {
        mQualityWrapper.visibility = View.GONE
    }

    interface StreamFragmentListener {
        fun onSeek()

        fun refreshLayout()
    }

    companion object {
        private const val SHOW_TIMEOUT = 3000

        private var rightInset = 0
        private var pipDisabling = false // Tracks the PIP disabling animation.

        @JvmStatic
        fun newInstance(args: Bundle?): StreamFragment {
            val fragment = StreamFragment()
            fragment.setArguments(args)
            return fragment
        }

        /**
         * Gets a Rect representing the usable area of the screen
         *
         * @return A Rect representing the usable area of the screen
         */
        @JvmStatic
        fun getScreenRect(activity: Activity?): Rect {
            if (activity != null) {
                val display = activity.windowManager.defaultDisplay
                val metrics = DisplayMetrics()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity.isInMultiWindowMode && !activity.isInPictureInPictureMode && !pipDisabling) {
                    display.getMetrics(metrics)
                } else {
                    display.getRealMetrics(metrics)
                }

                val width: Int = metrics.widthPixels
                val height: Int = metrics.heightPixels

                return Rect(
                    0,
                    0,
                    min(width.toDouble(), height.toDouble()).toInt(),
                    max(width.toDouble(), height.toDouble()).toInt()
                )
            }

            return Rect()
        }
    }
}
