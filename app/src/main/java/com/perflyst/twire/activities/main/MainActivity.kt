package com.perflyst.twire.activities.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.animation.Animation
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.insets.GradientProtection
import androidx.core.view.insets.Protection
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.perflyst.twire.BuildConfig
import com.perflyst.twire.R
import com.perflyst.twire.activities.ThemeActivity
import com.perflyst.twire.adapters.MainActivityAdapter
import com.perflyst.twire.adapters.StreamsAdapter
import com.perflyst.twire.databinding.ActivityMainBinding
import com.perflyst.twire.fragments.ChangelogDialogFragment
import com.perflyst.twire.fragments.NavigationDrawerFragment
import com.perflyst.twire.misc.TooltipWindow
import com.perflyst.twire.misc.UniversalOnScrollListener
import com.perflyst.twire.service.AnimationService
import com.perflyst.twire.service.Service
import com.perflyst.twire.service.Settings.isTipsShown
import com.perflyst.twire.service.Settings.lastVersionCode
import com.perflyst.twire.service.Settings.showChangelogs
import com.perflyst.twire.tasks.ScrollToStartPositionTask
import com.perflyst.twire.utils.AnimationListenerAdapter
import com.perflyst.twire.utils.Execute
import com.perflyst.twire.views.recyclerviews.AutoSpanRecyclerView
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.AutoSpanBehaviour
import dev.chrisbanes.insetter.Insetter
import dev.chrisbanes.insetter.Side.LEFT
import dev.chrisbanes.insetter.Side.RIGHT
import dev.chrisbanes.insetter.ViewState
import timber.log.Timber
import kotlin.math.abs

abstract class MainActivity<E> : ThemeActivity() {
    private lateinit var binding: ActivityMainBinding

    protected lateinit var mProgressView: LinearProgressIndicator

    protected lateinit var mSwipeRefreshLayout: SwipeRefreshLayout

    protected lateinit var mRecyclerView: AutoSpanRecyclerView

    protected lateinit var mToolbarShadow: View

    protected lateinit var mCircleIconWrapper: View

    protected lateinit var mTitleView: TextView

    protected lateinit var mErrorView: TextView

    protected lateinit var mErrorEmoteView: TextView

    protected lateinit var mMainToolbar: Toolbar

    protected lateinit var mDecorativeToolbar: Toolbar
    protected lateinit var mAdapter: MainActivityAdapter<E, *>
    protected var mDrawerFragment: NavigationDrawerFragment? = null
    protected lateinit var mScrollListener: UniversalOnScrollListener
    protected var mTooltip: TooltipWindow? = null

    // The position of the toolbars for the activity that started the transition to this activity
    private var fromToolbarPosition = 0f
    private var fromMainToolbarPosition = 0f
    private var isTransitioned = false

    /**
     * Refreshes the content of the activity
     */
    abstract fun refreshElements()

    /**
     * Construct the adapter used for this activity's list
     */
    protected abstract fun constructAdapter(recyclerView: AutoSpanRecyclerView): MainActivityAdapter<E, *>

    /**
     * Get the drawable resource int used to represent this activity
     *
     * @return the resource int
     */
    protected abstract val activityIconRes: Int

    /***
     * Get the string resource int used for the title of this activity
     * @return the resource int
     */
    protected abstract val activityTitleRes: Int

    /***
     * Construct the AutoSpanBehaviour used for this main activity's AutoSpanRecyclerView
     */
    protected abstract fun constructSpanBehaviour(): AutoSpanBehaviour?

    /**
     * Allows the child class to specialize the functionality in the onCreate method, although it is not required.
     */
    protected open fun customizeActivity() {
    }

    override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.getRoot())

        mProgressView = binding.progressView
        mSwipeRefreshLayout = binding.swipeContainer
        mRecyclerView = binding.mainList
        mToolbarShadow = binding.toolbarShadow
        mCircleIconWrapper = binding.iconContainer
        mTitleView = binding.txtTitle
        mErrorView = binding.errorView
        mErrorEmoteView = binding.emoteErrorView
        mMainToolbar = binding.mainToolbar
        mDecorativeToolbar = binding.mainDecorativeToolbar

        mDrawerFragment =
            supportFragmentManager.findFragmentById(R.id.drawer_fragment) as NavigationDrawerFragment?

        initErrorView()
        initTitleAndIcon()

        setSupportActionBar(mMainToolbar)
        if (supportActionBar != null) supportActionBar!!.title = ""
        mMainToolbar.setPadding(
            0,
            0,
            Service.dpToPixels(baseContext, 5f),
            0
        ) // to make sure the cast icon is aligned 16 dp from the right edge.
        mMainToolbar.bringToFront()
        mMainToolbar.setBackgroundColor(
            ContextCompat.getColor(
                this,
                com.balysv.materialripple.R.color.transparent
            )
        )
        mToolbarShadow.bringToFront()
        mToolbarShadow.setAlpha(0f)
        mTitleView.bringToFront()

        // Setup Drawer Fragment
        mDrawerFragment!!.setUp(
            findViewById(R.id.followed_channels_drawer_layout),
            mMainToolbar
        )

        // Set up the RecyclerView
        mRecyclerView.setBehaviour(constructSpanBehaviour())
        mAdapter = constructAdapter(mRecyclerView)
        mRecyclerView.setAdapter(mAdapter)
        mRecyclerView.setItemAnimator(null) // We want to implement our own animations
        mRecyclerView.setHasFixedSize(true)
        mScrollListener = UniversalOnScrollListener(
            this,
            mMainToolbar,
            mDecorativeToolbar,
            mToolbarShadow,
            mCircleIconWrapper,
            mTitleView,
            true
        )
        mRecyclerView.addOnScrollListener(mScrollListener)

        // Only animate when the view is first started, not when screen rotates
        if (savedInstance == null) {
            mTitleView.setAlpha(0f)
            initActivityAnimation()
        }

        Service.increaseNavigationDrawerEdge(binding.followedChannelsDrawerLayout)

        checkForTip()
        checkForUpdate()
        customizeActivity()

        // Handle insets
        Insetter.builder()
            .padding(WindowInsetsCompat.Type.displayCutout(), LEFT or RIGHT)
            .applyToView(binding.mainContent)
            .applyToView(binding.drawerFragment)

        Insetter.builder()
            .paddingTop(WindowInsetsCompat.Type.systemBars(), false)
            .applyToView(binding.mainToolbar)
            .applyToView(binding.mainList)

        Insetter.builder()
            .setOnApplyInsetsListener { view: View?, windowInsets: WindowInsetsCompat?, initialState: ViewState? ->
                val insets = windowInsets!!.getInsets(WindowInsetsCompat.Type.systemBars())
                binding.mainDecorativeToolbar.setMinimumHeight(
                    getResources().getDimension(R.dimen.additional_toolbar_height)
                        .toInt() + insets.top
                )
            }.applyToView(binding.getRoot())

        Insetter.builder()
            .marginTop(WindowInsetsCompat.Type.systemBars(), false)
            .applyToView(binding.iconContainer)

        binding.listProtection.setProtections(
            listOf<Protection?>(
                GradientProtection(
                    WindowInsetsCompat.Side.TOP,
                    Service.getColorAttribute(
                        androidx.appcompat.R.attr.colorPrimary,
                        R.color.primary,
                        this
                    )
                )
            )
        )
    }

    override fun onResume() {
        super.onResume()
        checkIsBackFromMainActivity()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (hasFocus) {
            // Check if the user has changed any size or style setting.
            if (!checkElementStyleChange()) {
                checkElementSizeChange()
            }
        }
    }

    override fun onDestroy() {
        if (mTooltip != null && mTooltip!!.isTooltipShown) {
            mTooltip!!.dismissTooltip()
        }

        super.onDestroy()
    }

    override fun onBackPressed() {
        val isFromOtherMain = handleBackPressed()

        // If this activity was not started from another main activity, then just use the usual onBackPressed.
        if (!isFromOtherMain) {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_main_activity, menu)

        return true
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        if (mAdapter.itemCount >= 0) {
            mRecyclerView.scrollToPosition(0)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val firstVisibleElement =
            if (mRecyclerView.manager.findFirstCompletelyVisibleItemPosition() == 0)
                mRecyclerView.manager.findFirstVisibleItemPosition()
            else
                mRecyclerView.manager.findFirstCompletelyVisibleItemPosition()

        outState.putInt(FIRST_VISIBLE_ELEMENT_POSITION, firstVisibleElement)
        super.onSaveInstanceState(outState)
    }

    /**
     * Initializes the error view, but does NOT find the view with ID's
     */
    protected fun initErrorView() {
        mErrorEmoteView.text = Service.errorEmote
        mErrorEmoteView.setAlpha(0f)
        mErrorView.setAlpha(0f)
    }

    /***
     * Set the title and icon that is used to identify this activity and the content of its list
     */
    protected fun initTitleAndIcon() {
        binding.imgIcon.setImageResource(this.activityIconRes)
        //binding.imgIcon.setImageDrawable(getResources().getDrawable());
        mTitleView.setText(this.activityTitleRes)
    }

    /**
     * Scrolls to the top of the recyclerview. When the position is reached refreshElements() is called
     */
    fun scrollToTopAndRefresh() {
        val scrollTask = ScrollToStartPositionTask(mRecyclerView, mScrollListener)
        Execute.background(scrollTask) { ignore: Void? ->
            refreshElements()
        }
    }

    /**
     * Shows the error views with an alpha animation
     */
    fun showErrorView() {
        mErrorEmoteView.visibility = View.VISIBLE
        mErrorView.visibility = View.VISIBLE

        mErrorEmoteView.animate().alpha(1f).start()
        mErrorView.animate().alpha(1f).start()
    }

    /**
     * Hide the error views with an alpha animation
     */
    fun hideErrorView() {
        mErrorEmoteView.animate().alpha(0f).start()
        mErrorView.animate().alpha(0f).start()
    }

    /**
     * Check if usability Tips should be shown to the user
     */
    private fun checkForTip() {
        if (!isTipsShown) {
            try {
                mTooltip = TooltipWindow(this, TooltipWindow.POSITION_TO_RIGHT)
                mMainToolbar.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
                    override fun onLayoutChange(
                        v: View,
                        left: Int,
                        top: Int,
                        right: Int,
                        bottom: Int,
                        oldLeft: Int,
                        oldTop: Int,
                        oldRight: Int,
                        oldBottom: Int
                    ) {
                        v.removeOnLayoutChangeListener(this)

                        if (!mTooltip!!.isTooltipShown) {
                            val anchor: View? = Service.getNavButtonView(mMainToolbar)
                            anchor?.addOnLayoutChangeListener { v1: View?, left1: Int, top1: Int, right1: Int, bottom1: Int, oldLeft1: Int, oldTop1: Int, oldRight1: Int, oldBottom1: Int ->
                                mTooltip!!.showToolTip(
                                    anchor,
                                    getString(R.string.tip_navigate)
                                )
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                Timber.e(e, "Failed to Show ToolTip")
            }
        }
    }

    private fun checkForUpdate() {
        val versionCode = BuildConfig.VERSION_CODE

        if (lastVersionCode != versionCode && showChangelogs) {
            ChangelogDialogFragment().show(supportFragmentManager, "ChangelogDialog")
        }
    }

    /**
     * Checks if the user has changed the element style of this adapter type.
     * If it has Update the adapter element style and refresh the elements.
     */
    fun checkElementStyleChange(): Boolean {
        val currentAdapterStyle = mAdapter.elementStyle
        val actualStyle = mAdapter.initElementStyle()

        if (currentAdapterStyle != actualStyle) {
            mAdapter.elementStyle = mAdapter.initElementStyle()
            scrollToTopAndRefresh()
            return true
        } else {
            return false
        }
    }

    fun checkElementSizeChange() {
        if (mRecyclerView.hasSizedChanged()) {
            scrollToTopAndRefresh()
        }
    }


    val recyclerView: RecyclerView
        /**
         * Returns the activity's recyclerview.
         *
         * @return The Recyclerview
         */
        get() = mRecyclerView

    /**
     * Decides which animation to run based the intent that started the activity.
     */
    fun initActivityAnimation() {
        val intent = this.intent
        fromToolbarPosition = intent.getFloatExtra(
            getString(R.string.decorative_toolbar_position_y), -1f
        )

        fromMainToolbarPosition = intent.getFloatExtra(
            getString(R.string.main_toolbar_position_y), -1f
        )

        // If the position is equal to the default value,
        // then the intent was not put into from another MainActivity
        if (fromToolbarPosition != -1f) {
            AnimationService.setActivityToolbarReset(
                mMainToolbar,
                mDecorativeToolbar,
                this,
                fromToolbarPosition,
                fromMainToolbarPosition
            )
        } else {
            AnimationService.setActivityToolbarCircularRevealAnimation(mDecorativeToolbar)
        }

        AnimationService.setActivityIconRevealAnimation(mCircleIconWrapper, mTitleView)
    }

    /**
     * Starts the transition animation to another Main Activity. The method takes an intent where the final result activity has been set.
     * The method puts extra necessary information on the intent before it is started.
     *
     * @param aIntent Intent containing the destination Activity
     */
    fun transitionToOtherMainActivity(aIntent: Intent) {
        hideErrorView()
        val manager = mRecyclerView.layoutManager as GridLayoutManager?
        if (manager == null) return
        val firstVisibleItemPosition = manager.findFirstVisibleItemPosition()
        val lastVisibleItemPosition = manager.findLastVisibleItemPosition()

        aIntent.putExtra(
            getString(R.string.decorative_toolbar_position_y),
            mDecorativeToolbar.translationY
        )

        aIntent.putExtra(
            getString(R.string.main_toolbar_position_y),
            mMainToolbar.translationY
        )

        val animationListener: Animation.AnimationListener = object : AnimationListenerAdapter() {
            override fun onAnimationEnd(animation: Animation?) {
                isTransitioned = true
                startActivity(aIntent, null)
            }
        }

        AnimationService.startAlphaHideAnimation(mCircleIconWrapper)
        val alphaHideAnimation = AnimationService.startAlphaHideAnimation(mTitleView)
        if (mRecyclerView.adapter != null && mRecyclerView.adapter!!
                .itemCount != 0
        ) {
            AnimationService.animateFakeClearing(
                lastVisibleItemPosition,
                firstVisibleItemPosition,
                mRecyclerView,
                animationListener,
                mAdapter is StreamsAdapter
            )
        } else {
            alphaHideAnimation.setAnimationListener(animationListener)
        }
    }

    /**
     * Checks if the user started this activity by pressing the back button on another main activity.
     * If so it runs the show animation for the activity's icon, text and visual elements.
     */
    open fun checkIsBackFromMainActivity() {
        if (isTransitioned) {
            val manager = mRecyclerView.layoutManager as GridLayoutManager?
            if (manager == null) return
            val delayBetween = 50
            val firstVisibleItemPosition = manager.findFirstVisibleItemPosition()
            val lastVisibleItemPosition = manager.findLastVisibleItemPosition()

            val startPositionCol =
                AnimationService.getColumnPosFromIndex(firstVisibleItemPosition, mRecyclerView)
            val startPositionRow =
                AnimationService.getRowPosFromIndex(firstVisibleItemPosition, mRecyclerView)

            // Show the Activity Icon and Text
            AnimationService.startAlphaRevealAnimation(mCircleIconWrapper)
            AnimationService.startAlphaRevealAnimation(mTitleView)

            // Fake fill the RecyclerViews with children again
            for (i in firstVisibleItemPosition..lastVisibleItemPosition) {
                val mView = mRecyclerView.getChildAt(i - firstVisibleItemPosition)

                val positionColumnDistance =
                    abs(AnimationService.getColumnPosFromIndex(i, mRecyclerView) - startPositionCol)
                val positionRowDistance =
                    abs(AnimationService.getRowPosFromIndex(i, mRecyclerView) - startPositionRow)
                val delay = (positionColumnDistance + positionRowDistance) * delayBetween

                //int delay = (i - firstVisibleItemPosition) * DELAY_BETWEEN;
                if (mView != null) {
                    AnimationService.startAlphaRevealAnimation(
                        delay,
                        mView,
                        mAdapter is StreamsAdapter
                    )
                }
            }
            isTransitioned = false
        }
    }

    /**
     * Starts appropriate animations if the activity has been started by another main activity. When the animations end super.onBackPressed() is called.
     * Returns true if that activity has been started through another main activity, else return false;
     */
    fun handleBackPressed(): Boolean {
        if (fromToolbarPosition != -1f) {
            val animationListener: Animation.AnimationListener =
                object : AnimationListenerAdapter() {
                    override fun onAnimationEnd(animation: Animation?) {
                        try {
                            super@MainActivity.onBackPressed()
                            overridePendingTransition(0, 0)
                        } catch (e: IllegalStateException) {
                            Timber.e(e)
                        }
                    }
                }

            // Animate the Activity Icon and text away
            AnimationService.startAlphaHideAnimation(mCircleIconWrapper)
            val alphaHideAnimation = AnimationService.startAlphaHideAnimation(mTitleView)

            val manager = mRecyclerView.layoutManager as GridLayoutManager?
            val firstVisibleItemPosition = manager!!.findFirstVisibleItemPosition()
            val lastVisibleItemPosition = manager.findLastVisibleItemPosition()
            var duration = alphaHideAnimation.getDuration().toInt()
            if (mRecyclerView.adapter != null && mRecyclerView.adapter!!
                    .itemCount != 0
            ) {
                duration = AnimationService.animateFakeClearing(
                    lastVisibleItemPosition,
                    firstVisibleItemPosition,
                    mRecyclerView,
                    animationListener,
                    mAdapter is StreamsAdapter
                )
            } else {
                alphaHideAnimation.setAnimationListener(animationListener)
            }
            AnimationService.setActivityToolbarPosition(
                duration,
                mMainToolbar,
                mDecorativeToolbar,
                this,
                mDecorativeToolbar.translationY,
                fromToolbarPosition,
                mMainToolbar.translationY,
                fromMainToolbarPosition
            )

            fromToolbarPosition = -1f

            return true
        }
        return false
    }

    companion object {
        private const val FIRST_VISIBLE_ELEMENT_POSITION = "firstVisibleElementPosition"
    }
}
