package com.perflyst.twire.chat

import com.perflyst.twire.model.ChatMessage
import com.perflyst.twire.model.Emote
import com.perflyst.twire.model.Emote.Companion.BTTV
import com.perflyst.twire.model.UserInfo
import com.perflyst.twire.service.Service
import com.perflyst.twire.service.Settings.chatEmoteBTTV
import com.perflyst.twire.service.Settings.chatEmoteFFZ
import com.perflyst.twire.service.Settings.chatEmoteSEVENTV
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.util.regex.Pattern

/**
 * Created by sebastian on 26/07/2017.
 */
internal class ChatEmoteManager(private val channel: UserInfo) {
    val globalCustomEmotes: MutableList<Emote> = ArrayList()
    val channelCustomEmotes: MutableList<Emote> = ArrayList()

    private val emotePattern: Pattern = Pattern.compile("(\\w+):((?:\\d+-\\d+,?)+)")


    /**
     * Connects to custom emote APIs.
     * Fetches and maps the emote keywords and id's
     * This must not be called on main UI thread
     */
    fun loadCustomEmotes(callback: EmoteFetchCallback) {
        emoteKeywordToEmote = HashMap()

        // Emote Settings
        val enabled_bttv = chatEmoteBTTV
        val enabled_ffz = chatEmoteFFZ
        val enabled_seventv = chatEmoteSEVENTV

        // BetterTTV emotes
        val BTTV_GLOBAL_URL = "https://api.betterttv.net/3/cached/emotes/global"
        val BTTV_CHANNEL_URL = "https://api.betterttv.net/3/cached/users/twitch/${channel.userId}"
        val CHANNEL_EMOTE_ARRAY = "channelEmotes"
        val SHARED_EMOTE_ARRAY = "sharedEmotes"

        try {
            val bttvResponse = if (enabled_bttv) Service.urlToJSONString(BTTV_GLOBAL_URL) else ""
            if (!bttvResponse.isEmpty()) {
                val globalEmotes = JSONArray(bttvResponse)

                for (i in 0..<globalEmotes.length()) {
                    val emote = ToBTTV(globalEmotes.getJSONObject(i))
                    globalCustomEmotes.add(emote)
                    emoteKeywordToEmote!!.put(emote.keyword, emote)
                }
            }

            val bttvChannelResponse =
                if (enabled_bttv) Service.urlToJSONString(BTTV_CHANNEL_URL) else ""
            if (!bttvChannelResponse.isEmpty()) {
                val topChannelEmotes = JSONObject(bttvChannelResponse)
                if (topChannelEmotes.has("message")) return

                val channelEmotes = topChannelEmotes.getJSONArray(CHANNEL_EMOTE_ARRAY)

                // Append shared emotes
                val sharedEmotes = topChannelEmotes.getJSONArray(SHARED_EMOTE_ARRAY)
                for (i in 0..<sharedEmotes.length()) {
                    channelEmotes.put(sharedEmotes.get(i))
                }

                // Read all the emotes
                for (i in 0..<channelEmotes.length()) {
                    val emote = ToBTTV(channelEmotes.getJSONObject(i))
                    emote.isCustomChannelEmote = true
                    channelCustomEmotes.add(emote)
                    emoteKeywordToEmote!!.put(emote.keyword, emote)
                }
            }
        } catch (e: JSONException) {
            Timber.e(e)
        }

        // FFZ emotes
        val FFZ_GLOBAL_URL = "https://api.frankerfacez.com/v1/set/global"
        val FFZ_CHANNEL_URL = "https://api.frankerfacez.com/v1/room/${channel.login}"
        val DEFAULT_SETS = "default_sets"
        val SETS = "sets"
        val EMOTICONS = "emoticons"

        try {
            val topObject =
                if (enabled_ffz) JSONObject(Service.urlToJSONString(FFZ_GLOBAL_URL)) else JSONObject()

            val defaultSets = if (topObject.has("defaultSets")) {
                topObject.getJSONArray(DEFAULT_SETS)
            } else {
                JSONArray()
            }

            val sets = if (topObject.has("sets")) {
                topObject.getJSONObject(SETS)
            } else {
                JSONObject()
            }

            for (setIndex in 0..<defaultSets.length()) {
                val emoticons =
                    sets.getJSONObject(defaultSets.get(setIndex).toString()).getJSONArray(EMOTICONS)
                for (emoteIndex in 0..<emoticons.length()) {
                    val emote = ToFFZ(emoticons.getJSONObject(emoteIndex))
                    globalCustomEmotes.add(emote)
                    emoteKeywordToEmote!!.put(emote.keyword, emote)
                }
            }

            val ffzResponse = if (enabled_ffz) Service.urlToJSONString(FFZ_CHANNEL_URL) else ""
            if (!ffzResponse.isEmpty()) {
                val channelTopObject = JSONObject(ffzResponse)
                if (!channelTopObject.has("error")) {
                    val channelSets = channelTopObject.getJSONObject(SETS)
                    val iterator = channelSets.keys()
                    while (iterator.hasNext()) {
                        val emoticons =
                            channelSets.getJSONObject(iterator.next()).getJSONArray(EMOTICONS)
                        for (emoteIndex in 0..<emoticons.length()) {
                            val emote = ToFFZ(emoticons.getJSONObject(emoteIndex))
                            emote.isCustomChannelEmote = true
                            channelCustomEmotes.add(emote)
                            emoteKeywordToEmote!!.put(emote.keyword, emote)
                        }
                    }
                }
            }
        } catch (e: JSONException) {
            Timber.e(e)
        }

        // 7TV emotes
        // API Doc: https://7tv.io/v3/docs
        val SEVENTV_GLOBAL_URL = "https://7tv.io/v3/emote-sets/global"
        val SEVENTV_USER_URL = "https://7tv.io/v3/users/twitch/${channel.userId}"

        try {
            if (enabled_seventv) {
                val emoteSets = HashMap<String, JSONObject>()

                // Get the global emote set
                emoteSets.put("global", JSONObject(Service.urlToJSONString(SEVENTV_GLOBAL_URL)))

                // Get the channel's emote sets
                val userData = JSONObject(Service.urlToJSONString(SEVENTV_USER_URL))
                if (!userData.isNull("emote_set")) emoteSets.put(
                    "channel",
                    userData.getJSONObject("emote_set")
                )

                // Load the emote sets
                for (entry in emoteSets.entries) {
                    val emoteSetData = entry.value
                    val emotes = emoteSetData.getJSONArray("emotes")
                    for (i in 0..<emotes.length()) {
                        val emote = To7TV(emotes.getJSONObject(i))
                        if (entry.key == "global") {
                            globalCustomEmotes.add(emote)
                        } else {
                            emote.isCustomChannelEmote = true
                            channelCustomEmotes.add(emote)
                        }
                        emoteKeywordToEmote!!.put(emote.keyword, emote)
                    }
                }
            }
        } catch (e: JSONException) {
            Timber.e(e)
        }

        try {
            callback.onEmoteFetched()
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    @Throws(JSONException::class)
    private fun ToBTTV(emoteObject: JSONObject): Emote {
        val EMOTE_ID = "id"
        val EMOTE_WORD = "code"

        return BTTV(emoteObject.getString(EMOTE_WORD), emoteObject.getString(EMOTE_ID))
    }

    @Throws(JSONException::class)
    private fun ToFFZ(emoteObject: JSONObject): Emote {
        val EMOTE_NAME = "name"
        val EMOTE_URLS = "urls"

        val urls = emoteObject.getJSONObject(EMOTE_URLS)
        val urlMap = HashMap<Int, String>()
        val iterator = urls.keys()
        while (iterator.hasNext()) {
            val key = iterator.next()
            urlMap.put(key.toInt(), urls.getString(key))
        }

        return Emote(emoteObject.getString(EMOTE_NAME), urlMap)
    }

    @Throws(JSONException::class)
    private fun To7TV(emoteObject: JSONObject): Emote {
        val hostObject = emoteObject.getJSONObject("data").getJSONObject("host")
        val baseUrl = "https:${hostObject.getString("url")}/"

        val files = hostObject.getJSONArray("files")
        val urlMap = HashMap<Int, String>()
        for (i in 0..<files.length()) {
            val file = files.getJSONObject(i)
            val name = file.getString("name")
            if (!name.endsWith(".webp")) continue

            val size = name.substring(0, 1).toInt()
            urlMap.put(size, baseUrl + name)
        }

        return Emote(emoteObject.getString("name"), urlMap)
    }

    /**
     * Finds and creates custom emotes in a message and returns them.
     *
     * @param message The message to find emotes in
     * @return The List of emotes in the message
     */
    fun findCustomEmotes(message: String): Map<Int, Emote> {
        return ChatMessage.getEmotesFromMessage(message, emoteKeywordToEmote!!)
    }

    /**
     * Finds and creates Twitch emotes in an unsplit irc line.
     *
     * @param line The line to find emotes in
     * @return The list of emotes from the line
     */
    fun findTwitchEmotes(line: String?, message: String): MutableMap<Int, Emote> {
        if (line == null) return mutableMapOf()

        val emotes: MutableMap<Int, Emote> = HashMap()
        val emoteMatcher = emotePattern.matcher(line)

        while (emoteMatcher.find()) {
            val emoteId = emoteMatcher.group(1)
            val stringPositions =
                emoteMatcher.group(2)!!.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            val positions = IntArray(stringPositions.size)
            var keyword = ""
            for (i in stringPositions.indices) {
                val stringPosition = stringPositions[i]
                val range = stringPosition.split("-".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                val start = range[0].toInt()

                positions[i] = start

                if (i == 0) {
                    val end = range[1].toInt()
                    keyword = message.substring(start, end + 1)
                }
            }

            for (position in positions) {
                emotes.put(position, Emote.Twitch(keyword, emoteId!!))
            }
        }

        return emotes
    }

    fun interface EmoteFetchCallback {
        fun onEmoteFetched()
    }

    companion object {
        private var emoteKeywordToEmote: MutableMap<String, Emote>? = null
    }
}
