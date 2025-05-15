package com.perflyst.twire.fragments

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.AnimRes
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.perflyst.twire.R
import com.perflyst.twire.activities.SearchActivity
import com.perflyst.twire.activities.main.MainActivity
import com.perflyst.twire.activities.main.MyChannelsActivity
import com.perflyst.twire.activities.main.MyStreamsActivity
import com.perflyst.twire.activities.main.TopGamesActivity
import com.perflyst.twire.activities.main.TopStreamsActivity
import com.perflyst.twire.activities.settings.SettingsActivity
import com.perflyst.twire.activities.settings.SettingsGeneralActivity
import com.perflyst.twire.activities.setup.LoginActivity
import com.perflyst.twire.databinding.FragmentNavigationDrawerBinding
import com.perflyst.twire.misc.TooltipWindow
import com.perflyst.twire.misc.Utils
import com.perflyst.twire.service.Settings.generalTwitchDisplayName
import com.perflyst.twire.service.Settings.isLoggedIn
import com.perflyst.twire.service.Settings.isTipsShown
import com.perflyst.twire.tasks.GetStreamsCountTask
import com.perflyst.twire.utils.Execute
import dev.chrisbanes.insetter.Insetter


class NavigationDrawerFragment : Fragment() {
    private var _binding: FragmentNavigationDrawerBinding? = null
    private val binding get() = _binding!!
    private lateinit var mStreamsCount: TextView
    private lateinit var mStreamsCountWrapper: FrameLayout
    private var containerView: View? = null
    private lateinit var mAppTitleView: TextView
    private lateinit var mUserNameTextView: TextView
    private lateinit var mAppIcon: ImageView
    private var mDrawerToggle: ActionBarDrawerToggle? = null
    private var mDrawerLayout: DrawerLayout? = null
    private var mIntent: Intent? = null
    private val themeTip: TooltipWindow? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentNavigationDrawerBinding.inflate(inflater, container, false)

        mStreamsCount = binding.streamsCount
        mStreamsCountWrapper = binding.streamsCountWrapper
        containerView = container
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

        return binding.getRoot()
    }

    override fun onStart() {
        super.onStart()
        if (mDrawerLayout != null) { // If this layout isn't null then we know that the drawer has been setup
            checkUserLogin()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (themeTip != null && themeTip.isTooltipShown) {
            themeTip.dismissTooltip()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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

    fun setUp(drawerLayout: DrawerLayout?, toolbar: Toolbar?) {
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

                if (mIntent != null) {
                    if (activity is MainActivity<*>) {
                        val fromActivity = activity as MainActivity<*>?
                        fromActivity!!.transitionToOtherMainActivity(mIntent!!)
                    } else if (context != null) {
                        startActivity(mIntent, null)
                    }
                    mIntent = null
                }
            }
        }

        // set the listener on the nav drawer
        mDrawerLayout!!.addDrawerListener(mDrawerToggle!!)

        // This simple method gives us the burger icon for the toolbar
        mDrawerLayout!!.post { mDrawerToggle!!.syncState() }
    }

    private fun setClickListeners() {
        // OnClick listeners for the items
        setOnClick(binding.topStreamsContainer, TopStreamsActivity::class.java)
        setOnClick(binding.topGamesContainer, TopGamesActivity::class.java)
        setOnClick(binding.myChannelsContainer, MyChannelsActivity::class.java)
        setOnClick(binding.myStreamsContainer, MyStreamsActivity::class.java)

        setInstantOnClick(
            binding.searchContainer,
            SearchActivity::class.java,
            R.anim.slide_in_bottom_anim
        )
        setInstantOnClick(
            binding.settingsContainer,
            SettingsActivity::class.java,
            R.anim.slide_in_right_anim
        )
    }

    private fun setInstantOnClick(view: View, activityClass: Class<*>?, @AnimRes inAnimation: Int) {
        view.setOnClickListener { view1: View? ->
            val intent = Intent(activity, activityClass)
            val searchAnim = ActivityOptions.makeCustomAnimation(
                activity,
                inAnimation,
                R.anim.fade_out_semi_anim
            )
            startActivity(intent, searchAnim.toBundle())
            mDrawerLayout!!.closeDrawer(containerView!!)
        }
    }

    private fun setOnClick(view: View, aActivity: Class<*>?) {
        if (requireActivity().javaClass == aActivity) {
            // Get the attribute highlight color
            val a = TypedValue()
            requireActivity().getTheme()
                .resolveAttribute(R.attr.navigationDrawerHighlighted, a, true)
            if (a.type >= TypedValue.TYPE_FIRST_COLOR_INT && a.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                val color = a.data
                view.setBackgroundColor(color)
            }

            setCloseDrawerOnClick(view, mDrawerLayout!!, containerView!!)
        } else {
            setStandardOnClick(view, activity, aActivity, mDrawerLayout!!, containerView!!)
        }
    }

    private fun setCloseDrawerOnClick(
        mViewToListen: View,
        mDrawerLayout: DrawerLayout,
        mDrawerView: View
    ) {
        mViewToListen.setOnClickListener { v: View? ->
            if (activity is MainActivity<*>) {
                (activity as MainActivity<*>).scrollToTopAndRefresh()
            } else {
                requireActivity().recreate()
            }
            mDrawerLayout.closeDrawer(mDrawerView)
        }
    }

    private fun setStandardOnClick(
        mViewToListen: View, mFromActivity: Activity?, mToClass: Class<*>?,
        mDrawerLayout: DrawerLayout, mDrawerView: View
    ) {
        mViewToListen.setOnClickListener { v: View? ->
            val intent = Intent(mFromActivity, mToClass)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT) // We don't want to use a transition animation

            mIntent = intent

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
        val settingsGeneralActivity = Intent(context, SettingsGeneralActivity::class.java)
        startActivity(settingsGeneralActivity)
    }


    private fun navigateToLogin() {
        val loginIntent = Intent(context, LoginActivity::class.java)
        loginIntent.putExtra(getString(R.string.login_intent_part_of_setup), false)
        startActivity(loginIntent)
    }
}
