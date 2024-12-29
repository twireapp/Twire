package com.perflyst.twire.tasks;

import android.util.Log;

import com.perflyst.twire.misc.Utils;
import com.perflyst.twire.model.Quality;
import com.perflyst.twire.service.Service;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Request;

/**
 * Async task. Gets the required access token for a specific streamer. Then starts the streamers live stream.
 * Requires to be executed with the username of the streamer and a reference to the VideoView
 */
public class GetLiveStreamURL implements Callable<Map<String, Quality>> {
    public static final String QUALITY_SOURCE = "chunked";
    public static final String QUALITY_AUTO = "auto";
    private final String LOG_TAG = getClass().getSimpleName();

    private final String streamerName;
    protected final String playerType;
    private final String proxy;

    public GetLiveStreamURL(String streamerName, String playerType, String proxy) {
        this.streamerName = streamerName;
        this.playerType = playerType;
        this.proxy = proxy;
    }

    protected JSONObject getToken(boolean isLive, String channelOrVod, String playerType) {
        return Service.graphQL("PlaybackAccessToken", "0828119ded1c13477966434e15800ff57ddacf13ba1911c129dc2200705b0712", new HashMap<>() {{
            put("isLive", isLive);
            put("isVod", !isLive);
            put("login", isLive ? channelOrVod : "");
            put("vodID", !isLive ? channelOrVod : "");
            put("playerType", playerType);
        }});
    }

    public Map<String, Quality> call() {
        String signature = "";
        String token = "";

        JSONObject dataObject = getToken(true, streamerName, playerType);
        if (dataObject == null)
            return new LinkedHashMap<>();

        try {
            JSONObject tokenJSON = dataObject.getJSONObject("streamPlaybackAccessToken");
            token = tokenJSON.getString("value");
            signature = tokenJSON.getString("signature");

            Log.d("ACCESS_TOKEN_STRING", token);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String streamUrl = String.format("https://usher.ttvnw.net/api/channel/hls/%s.m3u8" +
                "?player=twitchweb&" +
                "&token=%s" +
                "&sig=%s" +
                "&allow_audio_only=true" +
                "&allow_source=true" +
                "&type=any" +
                "&fast_bread=true" +
                "&p=%s", streamerName, Utils.safeEncode(token), signature, "" + new Random().nextInt(6));

        if (!proxy.isEmpty()) {
            String parameters = streamerName + ".m3u8?allow_source=true&allow_audio_only=true&fast_bread=true";
            streamUrl = proxy + "/playlist/" + Utils.safeEncode(parameters);
        }

        Log.d(LOG_TAG, "HSL Playlist URL: " + streamUrl);
        return parseM3U8(streamUrl);
    }

    LinkedHashMap<String, Quality> parseM3U8(String urlToRead) {
        Request request = new Request.Builder()
                .url(urlToRead)
                .header("Referer", "https://player.twitch.tv")
                .header("Origin", "https://player.twitch.tv")
                .build();

        String result = "";
        Service.SimpleResponse response = Service.makeRequest(request);
        if (response != null)
            result = response.body;

        LinkedHashMap<String, Quality> resultList = new LinkedHashMap<>();
        resultList.put(QUALITY_AUTO, new Quality("Auto", urlToRead));

        Pattern p = Pattern.compile("GROUP-ID=\"(.+)\",NAME=\"(.+)\".+\\n.+\\n(https?://\\S+)");
        Matcher m = p.matcher(result);

        while (m.find()) {
            resultList.put(m.group(1), new Quality(m.group(2), m.group(3)));
        }

        return resultList;
    }
}
