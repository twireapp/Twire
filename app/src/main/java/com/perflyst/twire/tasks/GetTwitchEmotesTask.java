package com.perflyst.twire.tasks;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.perflyst.twire.model.Emote;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.utils.Execute;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by idealMJ on 29/07/16.
 */
public class GetTwitchEmotesTask implements Runnable {
    private final String LOG_TAG = getClass().getSimpleName();

    private final WeakReference<Context> context;
    private final Delegate delegate;
    private final String[] emoteSets;
    private final List<Emote> twitchEmotes = new ArrayList<>();
    private final List<Emote> subscriberEmotes = new ArrayList<>();

    public GetTwitchEmotesTask(String[] emoteSets, Delegate delegate, Context context) {
        this.emoteSets = emoteSets;
        this.delegate = delegate;
        this.context = new WeakReference<>(context);
    }

    public void run() {
        try {
            String newUrl = "https://api.twitch.tv/helix/chat/emotes/set?emote_set_id=" + TextUtils.join("&emote_set_id=", emoteSets);
            JSONObject top = new JSONObject(Service.urlToJSONStringHelix(newUrl, context.get()));
            JSONArray emotes = top.getJSONArray("data");

            for (int i = 0; i < emotes.length(); i++) {
                JSONObject emoteObject = emotes.getJSONObject(i);
                Emote emote = Emote.Twitch(emoteObject.getString("name"), emoteObject.getString("id"));
                if (emoteObject.getString("emote_set_id").equals("0")) {
                    twitchEmotes.add(emote);
                } else {
                    emote.setSubscriberEmote(true);
                    subscriberEmotes.add(emote);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Collections.sort(twitchEmotes);

        Log.d("Chat", "Found twitch emotes: " + twitchEmotes.size());
        Execute.ui(() -> delegate.onEmotesLoaded(twitchEmotes, subscriberEmotes));
    }

    public interface Delegate {
        void onEmotesLoaded(List<Emote> emotes, List<Emote> subscriberEmotes);
    }
}
