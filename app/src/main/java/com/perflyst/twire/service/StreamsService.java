package com.perflyst.twire.service;

import android.content.Context;
import android.util.Log;

import com.perflyst.twire.model.ChannelInfo;
import com.perflyst.twire.model.StreamInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by SebastianRask on 08-06-2015.
 */
public class StreamsService {

    /**
     * Helper Method
     * Returns a list of Streams that are online.
     * Connects to twitch API by making a list of URLs from the streamer's streamername. The connection input is parsed to JSON objects.
     * The objects are converted into StreamInfo objects.
     *
     * @param streamers A map of streamers
     * @return
     */
    public static List<StreamInfo> getOnlineStreams(Map<String, ChannelInfo> streamers, Context context) {
        List<String> streamerIds = new ArrayList<>();
        for (String streamerName : streamers.keySet()) {
            streamerIds.add(streamers.get(streamerName).getUserId() + "");
        }

        return getOnlineStreams(streamers, getStreamURLS(streamerIds), context);
    }

    /**
     * Returns a list of all followed currently live streams for the user with specific oauth token.
     * This should not be used for time critical tasks.
     *
     * @param context
     * @param oauthToken
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

    /**
     * Returns a list of Streams that are online.
     * Connects to a given list of URL strings and parses the input to JSON objects. The objects are converted into StreamInfo objects.
     *
     * @param streamers  A map of streamers
     * @param urlStrings A list of URL strings
     * @return A list of online streams
     */
    private static List<StreamInfo> getOnlineStreams(Map<String, ChannelInfo> streamers, List<String> urlStrings, Context context) {
        final String ARRAY_NAME = "streams";
        List<JSONArray> jsonObjects = new ArrayList<>();
        List<StreamInfo> streamsList = new ArrayList<>();

        try {
            // Get the data download over with.
            if (Service.isNetworkConnectedThreadOnly(context)) {
                for (String url : urlStrings) {
                    jsonObjects.add(new JSONObject(Service.urlToJSONString(url)).getJSONArray(ARRAY_NAME));
                }
            }

            for (JSONArray array : jsonObjects) {
                for (int i = 0; i < array.length(); i++) {
                    JSONObject JSONStream = array.getJSONObject(i);
                    JSONObject JSONChannel = JSONStream.getJSONObject("channel");
                    String streamerName = JSONChannel.getString("name");

                    ChannelInfo streamer = streamers.get(streamerName);
                    streamsList.add(JSONService.getStreamInfo(context, JSONStream, streamer, false));
                }
            }
        } catch (JSONException | MalformedURLException e) {
            e.printStackTrace();
        }

        String LOG_TAG = "StreamsService";
        Log.d(LOG_TAG, "Found " + streamsList.size() + " online streams");
        return streamsList;
    }

    /**
     * Returns a list of URL Strings formatted correctly for connecting to twitch and receiving a number of JSON objects.
     * Mainly used for getting a list of online streams.
     *
     * @param streamerIds The ids of the streamers you to check is online.
     * @return List of String representing URLS
     */
    private static List<String> getStreamURLS(List<String> streamerIds) {
        final int MAXIMUM_STREAMS_FOR_QUERY = 100;
        final String BASE_URL = "https://api.twitch.tv/kraken/streams?channel="; // With this base url we only get JSON objects for the streamers that are only
        int streamersCount = streamerIds.size();

        List<String> resultList = new ArrayList<>();

        // Determine how many URLs we need.
        int numberURL = (int) Math.ceil((streamersCount * 1.0) / MAXIMUM_STREAMS_FOR_QUERY);
        for (int i = 0; i < numberURL; i++) {
            StringBuilder stringURL = new StringBuilder(BASE_URL);

            int iterationCount = MAXIMUM_STREAMS_FOR_QUERY;
            if (i == numberURL - 1) // Last iteration
                iterationCount = streamersCount - ((numberURL - 1) * MAXIMUM_STREAMS_FOR_QUERY); // Only iterate to the last index in the list.

            for (int j = 0; j < iterationCount; j++)
                stringURL.append(streamerIds.get(j + (i * MAXIMUM_STREAMS_FOR_QUERY))).append(",");

            resultList.add(stringURL.toString());
        }

        return resultList;
    }

    /* ------ UNDER THIS IS KINDA DEPRECATED -------*/
    private static StreamInfo jsonToStreamInfo(JSONObject JSONString, ChannelInfo streamer) throws Exception {
        StreamInfo stream = null;
        if (!JSONString.isNull("stream")) {
            JSONObject JSONStream = JSONString.getJSONObject("stream");
            JSONObject JSONPreview = JSONStream.getJSONObject("preview");
            JSONObject JSONChannel = JSONStream.getJSONObject("channel");

            String gameName = JSONStream.getString("game");
            int currentViewers = JSONStream.getInt("viewers");
            String title = streamer.getDisplayName() + " playing " + gameName;
            if (!JSONChannel.isNull("status")) {
                title = JSONChannel.getString("status");
            } else {
                Log.v("ifOnlineMakeStreamObj", "Status/title for " + streamer.getDisplayName() + " is null");
            }

            String[] previews = {
                    JSONPreview.getString("small"),
                    JSONPreview.getString("medium"),
                    JSONPreview.getString("large"),
            };

            String startedAtString = JSONStream.getString("created_at");
            int year = Integer.parseInt(startedAtString.substring(0, 4));
            int month = Integer.parseInt(startedAtString.substring(5, 7));
            int day = Integer.parseInt(startedAtString.substring(8, 10));
            int hour = Integer.parseInt(startedAtString.substring(11, 13));
            int minute = Integer.parseInt(startedAtString.substring(14, 16));

            Calendar startedAt = new GregorianCalendar(year, month - 1, day, hour, minute); // Month is somehow index based
            long startAtLong = startedAt.getTimeInMillis();

            stream = new StreamInfo(streamer, gameName, currentViewers, previews, startAtLong, title);
        }

        return stream;
    }

    /**
     * Checks whether or not a user is currently streaming and creates and returns a StreamInfo Object if true.
     * Can return null
     *
     * @param streamer The username of the streamer you want to check is online
     * @return boolean
     */

    public static StreamInfo ifOnlineMakeStreamObj(ChannelInfo streamer) {
        URL url;
        HttpURLConnection conn = null;
        Scanner in = null;
        String line;
        StringBuilder result = new StringBuilder();
        StreamInfo stream = null;

        try {
            url = new URL("https://api.twitch.tv/kraken/streams/" + streamer.getStreamerName());
            conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(5000);
            conn.setConnectTimeout(6000);
            conn.setRequestProperty("Accept", "application/vnd.twitchtv.v3+json");
            conn.setRequestMethod("GET");
            in = new Scanner(new InputStreamReader(conn.getInputStream()));

            while (in.hasNextLine()) {
                line = in.nextLine();
                result.append(line);
            }

            in.close();
            conn.disconnect();

            JSONObject JSONString = new JSONObject(result.toString());
            stream = jsonToStreamInfo(JSONString, streamer);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null)
                in.close();
            if (conn != null)
                conn.disconnect();
        }

        return stream;
    }

    /**
     * Checks whether or not a user is currently streaming and creates and returns a StreamInfo Object if true.
     * Can return null
     *
     * @param streamers The username of the streamers you want to check is online
     * @return boolean
     */

    public static List<StreamInfo> getOnlineStreams(List<ChannelInfo> streamers) {
        final String BASE_URL = "https://api.twitch.tv/kraken/streams/";
        List<StreamInfo> onlineStreams = new ArrayList<>();
        HttpURLConnection conn = null;
        Scanner in = null;

        try {
            for (ChannelInfo streamer : streamers) {
                URL url = new URL(BASE_URL + streamer.getStreamerName());
                StringBuilder result = new StringBuilder();
                conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(5000);
                conn.setConnectTimeout(6000);
                conn.setRequestProperty("Accept", "application/vnd.twitchtv.v3+json");
                conn.setRequestMethod("GET");
                in = new Scanner(new InputStreamReader(conn.getInputStream()));

                while (in.hasNextLine()) {
                    result.append(in.nextLine());
                }

                in.close();

                JSONObject JSONString = new JSONObject(result.toString());
                StreamInfo stream = jsonToStreamInfo(JSONString, streamer);
                if (stream != null)
                    onlineStreams.add(stream);
            }
        } catch (Exception e) {
            e.printStackTrace();
            e.getCause();
        } finally {
            if (conn != null)
                conn.disconnect();
            if (in != null)
                in.close();
        }

        return onlineStreams;
    }
}
