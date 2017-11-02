package com.sebastianrask.bettersubscription.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.sebastianrask.bettersubscription.model.Emote;
import com.sebastianrask.bettersubscription.service.Service;
import com.sebastianrask.bettersubscription.service.Settings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created by idealMJ on 29/07/16.
 */
public class GetTwitchEmotesTask extends AsyncTask<Void, Void, Void> {
	private final String LOG_TAG = getClass().getSimpleName();
	private final String url = "https://twitchemotes.com/api_cache/v3/global.json";

	private final String SETS_KEY = "emoticon_sets";
		private final String ID_KEY_INT = "id";
		private final String WORD_KEY_STRING = "code";
	private Context context;
	private Delegate delegate;
	private List<Emote> twitchEmotes = new ArrayList<>();
	private List<Emote> subscriberEmotes = new ArrayList<>();

	public GetTwitchEmotesTask(Delegate delegate, Context context) {
		this.delegate = delegate;
		this.context = context;
	}

	@Override
	protected Void doInBackground(Void... voids) {
		try {
			Settings settings = new Settings(context);
			String newUrl = "https://api.twitch.tv/kraken/users/" + settings.getGeneralTwitchUserID() + "/emotes?oauth_token=" + settings.getGeneralTwitchAccessToken();
			JSONObject top = new JSONObject(Service.urlToJSONString(newUrl));
			JSONObject sets = top.getJSONObject(SETS_KEY);
			Iterator<?> setKeys = sets.keys();

			while (setKeys.hasNext()) {
				String key = (String) setKeys.next();
				if (!key.equals("0") && sets.get(key) instanceof JSONArray) {
					JSONArray set = sets.getJSONArray(key);

					for (int i = 0; i < set.length(); i++) {
						JSONObject emoteObject = set.getJSONObject(i);

						String id = emoteObject.getInt(ID_KEY_INT) + "";
						String word = emoteObject.getString(WORD_KEY_STRING);
						Emote emote = new Emote(id, word, false);
						emote.setSubscriberEmote(true);
						subscriberEmotes.add(emote);
					}
				}
			}
			Log.d(LOG_TAG, newUrl);


			JSONObject emotesObject = new JSONObject(Service.urlToJSONString(url));
			Iterator<?> keys = emotesObject.keys();

			while( keys.hasNext() ) {
				String key = (String)keys.next();
				if (emotesObject.get(key) instanceof JSONObject ) {
					JSONObject emoteObject = emotesObject.getJSONObject(key);

					String emoteId = "" + emoteObject.getInt("id");
					twitchEmotes.add(new Emote(emoteId, key, false));
				}
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
