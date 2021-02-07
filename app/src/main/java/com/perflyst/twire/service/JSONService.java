package com.perflyst.twire.service;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.perflyst.twire.R;
import com.perflyst.twire.model.ChannelInfo;
import com.perflyst.twire.model.Game;
import com.perflyst.twire.model.StreamInfo;
import com.perflyst.twire.model.VideoOnDemand;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Created by Sebastian Rask on 25-04-2016.
 */
public class JSONService {
    private static final String LOG_TAG = JSONService.class.getSimpleName();

    public static VideoOnDemand getVod(JSONObject vodObject) throws JSONException {
        final String TITLE_STRING = "title";
        final String VIDEO_ID_STRING = "_id";
        final String GAME_TITLE_STRING = "game";
        final String VIDEO_LENGTH_INT = "length";
        final String VIDEO_VIEWS_INT = "views";
        final String RECORDED_DATE_STRING = "recorded_at";
        final String PREVIEW_URL_OBJECT = "preview";
        final String PREVIEW_URL_SMALL_STRING = "small";
        final String PREVIEW_URL_MEDIUM_STRING = "medium";
        final String PREVIEW_URL_LARGE_STRING = "large";
        final String CHANNEL_OBJECT = "channel";
        final String CHANNEL_NAME_STRING = "name";
        final String CHANNEL_DISPLAY_NAME_STRING = "display_name";

        JSONObject channel = vodObject.getJSONObject(CHANNEL_OBJECT);
        String gameTitle = "";
        if (!vodObject.isNull(GAME_TITLE_STRING)) {
            gameTitle = vodObject.getString(GAME_TITLE_STRING);
        }

        return new VideoOnDemand(vodObject.getString(TITLE_STRING), gameTitle, vodObject.getJSONObject(PREVIEW_URL_OBJECT).getString(PREVIEW_URL_MEDIUM_STRING), vodObject.getString(VIDEO_ID_STRING), channel.getString(CHANNEL_NAME_STRING), channel.getString(CHANNEL_DISPLAY_NAME_STRING), vodObject.getInt(VIDEO_VIEWS_INT), vodObject.getInt(VIDEO_LENGTH_INT), vodObject.has(RECORDED_DATE_STRING) ? vodObject.getString(RECORDED_DATE_STRING) : "");
    }

    public static StreamInfo getStreamInfo(Context context, JSONObject streamObject, @Nullable ChannelInfo aChannelInfo, boolean loadDescription) throws JSONException, MalformedURLException {
        final String PREVIEW_LINK_OBJECT = "preview";
        final String CHANNEL_OBJECT = "channel";
        final String CHANNEL_STATUS_STRING = "status";
        final String GAME_STRING = "game";
        final String STREAM_START_TIME_STRING = "created_at";
        final String CURRENT_VIEWERS_INT = "viewers";

        final String PREVIEW_SMALL_STRING = "small";
        final String PREVIEW_MEDIUM_STRING = "medium";
        final String PREVIEW_LARGE_STRING = "large";

        JSONObject JSONPreview = streamObject.getJSONObject(PREVIEW_LINK_OBJECT);
        JSONObject JSONChannel = streamObject.getJSONObject(CHANNEL_OBJECT);
        ChannelInfo mChannelInfo = aChannelInfo == null ? getStreamerInfo(JSONChannel, loadDescription) : aChannelInfo;

        String gameName = streamObject.getString(GAME_STRING);
        String title = context.getString(R.string.default_stream_title, mChannelInfo.getDisplayName(), gameName);
        int currentViewers = streamObject.getInt(CURRENT_VIEWERS_INT);

        if (!JSONChannel.isNull(CHANNEL_STATUS_STRING)) {
            title = JSONChannel.getString(CHANNEL_STATUS_STRING);
        } else {
            Log.i(LOG_TAG, "Status/title for " + mChannelInfo.getDisplayName() + " is null");
        }

        String[] previews = {
                JSONPreview.getString(PREVIEW_SMALL_STRING),
                JSONPreview.getString(PREVIEW_MEDIUM_STRING),
                JSONPreview.getString(PREVIEW_LARGE_STRING),
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

    public static ChannelInfo getStreamerInfo(JSONObject channel, boolean loadDescriptionOnSameThread) throws JSONException, MalformedURLException {
        final String DISPLAY_NAME_STRING = "display_name";
        final String TWITCH_NAME_STRING = "name";
        final String FOLLOWERS_INT = "followers";
        final String VIEWS_INT = "views";
        final String LOGO_URL_STRING = "logo";
        final String BANNER_URL_STRING = "banner";
        final String VIDEO_BANNER_URL_STRING = "video_banner";
        final String BACKGROUND_STRING = "background";
        final String PROFILE_BANNER_URL_STRING = "profile_banner";
        final String PROFILE_BANNER_BACKGROUND_COLOR_STRING = "profile_banner_background_color";
        final String BIO_STRING = "bio";
        final String USER_ID_INT = "_id";

        String displayName = channel.getString(DISPLAY_NAME_STRING);
        int userId = channel.getInt(USER_ID_INT);
        final String twitchName = channel.getString(TWITCH_NAME_STRING);
        int follows = channel.getInt(FOLLOWERS_INT);
        int views = channel.getInt(VIEWS_INT);
        URL logoURL = null,
                videoBannerURL = null,
                profileBannerURL = null;

        if (!channel.isNull(LOGO_URL_STRING)) {
            logoURL = new URL(channel.getString(LOGO_URL_STRING));
        }

        if (!channel.isNull(VIDEO_BANNER_URL_STRING)) {
            videoBannerURL = new URL(channel.getString(VIDEO_BANNER_URL_STRING));
        }

        if (!channel.isNull(PROFILE_BANNER_URL_STRING)) {
            profileBannerURL = new URL(channel.getString(PROFILE_BANNER_URL_STRING));
        }

        final ChannelInfo channelInfo = new ChannelInfo(userId, twitchName, displayName, "", follows, views, logoURL, videoBannerURL, profileBannerURL);

        // Load the user's description.
        final String descriptionURL = "https://api.twitch.tv/kraken/users/" + userId;
        if (!loadDescriptionOnSameThread) {
            new Thread(() -> {
                try {
                    JSONObject JSONStringTwo = new JSONObject(Service.urlToJSONString(descriptionURL));
                    String asyncDescription = JSONStringTwo.isNull(BIO_STRING) ? "" : JSONStringTwo.getString(BIO_STRING);
                    channelInfo.setStreamDescription(asyncDescription);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }).start();
        } else {
            JSONObject JSONStringTwo = new JSONObject(Service.urlToJSONString(descriptionURL));
            String description = JSONStringTwo.isNull(BIO_STRING) ? "" : JSONStringTwo.getString(BIO_STRING);
            channelInfo.setStreamDescription(description);
        }

        return channelInfo;
    }

    public static Game getGame(JSONObject game) throws JSONException {
        final String TITLE_STRING_KEY = "name";
        final String PREVIEW_OBJECT_KEY = "box";
        final String LARGE_STRING_KEY = "large";
        final String MEDIUM_STRING_KEY = "medium";
        final String SMALL_STRING_KEY = "small";

        JSONObject previewsObject = game.getJSONObject(PREVIEW_OBJECT_KEY);

        String gameTitle = game.getString(TITLE_STRING_KEY);
        String smallPreview = previewsObject.getString(SMALL_STRING_KEY);
        String mediumPreview = previewsObject.getString(MEDIUM_STRING_KEY);
        String largePreview = previewsObject.getString(LARGE_STRING_KEY);

        return new Game(gameTitle, smallPreview, mediumPreview, largePreview);
    }
}
