package com.perflyst.twire.tasks

import com.perflyst.twire.misc.Utils.safeEncode
import com.perflyst.twire.model.Quality
import com.perflyst.twire.service.Service
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.util.Random
import java.util.concurrent.Callable
import java.util.regex.Pattern

class GetStreamURL(
    channel: String,
    vod: String?,
    private val playerType: String?,
    private val proxy: String,
    private val isClip: Boolean
) : Callable<MutableMap<String, Quality>?> {
    private val channelOrVod: String
    private val isLive: Boolean = vod == null

    init {
        this.channelOrVod = if (isLive) channel else vod!!
    }

    private val token: JSONObject?
        get() {
            if (isClip) {
                return Service.graphQL(
                    "VideoAccessToken_Clip",
                    "36b89d2507fce29e5ca551df756d27c1cfe079e2609642b4390aa4c35796eb11",
                    object : HashMap<String, Any?>() {
                        init {
                            put("slug", channelOrVod)
                        }
                    })
            }


            return Service.graphQL(
                "PlaybackAccessToken",
                "0828119ded1c13477966434e15800ff57ddacf13ba1911c129dc2200705b0712",
                object : HashMap<String, Any?>() {
                    init {
                        put("isLive", isLive)
                        put("isVod", !isLive)
                        put("login", if (isLive) channelOrVod else "")
                        put("vodID", if (!isLive) channelOrVod else "")
                        put("playerType", playerType)
                    }
                })
        }

    override fun call(): MutableMap<String, Quality>? {
        var signature = ""
        var token = ""

        var dataObject = this.token
        if (dataObject == null) return LinkedHashMap()

        try {
            if (isClip) dataObject = dataObject.getJSONObject("clip")

            val tokenJSON =
                dataObject.getJSONObject(if (isLive) "streamPlaybackAccessToken" else if (isClip) "playbackAccessToken" else "videoPlaybackAccessToken")
            token = tokenJSON.getString("value")
            signature = tokenJSON.getString("signature")

            if (isClip) return handleClip(token, signature, dataObject)

            Timber.tag("ACCESS_TOKEN_STRING").d(token)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        var streamUrl =
            (if (isLive) "https://usher.ttvnw.net/api/channel/hls/$channelOrVod.m3u8" else "https://usher.ttvnw.net/vod/$channelOrVod") +
                    "?player=twitchweb&" +
                    "&token=${safeEncode(token)}" +
                    "&sig=$signature" +
                    "&allow_audio_only=true" +
                    "&allow_source=true" +
                    "&type=any" +
                    "&fast_bread=true" +
                    "&p=${Random().nextInt(6)}"

        if (isLive && !proxy.isEmpty()) {
            val parameters =
                "$channelOrVod.m3u8?allow_source=true&allow_audio_only=true&fast_bread=true"
            streamUrl = "$proxy/playlist/${safeEncode(parameters)}"
        }

        Timber.d("HSL Playlist URL: %s", streamUrl)
        return parseM3U8(streamUrl)
    }

    fun parseM3U8(urlToRead: String): LinkedHashMap<String, Quality> {
        val request = Request.Builder()
            .url(urlToRead)
            .header("Referer", "https://player.twitch.tv")
            .header("Origin", "https://player.twitch.tv")
            .build()

        var result: String? = ""
        val response = Service.makeRequest(request)
        if (response != null) result = response.body

        val resultList = LinkedHashMap<String, Quality>()
        if (result == null) return resultList
        resultList.put(QUALITY_AUTO, Quality("Auto", urlToRead))

        val p = Pattern.compile("GROUP-ID=\"(.+)\",NAME=\"(.+)\".+\\n.+\\n(https?://\\S+)")
        val m = p.matcher(result)

        while (m.find()) {
            resultList.put(m.group(1), Quality(m.group(2)!!, m.group(3)!!))
        }

        return resultList
    }

    @Throws(JSONException::class)
    private fun handleClip(
        token: String?,
        signature: String?,
        dataObject: JSONObject
    ): MutableMap<String, Quality> {
        val videoQualities = dataObject.getJSONArray("videoQualities")
        val qualities = LinkedHashMap<String, Quality>()
        for (i in 0..<videoQualities.length()) {
            val quality = videoQualities.getJSONObject(i)
            val qualityName = quality.getString("quality")
            var qualityUrl = quality.getString("sourceURL")
            qualityUrl += "?sig=$signature&token=${safeEncode(token)}"

            qualities.put(qualityName, Quality(qualityName, qualityUrl))
        }

        return qualities
    }

    companion object {
        const val QUALITY_SOURCE: String = "chunked"
        const val QUALITY_AUTO: String = "auto"
    }
}
