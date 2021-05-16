package com.perflyst.twire.tasks;

import android.util.Log;

import com.perflyst.twire.misc.SecretKeys;
import com.perflyst.twire.model.Quality;
import com.perflyst.twire.service.Service;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Random;

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
        Boolean ttvfun = false;

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


        //build ping request for ttv.lol
        Request ping = new Request.Builder()
                .url("https://api.ttv.lol/ping")
                .build();

        //get response code
        Service.SimpleResponse pingresponse = Service.makeRequest(ping);
        int responsecode = pingresponse.code;
        Log.d("Response Code", String.valueOf(responsecode));

        String vodURL = "";

        //if ping successful use ttv.lol otherwise use fallback twitch api
        if (responsecode == 200) {
            ttvfun = true;
            Log.d("Using ttv.lol api", String.valueOf(responsecode));
        } else {
            ttvfun = false;
            Log.d("Using default api", String.valueOf(responsecode));
        }

        if (ttvfun == true) {
            //modified api call here for ttv.lol
            //ttv.lol requires https, because without it you get a 301 redirect thats not supported
            vodURL = String.format("https://api.ttv.lol/vod/%s.m3u8", vodId);
            String streamParameters = String.format(
                    "?nauthsig=%s" +
                    "&nauth=%s", signature, token);
            //only encode the parameters of the url
            vodURL = vodURL + safeEncode(streamParameters);
        } else {
            //default twitch api call here
            vodURL = String.format("http://usher.twitch.tv/vod/%s?nauthsig=%s&nauth=%s", vodId, signature, safeEncode(token));
        }

        Log.d(LOG_TAG, "HSL Playlist URL: " + vodURL);
        return parseM3U8(vodURL);
    }
}
