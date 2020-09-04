package com.perflyst.twire.tasks;

import android.os.AsyncTask;

import com.perflyst.twire.service.Service;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Sebastian Rask on 17-09-2016.
 */
public class GetStreamViewersTask extends AsyncTask<Void, Void, Integer> {
    private final GetStreamViewersTaskDelegate delegate;
    private final int streamerUserId;


    public GetStreamViewersTask(GetStreamViewersTaskDelegate delegate, int streamerUserId) {
        this.delegate = delegate;
        this.streamerUserId = streamerUserId;
    }

    @Override
    protected Integer doInBackground(Void... voids) {
        try {
            final String BASE_URL = "https://api.twitch.tv/kraken/streams/";
            final String STREAM_OBJECT = "stream";
            final String VIEWERS_INT = "viewers";

            JSONObject topObject = new JSONObject(Service.urlToJSONString(BASE_URL + streamerUserId));
            JSONObject streamObject = topObject.getJSONObject(STREAM_OBJECT);

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
