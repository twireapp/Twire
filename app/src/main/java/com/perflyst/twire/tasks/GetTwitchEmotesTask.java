package com.perflyst.twire.tasks;

import com.perflyst.twire.TwireApplication;
import com.perflyst.twire.model.Emote;
import com.perflyst.twire.utils.Execute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import timber.log.Timber;

/**
 * Created by idealMJ on 29/07/16.
 */
public class GetTwitchEmotesTask implements Runnable {
    private final Delegate delegate;
    private final String[] emoteSets;
    private final List<Emote> twitchEmotes = new ArrayList<>();
    private final List<Emote> subscriberEmotes = new ArrayList<>();

    public GetTwitchEmotesTask(String[] emoteSets, Delegate delegate) {
        this.emoteSets = emoteSets;
        this.delegate = delegate;
    }

    public void run() {
        var emotes = TwireApplication.helix.getEmoteSets(null, List.of(emoteSets)).execute().getEmotes();

        for (var emoteData : emotes) {
            Emote emote = Emote.Twitch(emoteData.getName(), emoteData.getId());
            if (Objects.equals(emoteData.getEmoteSetId(), "0")) {
                twitchEmotes.add(emote);
            } else {
                emote.setSubscriberEmote(true);
                subscriberEmotes.add(emote);
            }
        }
        Collections.sort(twitchEmotes);

        Timber.tag("Chat").d("Found twitch emotes: %s", twitchEmotes.size());
        Execute.ui(() -> delegate.onEmotesLoaded(twitchEmotes, subscriberEmotes));
    }

    public interface Delegate {
        void onEmotesLoaded(List<Emote> emotes, List<Emote> subscriberEmotes);
    }
}
