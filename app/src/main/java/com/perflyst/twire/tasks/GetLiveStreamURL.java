package com.perflyst.twire.tasks;

import android.os.AsyncTask;
import android.util.Log;

import com.perflyst.twire.misc.SecretKeys;
import com.perflyst.twire.model.Quality;
import com.perflyst.twire.service.Service;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Async task. Gets the required access token for a specific streamer. Then starts the streamers live stream.
 * Requires to be executed with the username of the streamer and a reference to the VideoView
 */
public class GetLiveStreamURL extends AsyncTask<String, Void, LinkedHashMap<String, Quality>> {
    public static final String QUALITY_SOURCE = "chunked";
    public static final String QUALITY_AUTO = "auto";
    private final String LOG_TAG = getClass().getSimpleName();
    private final AsyncResponse callback;
    public boolean ttvfun = false;

    public GetLiveStreamURL(AsyncResponse aCallback) {
        callback = aCallback;
    }

    protected String formatQuery(boolean isLive, String channelOrVod, String PlayerType) {
        return "{\n" +
                "    \"operationName\": \"PlaybackAccessToken\",\n" +
                "    \"extensions\": {\n" +
                "        \"persistedQuery\": {\n" +
                "            \"version\": 1,\n" +
                "            \"sha256Hash\": \"0828119ded1c13477966434e15800ff57ddacf13ba1911c129dc2200705b0712\"\n" +
                "        }\n" +
                "    },\n" +
                "    \"variables\": {\n" +
                "        \"isLive\": " + isLive + ",\n" +
                "        \"login\": \"" + (isLive ? channelOrVod : "") + "\",\n" +
                "        \"isVod\": " + !isLive + ",\n" +
                "        \"vodID\": \"" + (!isLive ? channelOrVod : "") + "\",\n" +
                "        \"playerType\": \""+ PlayerType + "\"\n" +
                "    }\n" +
                "}";
    }

    @Override
    protected LinkedHashMap<String, Quality> doInBackground(String... params) {
        String streamerName = params[0];
        String PlayerType = params[1];
        String usettv = params[2];
        String proxyurl = params[3];
        String signature = "";
        String token = "";

        Log.d("Use TTV setting", usettv);

        Request request = new Request.Builder()
                .url("https://gql.twitch.tv/gql")
                .header("Client-ID", SecretKeys.TWITCH_WEB_CLIENT_ID)
                .post(RequestBody.create(MediaType.get("application/json"), formatQuery(true, streamerName, PlayerType)))
                .build();

        String resultString = Service.urlToJSONString(request);
        if (resultString == null)
            return new LinkedHashMap<>();

        try {
            JSONObject resultJSON = new JSONObject(resultString);
            JSONObject tokenJSON = resultJSON.getJSONObject("data").getJSONObject("streamPlaybackAccessToken");
            token = tokenJSON.getString("value");
            signature = tokenJSON.getString("signature");

            Log.d("ACCESS_TOKEN_STRING", token);
        } catch (JSONException e) {
            e.printStackTrace();
        }



        // check if the api is available
        int responsecode = 0;

        try {
            // build ping request for ttv.lol
            Request ping = new Request.Builder()
                .url(proxyurl + "/ping")
                .build();

            // get response code
            Service.SimpleResponse pingresponse = Service.makeRequest(ping);
            responsecode = pingresponse.code;
            Log.d("Response Code", String.valueOf(responsecode));
        } catch (Exception e) {
            e.printStackTrace();
            // when the API ping breaks then define the response code as 404
            responsecode = 404;
        }


        String streamUrl = "";

        //if ping successful use ttv.lol otherwise use fallback twitch api
        if (responsecode == 200 && usettv == "true") {
            ttvfun = true;
            Log.d("Using " + proxyurl + " api", String.valueOf(responsecode));
        } else {
            ttvfun = false;
            Log.d("Using default api", String.valueOf(responsecode));
        }

        if (ttvfun == true) {
            //modified api call here for ttv.lol
            //ttv.lol requires https, because without it you get a 301 redirect that`s not supported
            streamUrl = String.format(proxyurl + "/playlist/%s.m3u8", streamerName);


            //we don´t want to leak the user id or the token to a 3`rd party api as it is
            //not needed: https://github.com/TTV-LOL/extensions/issues/8
            String streamParameters = String.format(
                    "?player=twitchweb&" +
                    "&allow_audio_only=true" +
                    "&allow_source=true" +
                    "&type=any" +
                    "p=%S", "" + new Random().nextInt(6));
            //only encode the parameters of the url
            streamUrl = streamUrl + safeEncode(streamParameters);
        } else {
            //default twitch api call here
            streamUrl = String.format("http://usher.twitch.tv/api/channel/hls/%s.m3u8" +
                    "?player=twitchweb&" +
                    "&token=%s" +
                    "&sig=%s" +
                    "&allow_audio_only=true" +
                    "&allow_source=true" +
                    "&type=any" +
                    "&p=%s", streamerName, safeEncode(token), signature, "" + new Random().nextInt(6));
        }

        Log.d(LOG_TAG, "HSL Playlist URL: " + streamUrl);
        return parseM3U8(streamUrl, proxyurl);
    }

    @Override
    protected void onPostExecute(LinkedHashMap<String, Quality> result) {
        callback.finished(result);
    }

    LinkedHashMap<String, Quality> parseM3U8(String urlToRead, String proxyurl) {
        Request request;
        Log.d("M3U8 Url", urlToRead);
        if (ttvfun == true) {
            request = new Request.Builder()
                    .url(urlToRead)
                    .header("Referer", "https://player.twitch.tv")
                    .header("Origin", "https://player.twitch.tv")
                    //so apparently this header took me 2 hours of debugging because without it we get a 401 response here
                    .header("X-Donate-To", proxyurl.replace("api.", "") + "/donate")
                    .build();
            } else {
            request = new Request.Builder()
                    .url(urlToRead)
                    .header("Referer", "https://player.twitch.tv")
                    .header("Origin", "https://player.twitch.tv")
                    .build();
        }

        String result = "";
        Service.SimpleResponse response = Service.makeRequest(request);
        if (response != null)
            Log.d("M3U8 Response Code", String.valueOf(response.response.code()));
            result = response.body;
        Log.d("result", result);

        LinkedHashMap<String, Quality> resultList = new LinkedHashMap<>();

        resultList.put(QUALITY_AUTO, new Quality("Auto", urlToRead));

        Pattern p = Pattern.compile("GROUP-ID=\"(.+)\",NAME=\"(.+)\".+\\n.+\\n(https?://\\S+)");
        Matcher m = p.matcher(result);

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
