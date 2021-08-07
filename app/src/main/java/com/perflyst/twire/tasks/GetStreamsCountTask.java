package com.perflyst.twire.tasks;

import android.content.Context;
import android.os.AsyncTask;

import com.perflyst.twire.model.ChannelInfo;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.service.Settings;

import org.json.JSONObject;

/**
 * Created by Sebastian Rask on 26-06-2016.
 */
public class GetStreamsCountTask extends AsyncTask<Void, Void, Integer> {
    private final Settings settings;
    private final Delegate delegate;

    public GetStreamsCountTask(Context context, Delegate delegate) {
        this.settings = new Settings(context);
        this.delegate = delegate;
    }

    @Override
    protected Integer doInBackground(Void... params) {
        try {
            // build the api link
            String kraken_url = "https://api.twitch.tv/kraken/streams?limit=100&stream_type=live&offset=0&channel=";
            String user_logins = "";

            GetFollowsFromDB subscriptionsTask = new GetFollowsFromDB();
            subscriptionsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, settings.getContext());

            for (ChannelInfo si : subscriptionsTask.get().values()) {
                user_logins = user_logins + si.getUserId() + ",";
            }


            kraken_url = kraken_url + user_logins;
            final String STREAMS_ARRAY = "streams";

            String jsonString = Service.urlToJSONString(kraken_url);
            JSONObject fullDataObject = new JSONObject(jsonString);

            return fullDataObject.getJSONArray(STREAMS_ARRAY).length();
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
