package com.perflyst.twire.tasks;

import android.util.Log;

import com.perflyst.twire.misc.SecretKeys;
import com.perflyst.twire.model.Quality;
import com.perflyst.twire.service.Service;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Created by Sebastian Rask on 18-06-2016.
 */
public class GetVODStreamURL extends GetLiveStreamURL {
    private final String LOG_TAG = getClass().getSimpleName();

    public GetVODStreamURL(AsyncResponse aCallback) {
        super(aCallback);
    }

    @Override
    protected LinkedHashMap<String, Quality> doInBackground(String... params) {
        String vodId = params[0];
        String signature = "";
        String token = "";

        Request request = new Request.Builder()
                .url("https://gql.twitch.tv/gql")
                .header("Client-ID", SecretKeys.TWITCH_WEB_CLIENT_ID)
                .post(RequestBody.create(MediaType.get("application/json"), formatQuery(false, vodId)))
                .build();

        String resultString = Service.urlToJSONString(request);
        try {
            JSONObject resultJSON = new JSONObject(resultString);
            JSONObject tokenJSON = resultJSON.getJSONObject("data").getJSONObject("videoPlaybackAccessToken");
            token = tokenJSON.getString("value");
            signature = tokenJSON.getString("signature");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String vodURL = String.format("http://usher.twitch.tv/vod/%s?nauthsig=%s&nauth=%s", vodId, signature, safeEncode(token));
        Log.d(LOG_TAG, "HSL Playlist URL: " + vodURL);
        return parseM3U8(vodURL);
    }
}
