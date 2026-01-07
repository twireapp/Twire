package com.perflyst.twire.fragments

import android.app.Activity
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.transition.Fade
import androidx.transition.Slide
import androidx.transition.Transition
import com.perflyst.twire.R
import com.perflyst.twire.activities.SearchFragment
import com.perflyst.twire.activities.main.MainFragment
import com.perflyst.twire.activities.main.MyChannelsFragment
import com.perflyst.twire.activities.main.MyStreamsFragment
import com.perflyst.twire.activities.main.TopGamesFragment
import com.perflyst.twire.activities.main.TopStreamsFragment
import com.perflyst.twire.activities.settings.SettingsFragment
import com.perflyst.twire.activities.settings.SettingsGeneralFragment
import com.perflyst.twire.activities.setup.LoginFragment
import com.perflyst.twire.databinding.FragmentNavigationDrawerBinding
import com.perflyst.twire.misc.TooltipWindow
import com.perflyst.twire.misc.Utils
import com.perflyst.twire.misc.navigate
import com.perflyst.twire.service.Settings.generalTwitchDisplayName
import com.perflyst.twire.service.Settings.isLoggedIn
import com.perflyst.twire.service.Settings.isTipsShown
import com.perflyst.twire.tasks.GetStreamsCountTask
import com.perflyst.twire.utils.Execute
import dev.chrisbanes.insetter.Insetter


class NavigationDrawerFragment :
    BindingFragment<FragmentNavigationDrawerBinding>(FragmentNavigationDrawerBinding::inflate) {
    private lateinit var mStreamsCount: TextView
    private lateinit var mStreamsCountWrapper: FrameLayout
    private lateinit var containerView: View
    private lateinit var mAppTitleView: TextView
    private lateinit var mUserNameTextView: TextView
    private lateinit var mAppIcon: ImageView
    private lateinit var mDrawerToggle: ActionBarDrawerToggle
    private lateinit var mDrawerLayout: DrawerLayout
    private var mFragment: Class<out Fragment>? = null
    private val themeTip: TooltipWindow? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mStreamsCount = binding.streamsCount
        mStreamsCountWrapper = binding.streamsCountWrapper
        containerView = requireParentFragment().requireView().findViewById(R.id.drawer_fragment)
        mAppTitleView = binding.txtAppName
        mUserNameTextView = binding.txtTwitchDisplayname
        mAppIcon = binding.imgAppIcon

        initHeaderImage(binding.imgDrawerBanner)
        fetchAndSetOnlineSteamsCount()
        setClickListeners()
        checkUserLogin()

        Insetter.builder().paddingBottom(WindowInsetsCompat.Type.systemBars(), false).applyToView(
            binding.drawerContainer
        )
    }

    override fun onStart() {
        super.onStart()
        checkUserLogin()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (themeTip != null && themeTip.isTooltipShown) {
            themeTip.dismissTooltip()
        }
    }

    private fun fetchAndSetOnlineSteamsCount() {
        val getStreamsCountTask = GetStreamsCountTask(context)
        Execute.background(getStreamsCountTask) { count: Int? ->
            if (count!! >= 0) {
                showAndSetStreamCount(count)
            }
        }
    }

    private fun showAndSetStreamCount(count: Int) {
        mStreamsCountWrapper.visibility = View.VISIBLE
        val alphaAnimation: Animation = AlphaAnimation(0f, 1f)
        alphaAnimation.setDuration(240)
        alphaAnimation.fillAfter = true
        mStreamsCountWrapper.startAnimation(alphaAnimation)
        Utils.setNumber(mStreamsCount, count.toLong())
    }

    fun setUp(drawerLayout: DrawerLayout, toolbar: Toolbar?) {
        mDrawerLayout = drawerLayout

        // Create listener for changes in the nav drawer state.
        mDrawerToggle = object : ActionBarDrawerToggle(
            activity,
            mDrawerLayout,
            toolbar,
            R.string.drawer_open,
            R.string.drawer_close
        ) {
            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)

                if (!isTipsShown) {
                    // Disable tips as soon as drawer is opened the first time
                    isTipsShown = true
                }

                mAppIcon.startAnimation(
                    AnimationUtils.loadAnimation(
                        activity,
                        R.anim.anim_icon_rotation
                    )
                )
            }

            override fun onDrawerClosed(drawerView: View) {
                super.onDrawerClosed(drawerView)

                if (mFragment != null) {
                    if (parentFragment is MainFragment<*>) {
                        val fromActivity = parentFragment as MainFragment<*>?
                        fromActivity!!.transitionToOtherMainActivity(mFragment!!)
                    } else if (context != null) {
                        navigate(mFragment!!, single = true)
                    }
                    mFragment = null
                }
            }
        }

        // set the listener on the nav drawer
        mDrawerLayout.addDrawerListener(mDrawerToggle)

        // This simple method gives us the burger icon for the toolbar
        mDrawerLayout.post { mDrawerToggle.syncState() }
    }

    private fun setClickListeners() {
        // OnClick listeners for the items
        setOnClick(binding.topStreamsContainer, TopStreamsFragment::class.java)
        setOnClick(binding.topGamesContainer, TopGamesFragment::class.java)
        setOnClick(binding.myChannelsContainer, MyChannelsFragment::class.java)
        setOnClick(binding.myStreamsContainer, MyStreamsFragment::class.java)

        setInstantOnClick(
            binding.searchContainer,
            SearchFragment::class.java,
            Slide()
        )
        setInstantOnClick(
            binding.settingsContainer,
            SettingsFragment::class.java,
            Slide(Gravity.END)
        )
    }

    private fun setInstantOnClick(
        view: View,
        fragmentClass: Class<out Fragment>?,
        inAnimation: Transition
    ) {
        view.setOnClickListener { view1: View? ->
            navigate(fragmentClass!!, enterAnim = inAnimation, exitAnim = Fade())
            mDrawerLayout.closeDrawer(containerView)
        }
    }

    private fun setOnClick(view: View, aActivity: Class<out Fragment>?) {
        if (requireParentFragment().javaClass == aActivity) {
            // Get the attribute highlight color
            val a = TypedValue()
            requireActivity().getTheme()
                .resolveAttribute(R.attr.navigationDrawerHighlighted, a, true)
            if (a.type >= TypedValue.TYPE_FIRST_COLOR_INT && a.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                val color = a.data
                view.setBackgroundColor(color)
            }

            setCloseDrawerOnClick(view, mDrawerLayout, containerView)
        } else {
            setStandardOnClick(view, activity, aActivity, mDrawerLayout, containerView)
        }
    }

    private fun setCloseDrawerOnClick(
        mViewToListen: View,
        mDrawerLayout: DrawerLayout,
        mDrawerView: View
    ) {
        mViewToListen.setOnClickListener { v: View? ->
            if (parentFragment is MainFragment<*>) {
                (parentFragment as MainFragment<*>).scrollToTopAndRefresh()
            } else {
                requireActivity().recreate()
            }
            mDrawerLayout.closeDrawer(mDrawerView)
        }
    }

    private fun setStandardOnClick(
        mViewToListen: View, mFromActivity: Activity?, mToClass: Class<out Fragment>?,
        mDrawerLayout: DrawerLayout, mDrawerView: View
    ) {
        mViewToListen.setOnClickListener { v: View? ->
            mFragment = mToClass

            // Close the drawer. This way the intent will be used to launch the next activity,
            // as the OnCloseListener will start the activity, now that the mIntent contains an actual reference
            mDrawerLayout.closeDrawer(mDrawerView)
        }
    }

    private fun checkUserLogin() {
        if (isLoggedIn) {
            mUserNameTextView.text = getString(
                R.string.navigation_drawer_logged_in_textview,
                generalTwitchDisplayName
            )
        } else {
            mUserNameTextView.setText(R.string.navigation_drawer_not_logged_in)
        }
    }


    private fun initHeaderImage(headerImageView: ImageView) {
        headerImageView.setImageResource(R.drawable.nav_top)
        headerImageView.setOnClickListener { v: View? ->
            if (isLoggedIn) {
                navigateToAccountManagement()
            } else {
                navigateToLogin()
            }
        }
    }

    private fun navigateToAccountManagement() {
        navigate(SettingsGeneralFragment::class.java)
    }


    private fun navigateToLogin() {
        val args = Bundle()
        args.putBoolean(getString(R.string.login_intent_part_of_setup), false)
        navigate(LoginFragment::class.java, args)
    }
}
