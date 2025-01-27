package com.perflyst.twire.chat;

import androidx.annotation.Nullable;

import com.perflyst.twire.model.ChatMessage;
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

    ChatEmoteManager(UserInfo channel) {
        this.channel = channel;
    }


    /**
     * Connects to custom emote APIs.
     * Fetches and maps the emote keywords and id's
     * This must not be called on main UI thread
     */
    void loadCustomEmotes(EmoteFetchCallback callback) {
        emoteKeywordToEmote = new HashMap<>();

        // Emote Settings
        boolean enabled_bttv = Settings.getChatEmoteBTTV();
        boolean enabled_ffz = Settings.getChatEmoteFFZ();
        boolean enabled_seventv = Settings.getChatEmoteSEVENTV();

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
                    emoteKeywordToEmote.put(emote.getKeyword(), emote);
                }
            }

            String bttvChannelResponse = enabled_bttv ? Service.urlToJSONString(BTTV_CHANNEL_URL) : "";
            if (!bttvChannelResponse.isEmpty()) {
                JSONObject topChannelEmotes = new JSONObject(bttvChannelResponse);
                if (topChannelEmotes.has("message")) return;

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
                    emoteKeywordToEmote.put(emote.getKeyword(), emote);
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
                    emoteKeywordToEmote.put(emote.getKeyword(), emote);
                }
            }

            String ffzResponse = enabled_ffz ? Service.urlToJSONString(FFZ_CHANNEL_URL) : "";
            if (!ffzResponse.isEmpty()) {
                JSONObject channelTopObject = new JSONObject(ffzResponse);
                if (!channelTopObject.has("error")) {
                    JSONObject channelSets = channelTopObject.getJSONObject(SETS);
                    for (Iterator<String> iterator = channelSets.keys(); iterator.hasNext(); ) {
                        JSONArray emoticons = channelSets.getJSONObject(iterator.next()).getJSONArray(EMOTICONS);
                        for (int emoteIndex = 0; emoteIndex < emoticons.length(); emoteIndex++) {
                            Emote emote = ToFFZ(emoticons.getJSONObject(emoteIndex));
                            emote.setCustomChannelEmote(true);
                            customChannel.add(emote);
                            emoteKeywordToEmote.put(emote.getKeyword(), emote);
                        }
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // 7TV emotes
        // API Doc: https://7tv.io/v3/docs
        final String SEVENTV_GLOBAL_URL = "https://7tv.io/v3/emote-sets/global";
        final String SEVENTV_USER_URL = "https://7tv.io/v3/users/twitch/" + channel.getUserId();

        try {
            if (enabled_seventv) {
                HashMap<String, JSONObject> emoteSets = new HashMap<>();

                // Get the global emote set
                emoteSets.put("global", new JSONObject(Service.urlToJSONString(SEVENTV_GLOBAL_URL)));

                // Get the channel's emote sets
                JSONObject userData = new JSONObject(Service.urlToJSONString(SEVENTV_USER_URL));
                if (!userData.isNull("emote_set"))
                    emoteSets.put("channel", userData.getJSONObject("emote_set"));

                // Load the emote sets
                for (var entry : emoteSets.entrySet()) {
                    JSONObject emoteSetData = entry.getValue();
                    JSONArray emotes = emoteSetData.getJSONArray("emotes");
                    for (int i = 0; i < emotes.length(); i++) {
                        Emote emote = To7TV(emotes.getJSONObject(i));
                        if (entry.getKey().equals("global")) {
                            customGlobal.add(emote);
                        } else {
                            emote.setCustomChannelEmote(true);
                            customChannel.add(emote);
                        }
                        emoteKeywordToEmote.put(emote.getKeyword(), emote);
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

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
            urlMap.put(Integer.parseInt(key), urls.getString(key));
        }

        return new Emote(emoteObject.getString(EMOTE_NAME), urlMap);
    }

    private Emote To7TV(JSONObject emoteObject) throws JSONException {
        JSONObject hostObject = emoteObject.getJSONObject("data").getJSONObject("host");
        String baseUrl = String.format("https:%s/", hostObject.getString("url"));

        JSONArray files = hostObject.getJSONArray("files");
        HashMap<Integer, String> urlMap = new HashMap<>();
        for (int i = 0; i < files.length(); i++) {
            JSONObject file = files.getJSONObject(i);
            String name = file.getString("name");
            if (!name.endsWith(".webp")) continue;

            Integer size = Integer.parseInt(name.substring(0, 1));
            urlMap.put(size, baseUrl + name);
        }

        return new Emote(emoteObject.getString("name"), urlMap);
    }

    /**
     * Finds and creates custom emotes in a message and returns them.
     *
     * @param message The message to find emotes in
     * @return The List of emotes in the message
     */
    Map<Integer, Emote> findCustomEmotes(String message) {
        return ChatMessage.getEmotesFromMessage(message, emoteKeywordToEmote);
    }

    /**
     * Finds and creates Twitch emotes in an unsplit irc line.
     *
     * @param line The line to find emotes in
     * @return The list of emotes from the line
     */
    Map<Integer, Emote> findTwitchEmotes(@Nullable String line, String message) {
        if (line == null)
            return Collections.emptyMap();

        Map<Integer, Emote> emotes = new HashMap<>();
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

            for (int position : positions) {
                emotes.put(position, Emote.Twitch(keyword, emoteId));
            }
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
