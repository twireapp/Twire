package com.perflyst.twire.fragments;


import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.AnimRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.perflyst.twire.R;
import com.perflyst.twire.activities.SearchActivity;
import com.perflyst.twire.activities.main.MainActivity;
import com.perflyst.twire.activities.main.MyChannelsActivity;
import com.perflyst.twire.activities.main.MyStreamsActivity;
import com.perflyst.twire.activities.main.TopGamesActivity;
import com.perflyst.twire.activities.main.TopStreamsActivity;
import com.perflyst.twire.activities.settings.SettingsActivity;
import com.perflyst.twire.activities.settings.SettingsGeneralActivity;
import com.perflyst.twire.activities.setup.LoginActivity;
import com.perflyst.twire.databinding.FragmentNavigationDrawerBinding;
import com.perflyst.twire.misc.TooltipWindow;
import com.perflyst.twire.misc.Utils;
import com.perflyst.twire.service.Settings;
import com.perflyst.twire.tasks.GetStreamsCountTask;
import com.perflyst.twire.utils.Execute;

import dev.chrisbanes.insetter.Insetter;

public class NavigationDrawerFragment extends Fragment {

    private FragmentNavigationDrawerBinding binding;
    protected TextView mStreamsCount;
    protected FrameLayout mStreamsCountWrapper;
    protected View containerView;
    protected TextView mAppTitleView;
    protected TextView mUserNameTextView;
    protected ImageView mAppIcon;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private Intent mIntent;
    private TooltipWindow themeTip;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentNavigationDrawerBinding.inflate(inflater, container, false);

        mStreamsCount = binding.streamsCount;
        mStreamsCountWrapper = binding.streamsCountWrapper;
        containerView = container;
        mAppTitleView = binding.txtAppName;
        mUserNameTextView = binding.txtTwitchDisplayname;
        mAppIcon = binding.imgAppIcon;

        initHeaderImage(binding.imgDrawerBanner);
        fetchAndSetOnlineSteamsCount();
        setClickListeners();
        checkUserLogin();

        Insetter.builder().paddingBottom(WindowInsetsCompat.Type.systemBars(), false).applyToView(binding.drawerContainer);

        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mDrawerLayout != null) { // If this layout isn't null then we know that the drawer has been setup
            checkUserLogin();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (themeTip != null && themeTip.isTooltipShown()) {
            themeTip.dismissTooltip();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void fetchAndSetOnlineSteamsCount() {
        GetStreamsCountTask getStreamsCountTask = new GetStreamsCountTask(getContext());
        Execute.background(getStreamsCountTask, count -> {
            if (count >= 0 && mStreamsCountWrapper != null && mStreamsCount != null) {
                showAndSetStreamCount(count);
            }
        });
    }

    private void showAndSetStreamCount(int count) {
        mStreamsCountWrapper.setVisibility(View.VISIBLE);
        Animation alphaAnimation = new AlphaAnimation(0f, 1f);
        alphaAnimation.setDuration(240);
        alphaAnimation.setFillAfter(true);
        mStreamsCountWrapper.startAnimation(alphaAnimation);
        Utils.setNumber(mStreamsCount, count);
    }

    public void setUp(DrawerLayout drawerLayout, Toolbar toolbar) {
        mDrawerLayout = drawerLayout;

        // Create listener for changes in the nav drawer state.
        mDrawerToggle = new ActionBarDrawerToggle(getActivity(), mDrawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                if (mAppIcon == null) {
                    return;
                }
                super.onDrawerOpened(drawerView);

                if (!Settings.isTipsShown()) {
                    // Disable tips as soon as drawer is opened the first time
                    Settings.setTipsShown(true);
                }

                mAppIcon.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.anim_icon_rotation));
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);

                if (mIntent != null) {
                    if (getActivity() instanceof MainActivity) {
                        MainActivity fromActivity = (MainActivity) getActivity();
                        fromActivity.transitionToOtherMainActivity(mIntent);
                    } else if (getContext() != null) {
                        startActivity(mIntent, null);
                    }
                    mIntent = null;
                }
            }
        };

        // set the listener on the nav drawer
        mDrawerLayout.addDrawerListener(mDrawerToggle);

        // This simple method gives us the burger icon for the toolbar
        mDrawerLayout.post(() -> mDrawerToggle.syncState());
    }

    private void setClickListeners() {
        // OnClick listeners for the items
        setOnClick(binding.topStreamsContainer, TopStreamsActivity.class);
        setOnClick(binding.topGamesContainer, TopGamesActivity.class);
        setOnClick(binding.myChannelsContainer, MyChannelsActivity.class);
        setOnClick(binding.myStreamsContainer, MyStreamsActivity.class);

        setInstantOnClick(binding.searchContainer, SearchActivity.class, R.anim.slide_in_bottom_anim);
        setInstantOnClick(binding.settingsContainer, SettingsActivity.class, R.anim.slide_in_right_anim);
    }

    private void setInstantOnClick(View view, final Class activityClass, @AnimRes final int inAnimation) {
        view.setOnClickListener(view1 -> {
            Intent intent = new Intent(getActivity(), activityClass);

            ActivityOptions searchAnim = ActivityOptions.makeCustomAnimation(getActivity(), inAnimation, R.anim.fade_out_semi_anim);
            startActivity(intent, searchAnim.toBundle());
            mDrawerLayout.closeDrawer(containerView);
        });
    }

    private void setOnClick(View view, Class aActivity) {
        if (getActivity().getClass() == aActivity) {
            // Get the attribute highlight color
            TypedValue a = new TypedValue();
            getActivity().getTheme().resolveAttribute(R.attr.navigationDrawerHighlighted, a, true);
            if (a.type >= TypedValue.TYPE_FIRST_COLOR_INT && a.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                int color = a.data;
                view.setBackgroundColor(color);
            }

            setCloseDrawerOnClick(view, mDrawerLayout, containerView);
        } else {
            setStandardOnClick(view, getActivity(), aActivity, mDrawerLayout, containerView);
        }

    }

    private void setCloseDrawerOnClick(View mViewToListen, final DrawerLayout mDrawerLayout, final View mDrawerView) {
        mViewToListen.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).scrollToTopAndRefresh();
            } else {
                getActivity().recreate();
            }

            mDrawerLayout.closeDrawer(mDrawerView);
        });
    }

    private void setStandardOnClick(View mViewToListen, final Activity mFromActivity, final Class mToClass,
                                    final DrawerLayout mDrawerLayout, final View mDrawerView) {
        mViewToListen.setOnClickListener(v -> {
            Intent intent = new Intent(mFromActivity, mToClass);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); // We don't want to use a transition animation

            mIntent = intent;

            // Close the drawer. This way the intent will be used to launch the next activity,
            // as the OnCloseListener will start the activity, now that the mIntent contains an actual reference
            mDrawerLayout.closeDrawer(mDrawerView);
        });
    }

    private void checkUserLogin() {
        if (Settings.isLoggedIn()) {
            mUserNameTextView.setText(getString(R.string.navigation_drawer_logged_in_textview, Settings.getGeneralTwitchDisplayName()));
        } else {
            mUserNameTextView.setText(R.string.navigation_drawer_not_logged_in);
        }
    }


    private void initHeaderImage(final ImageView headerImageView) {
        headerImageView.setImageResource(R.drawable.nav_top);
        headerImageView.setOnClickListener(v -> {

            if (Settings.isLoggedIn()) {
                navigateToAccountManagement();
            } else {
                navigateToLogin();
            }
        });
    }

    private void navigateToAccountManagement() {
        Intent settingsGeneralActivity = new Intent(getContext(), SettingsGeneralActivity.class);
        startActivity(settingsGeneralActivity);
    }


    private void navigateToLogin() {
        Intent loginIntent = new Intent(getContext(), LoginActivity.class);
        loginIntent.putExtra(getString(R.string.login_intent_part_of_setup), false);
        startActivity(loginIntent);
    }

}
