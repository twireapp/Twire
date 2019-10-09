package com.perflyst.twire.tasks;

import android.content.Context;
import android.os.AsyncTask;

import com.perflyst.twire.service.Service;
import com.perflyst.twire.service.Settings;

import org.json.JSONObject;

/**
 * Created by Sebastian Rask on 26-06-2016.
 */
public class GetStreamsCountTask extends AsyncTask<Void, Void, Integer> {
    private Context context;
    private Delegate delegate;

    public GetStreamsCountTask(Context context, Delegate delegate) {
        this.context = context;
        this.delegate = delegate;
    }

    @Override
    protected Integer doInBackground(Void... params) {
        try {
            final String URL = "https://api.twitch.tv/kraken/streams/followed?oauth_token=" + new Settings(context).getGeneralTwitchAccessToken() + "&offset=0&stream_type=live";
            final String TOTAL_STREAMS_INT = "_total";

            String jsonString = Service.urlToJSONString(URL);
            JSONObject fullDataObject = new JSONObject(jsonString);

            return fullDataObject.getInt(TOTAL_STREAMS_INT);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    protected void onPostExecute(Integer integer) {
        super.onPostExecute(integer);
        delegate.TaskFinished(integer);
    }

    public interface Delegate {
        void TaskFinished(int count);
    }
}
