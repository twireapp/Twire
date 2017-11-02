package com.sebastianrask.bettersubscription.tasks;

import android.os.AsyncTask;

import com.sebastianrask.bettersubscription.model.Panel;
import com.sebastianrask.bettersubscription.service.Service;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Sebastian Rask on 24-02-2017.
 */

public class GetPanelsTask extends AsyncTask<Void, Void, List<Panel>> {
	private final String DISPLAY_ORDER_INT_KEY = "display_order";
	private final String USER_ID_INT_KEY = "user_id";
	private final String HTML_DESCRIPTION_STRING_KEY = "html_description";
	private final String DATA_OBJECT_KEY = "data";
		private final String LINK_STRING_KEY = "link";
		private final String IMAGE_URL_STRING_KEY = "image";
		private final String DESCRIPTION_STRING_KEY = "description";
		private final String TITLE_STRING_KEY = "title";

	private String mStreamerName;
	private Delegate mDelegate;

	public GetPanelsTask(String mStreamerName, Delegate mDelegate) {
		this.mStreamerName = mStreamerName;
		this.mDelegate = mDelegate;
	}

	@Override
	protected List<Panel> doInBackground(Void... voids) {
		List<Panel> result = new ArrayList<>();

		try {
			String jsonString = Service.urlToJSONString("https://api.twitch.tv/api/channels/" + mStreamerName + "/panels");
			JSONArray json = new JSONArray(jsonString);
			for (int i = 0; i < json.length(); i++) {
				JSONObject panelObject = json.getJSONObject(i);
				JSONObject dataObject = panelObject.getJSONObject(DATA_OBJECT_KEY);

				int order = panelObject.getInt(DISPLAY_ORDER_INT_KEY);
				int userId = panelObject.getInt(USER_ID_INT_KEY);
				String html = panelObject.has(HTML_DESCRIPTION_STRING_KEY) ? panelObject.getString(HTML_DESCRIPTION_STRING_KEY) : "";
				String link = dataObject.has(LINK_STRING_KEY) ? dataObject.getString(LINK_STRING_KEY) : null;
				String imageUrl = dataObject.getString(IMAGE_URL_STRING_KEY);
				String description = dataObject.has(DESCRIPTION_STRING_KEY) ? dataObject.getString(DESCRIPTION_STRING_KEY) : "";
				String title = dataObject.has(TITLE_STRING_KEY) ? dataObject.getString(TITLE_STRING_KEY) : "";

				Panel panel = new Panel(mStreamerName, userId, order, description, imageUrl, link, title, html);
				result.add(panel);
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}

		return result;
	}

	@Override
	protected void onPostExecute(List<Panel> s) {
		super.onPostExecute(s);
		mDelegate.onPanelsFetched(s);
	}

	public interface Delegate {
		void onPanelsFetched(List<Panel> result);
	}
}
