package com.perflyst.twire.activities.stream

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.transition.Fade
import android.transition.Slide
import android.transition.Transition
import android.transition.TransitionSet
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.perflyst.twire.R
import com.perflyst.twire.activities.ThemeActivity
import com.perflyst.twire.fragments.ChatFragment
import com.perflyst.twire.fragments.ChatFragment.Companion.getInstance
import com.perflyst.twire.fragments.StreamFragment
import com.perflyst.twire.fragments.StreamFragment.Companion.getScreenRect
import com.perflyst.twire.fragments.StreamFragment.Companion.newInstance
import com.perflyst.twire.fragments.StreamFragment.StreamFragmentListener
import com.perflyst.twire.service.Settings.chatLandscapeWidth
import timber.log.Timber

abstract class StreamActivity : ThemeActivity(), StreamFragmentListener {
    var mStreamFragment: StreamFragment? = null
    var mChatFragment: ChatFragment? = null
    private var mBackstackLost = false
    private var onStopCalled = false
    private var initialOrientation = 0

    protected abstract val layoutResource: Int

    protected abstract val videoContainerResource: Int

    protected abstract val streamArguments: Bundle?

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(this.layoutResource)

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)

        window.navigationBarColor = ContextCompat.getColor(this, R.color.black)
        window.statusBarColor = ContextCompat.getColor(this, R.color.black)

        initialOrientation = getResources().configuration.orientation

        if (savedInstanceState == null) {
            val fm = supportFragmentManager

            window.setEnterTransition(constructTransitions())
            window.setReturnTransition(constructTransitions())

            // If the Fragment is non-null, then it is currently being
            // retained across a configuration change.
            if (mChatFragment == null) {
                mChatFragment = getInstance(this.streamArguments)
                fm.beginTransaction().replace(R.id.chat_fragment, mChatFragment!!).commit()
            }

            if (mStreamFragment == null) {
                mStreamFragment = newInstance(this.streamArguments)
                fm.beginTransaction().replace(
                    this.videoContainerResource,
                    mStreamFragment!!,
                    getString(R.string.stream_fragment_tag)
                ).commit()
            }
        }

        updateOrientation()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateOrientation()
    }

    protected fun resetStream() {
        val fm = supportFragmentManager
        mStreamFragment = newInstance(this.streamArguments)
        fm.beginTransaction().replace(this.videoContainerResource, mStreamFragment!!).commit()
    }

    public override fun onPause() {
        super.onPause()
    }

    override fun onBackPressed() {
        if (mChatFragment == null || !mChatFragment!!.notifyBackPressed()) {
            return
        }

        // Eww >(
        if (mStreamFragment != null) {
            val isCurrentlyLandscape =
                getResources().configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val wasInitiallyLandscape = initialOrientation == Configuration.ORIENTATION_LANDSCAPE
            if (isCurrentlyLandscape && !wasInitiallyLandscape) {
                mStreamFragment!!.toggleFullscreen()
            } else if (mStreamFragment!!.chatOnlyViewVisible) {
                this.finish()
                this.overrideTransition()
            } else {
                super.onBackPressed()
                try {
                    mStreamFragment!!.backPressed()
                } catch (e: NullPointerException) {
                    Timber.e(e)
                }
                this.overrideTransition()
            }
        } else {
            super.onBackPressed()
            this.overrideTransition()
        }
    }

    @RequiresApi(24)
    public override fun onUserLeaveHint() {
        super.onUserLeaveHint()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return
        }

        if (mStreamFragment!!.playWhenReady && applicationContext.packageManager
                .hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        ) {
            enterPictureInPictureMode()
        }
    }

    private fun constructTransitions(): TransitionSet {
        val slideTargets =
            intArrayOf(R.id.ChatRecyclerView, R.id.chat_input, R.id.chat_input_divider)

        val slideTransition: Transition = Slide(Gravity.BOTTOM)
        val fadeTransition: Transition = Fade()

        for (slideTarget in slideTargets) {
            slideTransition.addTarget(slideTarget)
            fadeTransition.excludeTarget(slideTarget, true)
        }

        val set = TransitionSet()
        set.addTransition(slideTransition)
        set.addTransition(fadeTransition)
        return set
    }

    private fun overrideTransition() {
        this.overridePendingTransition(R.anim.fade_in_semi_anim, R.anim.slide_out_bottom_anim)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_stream, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { // Call the super method as we also want the user to go all the way back to last mActivity if the user is in full screen mode
            if (mStreamFragment != null) {
                if (!mStreamFragment!!.isVideoInterfaceShowing) {
                    return false
                }

                if (mStreamFragment!!.chatOnlyViewVisible) {
                    finish()
                } else {
                    super.onBackPressed()
                    mStreamFragment!!.backPressed()
                }
                overrideTransition()
            }

            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onAttachFragment(fragment: Fragment) {
        if (fragment is StreamFragment) {
            val streamFragment = fragment
            streamFragment.streamFragmentCallback = this
        }

        if (mChatFragment == null && fragment is ChatFragment) mChatFragment = fragment

        if (mStreamFragment == null && fragment is StreamFragment) mStreamFragment = fragment
    }

    override fun onSeek() {
        mChatFragment!!.clearMessages()
    }

    override fun refreshLayout() {
        updateOrientation()
    }

    val mainContentLayout: View?
        get() = findViewById(R.id.main_content)

    fun updateOrientation() {
        val landscape =
            getResources().configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        val chat = findViewById<View>(R.id.chat_fragment)
        if (landscape) {
            val lp =
                findViewById<View?>(R.id.chat_landscape_fragment)?.layoutParams as RelativeLayout.LayoutParams
            lp.width = (getScreenRect(this).height() * (chatLandscapeWidth / 100.0)).toInt()
            Timber.d("TARGET WIDTH: %s", lp.width)
            chat.setLayoutParams(lp)
        } else {
            chat.setLayoutParams(findViewById<View?>(R.id.chat_placement_wrapper)?.layoutParams)
        }

        val layoutParams = findViewById<View?>(this.videoContainerResource)?.layoutParams
        layoutParams?.height =
            if (landscape) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT
    }

    public override fun onStop() {
        super.onStop()
        onStopCalled = true
    }

    override fun onResume() {
        super.onResume()
        onStopCalled = false
    }

    override fun onPictureInPictureModeChanged(enabled: Boolean, newConfig: Configuration?) {
        super.onPictureInPictureModeChanged(enabled, newConfig)
        mBackstackLost = mBackstackLost or enabled

        if (!enabled && onStopCalled) {
            finish()
        }
    }

    override fun finish() {
        if (mBackstackLost) {
            navToLauncherTask(applicationContext)
            finishAndRemoveTask()
        } else {
            super.finish()
        }
    }

    fun navToLauncherTask(appContext: Context) {
        val activityManager = ContextCompat.getSystemService(
            appContext,
            ActivityManager::class.java
        )
        // iterate app tasks available and navigate to launcher task (browse task)
        if (activityManager != null) {
            val appTasks = activityManager.getAppTasks()
            for (task in appTasks) {
                val baseIntent = task.taskInfo.baseIntent
                val categories = baseIntent.categories
                if (categories != null && categories.contains(Intent.CATEGORY_LAUNCHER)) {
                    task.moveToFront()
                    return
                }
            }
        }
    }
}
