package com.perflyst.twire.tasks;

import com.perflyst.twire.service.Service;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.Callable;

/**
 * Created by Sebastian Rask on 22-01-2017.
 */

public class GetStreamChattersTask implements Callable<ArrayList<String>> {
    private final String mStreamTwitchName;

    public GetStreamChattersTask(String mStreamTwitchName) {
        this.mStreamTwitchName = mStreamTwitchName;
    }

    public ArrayList<String> call() {
        try {
            final String BASE_URL = "https://tmi.twitch.tv/group/user/" + mStreamTwitchName + "/chatters";

            JSONObject topObject = new JSONObject(Service.urlToJSONString(BASE_URL));

            return null;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
