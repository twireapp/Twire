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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.perflyst.twire.R
import com.perflyst.twire.fragments.BindingFragment
import com.perflyst.twire.fragments.ChatFragment
import com.perflyst.twire.fragments.ChatFragment.Companion.getInstance
import com.perflyst.twire.fragments.PlayerFragment
import com.perflyst.twire.fragments.PlayerFragment.Companion.getScreenRect
import com.perflyst.twire.fragments.PlayerFragment.Companion.newInstance
import com.perflyst.twire.fragments.PlayerFragment.PlayerFragmentListener
import com.perflyst.twire.misc.addBackPressed
import com.perflyst.twire.misc.popBackStack
import com.perflyst.twire.service.Settings.chatLandscapeWidth
import timber.log.Timber

abstract class VideoFragment<T : ViewBinding>(inflate: (LayoutInflater, ViewGroup?, Boolean) -> T) :
    BindingFragment<T>(inflate), PlayerFragmentListener {
    lateinit var mPlayerFragment: PlayerFragment
    lateinit var mChatFragment: ChatFragment
    private var mBackstackLost = false
    private var onStopCalled = false
    private var initialOrientation = 0
    private var userLeaveHintListener: Runnable? = null

    protected abstract val videoContainerResource: Int

    protected abstract val streamArguments: Bundle?

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)

        val window = activity.window
        window.navigationBarColor = ContextCompat.getColor(activity, R.color.black)
        window.statusBarColor = ContextCompat.getColor(activity, R.color.black)

        initialOrientation = resources.configuration.orientation

        if (savedInstanceState == null) {
            val fm = childFragmentManager

            window.setEnterTransition(constructTransitions())
            window.setReturnTransition(constructTransitions())

            mChatFragment = getInstance(this.streamArguments)
            fm.beginTransaction().replace(R.id.chat_fragment, mChatFragment).commit()

            mPlayerFragment = newInstance(this.streamArguments)
            fm.beginTransaction().replace(
                this.videoContainerResource,
                mPlayerFragment,
                getString(R.string.stream_fragment_tag)
            ).commit()
        }

        updateOrientation()

        addBackPressed(this::onBackPressed)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            userLeaveHintListener = Runnable { onUserLeaveHint() }
            requireActivity().addOnUserLeaveHintListener(userLeaveHintListener!!)
        }
    }

    override fun onDestroyView() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            userLeaveHintListener?.let { requireActivity().removeOnUserLeaveHintListener(it) }
        }
        super.onDestroyView()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateOrientation()
    }

    protected fun resetStream() {
        val fm = childFragmentManager
        mPlayerFragment = newInstance(this.streamArguments)
        fm.beginTransaction().replace(this.videoContainerResource, mPlayerFragment).commit()
    }

    open fun onBackPressed(): Boolean {
        if (!mChatFragment.notifyBackPressed()) {
            return true
        }

        // Eww >(
        val isCurrentlyLandscape =
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val wasInitiallyLandscape = initialOrientation == Configuration.ORIENTATION_LANDSCAPE
        if (isCurrentlyLandscape && !wasInitiallyLandscape) {
            mPlayerFragment.toggleFullscreen()
        } else if (mPlayerFragment.chatOnlyViewVisible) {
            this.finish()
        } else {
            try {
                mPlayerFragment.backPressed()
            } catch (e: NullPointerException) {
                Timber.e(e)
            }
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun onUserLeaveHint() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return
        }

        if (mPlayerFragment.playWhenReady && requireActivity().applicationContext.packageManager
                .hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        ) {
            requireActivity().enterPictureInPictureMode()
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

    override fun onAttachFragment(fragment: Fragment) {
        if (fragment is PlayerFragment) {
            fragment.playerFragmentCallback = this
        }

        if (fragment is ChatFragment) mChatFragment = fragment

        if (fragment is PlayerFragment) mPlayerFragment = fragment
    }

    override fun onSeek() {
        mChatFragment.clearMessages()
    }

    override fun refreshLayout() {
        updateOrientation()
    }

    val mainContentLayout: View?
        get() = view?.findViewById(R.id.main_content)

    fun updateOrientation() {
        val landscape =
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        val chat = requireView().findViewById<View>(R.id.chat_fragment)
        if (landscape) {
            val lp =
                requireView().findViewById<View?>(R.id.chat_landscape_fragment)?.layoutParams as RelativeLayout.LayoutParams
            lp.width =
                (getScreenRect(requireActivity()).height() * (chatLandscapeWidth / 100.0)).toInt()
            Timber.d("TARGET WIDTH: %s", lp.width)
            chat.setLayoutParams(lp)
        } else {
            chat.setLayoutParams(requireView().findViewById<View?>(R.id.chat_placement_wrapper)?.layoutParams)
        }

        val layoutParams =
            requireView().findViewById<View?>(this.videoContainerResource)?.layoutParams
        layoutParams?.height =
            if (landscape) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT
    }

    override fun onStop() {
        super.onStop()
        onStopCalled = true
    }

    override fun onResume() {
        super.onResume()
        onStopCalled = false
    }

    override fun onPictureInPictureModeChanged(enabled: Boolean) {
        super.onPictureInPictureModeChanged(enabled)
        mBackstackLost = mBackstackLost or enabled

        if (!enabled && onStopCalled) {
            finish()
        }
    }

    fun finish() {
        if (mBackstackLost) {
            navToLauncherTask(requireActivity().applicationContext)
            popBackStack()
        } else {
            popBackStack()
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
