package com.perflyst.twire.tasks;

import android.content.Context;

import com.perflyst.twire.service.Service;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;

/**
 * Created by Sebastian Rask on 17-09-2016.
 */
public class GetStreamViewersTask implements Callable<Integer> {
    private final String streamerUserId;
    private final WeakReference<Context> context;


    public GetStreamViewersTask(String streamerUserId, Context context) {
        this.streamerUserId = streamerUserId;
        this.context = new WeakReference<>(context);
    }

    public Integer call() {
        try {
            final String URL = "https://api.twitch.tv/helix/streams?user_id=" + this.streamerUserId + "&first=1";
            final String VIEWERS_INT = "viewer_count";

            JSONObject topObject = new JSONObject(Service.urlToJSONStringHelix(URL, this.context.get()));
            JSONArray steamArray = topObject.getJSONArray("data");
            JSONObject streamObject = steamArray.getJSONObject(0);

            return streamObject.getInt(VIEWERS_INT);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return -1;
    }
}
