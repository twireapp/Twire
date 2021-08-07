package com.perflyst.twire.activities.main;

import android.os.AsyncTask;
import android.util.Log;

import com.perflyst.twire.R;
import com.perflyst.twire.adapters.MainActivityAdapter;
import com.perflyst.twire.adapters.StreamsAdapter;
import com.perflyst.twire.model.ChannelInfo;
import com.perflyst.twire.model.StreamInfo;
import com.perflyst.twire.service.JSONService;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.tasks.GetFollowsFromDB;
import com.perflyst.twire.views.recyclerviews.AutoSpanRecyclerView;
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.AutoSpanBehaviour;
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.StreamAutoSpanBehaviour;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MyStreamsActivity extends LazyMainActivity<StreamInfo> {

    @Override
    protected int getActivityIconRes() {
        return R.drawable.ic_favorite;
    }

    @Override
    protected int getActivityTitleRes() {
        return R.string.my_streams_activity_title;
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
    public void addToAdapter(List<StreamInfo> aObjectList) {
        mOnScrollListener.checkForNewElements(mRecyclerView);
        mAdapter.addList(aObjectList);
        Log.i(LOG_TAG, "Adding Top Streams: " + aObjectList.size());
    }

    @Override
    public List<StreamInfo> getVisualElements() throws JSONException, ExecutionException, InterruptedException, MalformedURLException {
        // build the api link
        String kraken_url = "https://api.twitch.tv/kraken/streams?limit=" + getLimit() + "&stream_type=live&offset=" + getCurrentOffset()+ "&channel=";
        String user_logins = "";

        GetFollowsFromDB subscriptionsTask = new GetFollowsFromDB();
        subscriptionsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getBaseContext());

        for (ChannelInfo si : subscriptionsTask.get().values()) {
            user_logins = user_logins + si.getUserId() + ",";
        }


        kraken_url = kraken_url + user_logins;

        final String ARRAY_KEY = "streams";

        List<StreamInfo> mResultList = new ArrayList<>();
        String jsonString = Service.urlToJSONString(kraken_url);
        JSONObject fullDataObject = new JSONObject(jsonString);
        JSONArray topStreamsArray = fullDataObject.getJSONArray(ARRAY_KEY);

        for (int i = 0; i < topStreamsArray.length(); i++) {
            JSONObject streamObject = topStreamsArray.getJSONObject(i);
            mResultList.add(JSONService.getStreamInfo(getBaseContext(), streamObject, null, false));
        }

        int elementsToFetch = mAdapter.getItemCount() + mResultList.size();
        if (!mResultList.isEmpty()) {
            elementsToFetch += 1;
        }
        setMaxElementsToFetch(elementsToFetch);

        shouldShowErrorView();

        return mResultList;
    }
}
