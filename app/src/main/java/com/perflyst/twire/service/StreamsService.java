package com.perflyst.twire.service;

import android.content.Context;

import com.perflyst.twire.model.StreamInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by SebastianRask on 08-06-2015.
 */
public class StreamsService {

    /**
     * Returns a list of all followed currently live streams for the user with specific oauth token.
     * This should not be used for time critical tasks.
     *
     * @return list of all currently live followed streams
     */
    public static List<StreamInfo> fetchAllLiveStreams(Context context, String oauthToken) {
        final String BASE_URL = "https://api.twitch.tv/kraken/streams/followed?oauth_token=" + oauthToken + "&limit=100&offset=%s&stream_type=live";

        List<StreamInfo> mResultList = new ArrayList<>();
        try {
            boolean endReached = false;
            while (!endReached) {
                final String ARRAY_KEY = "streams";
                final String TOTAL_STREAMS_INT = "_total";

                String jsonString = Service.urlToJSONString(String.format(Locale.ROOT, BASE_URL, mResultList.size()));
                JSONObject fullDataObject = new JSONObject(jsonString);
                JSONArray streamsArray = fullDataObject.getJSONArray(ARRAY_KEY);

                int totalElements = fullDataObject.getInt(TOTAL_STREAMS_INT);
                endReached = totalElements == mResultList.size() || streamsArray.length() == 0;

                for (int i = 0; i < streamsArray.length(); i++) {
                    JSONObject streamObject = streamsArray.getJSONObject(i);
                    mResultList.add(JSONService.getStreamInfo(context, streamObject, null, false));
                }
            }
        } catch (JSONException | MalformedURLException ex) {
            ex.printStackTrace();
        }

        return mResultList;
    }
}
