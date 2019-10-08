package com.perflyst.twire.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.perflyst.twire.R;
import com.perflyst.twire.activities.main.LazyFetchingActivity;
import com.perflyst.twire.adapters.ChannelsAdapter;
import com.perflyst.twire.adapters.GamesAdapter;
import com.perflyst.twire.adapters.MainActivityAdapter;
import com.perflyst.twire.adapters.StreamsAdapter;
import com.perflyst.twire.misc.LazyFetchingOnScrollListener;
import com.perflyst.twire.model.ChannelInfo;
import com.perflyst.twire.model.Game;
import com.perflyst.twire.model.StreamInfo;
import com.perflyst.twire.service.JSONService;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.views.recyclerviews.AutoSpanRecyclerView;
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.AutoSpanBehaviour;
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.ChannelAutoSpanBehaviour;
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.GameAutoSpanBehaviour;
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.StreamAutoSpanBehaviour;
import com.rey.material.widget.ProgressView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;


public class SearchActivity extends ThemeActivity {
    @BindView(R.id.container)
    protected ViewPager mViewPager;
    @BindView(R.id.ic_back_arrow)
    protected ImageView mBackIcon;
    @BindView(R.id.edit_text_search)
    protected EditText mSearchText;
    private String LOG_TAG = getClass().getSimpleName();
    private String query;
    private SearchFragment mStreamsFragment, mChannelsFragment, mGamesFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        ButterKnife.bind(this);

        setUpTabs();

        mSearchText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d(LOG_TAG, "Text changed. Resetting fragments");
                String newQuery = s.toString().replace(" ", "%20");
                mChannelsFragment.reset(newQuery);
                mStreamsFragment.reset(newQuery);
                mGamesFragment.reset(newQuery);
                query = newQuery;
            }

            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
        });
        mBackIcon.setOnClickListener(v -> onBackPressed());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_in_semi_anim, R.anim.slide_out_bottom_anim);
    }

    private void setUpTabs() {
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = findViewById(R.id.tabs);
        assert tabLayout != null;
        tabLayout.setupWithViewPager(mViewPager);
    }

    public String getQuery() {
        return query;
    }

    public static class SearchGamesFragment extends SearchFragment<Game> {
        private String LOG_TAG = getClass().getSimpleName();

        static SearchGamesFragment newInstance() {
            return new SearchGamesFragment();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return super.onCreateView(inflater, container, savedInstanceState);
        }

        @Override
        public String getElementsURL(String query) {
            return "https://api.twitch.tv/kraken/search/games?type=suggest&query=" + query;
        }

        @Override
        public AutoSpanBehaviour constructBehaviour() {
            return new GameAutoSpanBehaviour();
        }

        @Override
        public MainActivityAdapter constructAdapter() {
            return new GamesAdapter(mRecyclerView, getContext(), getActivity());
        }

        @Override
        public void notifyUserNoElementsAdded() {
        }

        @Override
        public List<Game> getVisualElements() throws JSONException {
            List<Game> mGames = new ArrayList<>();
            if (query != null) {
                String URL = getElementsURL(query);
                JSONObject fullDataObject = new JSONObject(Service.urlToJSONString(URL));
                String GAMES_ARRAY = "games";
                JSONArray gamesArray = fullDataObject.getJSONArray(GAMES_ARRAY);

                for (int i = 0; i < gamesArray.length(); i++) {
                    JSONObject gameObject = gamesArray.getJSONObject(i);
                    mGames.add(JSONService.getGame(gameObject));
                }
            }
            return mGames;
        }
    }

    public static class SearchStreamsFragment extends SearchFragment<StreamInfo> {
        private String LOG_TAG = getClass().getSimpleName();

        static SearchStreamsFragment newInstance() {
            return new SearchStreamsFragment();
        }

        @Override
        public String getElementsURL(String searchQuery) {
            return "https://api.twitch.tv/kraken/search/streams?query=" + searchQuery + "&limit=" + getLimit() + "&offset=" + getCurrentOffset();
        }

        @Override
        public AutoSpanBehaviour constructBehaviour() {
            return new StreamAutoSpanBehaviour();
        }

        @Override
        public MainActivityAdapter constructAdapter() {
            StreamsAdapter adapter = new StreamsAdapter(mRecyclerView, getActivity());
            adapter.setConsiderPriority(false);
            return adapter;
        }

        @Override
        public void notifyUserNoElementsAdded() {

        }

        @Override
        public List<StreamInfo> getVisualElements() throws JSONException, MalformedURLException {
            List<StreamInfo> mStreams = new ArrayList<>();
            if (query != null) {
                String URL = getElementsURL(query);
                JSONObject fullDataObject = new JSONObject(Service.urlToJSONString(URL));
                String STREAMS_ARRAY = "streams";
                JSONArray mStreamsArray = fullDataObject.getJSONArray(STREAMS_ARRAY);
                String TOTAL_IN_QUERY_INT = "_total";
                int totalChannels = fullDataObject.getInt(TOTAL_IN_QUERY_INT);

                for (int i = 0; i < mStreamsArray.length(); i++) {
                    JSONObject streamObject = mStreamsArray.getJSONObject(i);
                    mStreams.add(JSONService.getStreamInfo(getContext(), streamObject, null, false));
                }
            }

            return mStreams;
        }
    }

    public static class SearchChannelsFragment extends SearchFragment<ChannelInfo> {
        private String LOG_TAG = getClass().getSimpleName();

        static SearchChannelsFragment newInstance() {
            return new SearchChannelsFragment();
        }

        @Override
        public void reset(String searchQuery) {
            super.reset(searchQuery);
        }

        @Override
        public String getElementsURL(String searchQuery) {
            return "https://api.twitch.tv/kraken/search/channels?limit=" + getLimit() + "&offset=" + getCurrentOffset() + "&query=" + searchQuery;
        }

        @Override
        public AutoSpanBehaviour constructBehaviour() {
            return new ChannelAutoSpanBehaviour();
        }

        @Override
        public MainActivityAdapter constructAdapter() {
            return new ChannelsAdapter(mRecyclerView, getContext(), getActivity());
        }

        @Override
        public void notifyUserNoElementsAdded() {
        }

        @Override
        public List<ChannelInfo> getVisualElements() throws JSONException, MalformedURLException {
            List<ChannelInfo> mStreamers = new ArrayList<>();
            if (query != null) {
                String URL = getElementsURL(query);
                JSONObject fullDataObject = new JSONObject(Service.urlToJSONString(URL));
                String CHANNELS_ARRAY = "channels";
                JSONArray mChannelArray = fullDataObject.getJSONArray(CHANNELS_ARRAY);
                String TOTAL_IN_QUERY_INT = "_total";
                int totalChannels = fullDataObject.getInt(TOTAL_IN_QUERY_INT);

                for (int i = 0; i < mChannelArray.length(); i++) {
                    JSONObject channel = mChannelArray.getJSONObject(i);
                    mStreamers.add(JSONService.getStreamerInfo(channel, false));
                }

            }
            return mStreamers;
        }
    }

    public static abstract class SearchFragment<T> extends Fragment implements LazyFetchingActivity<T> {
        protected MainActivityAdapter mAdapter;
        @BindView(R.id.span_recyclerview)
        protected AutoSpanRecyclerView mRecyclerView;
        @BindView(R.id.circle_progress)
        protected ProgressView mProgressView;
        String query = null;
        private String LOG_TAG = getClass().getSimpleName();
        private LazyFetchingOnScrollListener<T> lazyFetchingOnScrollListener;
        private int limit = 10,
                offset = 0,
                maxElementsToFetch = 500;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public void onResume() {
            super.onResume();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_search, container, false);
            ButterKnife.bind(this, rootView);

            lazyFetchingOnScrollListener = new LazyFetchingOnScrollListener<>("SearchFragment", this);

            setupRecyclerViewAndAdapter();
            checkForQuery();

            return rootView;
        }

        private void setupRecyclerViewAndAdapter() {
            mRecyclerView.setBehaviour(constructBehaviour());
            mRecyclerView.setOnScrollListener(lazyFetchingOnScrollListener);
            mRecyclerView.setItemAnimator(null);
            mRecyclerView.setHasFixedSize(true);

            if (mAdapter == null) {
                mAdapter = constructAdapter();
                mAdapter.setTopMargin((int) getResources().getDimension(R.dimen.search_new_adapter_top_margin));
                mAdapter.setSortElements(false);
            }

            mRecyclerView.setAdapter(mAdapter);
        }

        private void checkForQuery() {
            if (!(getActivity() instanceof SearchActivity)) {
                throw new IllegalStateException("This fragment can only be used with SearchActivity");
            }

            String activityQuery = ((SearchActivity) getActivity()).getQuery();
            if (activityQuery != null && !activityQuery.equals(query)) {
                reset(activityQuery);
            }
        }

        public void reset(String searchQuery) {
            if (mAdapter != null && mRecyclerView != null) {
                query = searchQuery;
                setCurrentOffset(0);
                mAdapter.clearNoAnimation();
                mRecyclerView.scrollToPosition(0);
                startProgress();
                startRefreshing();
                lazyFetchingOnScrollListener.resetAndFetch(mRecyclerView);
                mProgressView.start();
            }
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
        public void stopProgress() {
            mProgressView.stop();
        }

        @Override
        public void startProgress() {
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
        public void stopRefreshing() {
        }

        @Override
        public void startRefreshing() {
        }

        @Override
        public void addToAdapter(List<T> aObjectList) {
            mAdapter.addList(aObjectList);
        }

        @Override
        public int getMaxElementsToFetch() {
            return maxElementsToFetch;
        }

        @Override
        public void setMaxElementsToFetch(int aMax) {
            maxElementsToFetch = aMax;
        }

        public abstract String getElementsURL(String searchQuery);

        public abstract AutoSpanBehaviour constructBehaviour();

        public abstract MainActivityAdapter constructAdapter();
    }

    private class SectionsPagerAdapter extends FragmentPagerAdapter {
        private final int POSITION_STREAMS = 0;
        private final int POSITION_CHANNELS = 1;
        private final int POSITION_GAMES = 2;


        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            mStreamsFragment = SearchStreamsFragment.newInstance();
            mChannelsFragment = SearchChannelsFragment.newInstance();
            mGamesFragment = SearchGamesFragment.newInstance();
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case POSITION_STREAMS:
                    return mStreamsFragment;
                case POSITION_CHANNELS:
                    return mChannelsFragment;
                case POSITION_GAMES:
                    return mGamesFragment;
                default:
                    return SearchStreamsFragment.newInstance();
            }
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case POSITION_STREAMS:
                    return getResources().getString(R.string.streams_tab);
                case POSITION_CHANNELS:
                    return getResources().getString(R.string.streamers_tab);
                case POSITION_GAMES:
                    return getResources().getString(R.string.games_tab);
            }
            return null;
        }
    }
}
