package com.perflyst.twire.activities;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.perflyst.twire.R;
import com.perflyst.twire.activities.main.LazyFetchingActivity;
import com.perflyst.twire.adapters.ChannelsAdapter;
import com.perflyst.twire.adapters.GamesAdapter;
import com.perflyst.twire.adapters.MainActivityAdapter;
import com.perflyst.twire.adapters.StreamsAdapter;
import com.perflyst.twire.misc.LazyFetchingOnScrollListener;
import com.perflyst.twire.model.ChannelInfo;
import com.perflyst.twire.model.Game;
import com.perflyst.twire.model.MainElement;
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
    private static final int POSITION_STREAMS = 0;
    private static final int POSITION_CHANNELS = 1;
    private static final int POSITION_GAMES = 2;
    private static final int TOTAL_COUNT = 3;
    private final String LOG_TAG = getClass().getSimpleName();
    @BindView(R.id.search_viewPager2)
    protected ViewPager2 mViewPager2;
    @BindView(R.id.ic_back_arrow)
    protected ImageView mBackIcon;
    @BindView(R.id.edit_text_search)
    protected EditText mSearchText;
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
        // Set up the ViewPager2 with the sections adapter.
        mViewPager2.setAdapter(new SearchStateAdapter(this));

        TabLayout tabLayout = findViewById(R.id.search_tabLayout);
        new TabLayoutMediator(tabLayout, mViewPager2, (tab, position) -> {
            switch (position) {
                default: // Deliberate fall-through to stream tab
                case POSITION_STREAMS:
                    tab.setText(R.string.streams_tab);
                    break;
                case POSITION_CHANNELS:
                    tab.setText(R.string.streamers_tab);
                    break;
                case POSITION_GAMES:
                    tab.setText(R.string.games_tab);
                    break;
            }
        }).attach();
    }

    public String getQuery() {
        return query;
    }

    public static class SearchGamesFragment extends SearchFragment<Game> {
        static SearchGamesFragment newInstance() {
            return new SearchGamesFragment();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return super.onCreateView(inflater, container, savedInstanceState);
        }

        @Override
        public String getElementsURL() {
            return "https://api.twitch.tv/helix/search/categories?query=" + query + "&first=" + getLimit() + getPagination();
        }

        @Override
        public AutoSpanBehaviour constructBehaviour() {
            return new GameAutoSpanBehaviour();
        }

        @Override
        public MainActivityAdapter<Game, ?> constructAdapter() {
            return new GamesAdapter(mRecyclerView, getContext(), getActivity());
        }

        @Override
        public void notifyUserNoElementsAdded() {
        }

        @Override
        public List<Game> getVisualElements() throws JSONException {
            List<Game> mGames = new ArrayList<>();
            if (query != null) {
                String URL = getElementsURL();
                JSONObject fullDataObject = new JSONObject(Service.urlToJSONStringHelix(URL, getContext()));
                String GAMES_ARRAY = "data";
                JSONArray gamesArray = fullDataObject.getJSONArray(GAMES_ARRAY);

                for (int i = 0; i < gamesArray.length(); i++) {
                    JSONObject gameObject = gamesArray.getJSONObject(i);
                    mGames.add(JSONService.getGame(gameObject));
                }

                setCursorFromResponse(fullDataObject);
            }
            return mGames;
        }
    }

    public static class SearchStreamsFragment extends SearchFragment<StreamInfo> {
        static SearchStreamsFragment newInstance() {
            return new SearchStreamsFragment();
        }

        @Override
        public String getElementsURL() {
            return "https://api.twitch.tv/helix/search/channels?query=" + query + "&first=" + getLimit() + getPagination() + "&live_only=true";
        }

        @Override
        public AutoSpanBehaviour constructBehaviour() {
            return new StreamAutoSpanBehaviour();
        }

        @Override
        public MainActivityAdapter<StreamInfo, ?> constructAdapter() {
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
                String URL = getElementsURL();
                JSONObject fullDataObject = new JSONObject(Service.urlToJSONStringHelix(URL, getContext()));
                String STREAMS_ARRAY = "data";
                JSONArray mStreamsArray = fullDataObject.getJSONArray(STREAMS_ARRAY);

                List<String> ids = new ArrayList<>();
                for (int i = 0; i < mStreamsArray.length(); i++) {
                    JSONObject streamObject = mStreamsArray.getJSONObject(i);
                    ids.add(streamObject.getString("id"));
                }

                String url = "https://api.twitch.tv/helix/streams?" + Joiner.on("&").join(Lists.transform(ids, id -> "user_id=" + id));
                JSONArray result = new JSONObject(Service.urlToJSONStringHelix(url, getContext())).getJSONArray("data");
                for (int i = 0; i < result.length(); i++) {
                    mStreams.add(JSONService.getStreamInfo(getContext(), result.getJSONObject(i), false));
                }

                setCursorFromResponse(fullDataObject);
            }

            return mStreams;
        }
    }

    public static class SearchChannelsFragment extends SearchFragment<ChannelInfo> {
        static SearchChannelsFragment newInstance() {
            return new SearchChannelsFragment();
        }

        @Override
        public void reset(String searchQuery) {
            super.reset(searchQuery);
        }

        @Override
        public String getElementsURL() {
            return "https://api.twitch.tv/helix/search/channels?query=" + query + "&limit=" + getLimit() + getPagination();
        }

        @Override
        public AutoSpanBehaviour constructBehaviour() {
            return new ChannelAutoSpanBehaviour();
        }

        @Override
        public MainActivityAdapter<ChannelInfo, ?> constructAdapter() {
            return new ChannelsAdapter(mRecyclerView, getContext(), getActivity());
        }

        @Override
        public void notifyUserNoElementsAdded() {
        }

        @Override
        public List<ChannelInfo> getVisualElements() throws JSONException, MalformedURLException {
            List<ChannelInfo> mStreamers = new ArrayList<>();
            if (query != null) {
                String URL = getElementsURL();
                JSONObject fullDataObject = new JSONObject(Service.urlToJSONStringHelix(URL, getContext()));
                String CHANNELS_ARRAY = "data";
                JSONArray mChannelArray = fullDataObject.getJSONArray(CHANNELS_ARRAY);

                List<String> ids = new ArrayList<>();
                for (int i = 0; i < mChannelArray.length(); i++) {
                    JSONObject channel = mChannelArray.getJSONObject(i);
                    ids.add(channel.getString("id"));
                }

                String url = "https://api.twitch.tv/helix/users?" + Joiner.on("&").join(Lists.transform(ids, id -> "id=" + id));
                JSONArray result = new JSONObject(Service.urlToJSONStringHelix(url, getContext())).getJSONArray("data");
                for (int i = 0; i < result.length(); i++) {
                    mStreamers.add(JSONService.getStreamerInfo(getContext(), result.getJSONObject(i)));
                }

                setCursorFromResponse(fullDataObject);
            }
            return mStreamers;
        }
    }

    public static abstract class SearchFragment<E extends Comparable<E> & MainElement> extends Fragment implements LazyFetchingActivity<E> {
        protected MainActivityAdapter<E, ?> mAdapter;
        @BindView(R.id.span_recyclerview)
        protected AutoSpanRecyclerView mRecyclerView;
        @BindView(R.id.circle_progress)
        protected ProgressView mProgressView;
        String query = null;
        private LazyFetchingOnScrollListener<E> lazyFetchingOnScrollListener;
        private int limit = 20,
                maxElementsToFetch = 500;
        private String cursor = null;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_search, container, false);
            ButterKnife.bind(this, rootView);

            lazyFetchingOnScrollListener = new LazyFetchingOnScrollListener<>("SearchFragment", this);

            setupRecyclerViewAndAdapter();
            checkForQuery();

            // this let keyboard only close when there is a blank page.
            // on swipe or clicking on buttons (stream,games..) won't make keyboard close
            mRecyclerView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    InputMethodManager imm = (InputMethodManager)
                            getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    return false;
                }
            });

            return rootView;
        }

        private void setupRecyclerViewAndAdapter() {
            mRecyclerView.setBehaviour(constructBehaviour());
            mRecyclerView.addOnScrollListener(lazyFetchingOnScrollListener);
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
                setCursor(null);
                mAdapter.clearNoAnimation();
                mRecyclerView.scrollToPosition(0);
                startProgress();
                startRefreshing();
                lazyFetchingOnScrollListener.resetAndFetch(mRecyclerView);
                mProgressView.start();
            }
        }

        protected String getPagination() {
            return getCursor() == null ? "" : ("&after=" + getCursor());
        }

        protected void setCursorFromResponse(JSONObject response) throws JSONException {
            setCursor(response.getJSONObject("pagination").getString("cursor"));
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
        public String getCursor() {
            return cursor;
        }

        @Override
        public void setCursor(String cursor) {
            this.cursor = cursor;
        }

        @Override
        public void stopRefreshing() {
        }

        @Override
        public void startRefreshing() {
        }

        @Override
        public void addToAdapter(List<E> aObjectList) {
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

        public abstract String getElementsURL();

        public abstract AutoSpanBehaviour constructBehaviour();

        public abstract MainActivityAdapter<E, ?> constructAdapter();
    }

    private class SearchStateAdapter extends FragmentStateAdapter {

        SearchStateAdapter(final FragmentActivity fa) {
            super(fa);
            mStreamsFragment = SearchStreamsFragment.newInstance();
            mChannelsFragment = SearchChannelsFragment.newInstance();
            mGamesFragment = SearchGamesFragment.newInstance();
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                default: // Deliberate fall-through to stream tab
                case POSITION_STREAMS:
                    return mStreamsFragment;
                case POSITION_CHANNELS:
                    return mChannelsFragment;
                case POSITION_GAMES:
                    return mGamesFragment;
            }
        }

        @Override
        public int getItemCount() {
            return TOTAL_COUNT;
        }
    }
}
