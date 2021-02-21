package com.perflyst.twire.tasks;

import android.os.AsyncTask;

import com.perflyst.twire.service.Service;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Sebastian Rask on 22-01-2017.
 */

public class GetStreamChattersTask extends AsyncTask<Void, Void, ArrayList<String>> {
    private final GetStreamChattersTaskDelegate delegate;
    private final String mStreamTwitchName;

    public GetStreamChattersTask(GetStreamChattersTaskDelegate delegate, String mStreamTwitchName) {
        this.delegate = delegate;
        this.mStreamTwitchName = mStreamTwitchName;
    }

    @Override
    protected ArrayList<String> doInBackground(Void... voids) {
        try {
            final String BASE_URL = "https://tmi.twitch.tv/group/user/" + mStreamTwitchName + "/chatters";

            JSONObject topObject = new JSONObject(Service.urlToJSONString(BASE_URL));

            return null;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(ArrayList<String> chatters) {
        super.onPostExecute(chatters);
        if (chatters != null) {
            delegate.onChattersFetched(chatters);
        } else {
            delegate.onChattersFetchFailed();
        }
    }

    public interface GetStreamChattersTaskDelegate {
        void onChattersFetched(ArrayList<String> chatters);

        void onChattersFetchFailed();
    }
}
