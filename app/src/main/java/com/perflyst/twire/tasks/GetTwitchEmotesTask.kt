package com.perflyst.twire.tasks

import com.perflyst.twire.TwireApplication.Companion.helix
import com.perflyst.twire.model.Emote
import com.perflyst.twire.model.Emote.Companion.Twitch
import com.perflyst.twire.utils.Execute
import timber.log.Timber

/**
 * Created by idealMJ on 29/07/16.
 */
class GetTwitchEmotesTask(
    private val emoteSets: MutableList<String>,
    private val delegate: Delegate
) : Runnable {
    private val twitchEmotes: MutableList<Emote> = ArrayList()
    private val subscriberEmotes: MutableList<Emote> = ArrayList()

    override fun run() {
        val emotes = emoteSets.chunked(25)
            .flatMap { helix.getEmoteSets(null, it).execute().emotes }

        for (emoteData in emotes) {
            val emote = Twitch(emoteData.name, emoteData.id)
            if (emoteData.emoteSetId == "0") {
                twitchEmotes.add(emote)
            } else {
                emote.isSubscriberEmote = true
                subscriberEmotes.add(emote)
            }
        }
        twitchEmotes.sort()

        Timber.tag("Chat").d("Found twitch emotes: %s", twitchEmotes.size)
        Execute.ui { delegate.onEmotesLoaded(twitchEmotes, subscriberEmotes) }
    }

    fun interface Delegate {
        fun onEmotesLoaded(emotes: MutableList<Emote>?, subscriberEmotes: MutableList<Emote>?)
    }
}
