package com.perflyst.twire.tasks;

import android.util.Log;

import com.perflyst.twire.misc.Utils;
import com.perflyst.twire.model.Quality;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;

/**
 * Created by Sebastian Rask on 18-06-2016.
 */
public class GetVODStreamURL extends GetLiveStreamURL {
    private final String LOG_TAG = getClass().getSimpleName();
    private final String vodId;

    public GetVODStreamURL(String vodId, String playerType) {
        super(null, playerType, null);

        this.vodId = vodId;
    }

    @Override
    public LinkedHashMap<String, Quality> call() {
        String signature = "";
        String token = "";

        JSONObject dataObject = getToken(false, vodId, playerType);
        if (dataObject == null)
            return new LinkedHashMap<>();

        try {
            JSONObject tokenJSON = dataObject.getJSONObject("videoPlaybackAccessToken");
            token = tokenJSON.getString("value");
            signature = tokenJSON.getString("signature");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String vodURL = String.format("https://usher.ttvnw.net/vod/%s?allow_source=true&nauthsig=%s&nauth=%s", vodId, signature, Utils.safeEncode(token));
        Log.d(LOG_TAG, "HSL Playlist URL: " + vodURL);
        return parseM3U8(vodURL);
    }
}
