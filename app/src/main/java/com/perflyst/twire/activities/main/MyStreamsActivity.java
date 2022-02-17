package com.perflyst.twire.activities.main;

import android.os.AsyncTask;
import android.util.Log;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.perflyst.twire.R;
import com.perflyst.twire.adapters.MainActivityAdapter;
import com.perflyst.twire.adapters.StreamsAdapter;
import com.perflyst.twire.model.ChannelInfo;
import com.perflyst.twire.model.StreamInfo;
import com.perflyst.twire.service.JSONService;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.service.TempStorage;
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
        String helix_url = "https://api.twitch.tv/helix/streams?first=100";

        ArrayList<ChannelInfo> channels;
        if (TempStorage.hasLoadedStreamers()) {
            channels = new ArrayList<>(TempStorage.getLoadedStreamers());
        } else {
            GetFollowsFromDB subscriptionsTask = new GetFollowsFromDB();
            subscriptionsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getBaseContext());
            channels = new ArrayList<>(subscriptionsTask.get().values());
        }

        // loop over all the channels in the DB in chunks of 100
        List<StreamInfo> mResultList = new ArrayList<>();
        final String ARRAY_KEY = "data";
        for (List<ChannelInfo> chunk : Lists.partition(channels, 100)) {
            String url = helix_url + Joiner.on("").join(Lists.transform(chunk, channelInfo -> "&user_id=" + channelInfo.getUserId()));
            // request the url
            String temp_jsonString = Service.urlToJSONStringHelix(url, this);
            JSONObject fullDataObject = new JSONObject(temp_jsonString);
            // create the array
            JSONArray temp_array = fullDataObject.getJSONArray(ARRAY_KEY);
            // append the new array to the final one
            for (int i = 0; i < temp_array.length(); i++) {
                JSONObject streamObject = temp_array.getJSONObject(i);
                mResultList.add(JSONService.getStreamInfo(getBaseContext(), streamObject, false));
            }
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
