package com.perflyst.twire.chat;

import androidx.annotation.Nullable;

import com.perflyst.twire.model.ChatEmote;
import com.perflyst.twire.model.Emote;
import com.perflyst.twire.model.UserInfo;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.service.Settings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by sebastian on 26/07/2017.
 */

class ChatEmoteManager {
    private static Map<String, Emote> emoteKeywordToEmote;

    private final List<Emote> customGlobal = new ArrayList<>();
    private final List<Emote> customChannel = new ArrayList<>();

    private final Pattern emotePattern = Pattern.compile("(\\w+):((?:\\d+-\\d+,?)+)");

    private final UserInfo channel;
    private final Settings settings;

    ChatEmoteManager(UserInfo channel, Settings settings) {
        this.channel = channel;
        this.settings = settings;
    }


    /**
     * Connects to custom emote APIs.
     * Fetches and maps the emote keywords and id's
     * This must not be called on main UI thread
     */
    void loadCustomEmotes(EmoteFetchCallback callback) {
        Map<String, Emote> result = new HashMap<>();

        // Emote Settings
        boolean enabled_bttv = settings.getChatEmoteBTTV();
        boolean enabled_ffz = settings.getChatEmoteFFZ();
        boolean enabled_seventv = settings.getChatEmoteSEVENTV();

        // BetterTTV emotes
        final String BTTV_GLOBAL_URL = "https://api.betterttv.net/3/cached/emotes/global";
        final String BTTV_CHANNEL_URL = "https://api.betterttv.net/3/cached/users/twitch/" + channel.getUserId();
        final String CHANNEL_EMOTE_ARRAY = "channelEmotes";
        final String SHARED_EMOTE_ARRAY = "sharedEmotes";

        try {
            String bttvResponse = enabled_bttv ? Service.urlToJSONString(BTTV_GLOBAL_URL) : "";
            if (!bttvResponse.isEmpty()) {
                JSONArray globalEmotes = new JSONArray(bttvResponse);

                for (int i = 0; i < globalEmotes.length(); i++) {
                    Emote emote = ToBTTV(globalEmotes.getJSONObject(i));
                    customGlobal.add(emote);
                    result.put(emote.getKeyword(), emote);
                }
            }

            String bttvChannelResponse = enabled_bttv ? Service.urlToJSONString(BTTV_CHANNEL_URL) : "";
            if (!bttvChannelResponse.isEmpty()) {
                JSONObject topChannelEmotes = new JSONObject(bttvChannelResponse);
                JSONArray channelEmotes = topChannelEmotes.getJSONArray(CHANNEL_EMOTE_ARRAY);

                // Append shared emotes
                JSONArray sharedEmotes = topChannelEmotes.getJSONArray(SHARED_EMOTE_ARRAY);
                for (int i = 0; i < sharedEmotes.length(); i++) {
                    channelEmotes.put(sharedEmotes.get(i));
                }

                // Read all the emotes
                for (int i = 0; i < channelEmotes.length(); i++) {
                    Emote emote = ToBTTV(channelEmotes.getJSONObject(i));
                    emote.setCustomChannelEmote(true);
                    customChannel.add(emote);
                    result.put(emote.getKeyword(), emote);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // FFZ emotes
        final String FFZ_GLOBAL_URL = "https://api.frankerfacez.com/v1/set/global";
        final String FFZ_CHANNEL_URL = "https://api.frankerfacez.com/v1/room/" + channel.getLogin();
        final String DEFAULT_SETS = "default_sets";
        final String SETS = "sets";
        final String EMOTICONS = "emoticons";

        try {
            JSONObject topObject = enabled_ffz ? new JSONObject(Service.urlToJSONString(FFZ_GLOBAL_URL)) : new JSONObject();

            JSONArray defaultSets;
            if (topObject.has("defaultSets")) {
                defaultSets = topObject.getJSONArray(DEFAULT_SETS);
            } else {
                defaultSets = new JSONArray();
            }

            JSONObject sets;
            if (topObject.has("sets")) {
                sets = topObject.getJSONObject(SETS);
            } else {
                sets = new JSONObject();
            }

            for (int setIndex = 0; setIndex < defaultSets.length(); setIndex++) {
                JSONArray emoticons = sets.getJSONObject(defaultSets.get(setIndex).toString()).getJSONArray(EMOTICONS);
                for (int emoteIndex = 0; emoteIndex < emoticons.length(); emoteIndex++) {
                    Emote emote = ToFFZ(emoticons.getJSONObject(emoteIndex));
                    customGlobal.add(emote);
                    result.put(emote.getKeyword(), emote);
                }
            }

            String ffzResponse = enabled_ffz ? Service.urlToJSONString(FFZ_CHANNEL_URL) : "";
            if (!ffzResponse.isEmpty()) {
                JSONObject channelTopObject = new JSONObject(ffzResponse);
                JSONObject channelSets = channelTopObject.getJSONObject(SETS);
                for (Iterator<String> iterator = channelSets.keys(); iterator.hasNext(); ) {
                    JSONArray emoticons = channelSets.getJSONObject(iterator.next()).getJSONArray(EMOTICONS);
                    for (int emoteIndex = 0; emoteIndex < emoticons.length(); emoteIndex++) {
                        Emote emote = ToFFZ(emoticons.getJSONObject(emoteIndex));
                        emote.setCustomChannelEmote(true);
                        customChannel.add(emote);
                        result.put(emote.getKeyword(), emote);
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // 7TV emotes
        // API Doc: https://github.com/SevenTV/ServerGo/blob/master/docs/rest-api.md
        final String SEVENTV_GLOBAL_URL = "https://api.7tv.app/v2/emotes/global";
        final String SEVENTV_CHANNEL_URL = "https://api.7tv.app/v2/users/" + channel.getLogin() + "/emotes";

        try {
            String seventvResponseglobal = enabled_seventv ? Service.urlToJSONString(SEVENTV_GLOBAL_URL) : "";
            if (!seventvResponseglobal.isEmpty()) {
                // get global emotes
                JSONArray seventvemotesglobal = new JSONArray(seventvResponseglobal);

                // Read all the emotes
                for (int i = 0; i < seventvemotesglobal.length(); i++) {
                    Emote emote = To7TV(seventvemotesglobal.getJSONObject(i));
                    emote.setCustomChannelEmote(false);
                    customChannel.add(emote);
                    result.put(emote.getKeyword(), emote);
                }
            }

            String seventvResponsechannel = enabled_seventv ? Service.urlToJSONString(SEVENTV_CHANNEL_URL) : "";
            if (!seventvResponseglobal.isEmpty()) {
                // get channel emotes
                JSONArray seventvemoteschannel;
                try {
                    JSONObject response = new JSONObject(seventvResponsechannel);
                    seventvemoteschannel = new JSONArray();
                } catch (JSONException e) {
                    // If there is an exception above then the User has custom emotes
                    seventvemoteschannel = new JSONArray(seventvResponsechannel);
                }

                // Read all the emotes
                for (int i = 0; i < seventvemoteschannel.length(); i++) {
                    Emote emote = To7TV(seventvemoteschannel.getJSONObject(i));
                    emote.setCustomChannelEmote(true);
                    customChannel.add(emote);
                    result.put(emote.getKeyword(), emote);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        emoteKeywordToEmote = result;

        try {
            callback.onEmoteFetched();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Emote ToBTTV(JSONObject emoteObject) throws JSONException {
        final String EMOTE_ID = "id";
        final String EMOTE_WORD = "code";

        return Emote.BTTV(emoteObject.getString(EMOTE_WORD), emoteObject.getString(EMOTE_ID));
    }

    private Emote ToFFZ(JSONObject emoteObject) throws JSONException {
        final String EMOTE_NAME = "name";
        final String EMOTE_URLS = "urls";

        JSONObject urls = emoteObject.getJSONObject(EMOTE_URLS);
        HashMap<Integer, String> urlMap = new HashMap<>();
        for (Iterator<String> iterator = urls.keys(); iterator.hasNext(); ) {
            String key = iterator.next();
            urlMap.put(Integer.parseInt(key), "https:" + urls.getString(key));
        }

        return Emote.FFZ(emoteObject.getString(EMOTE_NAME), urlMap);
    }

    private Emote To7TV(JSONObject emoteObject) throws JSONException {
        final String EMOTE_ID = "id";
        final String EMOTE_WORD = "name";

        return Emote.SevenTV(emoteObject.getString(EMOTE_WORD), emoteObject.getString(EMOTE_ID));
    }

    /**
     * Finds and creates custom emotes in a message and returns them.
     *
     * @param message The message to find emotes in
     * @return The List of emotes in the message
     */
    List<ChatEmote> findCustomEmotes(String message) {
        List<ChatEmote> emotes = new ArrayList<>();

        int position = 0;
        for (String part : message.split(" ")) {
            if (emoteKeywordToEmote.containsKey(part)) {
                Emote emote = emoteKeywordToEmote.get(part);

                int[] positions = {position};
                final ChatEmote chatEmote = new ChatEmote(emote, positions);
                emotes.add(chatEmote);
            }

            position += part.length() + 1;
        }

        return emotes;
    }

    /**
     * Finds and creates Twitch emotes in an unsplit irc line.
     *
     * @param line The line to find emotes in
     * @return The list of emotes from the line
     */
    List<ChatEmote> findTwitchEmotes(@Nullable String line, String message) {
        if (line == null)
            return Collections.emptyList();

        List<ChatEmote> emotes = new ArrayList<>();
        Matcher emoteMatcher = emotePattern.matcher(line);

        while (emoteMatcher.find()) {
            String emoteId = emoteMatcher.group(1);
            String[] stringPositions = emoteMatcher.group(2).split(",");
            int[] positions = new int[stringPositions.length];
            String keyword = "";
            for (int i = 0; i < stringPositions.length; i++) {
                String stringPosition = stringPositions[i];
                String[] range = stringPosition.split("-");
                int start = Integer.parseInt(range[0]);

                positions[i] = start;

                if (i == 0) {
                    int end = Integer.parseInt(range[1]);
                    keyword = message.substring(start, end + 1);
                }
            }

            emotes.add(new ChatEmote(Emote.Twitch(keyword, emoteId), positions));
        }

        return emotes;
    }

    List<Emote> getGlobalCustomEmotes() {
        return customGlobal;
    }

    List<Emote> getChannelCustomEmotes() {
        return customChannel;
    }

    public interface EmoteFetchCallback {
        void onEmoteFetched();
    }
}
