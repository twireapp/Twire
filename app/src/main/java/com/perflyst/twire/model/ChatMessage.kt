package com.perflyst.twire.model

import com.perflyst.twire.chat.ChatManager
import java.util.Locale


data class ChatMessage(
    @JvmField val message: String,
    @JvmField val name: String,
    @JvmField val color: String?,
    @JvmField val badges: MutableList<Badge?>,
    @JvmField val emotes: Map<Int, Emote>,
    @JvmField var isHighlight: Boolean
) {
    var id: String? = null

    @JvmField
    var systemMessage: String = ""

    init {
        ChatManager.ffzBadgeMap?.let { ffzBadgeMap ->
            // Load any special FFZ badges the user has
            for (badge in ffzBadgeMap[name.lowercase(Locale.getDefault())]) {
                if (badge.replaces != null) {
                    for (i in badges.indices) {
                        if (badges[i] != null && badges[i]!!.name == badge.replaces) {
                            badges[i] = badge
                            break
                        }
                    }
                } else {
                    badges.add(badge)
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun getEmotesFromMessage(message: String, emoteMap: Map<String, Emote>): Map<Int, Emote> {
            var position = 0
            val foundEmotes: MutableMap<Int, Emote> = HashMap()
            for (word in message.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()) {
                val emote = emoteMap[word]
                if (emote != null) {
                    foundEmotes[position] = emote
                }
                position += word.length + 1
            }

            return foundEmotes
        }
    }
}
