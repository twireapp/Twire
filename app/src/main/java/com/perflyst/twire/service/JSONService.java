package com.perflyst.twire.service;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.perflyst.twire.R;
import com.perflyst.twire.model.ChannelInfo;
import com.perflyst.twire.model.Game;
import com.perflyst.twire.model.StreamInfo;
import com.perflyst.twire.model.VideoOnDemand;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Sebastian Rask on 25-04-2016.
 */
public class JSONService {
    private static final String LOG_TAG = JSONService.class.getSimpleName();

    // based on https://github.com/twurple/twurple/blob/6d3ca508fe0a21fadd77b63b62d0df66b9150f97/packages/api/src/api/helix/video/HelixVideo.ts#L175
    public static int getVodlenght(String length) {
        int all = 0;
        String[] letters = {
          "h",
          "m",
          "s"
        };
        int[] times = {
                3600,
                60,
                1
        };
        String[] regex = {
                "[0-9]{1,}[h]",
                "[0-9]{1,}[m]",
                "[0-9]{1,}[s]"
        };

        for (int i=0; i < regex.length; i++) {
            Pattern pattern = Pattern.compile(regex[i]);
            Matcher matcher = pattern.matcher(length);
            if (matcher.find()) {
                String match = matcher.group().replace(letters[i], "");
                int temp = Integer.parseInt(match);
                all = all + (temp * times[i]);
            }
        }
        return all;
    }

    public static VideoOnDemand getVod(JSONObject vodObject) throws JSONException {
        final String TITLE_STRING = "title";
        final String VIDEO_ID_STRING = "id";
        final String VIDEO_LENGTH_INT = "duration";
        final String VIDEO_VIEWS_INT = "view_count";
        final String RECORDED_DATE_STRING = "created_at";
        final String PREVIEW_URL_IMAGE = "thumbnail_url";
        final String CHANNEL_NAME_STRING = "user_login";
        final String CHANNEL_DISPLAY_NAME_STRING = "user_name";

        String gameTitle = "";

        return new VideoOnDemand(vodObject.getString(TITLE_STRING), gameTitle, vodObject.getString(PREVIEW_URL_IMAGE).replace("%{width}", "320").replace("%{height}", "180"), vodObject.getString(VIDEO_ID_STRING), vodObject.getString(CHANNEL_NAME_STRING), vodObject.getString(CHANNEL_DISPLAY_NAME_STRING), vodObject.getInt(VIDEO_VIEWS_INT), getVodlenght(vodObject.getString(VIDEO_LENGTH_INT)), vodObject.has(RECORDED_DATE_STRING) ? vodObject.getString(RECORDED_DATE_STRING) : "");
    }

    public static StreamInfo getStreamInfo(Context context, JSONObject streamObject, @Nullable ChannelInfo aChannelInfo, boolean loadDescription) throws JSONException, MalformedURLException {
        Settings settings = new Settings(context);
        final String PREVIEW_LINK_OBJECT = "thumbnail_url";
        final String CHANNEL_OBJECT = "channel";
        final String CHANNEL_STATUS_STRING = "title";
        final String GAME_STRING = "game_name";
        final String STREAM_START_TIME_STRING = "started_at";
        final String CURRENT_VIEWERS_INT = "viewer_count";
        final String USER_ID = "user_id";
        String prepend_image = "";

        String user_id = streamObject.getString(USER_ID);

        // Get the Channel Object using Helix
        final String ARRAY_KEY = "data";
        String channel_request_url = "https://api.twitch.tv/helix/users?id=" + user_id;
        String channel_string = Service.urlToJSONStringHelix(channel_request_url, context);
        JSONObject channel_object = new JSONObject(channel_string);
        JSONArray temp_array = channel_object.getJSONArray(ARRAY_KEY);

        JSONObject JSONChannel = temp_array.getJSONObject(0);
        ChannelInfo mChannelInfo = aChannelInfo == null ? getStreamerInfo(context, JSONChannel) : aChannelInfo;

        String gameName = streamObject.getString(GAME_STRING);

        String title = context.getString(R.string.default_stream_title, mChannelInfo.getDisplayName(), gameName);
        int currentViewers = streamObject.getInt(CURRENT_VIEWERS_INT);

        // Get the Stream Title with Helix
        String channel_stream = Service.urlToJSONStringHelix("https://api.twitch.tv/helix/streams?first=1&user_id=" + user_id, context);
        JSONObject channel_stream_object = new JSONObject(channel_stream);
        JSONArray channel_stream_array = channel_stream_object.getJSONArray("data");
        channel_stream_object = channel_stream_array.getJSONObject(0);
        title = channel_stream_object.getString("title");

        if (settings.getGeneralUseImageProxy()) {
            prepend_image = settings.getImageProxyUrl();
        }

        // Helix has no previews Object but we can built that by hand
        String[] previews = {
                prepend_image + streamObject.getString(PREVIEW_LINK_OBJECT).replace("{width}", "80").replace("{height}", "45"),
                prepend_image + streamObject.getString(PREVIEW_LINK_OBJECT).replace("{width}", "320").replace("{height}", "180"),
                prepend_image + streamObject.getString(PREVIEW_LINK_OBJECT).replace("{width}", "640").replace("{height}", "360"),
        };

        String startedAtString = streamObject.getString(STREAM_START_TIME_STRING);
        int year = Integer.parseInt(startedAtString.substring(0, 4));
        int month = Integer.parseInt(startedAtString.substring(5, 7));
        int day = Integer.parseInt(startedAtString.substring(8, 10));
        int hour = Integer.parseInt(startedAtString.substring(11, 13));
        int minute = Integer.parseInt(startedAtString.substring(14, 16));

        Calendar startedAt = new GregorianCalendar(year, month - 1, day, hour, minute); // Month is somehow index based
        long startAtLong = startedAt.getTimeInMillis();


        return new StreamInfo(mChannelInfo, gameName, currentViewers, previews, startAtLong, title);
    }

    public static ChannelInfo getStreamerInfo(Context context, JSONObject channel) throws JSONException, MalformedURLException {
        final String DISPLAY_NAME_STRING = "display_name";
        final String TWITCH_NAME_STRING = "login";
        final String FOLLOWERS_INT = "followers";
        final String VIEWS_INT = "view_count";
        final String LOGO_URL_STRING = "profile_image_url";
        final String BANNER_URL_STRING = "banner";
        final String VIDEO_BANNER_URL_STRING = "offline_image_url";
        final String BACKGROUND_STRING = "background";
        final String PROFILE_BANNER_URL_STRING = "profile_banner";
        final String PROFILE_BANNER_BACKGROUND_COLOR_STRING = "profile_banner_background_color";
        final String BIO_STRING = "description";
        final String USER_ID_INT = "id";

        String displayName = channel.getString(DISPLAY_NAME_STRING);
        int userId = channel.getInt(USER_ID_INT);
        final String twitchName = channel.getString(TWITCH_NAME_STRING);

        // Fetch the Total Follower Count here
        String userFollows = Service.urlToJSONStringHelix("https://api.twitch.tv/helix/users/follows?first=1&to_id=" + userId, context);
        JSONObject fullDataObject = new JSONObject(userFollows);
        int follows = fullDataObject.getInt("total");

        int views = channel.getInt(VIEWS_INT);
        URL logoURL = null,
                videoBannerURL = null,
                profileBannerURL = null;

        if (!channel.isNull(LOGO_URL_STRING)) {
            logoURL = new URL(channel.getString(LOGO_URL_STRING));
        }

        if (!channel.getString(VIDEO_BANNER_URL_STRING).equals("")) {
            videoBannerURL = new URL(channel.getString(VIDEO_BANNER_URL_STRING));
        }

        if (!channel.isNull(PROFILE_BANNER_URL_STRING)) {
            profileBannerURL = new URL(channel.getString(PROFILE_BANNER_URL_STRING));
        }

        final ChannelInfo channelInfo = new ChannelInfo(userId, twitchName, displayName, "", follows, views, logoURL, videoBannerURL, profileBannerURL);

        channelInfo.setStreamDescription(channel.getString(BIO_STRING));

        return channelInfo;
    }

    public static Game getGame(JSONObject game) throws JSONException {
        final String TITLE_STRING_KEY = "name";
        final String PREVIEW_OBJECT_KEY = "box_art_url";

        String gameTitle = game.getString(TITLE_STRING_KEY);
        String smallPreview = game.getString(PREVIEW_OBJECT_KEY).replace("{width}", "52").replace("{height}", "72");
        String mediumPreview = game.getString(PREVIEW_OBJECT_KEY).replace("{width}", "136").replace("{height}", "190");
        String largePreview = game.getString(PREVIEW_OBJECT_KEY).replace("{width}", "272").replace("{height}", "380");

        return new Game(gameTitle, smallPreview, mediumPreview, largePreview);
    }
}
