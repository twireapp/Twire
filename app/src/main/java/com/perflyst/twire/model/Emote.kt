package com.perflyst.twire.model

import java.io.Serializable

/**
 * Created by Sebastian Rask Jepsen on 28/07/16.
 */
data class Emote(@JvmField val keyword: String) : Comparable<Emote>, Serializable {
    private var urlParts: Array<out String>? = null
    private var urlMap: HashMap<Int, String>? = null

    @JvmField
    var isSubscriberEmote: Boolean = false

    @JvmField
    var isCustomChannelEmote: Boolean = false

    constructor(emoteKeyword: String, vararg urlParts: String) : this(emoteKeyword) {
        this.urlParts = urlParts
    }

    constructor(emoteKeyword: String, urlMap: HashMap<Int, String>) : this(emoteKeyword) {
        this.urlMap = urlMap
    }

    fun getEmoteUrl(size: Int, isDarkTheme: Boolean): String? {
        if (urlMap != null) {
            for (i in size downTo 1) {
                if (urlMap!!.containsKey(i)) {
                    return urlMap!![i]
                }
            }
        } else if (urlParts != null) {
            return buildString {
                for (part in urlParts!!) {
                    when (part) {
                        "@size" -> append(size)
                        "@theme" -> append(if (isDarkTheme) "dark" else "light")
                        else -> append(part)
                    }
                }
            }
        }

        return null
    }

    fun getBestAvailableSize(size: Int): Int {
        if (urlMap != null) {
            for (i in size downTo 1) {
                if (urlMap!!.containsKey(i)) {
                    return i
                }
            }

            return 1
        }

        return size
    }

    override fun compareTo(other: Emote): Int {
        return if (this.isCustomChannelEmote && !other.isCustomChannelEmote) {
            -1
        } else if (other.isCustomChannelEmote && !this.isCustomChannelEmote) {
            1
        } else {
            keyword.compareTo(other.keyword)
        }
    }

    companion object {
        @JvmStatic
        fun Twitch(keyword: String, id: String): Emote {
            return Emote(
                keyword,
                "https://static-cdn.jtvnw.net/emoticons/v2/$id/default/",
                "@theme",
                "/",
                "@size",
                ".0"
            )
        }

        @JvmStatic
        fun BTTV(keyword: String, id: String): Emote {
            return Emote(
                keyword,
                "https://cdn.betterttv.net/emote/$id/", "@size", "x"
            )
        }
    }
}
