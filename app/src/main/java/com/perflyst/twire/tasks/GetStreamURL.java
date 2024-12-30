package com.perflyst.twire.tasks;

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
import timber.log.Timber;

public class GetStreamURL implements Callable<Map<String, Quality>> {
    public static final String QUALITY_SOURCE = "chunked";
    public static final String QUALITY_AUTO = "auto";

    private final String channelOrVod;
    protected final String playerType;
    private final String proxy;
    private final boolean isLive;

    public GetStreamURL(String channel, String vod, String playerType, String proxy) {
        this.isLive = vod == null;
        this.channelOrVod = isLive ? channel : vod;
        this.playerType = playerType;
        this.proxy = proxy;
    }

    protected JSONObject getToken() {
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

        JSONObject dataObject = getToken();
        if (dataObject == null)
            return new LinkedHashMap<>();

        try {
            JSONObject tokenJSON = dataObject.getJSONObject(isLive ? "streamPlaybackAccessToken" : "videoPlaybackAccessToken");
            token = tokenJSON.getString("value");
            signature = tokenJSON.getString("signature");

            Timber.tag("ACCESS_TOKEN_STRING").d(token);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String streamUrl = String.format((isLive ? "https://usher.ttvnw.net/api/channel/hls/%s.m3u8" : "https://usher.ttvnw.net/vod/%s") +
                "?player=twitchweb&" +
                "&token=%s" +
                "&sig=%s" +
                "&allow_audio_only=true" +
                "&allow_source=true" +
                "&type=any" +
                "&fast_bread=true" +
                "&p=%s", channelOrVod, Utils.safeEncode(token), signature, "" + new Random().nextInt(6));

        if (isLive && !proxy.isEmpty()) {
            String parameters = channelOrVod + ".m3u8?allow_source=true&allow_audio_only=true&fast_bread=true";
            streamUrl = proxy + "/playlist/" + Utils.safeEncode(parameters);
        }

        Timber.d("HSL Playlist URL: %s", streamUrl);
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
