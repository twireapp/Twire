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
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ChannelActivity extends ThemeActivity {
    private static final String fragmentStreamerInfoArg = "streamerInfoArg",
            fragmentVodsBroadCastsOnlyArg = "vodsBroadcastsOnlyArg",
            fragmentVodsStreamerInfoArg = "streamerNameArg";
    private final static int POSITION_DESC = 0;
    private final static int POSITION_BROADCASTS = 1;
    private final static int POSITION_HIGHLIGHTS = 2;
    private final static int TOTAL_COUNT = 3;
    private final int SHOW_FAB_DELAY = 300;
    private ChannelInfo info;
    private ImageView streamerImage;
    private Toolbar toolbar,
            additionalToolbar;
    private ViewPager2 mViewPager2;
    private TabLayout mTabLayout;
    private AppBarLayout mAppBar;
    private FloatingActionButton mFab;
    private int COLOR_FADE_DURATION = 0;
    private ChannelFragment mDescriptionFragment, mBroadcastsFragment, mHighlightsFragment;
    private FollowHandler mFollowHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_streamer_info);

        // Get the various handles of view and layouts that is part of this view
        streamerImage = findViewById(R.id.profileImageView);
        TextView streamerInfoName = findViewById(R.id.twitch_name);
        TextView streamerViewers = findViewById(R.id.txt_viewers);
        TextView streamerFollowers = findViewById(R.id.txt_followers);
        toolbar = findViewById(R.id.StreamerInfo_Toolbar);
        additionalToolbar = findViewById(R.id.additional_toolbar);
        mViewPager2 = findViewById(R.id.streamer_info_viewPager2);
        mTabLayout = findViewById(R.id.streamer_info_tabLayout);
        mAppBar = findViewById(R.id.appbar);
        mFab = findViewById(R.id.fab);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Get the StreamerInfo object sent with the intent to open this activity
        Intent intent = getIntent();
        info = intent.getParcelableExtra(getResources().getString(R.string.channel_info_intent_object));
        assert info != null;

        streamerInfoName.setText(info.getDisplayName());
        info.getFollowers(getApplicationContext(), followers -> streamerFollowers.setText(getReadableInt(followers.or(0))));
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
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.overridePendingTransition(R.anim.fade_in_semi_anim, R.anim.slide_out_bottom_anim);
    }

    private void setUpTabs() {
        mViewPager2.setAdapter(new ChannelStateAdapter(this));

        new TabLayoutMediator(mTabLayout, mViewPager2, (tab, position) -> {
            switch (position) {
                default: // Deliberate fall-through to description tab
                case POSITION_DESC:
                    tab.setText(R.string.streamerInfo_desc_tab);
                    break;
                case POSITION_BROADCASTS:
                    tab.setText(R.string.streamerInfo_broadcasts_tab);
                    break;
                case POSITION_HIGHLIGHTS:
                    tab.setText(R.string.streamerInfo_highlights_tab);
                    break;
            }
        }).attach();

        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mAppBar.setExpanded(tab.getText() != null &&
                        tab.getText().equals(getResources().getString(R.string.streamerInfo_desc_tab)), true);
                mViewPager2.setCurrentItem(tab.getPosition(), true);
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
        Target<Bitmap> mTarget = getLightThemeTarget();
        if (new Settings(this).isDarkTheme()) {
            mTarget = getNightThemeTarget();
        }

        Glide.with(getBaseContext())
                .asBitmap()
                .load(info.getMediumPreview())
                .into(mTarget);
    }

    private Target<Bitmap> getNightThemeTarget() {
        return new CustomTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
                streamerImage.setImageBitmap(bitmap);
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {

            }
        };
    }

    private Target<Bitmap> getLightThemeTarget() {
        return new CustomTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
                streamerImage.setImageBitmap(bitmap);

                Palette palette = Palette.from(bitmap).generate();
                int defaultColor = Service.getColorAttribute(R.attr.colorPrimary, R.color.primary, getBaseContext());
                int defaultDarkColor = Service.getColorAttribute(R.attr.colorPrimaryDark, R.color.primaryDark, getBaseContext());

                int vibrant = palette.getVibrantColor(defaultColor);
                int vibrantDark = palette.getDarkVibrantColor(defaultColor);
                int vibrantLight = palette.getLightVibrantColor(defaultColor);

                int muted = palette.getMutedColor(defaultColor);
                int mutedDark = palette.getDarkMutedColor(defaultColor);

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
                    float[] newSwatch = {swatchValues[0], 0.85f, 0.85f};
                    float[] newSwatchComposite = {(swatchValues[0] + 180) % 360, newSwatch[1], newSwatch[2]};
                    float[] newSwatchDark = {newSwatch[0], newSwatch[1], 0.6f};

                    int newColorDark = Color.HSVToColor(newSwatchDark);
                    int newColor = Color.HSVToColor(newSwatch);
                    int compositeNewColor = Color.HSVToColor(newSwatchComposite);

                    int primaryColor = Service.getBackgroundColorFromView(toolbar, defaultColor);
                    int primaryColorDark = Service.getBackgroundColorFromView(mTabLayout, defaultDarkColor);

                    Service.animateBackgroundColorChange(toolbar, newColor, primaryColor, COLOR_FADE_DURATION);
                    Service.animateBackgroundColorChange(additionalToolbar, newColor, primaryColor, COLOR_FADE_DURATION);
                    Service.animateBackgroundColorChange(mTabLayout, newColorDark, primaryColorDark, COLOR_FADE_DURATION);
                    mFab.setBackgroundTintList(ColorStateList.valueOf(compositeNewColor));
                    mTabLayout.setSelectedTabIndicatorColor(compositeNewColor);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        Window window = getWindow();
                        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                        window.setStatusBarColor(newColorDark);
                    }
                }
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {

            }
        };
    }

    private void initiateFAB() {
        mFollowHandler = new FollowHandler(
                info,
                getBaseContext(),
                () -> mFab.hide()
        );

        // If the channel got imported from Twitch, then hide the Follow/Unfollow Button
        if (mFollowHandler.isStreamerTwitch()) {
            hideFAB();
        } else {
            mFab.setOnClickListener(v -> {
                if (mFollowHandler.isStreamerFollowed()) {
                    mFollowHandler.unfollowStreamer();
                } else {
                    mFollowHandler.followStreamer();
                }

                hideFAB();
                new Handler().postDelayed(() -> {
                    updateFABIcon(mFollowHandler.isStreamerFollowed());
                    showFAB();
                }, SHOW_FAB_DELAY);
            });
            updateFABIcon(mFollowHandler.isStreamerFollowed());
        }
    }

    private void updateFABIcon(boolean isFollowing) {
        @DrawableRes int imageRes = isFollowing
                ? R.drawable.ic_heart_broken
                : R.drawable.ic_favorite;
        mFab.setImageResource(imageRes);
    }

    private void hideFAB() {
        int HIDE_FAB_DURATION = 200;
        mFab.animate()
                .translationY(getResources().getDimension(R.dimen.streamerInfo_fab_size) + getResources().getDimension(R.dimen.streamerInfo_fab_margin))
                .setDuration(HIDE_FAB_DURATION)
                .setInterpolator(new AccelerateInterpolator())
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        mFab.setClickable(false);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mFab.setVisibility(View.INVISIBLE);
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

    private void showFAB() {
        int SHOW_FAB_DURATION = 300;
        mFab.animate()
                .translationY(0)
                .setDuration(SHOW_FAB_DURATION)
                .setInterpolator(new OvershootInterpolator())
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        mFab.setVisibility(View.VISIBLE);
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

            if (getArguments() != null) {
                info = getArguments().getParcelable(fragmentStreamerInfoArg);
            }

            mPanelsRecyclerView = rootView.findViewById(R.id.panel_recyclerview);
            TextView mDescription = rootView.findViewById(R.id.description);
            findErrorView(rootView);

            if (info != null && info.getStreamDescription() != null && !info.getStreamDescription().equals("null") && !info.getStreamDescription().isEmpty()) {
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
            mPanelsRecyclerView.setAdapter(mPanelsAdapter);
            mPanelsRecyclerView.setLayoutManager(llm);

            GetPanelsTask mTask = new GetPanelsTask(info.getLogin(), mPanelsAdapter::addPanels);
            mTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    public static class VodFragment extends ChannelFragment implements LazyFetchingActivity<VideoOnDemand> {
        protected AutoSpanRecyclerView mRecyclerView;
        protected VODAdapter mAdapter;
        private ChannelInfo channelInfo;
        private boolean broadcasts, showError;
        private int limit = 20,
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
            if (args != null) {
                channelInfo = args.getParcelable(fragmentVodsStreamerInfoArg);
                broadcasts = args.getBoolean(fragmentVodsBroadCastsOnlyArg);
            }

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

        private String pagination = "";

        private String getUrl() {
            String type = broadcasts ? "archive" : "highlight";
            return "https://api.twitch.tv/helix/videos?user_id=" + channelInfo.getUserId() + "&first=" + getLimit() + "&type=" + type + (pagination != "" ? "&after=" + pagination : "");
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
        public String getCursor() {
            return pagination;
        }

        @Override
        public void setCursor(String cursor) {
            pagination = cursor;
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

        private boolean archive_done = false;
        private boolean highlight_done = false;

        @Override
        public List<VideoOnDemand> getVisualElements() throws JSONException {
            String type = broadcasts ? "archive" : "highlight";

            List<VideoOnDemand> result = new ArrayList<>();

            if (type == "archive" && archive_done || type == "highlight" && highlight_done) {
                return result;
            }

            if (type == "archive") {
                archive_done = true;
            } else if (type == "highlight") {
                highlight_done = true;
            }

            ArrayList<String> vods = new ArrayList<>();
            int fetch = 1;
            // this takes around 1.5 Seconds on WIFI with 120 Vods
            while (fetch == 1) {
                JSONObject vodsTopObject = new JSONObject(Service.urlToJSONStringHelix(getUrl(), getContext()));
                JSONArray temp_vods = vodsTopObject.getJSONArray("data");
                for (int x=0; x < temp_vods.length(); x++) {
                    vods.add(temp_vods.getString(x));
                }

                // check if the request returns a cursor for our pagination system
                if (vodsTopObject.getJSONObject("pagination").has("cursor")) {
                    // set our pagination cursor and keep fetching data
                    if (pagination == vodsTopObject.getJSONObject("pagination").getString("cursor")) {
                        pagination = "";
                        fetch = 0;
                    }
                    pagination = vodsTopObject.getJSONObject("pagination").getString("cursor");
                } else {
                    // there is no cursor = no more data
                    pagination = "";
                    fetch = 0;
                }
            }

            setMaxElementsToFetch(vods.size());
            for (int i = 0; i < vods.size(); i++) {
                JSONObject temp_object = new JSONObject(vods.get(i));
                VideoOnDemand vod = JSONService.getVod(temp_object);
                vod.setChannelInfo(channelInfo);
                vod.setBroadcast(this.broadcasts);
                result.add(vod);
            }

            if (vods.size() <= 0 && getActivity() != null) {
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

    private class ChannelStateAdapter extends FragmentStateAdapter {

        ChannelStateAdapter(final FragmentActivity fa) {
            super(fa);
            mDescriptionFragment = InfoFragment.newInstance(info);
            mBroadcastsFragment = VodFragment.newInstance(true, info);
            mHighlightsFragment = VodFragment.newInstance(false, info);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                default: // Deliberate fall-through to description tab
                case POSITION_DESC:
                    return mDescriptionFragment;
                case POSITION_BROADCASTS:
                    return mBroadcastsFragment;
                case POSITION_HIGHLIGHTS:
                    return mHighlightsFragment;
            }
        }

        @Override
        public int getItemCount() {
            return TOTAL_COUNT;
        }
    }
}
