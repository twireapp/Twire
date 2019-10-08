package com.perflyst.twire.activities;

import android.animation.Animator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.perflyst.twire.R;
import com.perflyst.twire.activities.main.LazyFetchingActivity;
import com.perflyst.twire.adapters.PanelAdapter;
import com.perflyst.twire.adapters.VODAdapter;
import com.perflyst.twire.misc.FollowHandler;
import com.perflyst.twire.misc.LazyFetchingOnScrollListener;
import com.perflyst.twire.model.ChannelInfo;
import com.perflyst.twire.model.VideoOnDemand;
import com.perflyst.twire.service.JSONService;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.service.Settings;
import com.perflyst.twire.tasks.GetPanelsTask;
import com.perflyst.twire.views.recyclerviews.AutoSpanRecyclerView;
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.VODAutoSpanBehaviour;
import com.rey.material.widget.ProgressView;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ChannelActivity extends ThemeActivity {
    private static final String fragmentStreamerInfoArg = "streamerInfoArg",
            fragmentVodsBroadCastsOnlyArg = "vodsBroadcastsOnlyArg",
            fragmentVodsStreamerInfoArg = "streamerNameArg";
    private final String LOG_TAG = getClass().getSimpleName();
    private final int SHOW_FAB_DELAY = 300;
    private ChannelInfo info;
    private ImageView streamerImage;
    private LinearLayout additionalInfoLayout;
    private Toolbar toolbar,
            additionalToolbar;
    private ViewPager mViewPager;
    private TabLayout mTabs;
    private AppBarLayout mAppBar;
    private FloatingActionButton mFab;
    private int COLOR_FADE_DURATION = 0;
    private Target mLoadingTarget;
    private ChannelFragment mDescriptionFragment, mBroadcastsFragment, mHighlightsFragment;
    private FollowHandler mFollowHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_streamer_info);

        // Get the various handles of view and layouts that is part of this view
        streamerImage = findViewById(R.id.profileImageView);
        additionalInfoLayout = findViewById(R.id.additional_info_wrapper);
        TextView streamerInfoName = findViewById(R.id.twitch_name);
        TextView streamerViewers = findViewById(R.id.txt_viewers);
        TextView streamerFollowers = findViewById(R.id.txt_followers);
        toolbar = findViewById(R.id.StreamerInfo_Toolbar);
        additionalToolbar = findViewById(R.id.additional_toolbar);
        mViewPager = findViewById(R.id.container);
        mTabs = findViewById(R.id.tabs);
        mAppBar = findViewById(R.id.appbar);
        mFab = findViewById(R.id.fab);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Get the StreamerInfo object sent with the intent to open this activity
        Intent intent = getIntent();
        info = intent.getParcelableExtra(getResources().getString(R.string.channel_info_intent_object));

        streamerInfoName.setText(info.getDisplayName());
        streamerFollowers.setText(getReadableInt(info.getFollowers()));
        streamerViewers.setText(getReadableInt(info.getViews()));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            streamerImage.setTransitionName(getString(R.string.streamerInfo_transition));
        }

        setUpTabs();
        initStreamerImageAndColors();
        initiateFAB();
    }

    @Override
    public void onStart() {
        super.onStart();
        overridePendingTransition(R.anim.slide_in_bottom_anim, R.anim.fade_out_semi_anim);
    }

    @Override
    public void onResume() {
        COLOR_FADE_DURATION = 800;
        initiateFAB();
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_streamer_info, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.overridePendingTransition(R.anim.fade_in_semi_anim, R.anim.slide_out_bottom_anim);
    }

    private void setUpTabs() {
        assert mViewPager != null;
        mViewPager.setAdapter(new SectionsPagerAdapter(getSupportFragmentManager()));

        assert mTabs != null;
        mTabs.setupWithViewPager(mViewPager);

        mTabs.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getText() != null &&
                        tab.getText().equals(getResources().getString(R.string.streamerInfo_desc_tab))) {
                    mAppBar.setExpanded(true, true);
                } else {
                    mAppBar.setExpanded(false, true);
                }
                mViewPager.setCurrentItem(tab.getPosition(), true);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

    }

    private void initStreamerImageAndColors() {
        Target mTarget = getNightThemeTarget();
        String theme = new Settings(this).getTheme();
        if (!theme.equals(getString(R.string.night_theme_name)) && !theme.equals(getString(R.string.true_night_theme_name))) {
            mTarget = getLightThemeTarget();
        }

        mLoadingTarget = mTarget;
        Picasso.with(getBaseContext())
                .load(info.getMediumPreview())
                .into(mTarget);
    }

    private Target getNightThemeTarget() {
        return new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                streamerImage.setImageBitmap(bitmap);
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {

            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {

            }
        };
    }

    private Target getLightThemeTarget() {
        return new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                streamerImage.setImageBitmap(bitmap);

                Palette palette = Palette.from(bitmap).generate();
                int defaultColor = Service.getColorAttribute(R.attr.colorPrimary, R.color.primary, getBaseContext());
                int defaultDarkColor = Service.getColorAttribute(R.attr.colorPrimaryDark, R.color.primaryDark, getBaseContext());

                int vibrant = palette.getVibrantColor(defaultColor);
                int vibrantDark = palette.getDarkVibrantColor(defaultColor);
                int vibrantLight = palette.getLightVibrantColor(defaultColor);

                int muted = palette.getMutedColor(defaultColor);
                int mutedDark = palette.getDarkMutedColor(defaultColor);
                int mutedLight = palette.getLightMutedColor(defaultColor);

                Palette.Swatch swatch;

                if (vibrant != defaultColor) {
                    swatch = palette.getVibrantSwatch();
                } else if (vibrantDark != defaultColor) {
                    swatch = palette.getDarkVibrantSwatch();
                } else if (vibrantLight != defaultColor) {
                    swatch = palette.getLightVibrantSwatch();
                } else if (muted != defaultColor) {
                    swatch = palette.getMutedSwatch();
                } else if (mutedDark != defaultColor) {
                    swatch = palette.getDarkMutedSwatch();
                } else {
                    swatch = palette.getLightMutedSwatch();
                }

                if (swatch != null) {
                    float[] swatchValues = swatch.getHsl();
                    float[] newSwatch = {swatchValues[0], (float) 0.85, (float) 0.85};
                    float[] newSwatchComposite = {(swatchValues[0] + 180) % 360, newSwatch[1], newSwatch[2]};
                    float[] newSwatchDark = {newSwatch[0], newSwatch[1], (float) 0.6};

                    int newColorDark = Color.HSVToColor(newSwatchDark);
                    int newColor = Color.HSVToColor(newSwatch);
                    int compositeNewColor = Color.HSVToColor(newSwatchComposite);

                    int primaryColor = Service.getBackgroundColorFromView(toolbar, defaultColor);
                    int primaryColorDark = Service.getBackgroundColorFromView(mTabs, defaultDarkColor);

                    Service.animateBackgroundColorChange(toolbar, newColor, primaryColor, COLOR_FADE_DURATION);
                    Service.animateBackgroundColorChange(additionalToolbar, newColor, primaryColor, COLOR_FADE_DURATION);
                    Service.animateBackgroundColorChange(mTabs, newColorDark, primaryColorDark, COLOR_FADE_DURATION);
                    mFab.setBackgroundTintList(ColorStateList.valueOf(compositeNewColor));
                    mTabs.setSelectedTabIndicatorColor(compositeNewColor);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        Window window = getWindow();
                        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                        window.setStatusBarColor(newColorDark);
                    }
                }
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
            }
        };
    }

    private void initiateFAB() {
        mFollowHandler = new FollowHandler(
                info,
                getBaseContext(),
                new FollowHandler.Delegate() {
                    @Override
                    public void streamerIsFollowed() {
                    }

                    @Override
                    public void streamerIsNotFollowed() {
                    }

                    @Override
                    public void userIsNotLoggedIn() {
                        mFab.setVisibility(View.GONE);
                    }

                    @Override
                    public void followSuccess() {
                    }

                    @Override
                    public void followFailure() {
                    }

                    @Override
                    public void unfollowSuccess() {
                    }

                    @Override
                    public void unfollowFailure() {
                    }
                }
        );

        mFab.setOnClickListener(v -> {
            if (mFollowHandler.isStreamerFollowed()) {
                mFollowHandler.unfollowStreamer();
            } else {
                mFollowHandler.followStreamer();
            }

            hideFAB();
            new Handler().postDelayed(() -> {
                updateFABIcon(!mFollowHandler.isStreamerFollowed());
                showFAB();
            }, SHOW_FAB_DELAY);
        });

        updateFABIcon(mFollowHandler.isStreamerFollowed());
    }

    private void updateFABIcon(boolean isFollowing) {
        @DrawableRes int imageRes = isFollowing
                ? R.drawable.ic_heart_broken_24dp
                : R.drawable.ic_heart_24dp;
        mFab.setImageResource(imageRes);
    }

    private void hideFAB() {
        mFab.setClickable(false);
        int HIDE_FAB_DURATION = 200;
        mFab.animate()
                .translationY(getResources().getDimension(R.dimen.streamerInfo_fab_size) + getResources().getDimension(R.dimen.streamerInfo_fab_margin))
                .setDuration(HIDE_FAB_DURATION)
                .setInterpolator(new AccelerateInterpolator())
                .start();
    }

    private void showFAB() {
        int SHOW_FAB_DURATION = 300;
        mFab.animate()
                .translationY(0)
                .setDuration(SHOW_FAB_DURATION)
                .setInterpolator(new OvershootInterpolator())
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mFab.setClickable(true);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                })
                .start();
    }

    /**
     * Returns the URL string we need to connect to.
     * Both if the user wants to follow AND unfollow the current streamer
     */
    private String getBaseFollowString() {
        Settings settings = new Settings(getBaseContext());
        return "https://api.twitch.tv/kraken/users/" + settings.getGeneralTwitchUserID() + "/follows/channels/" + info.getUserId() + "?oauth_token=" + settings.getGeneralTwitchAccessToken();
    }

    /**
     * Make an int more readable by separating every third number by a space.
     * Example: 1000000 becomes "1 000 000"
     */
    private String getReadableInt(int number) {
        StringBuilder result = new StringBuilder();
        String numberAsString = number + "";
        int x = 1;
        for (int i = numberAsString.length() - 1; i >= 0; i--) {
            result.insert(0, numberAsString.charAt(i));

            if (x % 3 == 0 && i != numberAsString.length() - 1) {
                result.insert(0, " ");
            }

            x++;
        }

        return result.toString();
    }

    public static abstract class ChannelFragment extends Fragment {
        TextView mErrorEmote, mErrorText;

        void findErrorView(View rootView) {
            mErrorEmote = rootView.findViewById(R.id.emote_error_view);
            mErrorText = rootView.findViewById(R.id.error_view);
        }

        protected void showError() {
            mErrorEmote.setVisibility(View.VISIBLE);
            mErrorText.setVisibility(View.VISIBLE);
            mErrorEmote.setText(Service.getErrorEmote());
        }
    }

    public static class InfoFragment extends ChannelFragment {
        private final String LOG_TAG = getClass().getSimpleName();
        private ChannelInfo info;

        private RecyclerView mPanelsRecyclerView;

        static InfoFragment newInstance(ChannelInfo info) {
            InfoFragment fragment = new InfoFragment();
            Bundle args = new Bundle();
            args.putParcelable(fragmentStreamerInfoArg, info);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_channel_description, container, false);

            info = getArguments().getParcelable(fragmentStreamerInfoArg);

            mPanelsRecyclerView = rootView.findViewById(R.id.panel_recyclerview);
            TextView mDescription = rootView.findViewById(R.id.description);
            findErrorView(rootView);

            if (info != null && info.getStreamDescription() != null && !info.getStreamDescription().equals("null") && !info.getStreamDescription().equals("")) {
                mDescription.setText(info.getStreamDescription());
            } else {
                showError();
            }

            setupPanels();

            return rootView;
        }

        private void setupPanels() {
            final PanelAdapter mPanelsAdapter = new PanelAdapter(getActivity());
            LinearLayoutManager llm = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
            llm.setAutoMeasureEnabled(true);
            mPanelsRecyclerView.setAdapter(mPanelsAdapter);
            mPanelsRecyclerView.setLayoutManager(llm);

            GetPanelsTask mTask = new GetPanelsTask(info.getStreamerName(), mPanelsAdapter::addPanels);
            mTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    public static class VodFragment extends ChannelFragment implements LazyFetchingActivity<VideoOnDemand> {
        protected AutoSpanRecyclerView mRecyclerView;
        protected VODAdapter mAdapter;
        private String LOG_TAG = getClass().getSimpleName();
        private ChannelInfo channelInfo;
        private boolean broadcasts, showError;
        private int limit = 10,
                offset = 0,
                maxElementsToFetch = 500;
        private ProgressView progressView;

        public static VodFragment newInstance(boolean broadcastsOnly, ChannelInfo channelInfo) {
            VodFragment fragment = new VodFragment();
            Bundle args = new Bundle();
            args.putParcelable(fragmentVodsStreamerInfoArg, channelInfo);
            args.putBoolean(fragmentVodsBroadCastsOnlyArg, broadcastsOnly);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        protected void showError() {
            super.showError();
            mErrorText.setText(getString(R.string.no_elements_added_notice, getString(R.string.vods)));
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_channel_vods, container, false);

            Bundle args = getArguments();
            channelInfo = args.getParcelable(fragmentVodsStreamerInfoArg);
            broadcasts = args.getBoolean(fragmentVodsBroadCastsOnlyArg);

            mRecyclerView = rootView.findViewById(R.id.recyclerview_vods);
            progressView = rootView.findViewById(R.id.circle_progress);

            findErrorView(rootView);
            if (showError) {
                showError();
            }

            if (mAdapter == null) {
                mRecyclerView.setBehaviour(new VODAutoSpanBehaviour());
                mAdapter = new VODAdapter(mRecyclerView, getActivity());
                mAdapter.setShowName(false);
                progressView.start();
            }

            mAdapter.setTopMargin((int) getResources().getDimension(R.dimen.search_new_adapter_top_margin));
            mAdapter.setSortElements(false);
            mAdapter.disableInsertAnimation();
            LazyFetchingOnScrollListener<VideoOnDemand> lazyFetchingOnScrollListener = new LazyFetchingOnScrollListener<>("VodFragment", this);
            mRecyclerView.addOnScrollListener(lazyFetchingOnScrollListener);
            mRecyclerView.setAdapter(mAdapter);
            mRecyclerView.setItemAnimator(null);
            mRecyclerView.setHasFixedSize(true);

            lazyFetchingOnScrollListener.checkForNewElements(mRecyclerView);

            return rootView;
        }

        private String getUrl() {
            String type = broadcasts ? "archive" : "highlight";
            return "https://api.twitch.tv/kraken/channels/" + channelInfo.getUserId() + "/videos?hls=true&limit=" + getLimit() + "&offset=" + getCurrentOffset() + "&broadcast_type=" + type;
        }

        public void setShownames(boolean shownames) {
            if (mAdapter != null) {
                mAdapter.setShowName(false);
            }
        }

        @Override
        public void addToAdapter(List<VideoOnDemand> aObjectList) {
            mAdapter.addList(aObjectList);
        }

        @Override
        public void startRefreshing() {

        }

        @Override
        public void stopRefreshing() {

        }

        @Override
        public int getCurrentOffset() {
            return offset;
        }

        @Override
        public void setCurrentOffset(int aOffset) {
            offset = aOffset;
        }

        @Override
        public void startProgress() {

        }

        @Override
        public void stopProgress() {
            progressView.stop();
        }

        @Override
        public int getLimit() {
            return limit;
        }

        @Override
        public void setLimit(int aLimit) {
            limit = aLimit;
        }

        @Override
        public int getMaxElementsToFetch() {
            return maxElementsToFetch;
        }

        @Override
        public void setMaxElementsToFetch(int aMax) {
            maxElementsToFetch = aMax;
        }

        @Override
        public void notifyUserNoElementsAdded() {

        }

        @Override
        public List<VideoOnDemand> getVisualElements() throws JSONException {
            List<VideoOnDemand> result = new ArrayList<>();
            final String VIDEOS_ARRAY = "videos";
            final String TOTAL_VODS_INT = "_total";
            JSONObject vodsTopObject = new JSONObject(Service.urlToJSONString(getUrl()));
            JSONArray vods = vodsTopObject.getJSONArray(VIDEOS_ARRAY);

            setMaxElementsToFetch(vodsTopObject.getInt(TOTAL_VODS_INT));
            for (int i = 0; i < vods.length(); i++) {
                VideoOnDemand vod = JSONService.getVod(vods.getJSONObject(i));
                vod.setChannelInfo(channelInfo);
                vod.setBroadcast(this.broadcasts);
                result.add(vod);
            }

            if (vodsTopObject.getInt(TOTAL_VODS_INT) <= 0) {
                getActivity().runOnUiThread(() -> {
                    if (mErrorEmote != null && mErrorText != null) {
                        showError();
                        showError = true;
                    }
                });
            }

            return result;
        }
    }

    private class SectionsPagerAdapter extends FragmentPagerAdapter {
        private final int POSITION_DESC = 0;
        private final int POSITION_BROADCASTS = 1;
        private final int POSITION_HIGHLIGHTS = 2;


        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            mDescriptionFragment = InfoFragment.newInstance(info);
            mBroadcastsFragment = VodFragment.newInstance(true, info);
            mHighlightsFragment = VodFragment.newInstance(false, info);
        }

        @Override
        public Fragment getItem(int position) {
            if (position == POSITION_DESC) {
                return mDescriptionFragment;
            } else if (position == POSITION_BROADCASTS) {
                return mBroadcastsFragment;
            } else if (position == POSITION_HIGHLIGHTS) {
                return mHighlightsFragment;
            } else {
                return InfoFragment.newInstance(info);
            }
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case POSITION_DESC:
                    return getResources().getString(R.string.streamerInfo_desc_tab);
                case POSITION_BROADCASTS:
                    return getResources().getString(R.string.streamerInfo_broadcasts_tab);
                case POSITION_HIGHLIGHTS:
                    return getResources().getString(R.string.streamerInfo_highlights_tab);
            }
            return null;
        }
    }
}
