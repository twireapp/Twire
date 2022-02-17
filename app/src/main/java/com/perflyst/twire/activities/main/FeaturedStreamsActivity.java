package com.perflyst.twire.activities.main;

import android.util.Log;

import com.perflyst.twire.R;
import com.perflyst.twire.adapters.MainActivityAdapter;
import com.perflyst.twire.adapters.StreamsAdapter;
import com.perflyst.twire.model.StreamInfo;
import com.perflyst.twire.service.JSONService;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.views.recyclerviews.AutoSpanRecyclerView;
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.AutoSpanBehaviour;
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.StreamAutoSpanBehaviour;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

public class FeaturedStreamsActivity extends LazyMainActivity<StreamInfo> {

    @Override
    protected int getActivityIconRes() {
        return R.drawable.ic_home;
    }

    @Override
    protected int getActivityTitleRes() {
        return R.string.featured_activity_title;
    }

    @Override
    protected AutoSpanBehaviour constructSpanBehaviour() {
        return new StreamAutoSpanBehaviour();
    }

    @Override
    protected void customizeActivity() {
        super.customizeActivity();
        setLimit(10);
        setMaxElementsToFetch(200);
        ((StreamsAdapter) mAdapter).setConsiderPriority(true); // Make sure the adapter takes into account the streams' priority when comparing streams
    }

    @Override
    protected MainActivityAdapter<StreamInfo, ?> constructAdapter(AutoSpanRecyclerView recyclerView) {
        return new StreamsAdapter(recyclerView, this);
    }

    @Override
    public void addToAdapter(List<StreamInfo> aObjectList) {
        mOnScrollListener.checkForNewElements(mRecyclerView);
        mAdapter.addList(aObjectList);
        Log.i(LOG_TAG, "Adding Featured Streams: " + aObjectList.size());
    }

    /**
     * Methods for functionality and for controlling the SwipeRefreshLayout
     */

    private String pagination = "";

    @Override
    public List<StreamInfo> getVisualElements() throws JSONException, MalformedURLException {
        List<StreamInfo> resultList = new ArrayList<>();

        //Indentation is meant to mimic the structure of the JSON code
        final String URL = "https://api.twitch.tv/helix/streams?first=" + getLimit() + (pagination != "" ? "&after=" + pagination : "");

        String jsonString = Service.urlToJSONStringHelix(URL, this);
        JSONObject fullDataObject = new JSONObject(jsonString);
        JSONArray topFeaturedArray = fullDataObject.getJSONArray("data");

        for (int i = 0; i < topFeaturedArray.length(); i++) {
            // Get all the JSON objects we need to get all the required data.
            JSONObject streamObject = topFeaturedArray.getJSONObject(i);
            StreamInfo mStreamInfo = JSONService.getStreamInfo(getBaseContext(), streamObject, false);
            mStreamInfo.setPriority(1);
            resultList.add(mStreamInfo);
        }

        return resultList;
    }
}
