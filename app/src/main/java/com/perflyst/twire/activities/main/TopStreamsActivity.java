package com.perflyst.twire.activities.main;

import static com.perflyst.twire.misc.Utils.getSystemLanguage;

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

public class TopStreamsActivity extends LazyMainActivity<StreamInfo> {

    @Override
    protected int getActivityIconRes() {
        return R.drawable.ic_group;
    }

    @Override
    protected int getActivityTitleRes() {
        return R.string.top_streams_activity_title;
    }

    @Override
    protected AutoSpanBehaviour constructSpanBehaviour() {
        return new StreamAutoSpanBehaviour();
    }

    @Override
    protected MainActivityAdapter<StreamInfo, ?> constructAdapter(AutoSpanRecyclerView recyclerView) {
        return new StreamsAdapter(recyclerView, this);
    }

    @Override
    public void addToAdapter(List<StreamInfo> streamsToAdd) {
        mOnScrollListener.checkForNewElements(mRecyclerView);
        mAdapter.addList(streamsToAdd);
        Log.i(LOG_TAG, "Adding Top Streams: " + streamsToAdd.size());
    }

    private String pagination = "";

    @Override
    public List<StreamInfo> getVisualElements() throws JSONException, MalformedURLException {
        final String languageFilter = settings.getGeneralFilterTopStreamsByLanguage() ? "&language=" + getSystemLanguage() : "";
        final String URL = "https://api.twitch.tv/helix/streams?first=" + getLimit() + (pagination != "" ? "&after=" + pagination : "") + languageFilter;

        List<StreamInfo> mResultList = new ArrayList<>();
        String jsonString = Service.urlToJSONStringHelix(URL, this);
        JSONObject fullDataObject = new JSONObject(jsonString);
        JSONArray topStreamsArray = fullDataObject.getJSONArray("data");
        this.pagination = fullDataObject.getJSONObject("pagination").getString("cursor");

        for (int i = 0; i < topStreamsArray.length(); i++) {
            JSONObject streamObject = topStreamsArray.getJSONObject(i);
            mResultList.add(JSONService.getStreamInfo(getBaseContext(), streamObject, false));
        }

        return mResultList;
    }
}
