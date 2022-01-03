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
        String helix_url = "https://api.twitch.tv/helix/streams?first=" + getLimit();
        String user_logins = "";

        GetFollowsFromDB subscriptionsTask = new GetFollowsFromDB();
        subscriptionsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getBaseContext());

        ArrayList<String> requesturls = new ArrayList<>();
        int number = 0;
        int exactnumber = 0;

        // loop over all channel in the DB
        for (ChannelInfo si : subscriptionsTask.get().values()) {
            // if the number of channels, already in the url, is smaller than 99 and is not the last channel
            // e.g. if there are 160 Channels in the DB then this will result in 2 request urls ([0-99] and [100-159])
            if (number <= 99 && exactnumber != subscriptionsTask.get().values().size() -1) {
                user_logins = user_logins + "&user_id=" + si.getUserId();
                number++;
                // if the request url has 100 user ids or is the last channel in the list
            } else if (number > 99 || exactnumber == (subscriptionsTask.get().values().size() -1)) {
                // add the new request url to the list
                requesturls.add(helix_url + user_logins);
                // reset stuff
                user_logins = "";
                number = 0;
            }
            exactnumber++;
        }

        Log.d(LOG_TAG, requesturls.toString());

        JSONArray final_array = new JSONArray();
        final String ARRAY_KEY = "data";
        String jsonString;

        // for every request url in the list
        for (int i=0; i<requesturls.size(); i++) {
            String temp_jsonString;
            // request the url
            temp_jsonString = Service.urlToJSONStringHelix(requesturls.get(i), this);
            JSONObject fullDataObject = new JSONObject(temp_jsonString);
            // create the array
            JSONArray temp_array = fullDataObject.getJSONArray(ARRAY_KEY);
            // append the new array to the final one
            for (int x=0; x<temp_array.length(); x++) {
                final_array.put(temp_array.get(x));
            }
        }
        Log.d(LOG_TAG, final_array.toString());


        List<StreamInfo> mResultList = new ArrayList<>();

        for (int i = 0; i < final_array.length(); i++) {
            JSONObject streamObject = final_array.getJSONObject(i);
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
