package com.sebastianrask.bettersubscription.tasks;

import android.util.Log;

import com.sebastianrask.bettersubscription.service.Service;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by Sebastian Rask on 18-06-2016.
 */
public class GetVODStreamURL extends GetLiveStreamURL {
	private String LOG_TAG = getClass().getSimpleName();

	public GetVODStreamURL(AsyncResponse aCallback) {
		super(aCallback);
	}

	@Override
	protected HashMap<String, String> doInBackground(String... params) {
		String vodId = params[0];
		String sig = "";
		String token = "";

		try {
			String url = "http://api.twitch.tv/api/vods/" + vodId + "/access_token";
			Log.d(LOG_TAG, url);
			JSONObject topobject = new JSONObject(Service.urlToJSONString(url));
			token = topobject.getString("token").replaceAll("\\\\", ""); // Remove all backslashes from the returned string. We need the string to make a jsonobject
			sig = topobject.getString("sig");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		String vodURL = String.format("http://usher.twitch.tv/vod/%s?nauthsig=%s&nauth=%s", vodId, sig, token);
		Log.d(LOG_TAG, "HSL Playlist URL: " + vodURL);
		return parseM3U8(vodURL);
	}
}
