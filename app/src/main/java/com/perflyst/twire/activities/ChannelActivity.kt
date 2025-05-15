package com.perflyst.twire.activities

import android.animation.Animator
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.github.twitch4j.helix.domain.Clip
import com.github.twitch4j.helix.domain.Video
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.android.material.tabs.TabLayoutMediator
import com.perflyst.twire.R
import com.perflyst.twire.TwireApplication
import com.perflyst.twire.activities.main.LazyFetchingActivity
import com.perflyst.twire.adapters.ClipAdapter
import com.perflyst.twire.adapters.PanelAdapter
import com.perflyst.twire.adapters.VODAdapter
import com.perflyst.twire.fragments.ChatFragment.Companion.getInstance
import com.perflyst.twire.misc.FollowHandler
import com.perflyst.twire.misc.LazyFetchingOnScrollListener
import com.perflyst.twire.misc.Utils
import com.perflyst.twire.model.ChannelInfo
import com.perflyst.twire.model.Panel
import com.perflyst.twire.model.VideoOnDemand
import com.perflyst.twire.service.Service
import com.perflyst.twire.service.Settings.isDarkTheme
import com.perflyst.twire.tasks.GetPanelsTask
import com.perflyst.twire.utils.Execute
import com.perflyst.twire.views.recyclerviews.AutoSpanRecyclerView
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.VODAutoSpanBehaviour
import com.rey.material.widget.ProgressView
import java.util.Locale

class ChannelActivity : ThemeActivity() {
    private val showFabDelay = 300
    private lateinit var info: ChannelInfo
    private var streamerImage: ImageView? = null
    private var toolbar: Toolbar? = null
    private var additionalToolbar: Toolbar? = null
    private var mViewPager2: ViewPager2? = null
    private var mTabLayout: TabLayout? = null
    private var mAppBar: AppBarLayout? = null
    private var mFab: FloatingActionButton? = null
    private var colorFadeDuration = 0
    private var mFollowHandler: FollowHandler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_streamer_info)

        // Get the various handles of view and layouts that is part of this view
        streamerImage = findViewById(R.id.profileImageView)
        val streamerInfoName = findViewById<TextView>(R.id.twitch_name)
        val streamerFollowers = findViewById<TextView>(R.id.txt_followers)
        toolbar = findViewById(R.id.StreamerInfo_Toolbar)
        additionalToolbar = findViewById(R.id.additional_toolbar)
        mViewPager2 = findViewById(R.id.streamer_info_viewPager2)
        mTabLayout = findViewById(R.id.streamer_info_tabLayout)
        mAppBar = findViewById(R.id.appbar)
        mFab = findViewById(R.id.fab)

        toolbar!!.setTitle("")

        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }

        // Get the StreamerInfo object sent with the intent to open this activity
        val intent = getIntent()
        info =
            intent.getParcelableExtra(getString(R.string.channel_info_intent_object))!!
        checkNotNull(info)

        streamerInfoName.text = info.displayName
        info.getFollowers({ followers: Int? ->
            Utils.setNumber(
                streamerFollowers,
                followers!!.toLong()
            )
        }, 0)
        streamerImage!!.transitionName = getString(R.string.streamerInfo_transition)
        setUpTabs()
        initStreamerImageAndColors()
        initiateFAB()
    }

    public override fun onStart() {
        super.onStart()
        overridePendingTransition(R.anim.slide_in_bottom_anim, R.anim.fade_out_semi_anim)
    }

    override fun onResume() {
        colorFadeDuration = 800
        initiateFAB()
        super.onResume()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_streamer_info, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        onBackPressed()
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        this.overridePendingTransition(R.anim.fade_in_semi_anim, R.anim.slide_out_bottom_anim)
    }

    private fun setUpTabs() {
        mViewPager2!!.setAdapter(ChannelStateAdapter(this))

        val tabTitles = intArrayOf(
            R.string.streamerInfo_desc_tab,
            R.string.streamerInfo_broadcasts_tab,
            R.string.streamerInfo_highlights_tab,
            R.string.streamerInfo_clips_tab,
            R.string.streamerInfo_chat_tab
        )
        TabLayoutMediator(
            mTabLayout!!,
            mViewPager2!!
        ) { tab: TabLayout.Tab?, position: Int -> tab!!.setText(tabTitles[position]) }.attach()

        mTabLayout!!.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                mAppBar!!.setExpanded(
                    tab.text != null &&
                            tab.text == getString(R.string.streamerInfo_desc_tab), true
                )
                mViewPager2!!.setCurrentItem(tab.position, true)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })
    }

    private fun initStreamerImageAndColors() {
        var mTarget =
            this.lightThemeTarget
        if (isDarkTheme) {
            mTarget = this.nightThemeTarget
        }

        Glide.with(baseContext)
            .asBitmap()
            .load(info.logoURL.toString())
            .into<Target<Bitmap?>?>(mTarget)
    }

    private val nightThemeTarget: Target<Bitmap?>
        get() = object : CustomTarget<Bitmap?>() {
            override fun onResourceReady(
                bitmap: Bitmap,
                transition: Transition<in Bitmap?>?
            ) {
                val drawable =
                    RoundedBitmapDrawableFactory.create(getResources(), bitmap)
                drawable.isCircular = true
                streamerImage!!.setImageDrawable(drawable)
            }

            override fun onLoadCleared(placeholder: Drawable?) {
            }
        }

    private val lightThemeTarget: Target<Bitmap?>
        get() = object : CustomTarget<Bitmap?>() {
            override fun onResourceReady(
                bitmap: Bitmap,
                transition: Transition<in Bitmap?>?
            ) {
                val drawable =
                    RoundedBitmapDrawableFactory.create(getResources(), bitmap)
                drawable.isCircular = true
                streamerImage!!.setImageDrawable(drawable)

                val palette = Palette.from(bitmap).generate()
                val defaultColor = Service.getColorAttribute(
                    androidx.appcompat.R.attr.colorPrimary,
                    R.color.primary,
                    baseContext
                )
                val defaultDarkColor = Service.getColorAttribute(
                    androidx.appcompat.R.attr.colorPrimaryDark,
                    R.color.primaryDark,
                    baseContext
                )

                val vibrant = palette.getVibrantColor(defaultColor)
                val vibrantDark = palette.getDarkVibrantColor(defaultColor)
                val vibrantLight = palette.getLightVibrantColor(defaultColor)

                val muted = palette.getMutedColor(defaultColor)
                val mutedDark = palette.getDarkMutedColor(defaultColor)

                val swatch = if (vibrant != defaultColor) {
                    palette.vibrantSwatch
                } else if (vibrantDark != defaultColor) {
                    palette.darkVibrantSwatch
                } else if (vibrantLight != defaultColor) {
                    palette.lightVibrantSwatch
                } else if (muted != defaultColor) {
                    palette.mutedSwatch
                } else if (mutedDark != defaultColor) {
                    palette.darkMutedSwatch
                } else {
                    palette.lightMutedSwatch
                }

                if (swatch != null) {
                    val swatchValues = swatch.getHsl()
                    val newSwatch =
                        floatArrayOf(swatchValues[0], 0.85f, 0.85f)
                    val newSwatchComposite = floatArrayOf(
                        (swatchValues[0] + 180) % 360,
                        newSwatch[1],
                        newSwatch[2]
                    )
                    val newSwatchDark =
                        floatArrayOf(newSwatch[0], newSwatch[1], 0.6f)

                    val newColorDark = Color.HSVToColor(newSwatchDark)
                    val newColor = Color.HSVToColor(newSwatch)
                    val compositeNewColor =
                        Color.HSVToColor(newSwatchComposite)

                    val primaryColor =
                        Service.getBackgroundColorFromView(
                            toolbar,
                            defaultColor
                        )
                    val primaryColorDark =
                        Service.getBackgroundColorFromView(
                            mTabLayout,
                            defaultDarkColor
                        )

                    Service.animateBackgroundColorChange(
                        toolbar,
                        newColor,
                        primaryColor,
                        colorFadeDuration
                    )
                    Service.animateBackgroundColorChange(
                        additionalToolbar,
                        newColor,
                        primaryColor,
                        colorFadeDuration
                    )
                    Service.animateBackgroundColorChange(
                        mTabLayout,
                        newColorDark,
                        primaryColorDark,
                        colorFadeDuration
                    )
                    mFab!!.backgroundTintList = ColorStateList.valueOf(compositeNewColor)
                    mTabLayout!!.setSelectedTabIndicatorColor(compositeNewColor)

                    val window = getWindow()
                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                    window.statusBarColor = newColorDark
                }
            }

            override fun onLoadCleared(placeholder: Drawable?) {
            }
        }

    private fun initiateFAB() {
        mFollowHandler = FollowHandler(
            info,
            baseContext,
            (FollowHandler.Delegate { mFab!!.hide() })
        )

        // If the channel got imported from Twitch, then hide the Follow/Unfollow Button
        if (mFollowHandler!!.isStreamerTwitch()) {
            hideFAB()
        } else {
            mFab!!.setOnClickListener { v: View? ->
                if (mFollowHandler!!.isStreamerFollowed()) {
                    mFollowHandler!!.unfollowStreamer()
                } else {
                    mFollowHandler!!.followStreamer()
                }
                hideFAB()
                Handler().postDelayed({
                    updateFABIcon(mFollowHandler!!.isStreamerFollowed())
                    showFAB()
                }, showFabDelay.toLong())
            }
            updateFABIcon(mFollowHandler!!.isStreamerFollowed())
        }
    }

    private fun updateFABIcon(isFollowing: Boolean) {
        @DrawableRes val imageRes = if (isFollowing)
            R.drawable.ic_heart_broken
        else
            R.drawable.ic_favorite
        mFab!!.setImageResource(imageRes)
    }

    private fun hideFAB() {
        val hideFabDuration = 200
        mFab!!.animate()
            .translationY(
                getResources().getDimension(R.dimen.streamerInfo_fab_size) + getResources().getDimension(
                    R.dimen.streamerInfo_fab_margin
                )
            )
            .setDuration(hideFabDuration.toLong())
            .setInterpolator(AccelerateInterpolator())
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                    mFab!!.isClickable = false
                }

                override fun onAnimationEnd(animation: Animator) {
                    mFab!!.visibility = View.INVISIBLE
                }

                override fun onAnimationCancel(animation: Animator) {
                }

                override fun onAnimationRepeat(animation: Animator) {
                }
            })
            .start()
    }

    private fun showFAB() {
        val showFabDuration = 300
        mFab!!.animate()
            .translationY(0f)
            .setDuration(showFabDuration.toLong())
            .setInterpolator(OvershootInterpolator())
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                    mFab!!.visibility = View.VISIBLE
                }

                override fun onAnimationEnd(animation: Animator) {
                    mFab!!.isClickable = true
                }

                override fun onAnimationCancel(animation: Animator) {
                }

                override fun onAnimationRepeat(animation: Animator) {
                }
            })
            .start()
    }

    abstract class ChannelFragment : Fragment() {
        var mErrorEmote: TextView? = null
        var mErrorText: TextView? = null

        fun findErrorView(rootView: View) {
            mErrorEmote = rootView.findViewById(R.id.emote_error_view)
            mErrorText = rootView.findViewById(R.id.error_view)
        }

        protected open fun showError() {
            mErrorEmote!!.visibility = View.VISIBLE
            mErrorText!!.visibility = View.VISIBLE
            mErrorEmote!!.text = Service.errorEmote
        }
    }

    class InfoFragment : ChannelFragment() {
        private var info: ChannelInfo? = null

        private var mPanelsRecyclerView: RecyclerView? = null

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            val rootView = inflater.inflate(R.layout.fragment_channel_description, container, false)

            if (arguments != null) {
                info = requireArguments().getParcelable(FRAGMENT_STREAMER_INFO_ARG)
            }

            mPanelsRecyclerView = rootView.findViewById(R.id.panel_recyclerview)
            val mDescription = rootView.findViewById<TextView>(R.id.description)
            findErrorView(rootView)

            if (info != null && info!!.streamDescription != null && (info!!.streamDescription != "null") && !info!!.streamDescription!!.isEmpty()) {
                mDescription.text = info!!.streamDescription
            } else {
                showError()
            }

            setupPanels()

            return rootView
        }

        private fun setupPanels() {
            val mPanelsAdapter = PanelAdapter(requireActivity())
            val llm = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            mPanelsRecyclerView!!.setAdapter(mPanelsAdapter)
            mPanelsRecyclerView!!.setLayoutManager(llm)

            val mTask = GetPanelsTask(info!!.login)
            Execute.background(
                mTask
            ) { panels: MutableList<Panel> ->
                mPanelsAdapter.addPanels(panels)
            }
        }

        companion object {
            fun newInstance(info: ChannelInfo?): InfoFragment {
                val fragment = InfoFragment()
                val args = Bundle()
                args.putParcelable(FRAGMENT_STREAMER_INFO_ARG, info)
                fragment.setArguments(args)
                return fragment
            }
        }
    }

    class VodFragment : ChannelFragment(), LazyFetchingActivity<VideoOnDemand> {
        private lateinit var mRecyclerView: AutoSpanRecyclerView
        private var mAdapter: VODAdapter? = null
        private var channelInfo: ChannelInfo? = null
        private var broadcasts = false
        private var showError = false
        override var limit = 20
        override var maxElementsToFetch = 500
        private var progressView: ProgressView? = null

        override fun showError() {
            super.showError()
            mErrorText!!.text = getString(
                R.string.no_elements_added_notice,
                getString(R.string.vods)
            )
        }

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            val rootView = inflater.inflate(R.layout.fragment_channel_vods, container, false)

            val args = arguments
            if (args != null) {
                channelInfo = args.getParcelable(FRAGMENT_VODS_STREAMER_INFO_ARG)
                broadcasts = args.getBoolean(FRAGMENT_VODS_BROAD_CASTS_ONLY_ARG)
            }

            mRecyclerView = rootView.findViewById(R.id.recyclerview_vods)
            progressView = rootView.findViewById(R.id.circle_progress)

            findErrorView(rootView)
            if (showError) {
                showError()
            }

            if (mAdapter == null) {
                mRecyclerView.setBehaviour(VODAutoSpanBehaviour())
                mAdapter = VODAdapter(mRecyclerView, requireActivity())
                mAdapter!!.setShowName(false)
                progressView!!.start()
            }

            mAdapter!!.topMarginFirst =
                resources.getDimension(R.dimen.search_new_adapter_top_margin).toInt()
            mAdapter!!.setSortElements(false)
            mAdapter!!.disableInsertAnimation()
            val lazyFetchingOnScrollListener = LazyFetchingOnScrollListener<VideoOnDemand>(this)
            mRecyclerView.addOnScrollListener(lazyFetchingOnScrollListener)
            mRecyclerView.setAdapter(mAdapter)
            mRecyclerView.setItemAnimator(null)
            mRecyclerView.setHasFixedSize(true)

            lazyFetchingOnScrollListener.checkForNewElements(mRecyclerView)

            return rootView
        }

        override var cursor: String? = ""

        override fun addToAdapter(aObjectList: MutableList<VideoOnDemand>) {
            mAdapter!!.addList(aObjectList)
        }

        override fun startRefreshing() {
        }

        override fun stopRefreshing() {
        }

        override fun startProgress() {
        }

        override fun stopProgress() {
            progressView!!.stop()
        }

        override fun notifyUserNoElementsAdded() {
            if (mAdapter!!.itemCount > 0) return

            Execute.ui {
                if (mErrorEmote != null && mErrorText != null) {
                    showError()
                    showError = true
                }
            }
        }

        override val visualElements: MutableList<VideoOnDemand>
            get() {
                val result: MutableList<VideoOnDemand> = ArrayList()

                val response = TwireApplication.helix.getVideos(
                    null,
                    null,
                    channelInfo!!.userId,
                    null,
                    null,
                    null,
                    null,
                    if (broadcasts) Video.Type.ARCHIVE else Video.Type.HIGHLIGHT,
                    null,
                    cursor,
                    null
                ).execute()
                for (video in response.videos) {
                    val vod = VideoOnDemand(video)
                    vod.channelInfo = channelInfo
                    vod.isBroadcast = broadcasts
                    result.add(vod)
                }

                cursor = response.pagination.cursor

                return result
            }

        companion object {
            @JvmStatic
            fun newInstance(broadcastsOnly: Boolean, channelInfo: ChannelInfo?): VodFragment {
                val fragment = VodFragment()
                val args = Bundle()
                args.putParcelable(FRAGMENT_VODS_STREAMER_INFO_ARG, channelInfo)
                args.putBoolean(FRAGMENT_VODS_BROAD_CASTS_ONLY_ARG, broadcastsOnly)
                fragment.setArguments(args)
                return fragment
            }
        }
    }


    class ClipFragment : ChannelFragment(), LazyFetchingActivity<Clip> {
        private var mRecyclerView: AutoSpanRecyclerView? = null
        private var mAdapter: ClipAdapter? = null
        private var channelInfo: ChannelInfo? = null
        private var showError = false
        override var limit = 20
        override var maxElementsToFetch = 500
        private var progressView: ProgressView? = null

        override fun showError() {
            super.showError()
            mErrorText!!.text = getString(
                R.string.no_elements_added_notice,
                getString(R.string.streamerInfo_clips_tab).lowercase(
                    Locale.getDefault()
                )
            )
        }

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            val rootView = inflater.inflate(R.layout.fragment_channel_vods, container, false)

            val args = arguments
            if (args != null) {
                channelInfo = args.getParcelable(FRAGMENT_VODS_STREAMER_INFO_ARG)
            }

            mRecyclerView = rootView.findViewById(R.id.recyclerview_vods)
            progressView = rootView.findViewById(R.id.circle_progress)

            findErrorView(rootView)
            if (showError) {
                showError()
            }

            if (mAdapter == null) {
                mRecyclerView!!.setBehaviour(VODAutoSpanBehaviour())
                mAdapter = ClipAdapter(mRecyclerView!!, requireActivity())
                mAdapter!!.setShowName(false)
                progressView!!.start()
            }

            mAdapter!!.topMarginFirst =
                resources.getDimension(R.dimen.search_new_adapter_top_margin).toInt()
            mAdapter!!.setSortElements(false)
            mAdapter!!.disableInsertAnimation()
            val lazyFetchingOnScrollListener = LazyFetchingOnScrollListener<Clip>(this)
            mRecyclerView!!.addOnScrollListener(lazyFetchingOnScrollListener)
            mRecyclerView!!.setAdapter(mAdapter)
            mRecyclerView!!.setItemAnimator(null)
            mRecyclerView!!.setHasFixedSize(true)

            lazyFetchingOnScrollListener.checkForNewElements(mRecyclerView!!)

            return rootView
        }

        override var cursor: String? = ""

        override fun addToAdapter(aObjectList: MutableList<Clip>) {
            mAdapter!!.addList(aObjectList)
        }

        override fun startRefreshing() {
        }

        override fun stopRefreshing() {
        }

        override fun startProgress() {
        }

        override fun stopProgress() {
            progressView!!.stop()
        }

        override fun notifyUserNoElementsAdded() {
            if (mAdapter!!.itemCount > 0) return

            Execute.ui {
                if (mErrorEmote != null && mErrorText != null) {
                    showError()
                    showError = true
                }
            }
        }

        override val visualElements: MutableList<Clip>
            get() {
                val response = TwireApplication.helix.getClips(
                    null,
                    channelInfo!!.userId,
                    null,
                    null,
                    cursor,
                    null,
                    limit,
                    null,
                    null,
                    null
                ).execute()
                cursor = response.pagination.cursor

                return response.data
            }

        companion object {
            fun newInstance(channelInfo: ChannelInfo?): ClipFragment {
                val fragment = ClipFragment()
                val args = Bundle()
                args.putParcelable(FRAGMENT_VODS_STREAMER_INFO_ARG, channelInfo)
                fragment.setArguments(args)
                return fragment
            }
        }
    }

    private inner class ChannelStateAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        private val tabFragments: Array<Fragment>

        init {
            val mDescriptionFragment: ChannelFragment = InfoFragment.Companion.newInstance(info)
            val mBroadcastsFragment: ChannelFragment = VodFragment.Companion.newInstance(true, info)
            val mHighlightsFragment: ChannelFragment =
                VodFragment.Companion.newInstance(false, info)
            val mClipsFragment: ChannelFragment = ClipFragment.Companion.newInstance(info)

            val chatBundle = Bundle()
            chatBundle.putParcelable(getString(R.string.stream_fragment_streamerInfo), info)
            val mChatFragment = getInstance(chatBundle)

            tabFragments = arrayOf(
                mDescriptionFragment,
                mBroadcastsFragment,
                mHighlightsFragment,
                mClipsFragment,
                mChatFragment
            )
        }

        override fun createFragment(position: Int): Fragment {
            return tabFragments[position]
        }

        override fun getItemCount(): Int {
            return tabFragments.size
        }
    }

    companion object {
        private const val FRAGMENT_STREAMER_INFO_ARG = "streamerInfoArg"
        private const val FRAGMENT_VODS_BROAD_CASTS_ONLY_ARG = "vodsBroadcastsOnlyArg"
        private const val FRAGMENT_VODS_STREAMER_INFO_ARG = "streamerNameArg"
    }
}
