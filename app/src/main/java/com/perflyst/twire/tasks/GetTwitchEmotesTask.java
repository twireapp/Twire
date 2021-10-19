package com.perflyst.twire.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import androidx.core.text.HtmlCompat;

import com.perflyst.twire.model.Emote;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.service.Settings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created by idealMJ on 29/07/16.
 */
public class GetTwitchEmotesTask extends AsyncTask<Void, Void, Void> {
    private final String LOG_TAG = getClass().getSimpleName();

    private final WeakReference<Context> context;
    private final Delegate delegate;
    private final List<Emote> twitchEmotes = new ArrayList<>();
    private final List<Emote> subscriberEmotes = new ArrayList<>();

    public GetTwitchEmotesTask(Delegate delegate, Context context) {
        this.delegate = delegate;
        this.context = new WeakReference<>(context);
    }

    @Override
    protected Void doInBackground(Void... voids) {
        Settings settings = new Settings(context.get());


        /* Well so this has been broken at the moment because there is no replacement API. Please See: https://twitch.uservoice.com/forums/310213-developers/suggestions/43599900-get-the-authenticated-user-s-emotes-equivalent-to
        try {
            String newUrl = "ADD API LINK HERE" + settings.getGeneralTwitchUserID();
            JSONObject top = new JSONObject(Service.urlToJSONStringHelix(newUrl, settings.getGeneralTwitchAccessToken()));
            String SETS_KEY = "data";
            JSONObject sets = top.getJSONObject(SETS_KEY);
            Iterator<?> setKeys = sets.keys();

            while (setKeys.hasNext()) {
                String key = (String) setKeys.next();
                if (!key.equals("0") && sets.get(key) instanceof JSONArray) {
                    JSONArray set = sets.getJSONArray(key);
                    for (int i = 0; i < set.length(); i++) {
                        JSONObject emoteObject = set.getJSONObject(i);

                        String ID_KEY_INT = "id";
                        String id = emoteObject.getInt(ID_KEY_INT) + "";
                        String WORD_KEY_STRING = "name";
                        String word = emoteObject.getString(WORD_KEY_STRING);
                        Emote emote = Emote.Twitch(word, id);
                        emote.setSubscriberEmote(true);
                        subscriberEmotes.add(emote);
                    }
                }
            }
            Log.d(LOG_TAG, newUrl);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        */


        try {
            String url = "https://api.twitch.tv/helix/chat/emotes/global";
            JSONArray emotesArray = new JSONObject(Service.urlToJSONStringHelix(url, settings.getGeneralTwitchAccessToken())).getJSONArray("data");
            for (int i = 0; i < emotesArray.length(); i++) {
                JSONObject emoteObject = emotesArray.getJSONObject(i);
                String code = emoteObject.getString("name");

                // code is a escaped regex, so we need to convert it to any valid match for that regex
                code = HtmlCompat.fromHtml(code.replaceAll("\\\\", ""), HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
                        .replaceAll("(.)\\?", "")
                        .replaceAll("\\[(.).*?]", "$1")
                        .replaceAll("\\((.+)\\|.+\\)", "$1");

                // this is a string now: example: emotesv2_89f3f0761c7b4f708061e9e4be3b7d17
                String emoteId = emoteObject.getString("id");
                twitchEmotes.add(Emote.Twitch(code, emoteId));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Collections.sort(twitchEmotes);
        return null;
    }

    @Override
    protected void onPostExecute(Void re) {
        super.onPostExecute(re);
        Log.d("Chat", "Found twitch emotes: " + twitchEmotes.size());
        delegate.onEmotesLoaded(twitchEmotes, subscriberEmotes);
    }

    public interface Delegate {
        void onEmotesLoaded(List<Emote> emotes, List<Emote> subscriberEmotes);
    }
}
