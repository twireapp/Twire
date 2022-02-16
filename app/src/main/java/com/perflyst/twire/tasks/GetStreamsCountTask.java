package com.perflyst.twire.tasks;

import android.content.Context;
import android.os.AsyncTask;

import com.perflyst.twire.model.ChannelInfo;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.service.Settings;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Sebastian Rask on 26-06-2016.
 */
public class GetStreamsCountTask extends AsyncTask<Void, Void, Integer> {
    private final Settings settings;
    private final Delegate delegate;
    private final Context context;

    public GetStreamsCountTask(Context context, Delegate delegate) {
        this.settings = new Settings(context);
        this.context = context;
        this.delegate = delegate;
    }

    @Override
    protected Integer doInBackground(Void... params) {
        try {
            // build the api link
            String helix_url = "https://api.twitch.tv/helix/streams";
            String user_logins = "";

            GetFollowsFromDB subscriptionsTask = new GetFollowsFromDB();
            subscriptionsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, settings.getContext());

            ArrayList<String> requesturls = new ArrayList<>();
            boolean first_id = true;
            int number = 0;
            int exactnumber = 0;

            // loop over all channel in the DB
            for (ChannelInfo si : subscriptionsTask.get().values()) {
                // if the number of channels, already in the url, is smaller than 99 and is not the last channel
                // e.g. if there are 160 Channels in the DB then this will result in 2 request urls ([0-99] and [100-159])
                if (number <= 99 && exactnumber != subscriptionsTask.get().values().size() -1) {
                    if (first_id) {
                        // if this is the first id then use ?
                        user_logins = "?user_id=" + si.getUserId();
                        first_id = false;
                    } else {
                        // not the first id use &
                        user_logins = user_logins + "&user_id=" + si.getUserId();
                    }
                    number++;
                    // if the request url has 100 user ids or is the last channel in the list
                } else if (number > 99 || exactnumber == (subscriptionsTask.get().values().size() -1)) {
                    // add the new request url to the list
                    requesturls.add(helix_url + user_logins);
                    // reset stuff
                    first_id = true;
                    user_logins = "";
                    number = 0;
                }
                exactnumber++;
            }

            final String STREAMS_ARRAY = "data";
            JSONArray final_array = new JSONArray();
            // for every request url in the list
            for (int i=0; i<requesturls.size(); i++) {
                String temp_jsonString;
                // request the url
                temp_jsonString = Service.urlToJSONStringHelix(requesturls.get(i), context);
                JSONObject fullDataObject = new JSONObject(temp_jsonString);
                // create the array
                JSONArray temp_array = fullDataObject.getJSONArray(STREAMS_ARRAY);
                // append the new array to the final one
                for (int x=0; x<temp_array.length(); x++) {
                    final_array.put(temp_array.get(x));
                }
            }
            // return the length of the array
            return final_array.length();
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
