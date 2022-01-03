package com.perflyst.twire.tasks;

import android.content.Context;
import android.os.AsyncTask;

import com.perflyst.twire.service.Service;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Sebastian Rask on 17-09-2016.
 */
public class GetStreamViewersTask extends AsyncTask<Void, Void, Integer> {
    private final GetStreamViewersTaskDelegate delegate;
    private final int streamerUserId;
    private final Context context;


    public GetStreamViewersTask(GetStreamViewersTaskDelegate delegate, int streamerUserId, Context context) {
        this.delegate = delegate;
        this.streamerUserId = streamerUserId;
        this.context = context;
    }

    @Override
    protected Integer doInBackground(Void... voids) {
        try {
            final String URL = "https://api.twitch.tv/helix/streams?user_id=" + this.streamerUserId + "&first=1";
            final String VIEWERS_INT = "viewer_count";

            JSONObject topObject = new JSONObject(Service.urlToJSONStringHelix(URL, this.context));
            JSONArray steamArray = topObject.getJSONArray("data");
            JSONObject streamObject = steamArray.getJSONObject(0);

            return streamObject.getInt(VIEWERS_INT);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    protected void onPostExecute(Integer integer) {
        super.onPostExecute(integer);
        if (integer > -1) {
            delegate.onViewersFetched(integer);
        }
    }

    public interface GetStreamViewersTaskDelegate {
        void onViewersFetched(Integer currentViewers);
    }
}
