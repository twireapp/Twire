package com.perflyst.twire.tasks;

import android.os.AsyncTask;
import android.util.Log;

import com.perflyst.twire.model.Quality;
import com.perflyst.twire.service.Service;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Async task. Gets the required access token for a specific streamer. Then starts the streamers live stream.
 * Requires to be executed with the username of the streamer and a reference to the videoview
 */
public class GetLiveStreamURL extends AsyncTask<String, Void, LinkedHashMap<String, Quality>> {
    public static final String QUALITY_MOBILE = "mobile";
    public static final String QUALITY_LOW = "low";
    public static final String QUALITY_MEDIUM = "medium";
    public static final String QUALITY_HIGH = "high";
    public static final String QUALITY_SOURCE = "chunked";
    public static final String QUALITY_AUTO = "auto";
    public static final String QUALITY_AUDIO_ONLY = "audio_only";
    public static final String[] CAST_QUALITIES = {
            QUALITY_MOBILE,
            QUALITY_LOW,
            QUALITY_MEDIUM,
            QUALITY_HIGH,
            QUALITY_SOURCE
    };
    private String LOG_TAG = getClass().getSimpleName();
    private AsyncResponse callback;

    public GetLiveStreamURL(AsyncResponse aCallback) {
        callback = aCallback;
    }

    @Override
    protected LinkedHashMap<String, Quality> doInBackground(String... params) {
        String streamerName = params[0];
        String sig = "";
        String tokenString = "";

        String resultString = Service.urlToJSONString("https://api.twitch.tv/api/channels/" + streamerName + "/access_token");
        try {
            JSONObject resultJSON = new JSONObject(resultString);
            tokenString = resultJSON.getString("token");
            sig = resultJSON.getString("sig");

            Log.d("ACCESS_TOKEN_STRING", tokenString);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String streamUrl = String.format("http://usher.twitch.tv/api/channel/hls/%s.m3u8" +
                "?player=twitchweb&" +
                "&token=%s" +
                "&sig=%s" +
                "&allow_audio_only=true" +
                "&allow_source=true" +
                "&type=any" +
                "&p=%s", streamerName, safeEncode(tokenString), sig, "" + new Random().nextInt(6));

        Log.d(LOG_TAG, "HSL Playlist URL: " + streamUrl);
        return parseM3U8(streamUrl);
    }

    @Override
    protected void onPostExecute(LinkedHashMap<String, Quality> result) {
        callback.finished(result);
    }

    LinkedHashMap<String, Quality> parseM3U8(String urlToRead) {
        URL url;
        HttpURLConnection conn = null;
        Scanner in = null;
        String line;
        StringBuilder result = new StringBuilder();

        try {
            url = new URL(urlToRead.replace(" ", "%20"));

            conn = Service.openConnection(url);
            conn.setReadTimeout(3000);
            conn.setConnectTimeout(3000);
            conn.setRequestMethod("GET");

            InputStream inputs;
            if (conn.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST) {
                inputs = conn.getInputStream();
            } else {
                // Error
                inputs = conn.getErrorStream();
            }

            in = new Scanner(new InputStreamReader(inputs));

            while (in.hasNextLine()) {
                line = in.nextLine();
                result.append(line).append("\n");
            }

            in.close();
            conn.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null)
                in.close();
            if (conn != null)
                conn.disconnect();
        }

        LinkedHashMap<String, Quality> resultList = new LinkedHashMap<>();
        resultList.put(QUALITY_AUTO, new Quality("Auto", urlToRead));

        Pattern p = Pattern.compile("GROUP-ID=\"(.+)\",NAME=\"(.+)\".+\\n.+\\n(http://\\S+)");
        Matcher m = p.matcher(result.toString());

        while (m.find()) {
            resultList.put(m.group(1), new Quality(m.group(2), m.group(3)));
        }

        return resultList;
    }

    String safeEncode(String s) {
        try {
            return URLEncoder.encode(s, "utf-8");
        } catch (UnsupportedEncodingException ignore) {
            return s;
        }
    }

    public interface AsyncResponse {
        void finished(LinkedHashMap<String, Quality> url);
    }
}
