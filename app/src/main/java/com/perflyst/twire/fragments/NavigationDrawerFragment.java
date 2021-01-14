package com.perflyst.twire.fragments;


import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
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
import androidx.annotation.IdRes;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.perflyst.twire.R;
import com.perflyst.twire.activities.SearchActivity;
import com.perflyst.twire.activities.main.FeaturedStreamsActivity;
import com.perflyst.twire.activities.main.MainActivity;
import com.perflyst.twire.activities.main.MyChannelsActivity;
import com.perflyst.twire.activities.main.MyStreamsActivity;
import com.perflyst.twire.activities.main.TopGamesActivity;
import com.perflyst.twire.activities.main.TopStreamsActivity;
import com.perflyst.twire.activities.settings.SettingsActivity;
import com.perflyst.twire.activities.settings.SettingsGeneralActivity;
import com.perflyst.twire.activities.setup.LoginActivity;
import com.perflyst.twire.misc.TooltipWindow;
import com.perflyst.twire.service.Settings;
import com.perflyst.twire.tasks.GetStreamsCountTask;

import java.util.List;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;

public class NavigationDrawerFragment extends Fragment {


    @BindView(R.id.streams_count)
    protected TextView mStreamsCount;
    @BindView(R.id.streams_count_wrapper)
    protected FrameLayout mStreamsCountWrapper;
    @BindView(R.id.drawer_container)
    protected View containerView;
    @BindView(R.id.txt_app_name)
    protected TextView mAppTitleView;
    @BindView(R.id.txt_twitch_displayname)
    protected TextView mUserNameTextView;
    @BindView(R.id.img_app_icon)
    protected ImageView mAppIcon;
    @BindView(R.id.img_drawer_banner)
    protected ImageView mTopImage;
    @BindViews({R.id.my_streams_container, R.id.my_channels_container})
    List<View> mUserRequiredViews;
    private String LOG_TAG = getClass().getSimpleName();
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private Intent mIntent;
    private Settings mSettings;
    private TooltipWindow themeTip;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View mRoot = inflater.inflate(R.layout.fragment_navigation_drawer, container, false);
        ButterKnife.bind(this, mRoot);

        mSettings = new Settings(getActivity());

        initHeaderImage(mTopImage);
        fetchAndSetOnlineSteamsCount();

        return mRoot;
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

    private void fetchAndSetOnlineSteamsCount() {
        GetStreamsCountTask getStreamsCountTask = new GetStreamsCountTask(getContext(), count -> {
            if (count >= 0 && mStreamsCountWrapper != null && mStreamsCount != null) {
                showAndSetStreamCount(count);
            }
        });
        getStreamsCountTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void showAndSetStreamCount(int count) {
        mStreamsCountWrapper.setVisibility(View.VISIBLE);
        Animation alphaAnimation = new AlphaAnimation(0f, 1f);
        alphaAnimation.setDuration(240);
        alphaAnimation.setFillAfter(true);
        mStreamsCountWrapper.startAnimation(alphaAnimation);
        mStreamsCount.setText(Integer.toString(count));
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

                if (!mSettings.isTipsShown()) {
                    // Disable tips as soon as drawer is opened the first time
                    mSettings.setTipsShown(true);
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
                        ActivityCompat.startActivity(getContext(), mIntent, null);
                    }
                    mIntent = null;
                }
            }
        };

        // set the listener on the nav drawer
        mDrawerLayout.addDrawerListener(mDrawerToggle);

        // This simple method gives us the burger icon for the toolbar
        mDrawerLayout.post(() -> mDrawerToggle.syncState());

        setClickListeners();
        checkUserLogin();
    }

    private void setClickListeners() {
        // OnClick listeners for the items
        setOnClick(R.id.featured_streams_container, FeaturedStreamsActivity.class);
        setOnClick(R.id.top_streams_container, TopStreamsActivity.class);
        setOnClick(R.id.top_games_container, TopGamesActivity.class);
        setOnClick(R.id.my_channels_container, MyChannelsActivity.class);
        setOnClick(R.id.my_streams_container, MyStreamsActivity.class);

        setInstantOnClick(R.id.search_container, SearchActivity.class, R.anim.slide_in_bottom_anim);
        setInstantOnClick(R.id.settings_container, SettingsActivity.class, R.anim.slide_in_right_anim);
    }

    private void setInstantOnClick(@IdRes int viewRes, final Class activityClass, @AnimRes final int inAnimation) {
        View view = getActivity().findViewById(viewRes);
        view.setOnClickListener(view1 -> {
            Intent intent = new Intent(getActivity(), activityClass);

            ActivityOptionsCompat searchAnim = ActivityOptionsCompat.makeCustomAnimation(getActivity(), inAnimation, R.anim.fade_out_semi_anim);
            ActivityCompat.startActivity(getActivity(), intent, searchAnim.toBundle());
            mDrawerLayout.closeDrawer(containerView);
        });
    }

    private void setOnClick(@IdRes int viewID, Class aActivity) {
        View view = getActivity().findViewById(viewID);

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
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION); // We don't want to use a transition animation

            mIntent = intent;

            // Close the drawer. This way the intent will be used to launch the next activity,
            // as the OnCloseListener will start the activity, now that the mIntent contains an actual reference
            mDrawerLayout.closeDrawer(mDrawerView);
        });
    }

    private void checkUserLogin() {
        if (mSettings.isLoggedIn()) {
            mUserNameTextView.setText(getResources().getString(R.string.navigation_drawer_logged_in_textview, mSettings.getGeneralTwitchDisplayName()));
        } else {
            mUserNameTextView.setText(getString(R.string.navigation_drawer_not_logged_in));
        }

        if (!mSettings.isLoggedIn()) {
            for (View userView : mUserRequiredViews) {
                userView.setVisibility(View.GONE);
            }
        }
    }


    private void initHeaderImage(final ImageView headerImageView) {
        headerImageView.setImageResource(R.drawable.nav_top);
        headerImageView.setOnClickListener(v ->{

            if (mSettings.isLoggedIn()) {
                navigateToAccountManagement();
            }else{
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
