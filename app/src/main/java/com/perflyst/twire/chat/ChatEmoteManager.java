package com.perflyst.twire.chat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.perflyst.twire.R;
import com.perflyst.twire.model.ChatEmote;
import com.perflyst.twire.model.Emote;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.service.Settings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by sebastian on 26/07/2017.
 */

public class ChatEmoteManager {
    private static Map<String, String> bttvEmotesToId;

    private final List<Emote> bttvGlobal = new ArrayList<>();
    private final List<Emote> bttvChannel = new ArrayList<>();

    private Pattern bttvEmotesPattern = Pattern.compile("");
    private Pattern emotePattern = Pattern.compile("(\\d+):((?:\\d+-\\d+,?)+)");

    private Settings settings;
    private Context context;
    private int channelId;
    private String channelName;

    ChatEmoteManager(int channelId, String channelName, Context context) {
        this.context = context;
        this.channelId = channelId;
        this.channelName = channelName;
        this.settings = new Settings(context);
    }

    private static String getEmoteUrl(boolean isEmoteBttv, String emoteId, int size) {
        return isEmoteBttv
                ? "https://cdn.betterttv.net/emote/" + emoteId + "/" + size + "x"
                : "https://static-cdn.jtvnw.net/emoticons/v1/" + emoteId + "/" + size + ".0";
    }

    public static String getEmoteUrl(Emote emote, int size) {
        return getEmoteUrl(emote.isBetterTTVEmote(), emote.getEmoteId(), size);
    }

    /**
     * Connects to the Better Twitch Tv API.
     * Fetches and maps the emote keywords and id's
     * This must not be called on main UI thread
     */
    void loadBttvEmotes(EmoteFetchCallback callback) {
        Map<String, String> result = new HashMap<>();
        StringBuilder emotesPattern = new StringBuilder();

        final String BASE_GLOBAL_URL = "https://api.betterttv.net/2/emotes";
        final String BASE_CHANNEL_URL = "https://api.betterttv.net/2/channels/" + channelName;
        final String EMOTE_ARRAY = "emotes";
        final String EMOTE_ID = "id";
        final String EMOTE_WORD = "code";

        try {
            JSONObject topObject = new JSONObject(Service.urlToJSONString(BASE_GLOBAL_URL));
            JSONArray globalEmotes = topObject.getJSONArray(EMOTE_ARRAY);

            for (int i = 0; i < globalEmotes.length(); i++) {

                JSONObject emoteObject = globalEmotes.getJSONObject(i);
                String emoteKeyword = emoteObject.getString(EMOTE_WORD);
                String emoteId = emoteObject.getString(EMOTE_ID);
                result.put(emoteKeyword, emoteId);

                Emote emote = new Emote(emoteId, emoteKeyword, true);
                bttvGlobal.add(emote);

                if (emotesPattern.toString().equals("")) {
                    emotesPattern = new StringBuilder(Pattern.quote(emoteKeyword));
                } else {
                    emotesPattern.append("|").append(Pattern.quote(emoteKeyword));
                }
            }

            JSONObject topChannelEmotes = new JSONObject(Service.urlToJSONString(BASE_CHANNEL_URL));
            JSONArray channelEmotes = topChannelEmotes.getJSONArray(EMOTE_ARRAY);
            for (int i = 0; i < channelEmotes.length(); i++) {

                JSONObject emoteObject = channelEmotes.getJSONObject(i);
                String emoteKeyword = emoteObject.getString(EMOTE_WORD);
                String emoteId = emoteObject.getString(EMOTE_ID);
                result.put(emoteKeyword, emoteId);

                Emote emote = new Emote(emoteId, emoteKeyword, true);
                emote.setBetterTTVChannelEmote(true);
                bttvChannel.add(emote);

                if (emotesPattern.toString().equals("")) {
                    emotesPattern = new StringBuilder(Pattern.quote(emoteKeyword));
                } else {
                    emotesPattern.append("|").append(Pattern.quote(emoteKeyword));
                }
            }


        } catch (JSONException e) {
            e.printStackTrace();
        }

        bttvEmotesPattern = Pattern.compile("\\b(" + emotesPattern + ")\\b");
        bttvEmotesToId = result;

        try {
            callback.onEmoteFetched();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Connects to Twitch API to get the URL for the channels subscriber emote
     * This must not be executed on main UI Thread
     *
     * @return
     */
    Bitmap getSubscriberEmote() {
        Bitmap emote = null;

        final String URL = "https://api.twitch.tv/kraken/chat/" + channelId + "/badges";
        final String SUBSCRIBER_OBJECT = "subscriber";
        final String SUBSCRIBER_IMAGE_STRING = "image";

        try {
            JSONObject dataObject = new JSONObject(Service.urlToJSONString(URL));
            JSONObject subscriberObject = dataObject.getJSONObject(SUBSCRIBER_OBJECT);
            String imageUrl = subscriberObject.getString(SUBSCRIBER_IMAGE_STRING);

            emote = Service.getBitmapFromUrl(imageUrl);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (emote != null) {
            return emote;
        } else {
            return BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_missing_emote);
        }
    }

    /**
     * Finds and creates Better Twitch Tv emotes in a message and returns them.
     *
     * @param message The message to find emotes in
     * @return The List of emotes in the message
     */
    List<ChatEmote> findBttvEmotes(String message) {
        List<ChatEmote> emotes = new ArrayList<>();
        Matcher bttvEmoteMatcher = bttvEmotesPattern.matcher(message);

        while (bttvEmoteMatcher.find()) {
            String emoteKeyword = bttvEmoteMatcher.group();
            String emoteId = bttvEmotesToId.get(emoteKeyword);

            String[] positions = new String[]{bttvEmoteMatcher.start() + "-" + (bttvEmoteMatcher.end() - 1)};
            String emoteUrl = getEmoteFromId(emoteId, true);
            if (emoteUrl != null) {
                final ChatEmote chatEmote = new ChatEmote(positions, emoteUrl);
                emotes.add(chatEmote);
            }
        }

        return emotes;
    }

    /**
     * Finds and creates Twitch emotes in an unsplit irc line.
     *
     * @param message The line to find emotes in
     * @return The list of emotes from the line
     */
    List<ChatEmote> findTwitchEmotes(String message) {
        List<ChatEmote> emotes = new ArrayList<>();
        Matcher emoteMatcher = emotePattern.matcher(message);

        while (emoteMatcher.find()) {
            String emoteId = emoteMatcher.group(1);
            String[] positions = emoteMatcher.group(2).split(",");
            emotes.add(new ChatEmote(positions, getEmoteFromId(emoteId, false)));
        }

        return emotes;
    }

    /**
     * Returns a Bitmap of the emote with the specified emote id.
     * If the emote has not been cached from an earlier download the method
     * connects to the twitchemotes.com API to get the emote image.
     * The image is cached and converted to a bitmap which is returned.
     */
    String getEmoteFromId(String emoteId, boolean isBttvEmote) {
        int settingsSize = getApiEmoteSizeFromSettingsSize(settings.getEmoteSize());

        return getEmoteUrl(isBttvEmote, emoteId, settingsSize);
    }

    private int getApiEmoteSizeFromSettingsSize(int settingsSize) {
        return settingsSize == 1 ? 2 : settingsSize;
    }

    List<Emote> getGlobalBttvEmotes() {
        return bttvGlobal;
    }

    List<Emote> getChanncelBttvEmotes() {
        return bttvChannel;
    }

    public interface EmoteFetchCallback {
        void onEmoteFetched();
    }
}
