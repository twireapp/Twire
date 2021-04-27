package com.perflyst.twire.tasks;

import android.os.AsyncTask;

import com.perflyst.twire.model.Panel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Sebastian Rask on 24-02-2017.
 */

public class GetPanelsTask extends AsyncTask<Void, Void, List<Panel>> {

    private final String mStreamerName;
    private final Delegate mDelegate;

    public GetPanelsTask(String mStreamerName, Delegate mDelegate) {
        this.mStreamerName = mStreamerName;
        this.mDelegate = mDelegate;
    }

    @Override
    protected List<Panel> doInBackground(Void... voids) {
        List<Panel> result = new ArrayList<>();

        // This code is commented out because the /panels API has been removed.
        // If Twitch adds another API that allows us to get the panels, it should replace the code below.
        /*
        try {
            String jsonString = Service.urlToJSONString("https://api.twitch.tv/api/channels/" + mStreamerName + "/panels");
            JSONArray json = new JSONArray(jsonString);
            for (int i = 0; i < json.length(); i++) {
                JSONObject panelObject = json.getJSONObject(i);
                String DATA_OBJECT_KEY = "data";
                JSONObject dataObject = panelObject.getJSONObject(DATA_OBJECT_KEY);

                if (dataObject.length() == 0)
                    continue;

                String DISPLAY_ORDER_INT_KEY = "display_order";
                int order = panelObject.getInt(DISPLAY_ORDER_INT_KEY);
                String USER_ID_INT_KEY = "user_id";
                int userId = panelObject.getInt(USER_ID_INT_KEY);
                String HTML_DESCRIPTION_STRING_KEY = "html_description";
                String html = panelObject.has(HTML_DESCRIPTION_STRING_KEY) ? panelObject.getString(HTML_DESCRIPTION_STRING_KEY) : "";
                String LINK_STRING_KEY = "link";
                String link = dataObject.has(LINK_STRING_KEY) ? dataObject.getString(LINK_STRING_KEY) : null;
                String IMAGE_URL_STRING_KEY = "image";
                String imageUrl = dataObject.has(IMAGE_URL_STRING_KEY) ? dataObject.getString(IMAGE_URL_STRING_KEY) : null;
                String DESCRIPTION_STRING_KEY = "description";
                String description = dataObject.has(DESCRIPTION_STRING_KEY) ? dataObject.getString(DESCRIPTION_STRING_KEY) : "";
                String TITLE_STRING_KEY = "title";
                String title = dataObject.has(TITLE_STRING_KEY) ? dataObject.getString(TITLE_STRING_KEY) : "";

                Panel panel = new Panel(mStreamerName, userId, order, description, imageUrl, link, title, html);
                result.add(panel);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        */

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
